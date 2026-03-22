package com.commerce_pro_backend.finance.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.entity.*;
import com.commerce_pro_backend.finance.enums.InvoiceStatus;
import com.commerce_pro_backend.finance.mapper.FinanceMapper;
import com.commerce_pro_backend.finance.repository.*;
import com.commerce_pro_backend.finance.specification.CustomerInvoiceSpecification;
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
public class CustomerInvoiceService {

    private final CustomerInvoiceRepository invoiceRepository;
    private final TaxRateRepository         taxRateRepository;
    private final FinancialPeriodService    periodService;
    private final InvoiceNumberService      invoiceNumberService;
    private final JournalEntryService       journalEntryService;
    private final FinanceMapper             mapper;
    private final AuditService             auditService;
    private final CurrentUserService       currentUserService;

    // ── Read ──────────────────────────────────────────────────────────────────

    public PageResponse<CustomerInvoiceSummaryDTO> listInvoices(
            String search, InvoiceStatus status, String customerId,
            String periodId, LocalDate from, LocalDate to, Boolean overdue, Pageable pageable) {

        Specification<CustomerInvoice> spec = CustomerInvoiceSpecification
                .withFilters(search, status, customerId, periodId, from, to, overdue);
        Page<CustomerInvoice> page = invoiceRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream().map(mapper::toCustomerInvoiceSummaryDTO).toList());
    }

    public CustomerInvoiceDTO getById(String id) {
        return mapper.toCustomerInvoiceDTO(findOrThrow(id));
    }

    public List<CustomerInvoiceSummaryDTO> getByOrderId(String orderId) {
        return mapper.toCustomerInvoiceSummaryList(invoiceRepository.findByOrderId(orderId));
    }

    public PageResponse<CustomerInvoiceSummaryDTO> getByCustomer(String customerId, Pageable pageable) {
        Page<CustomerInvoice> page = invoiceRepository.findByCustomerId(customerId, pageable);
        return PageResponse.from(page, page.getContent().stream().map(mapper::toCustomerInvoiceSummaryDTO).toList());
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public CustomerInvoiceDTO createInvoice(CreateCustomerInvoiceRequest req) {
        String actor  = currentUserService.getCurrentUserId();
        FinancialPeriod period = periodService.resolvePeriodForDate(LocalDate.now());

        CustomerInvoice invoice = CustomerInvoice.builder()
                .invoiceNumber(invoiceNumberService.generateInvoiceNumber())
                .orderId(req.getOrderId()).orderNumber(null)
                .customerId(req.getCustomerId())
                .customerName("Customer " + req.getCustomerId()) // enriched in real impl
                .customerEmail("")
                .period(period)
                .status(InvoiceStatus.DRAFT)
                .dueDate(req.getDueDate())
                .paymentTerms(req.getPaymentTerms())
                .currency(req.getCurrency() != null ? req.getCurrency() : "USD")
                .taxJurisdictionId(req.getTaxJurisdictionId())
                .notes(req.getNotes())
                .createdBy(actor).updatedBy(actor)
                .build();

        // Build line items
        List<CustomerInvoiceItem> items = new ArrayList<>();
        int sortOrder = 0;
        for (InvoiceItemRequest itemReq : req.getItems()) {
            BigDecimal taxRate = BigDecimal.ZERO;
            if (itemReq.getTaxRateId() != null) {
                taxRate = taxRateRepository.findById(itemReq.getTaxRateId())
                        .map(TaxRate::getRate).orElse(BigDecimal.ZERO);
            }
            CustomerInvoiceItem item = CustomerInvoiceItem.builder()
                    .invoice(invoice)
                    .orderItemId(itemReq.getOrderItemId())
                    .productId(itemReq.getProductId())
                    .description(itemReq.getDescription())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .discountAmount(itemReq.getDiscountAmount() != null ? itemReq.getDiscountAmount() : BigDecimal.ZERO)
                    .taxRate(taxRate)
                    .taxRateId(itemReq.getTaxRateId())
                    .sortOrder(sortOrder)
                    .build();
            items.add(item);
            sortOrder++;
        }
        invoice.setItems(items);
        invoice.recalculateTotals();
        invoice = invoiceRepository.save(invoice);

        auditService.log(actor, AuditAction.FINANCE_INVOICE_CREATED, "CustomerInvoice", invoice.getId(),
                invoice.getInvoiceNumber(), null, null, null, true);
        log.info("Customer invoice created: {}", invoice.getInvoiceNumber());
        return mapper.toCustomerInvoiceDTO(invoice);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Transactional
    public CustomerInvoiceDTO sendInvoice(String id) {
        CustomerInvoice invoice = findOrThrow(id);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw ApiException.badRequest("Only DRAFT invoices can be sent. Current status: " + invoice.getStatus());
        }
        String actor = currentUserService.getCurrentUserId();
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setUpdatedBy(actor);
        invoice = invoiceRepository.save(invoice);

        // Auto-post journal entry: DR Accounts Receivable / CR Revenue
        journalEntryService.postInvoiceSent(invoice);

        auditService.log(actor, AuditAction.FINANCE_INVOICE_SENT, "CustomerInvoice", invoice.getId(),
                invoice.getInvoiceNumber(), null, "DRAFT", "SENT", true);
        return mapper.toCustomerInvoiceDTO(invoice);
    }

    @Transactional
    public CustomerInvoiceDTO recordPayment(String id, RecordPaymentRequest req) {
        CustomerInvoice invoice = findOrThrow(id);
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.VOID
                || invoice.getStatus() == InvoiceStatus.WRITTEN_OFF) {
            throw ApiException.badRequest("Invoice " + invoice.getInvoiceNumber() + " cannot receive payments — status: " + invoice.getStatus());
        }
        String actor = currentUserService.getCurrentUserId();
        BigDecimal newPaid = invoice.getPaidAmount().add(req.getAmount());
        if (newPaid.compareTo(invoice.getTotalAmount()) > 0) {
            throw ApiException.badRequest("Payment amount exceeds balance due (" + invoice.getBalanceDue() + ")");
        }
        invoice.setPaidAmount(newPaid);
        invoice.setBalanceDue(invoice.getTotalAmount().subtract(newPaid).max(BigDecimal.ZERO));

        String prevStatus = invoice.getStatus().name();
        if (invoice.getBalanceDue().compareTo(BigDecimal.ZERO) == 0) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(LocalDateTime.now());
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }
        invoice.setUpdatedBy(actor);
        invoice = invoiceRepository.save(invoice);

        // Auto-post journal entry: DR Cash / CR Accounts Receivable
        journalEntryService.postPaymentReceived(invoice, req.getAmount());

        auditService.log(actor, AuditAction.FINANCE_INVOICE_PAID, "CustomerInvoice", invoice.getId(),
                invoice.getInvoiceNumber(), null, prevStatus, invoice.getStatus().name(), true);
        return mapper.toCustomerInvoiceDTO(invoice);
    }

    @Transactional
    public CustomerInvoiceDTO voidInvoice(String id, VoidInvoiceRequest req) {
        CustomerInvoice invoice = findOrThrow(id);
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.VOID) {
            throw ApiException.badRequest("Cannot void invoice with status: " + invoice.getStatus());
        }
        String actor = currentUserService.getCurrentUserId();
        String prev  = invoice.getStatus().name();
        invoice.setStatus(InvoiceStatus.VOID);
        invoice.setVoidedAt(LocalDateTime.now());
        invoice.setVoidReason(req.getReason());
        invoice.setUpdatedBy(actor);
        invoice = invoiceRepository.save(invoice);
        auditService.log(actor, AuditAction.FINANCE_INVOICE_VOIDED, "CustomerInvoice", invoice.getId(),
                invoice.getInvoiceNumber(), null, prev, "VOID", true);
        return mapper.toCustomerInvoiceDTO(invoice);
    }

    @Transactional
    public CustomerInvoiceDTO writeOff(String id, String reason) {
        CustomerInvoice invoice = findOrThrow(id);
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.VOID
                || invoice.getStatus() == InvoiceStatus.WRITTEN_OFF) {
            throw ApiException.badRequest("Cannot write off invoice with status: " + invoice.getStatus());
        }
        String actor = currentUserService.getCurrentUserId();
        invoice.setStatus(InvoiceStatus.WRITTEN_OFF);
        invoice.setBalanceDue(BigDecimal.ZERO);
        invoice.setUpdatedBy(actor);
        invoice = invoiceRepository.save(invoice);
        auditService.log(actor, AuditAction.FINANCE_INVOICE_WRITTEN_OFF, "CustomerInvoice", invoice.getId(),
                invoice.getInvoiceNumber(), null, null, "WRITTEN_OFF", true);
        return mapper.toCustomerInvoiceDTO(invoice);
    }

    // ── AR Summary / Aging ────────────────────────────────────────────────────

    public ARSummaryDTO getARSummary() {
        LocalDate today    = LocalDate.now();
        LocalDate minus30  = today.minusDays(30);
        LocalDate minus60  = today.minusDays(60);
        LocalDate minus90  = today.minusDays(90);

        BigDecimal total    = invoiceRepository.sumTotalOutstanding();
        BigDecimal overdue  = invoiceRepository.sumOverdueAmount(today);
        BigDecimal current  = invoiceRepository.sumCurrentBucket(today);
        BigDecimal bucket1  = invoiceRepository.sumAgingBucket(today, minus30);
        BigDecimal bucket2  = invoiceRepository.sumAgingBucket(minus30, minus60);
        BigDecimal bucket3  = invoiceRepository.sumAgingBucket(minus60, minus90);
        BigDecimal bucket4  = total.subtract(current).subtract(bucket1).subtract(bucket2).subtract(bucket3).max(BigDecimal.ZERO);

        List<CustomerInvoice> overdueList = invoiceRepository.findOverdueInvoices(today);

        return ARSummaryDTO.builder()
                .totalOutstanding(total).current(current)
                .days1To30(bucket1).days31To60(bucket2).days61To90(bucket3).days90Plus(bucket4)
                .overdueInvoiceCount(overdueList.size()).overdueAmount(overdue)
                .dsoDays(BigDecimal.ZERO) // computed in AccountsReceivableService
                .overdueInvoices(mapper.toCustomerInvoiceSummaryList(overdueList))
                .build();
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    CustomerInvoice findOrThrow(String id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("CustomerInvoice", id));
    }
}
