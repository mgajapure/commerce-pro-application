package com.commerce_pro_backend.finance.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.entity.*;
import com.commerce_pro_backend.finance.enums.VendorInvoiceStatus;
import com.commerce_pro_backend.finance.mapper.FinanceMapper;
import com.commerce_pro_backend.finance.repository.VendorInvoiceRepository;
import com.commerce_pro_backend.finance.specification.VendorInvoiceSpecification;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.service.AuditService;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorInvoiceService {

    private final VendorInvoiceRepository vendorInvoiceRepository;
    private final VendorService           vendorService;
    private final FinancialPeriodService  periodService;
    private final InvoiceNumberService    invoiceNumberService;
    private final JournalEntryService     journalEntryService;
    private final FinanceMapper           mapper;
    private final AuditService            auditService;
    private final CurrentUserService      currentUserService;

    public PageResponse<VendorInvoiceSummaryDTO> listInvoices(
            String search, VendorInvoiceStatus status, String vendorId,
            String periodId, LocalDate from, LocalDate to, Pageable pageable) {
        Specification<VendorInvoice> spec = VendorInvoiceSpecification.withFilters(search, status, vendorId, periodId, from, to);
        Page<VendorInvoice> page = vendorInvoiceRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream().map(mapper::toVendorInvoiceSummaryDTO).toList());
    }

    public VendorInvoiceDTO getById(String id) {
        return mapper.toVendorInvoiceDTO(findOrThrow(id));
    }

    @Transactional
    public VendorInvoiceDTO createInvoice(CreateVendorInvoiceRequest req) {
        String actor  = currentUserService.getCurrentUserId();
        Vendor vendor = vendorService.findOrThrow(req.getVendorId());
        FinancialPeriod period = periodService.resolvePeriodForDate(LocalDate.now());

        VendorInvoice inv = VendorInvoice.builder()
                .vendorInvoiceNumber(invoiceNumberService.generateVendorInvoiceNumber())
                .vendorReference(req.getVendorReference()).vendor(vendor).period(period)
                .status(VendorInvoiceStatus.RECEIVED).receivedAt(LocalDateTime.now())
                .dueDate(req.getDueDate()).description(req.getDescription())
                .currency(req.getCurrency() != null ? req.getCurrency() : vendor.getPreferredCurrency())
                .earlyPaymentDiscountPct(req.getEarlyPaymentDiscountPct())
                .earlyPaymentDiscountDays(req.getEarlyPaymentDiscountDays())
                .notes(req.getNotes()).createdBy(actor).updatedBy(actor)
                .build();

        List<VendorInvoiceItem> items = new ArrayList<>();
        int sort = 0;
        for (VendorInvoiceItemRequest ir : req.getItems()) {
            VendorInvoiceItem item = VendorInvoiceItem.builder()
                    .vendorInvoice(inv).description(ir.getDescription())
                    .quantity(ir.getQuantity()).unitPrice(ir.getUnitPrice())
                    .taxRate(ir.getTaxRate() != null ? ir.getTaxRate() : BigDecimal.ZERO)
                    .expenseCategoryId(ir.getExpenseCategoryId()).sortOrder(sort++)
                    .build();
            items.add(item);
        }
        inv.setItems(items);
        inv.recalculateTotals();
        inv = vendorInvoiceRepository.save(inv);
        auditService.log(actor, AuditAction.FINANCE_VENDOR_INVOICE_CREATED, "VendorInvoice", inv.getId(),
                inv.getVendorInvoiceNumber(), null, null, null, true);
        return mapper.toVendorInvoiceDTO(inv);
    }

    @Transactional
    public VendorInvoiceDTO approve(String id) {
        VendorInvoice inv = findOrThrow(id);
        if (inv.getStatus() != VendorInvoiceStatus.RECEIVED && inv.getStatus() != VendorInvoiceStatus.UNDER_REVIEW) {
            throw ApiException.badRequest("Cannot approve invoice with status: " + inv.getStatus());
        }
        String actor = currentUserService.getCurrentUserId();
        inv.setStatus(VendorInvoiceStatus.APPROVED);
        inv.setApprovedAt(LocalDateTime.now());
        inv.setApprovedBy(actor);
        inv.setUpdatedBy(actor);
        inv = vendorInvoiceRepository.save(inv);
        journalEntryService.postVendorInvoiceApproved(inv);
        auditService.log(actor, AuditAction.FINANCE_VENDOR_INVOICE_APPROVED, "VendorInvoice", inv.getId(),
                inv.getVendorInvoiceNumber(), null, "RECEIVED", "APPROVED", true);
        return mapper.toVendorInvoiceDTO(inv);
    }

    @Transactional
    public VendorInvoiceDTO reject(String id, String reason) {
        VendorInvoice inv = findOrThrow(id);
        if (inv.getStatus() == VendorInvoiceStatus.PAID || inv.getStatus() == VendorInvoiceStatus.VOID) {
            throw ApiException.badRequest("Cannot reject invoice with status: " + inv.getStatus());
        }
        String actor = currentUserService.getCurrentUserId();
        inv.setStatus(VendorInvoiceStatus.REJECTED);
        inv.setRejectionReason(reason);
        inv.setUpdatedBy(actor);
        inv = vendorInvoiceRepository.save(inv);
        auditService.log(actor, AuditAction.FINANCE_VENDOR_INVOICE_REJECTED, "VendorInvoice", inv.getId(),
                inv.getVendorInvoiceNumber(), null, null, "REJECTED", true);
        return mapper.toVendorInvoiceDTO(inv);
    }

    @Transactional
    public VendorInvoiceDTO schedulePayment(String id, SchedulePaymentRequest req) {
        VendorInvoice inv = findOrThrow(id);
        if (inv.getStatus() != VendorInvoiceStatus.APPROVED) {
            throw ApiException.badRequest("Only APPROVED invoices can be scheduled. Status: " + inv.getStatus());
        }
        String actor = currentUserService.getCurrentUserId();
        inv.setStatus(VendorInvoiceStatus.SCHEDULED);
        inv.setScheduledPaymentDate(req.getScheduledPaymentDate());
        if (req.getNotes() != null) inv.setNotes(req.getNotes());
        inv.setUpdatedBy(actor);
        inv = vendorInvoiceRepository.save(inv);
        return mapper.toVendorInvoiceDTO(inv);
    }

    @Transactional
    public VendorInvoiceDTO recordPayment(String id, RecordPaymentRequest req) {
        VendorInvoice inv = findOrThrow(id);
        if (inv.getStatus() == VendorInvoiceStatus.PAID || inv.getStatus() == VendorInvoiceStatus.VOID) {
            throw ApiException.badRequest("Invoice already paid or voided");
        }
        String actor    = currentUserService.getCurrentUserId();
        BigDecimal paid = inv.getPaidAmount().add(req.getAmount());
        if (paid.compareTo(inv.getTotalAmount()) > 0) {
            throw ApiException.badRequest("Payment exceeds balance due (" + inv.getBalanceDue() + ")");
        }
        inv.setPaidAmount(paid);
        inv.setBalanceDue(inv.getTotalAmount().subtract(paid).max(BigDecimal.ZERO));
        String prev = inv.getStatus().name();
        if (inv.getBalanceDue().compareTo(BigDecimal.ZERO) == 0) {
            inv.setStatus(VendorInvoiceStatus.PAID);
            inv.setPaidAt(LocalDateTime.now());
        } else {
            inv.setStatus(VendorInvoiceStatus.PARTIALLY_PAID);
        }
        inv.setUpdatedBy(actor);
        inv = vendorInvoiceRepository.save(inv);
        journalEntryService.postVendorPayment(inv, req.getAmount());
        auditService.log(actor, AuditAction.FINANCE_VENDOR_INVOICE_PAID, "VendorInvoice", inv.getId(),
                inv.getVendorInvoiceNumber(), null, prev, inv.getStatus().name(), true);
        return mapper.toVendorInvoiceDTO(inv);
    }

    @Transactional
    public VendorInvoiceDTO markDisputed(String id, String notes) {
        VendorInvoice inv = findOrThrow(id);
        String actor = currentUserService.getCurrentUserId();
        inv.setStatus(VendorInvoiceStatus.DISPUTED);
        if (notes != null) inv.setNotes(notes);
        inv.setUpdatedBy(actor);
        return mapper.toVendorInvoiceDTO(vendorInvoiceRepository.save(inv));
    }

    public APSummaryDTO getAPSummary() {
        LocalDate today   = LocalDate.now();
        LocalDate minus30 = today.minusDays(30);
        LocalDate minus60 = today.minusDays(60);
        LocalDate minus90 = today.minusDays(90);

        BigDecimal total   = vendorInvoiceRepository.sumTotalOutstanding();
        BigDecimal overdue = vendorInvoiceRepository.sumOverdueAmount(today);
        BigDecimal current = vendorInvoiceRepository.sumCurrentBucket(today);
        BigDecimal b1      = vendorInvoiceRepository.sumAgingBucket(today, minus30);
        BigDecimal b2      = vendorInvoiceRepository.sumAgingBucket(minus30, minus60);
        BigDecimal b3      = vendorInvoiceRepository.sumAgingBucket(minus60, minus90);
        BigDecimal b4      = total.subtract(current).subtract(b1).subtract(b2).subtract(b3).max(BigDecimal.ZERO);

        List<VendorInvoice> upcoming = vendorInvoiceRepository.findDueSoon(today, today.plusDays(14));
        List<VendorInvoice> overdueList = vendorInvoiceRepository.findOverdue(today);

        return APSummaryDTO.builder()
                .totalOutstanding(total).current(current)
                .days1To30(b1).days31To60(b2).days61To90(b3).days90Plus(b4)
                .overdueInvoiceCount(overdueList.size()).overdueAmount(overdue)
                .dpoDays(BigDecimal.ZERO)
                .upcomingPayments(mapper.toVendorInvoiceSummaryList(upcoming))
                .overdueInvoices(mapper.toVendorInvoiceSummaryList(overdueList))
                .build();
    }

    public List<VendorInvoiceSummaryDTO> getDueSoon(int days) {
        LocalDate to = LocalDate.now().plusDays(days);
        return mapper.toVendorInvoiceSummaryList(
                vendorInvoiceRepository.findDueSoon(LocalDate.now(), to));
    }

    VendorInvoice findOrThrow(String id) {
        return vendorInvoiceRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("VendorInvoice", id));
    }
}
