package com.commerce_pro_backend.finance.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.entity.Vendor;
import com.commerce_pro_backend.finance.enums.VendorStatus;
import com.commerce_pro_backend.finance.mapper.FinanceMapper;
import com.commerce_pro_backend.finance.repository.VendorRepository;
import com.commerce_pro_backend.finance.specification.VendorSpecification;
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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorService {

    private final VendorRepository      vendorRepository;
    private final FinanceMapper         mapper;
    private final InvoiceNumberService  invoiceNumberService;
    private final AuditService          auditService;
    private final CurrentUserService    currentUserService;

    // ── Read ──────────────────────────────────────────────────────────────────

    public PageResponse<VendorSummaryDTO> listVendors(String search, VendorStatus status, String category, Pageable pageable) {
        Specification<Vendor> spec = VendorSpecification.withFilters(search, status, category);
        Page<Vendor> page = vendorRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream().map(mapper::toVendorSummaryDTO).toList());
    }

    public VendorDTO getById(String id) {
        return mapper.toVendorDTO(findOrThrow(id));
    }

    public VendorDTO getByCode(String code) {
        Vendor vendor = vendorRepository.findByVendorCode(code)
                .orElseThrow(() -> ApiException.notFound("Vendor", code));
        return mapper.toVendorDTO(vendor);
    }

    public List<VendorSummaryDTO> getActiveVendors() {
        return mapper.toVendorSummaryList(vendorRepository.findByStatus(VendorStatus.ACTIVE));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public VendorDTO createVendor(CreateVendorRequest req) {
        if (vendorRepository.existsByEmail(req.getEmail())) {
            throw ApiException.conflict("A vendor with email '" + req.getEmail() + "' already exists");
        }
        String actor = currentUserService.getCurrentUserId();
        Vendor vendor = Vendor.builder()
                .vendorCode(invoiceNumberService.generateVendorCode())
                .name(req.getName()).companyName(req.getCompanyName())
                .email(req.getEmail()).phone(req.getPhone()).taxId(req.getTaxId())
                .status(VendorStatus.ACTIVE)
                .paymentTerms(req.getPaymentTerms()).paymentTermsDays(req.getPaymentTermsDays())
                .preferredCurrency(req.getPreferredCurrency() != null ? req.getPreferredCurrency() : "USD")
                .addressLine1(req.getAddressLine1()).addressLine2(req.getAddressLine2())
                .city(req.getCity()).state(req.getState())
                .postalCode(req.getPostalCode()).country(req.getCountry())
                .category(req.getCategory()).notes(req.getNotes()).tags(req.getTags())
                .createdBy(actor).updatedBy(actor)
                .build();
        vendor = vendorRepository.save(vendor);
        auditService.log(actor, AuditAction.FINANCE_VENDOR_CREATED, "Vendor", vendor.getId(),
                vendor.getVendorCode() + " - " + vendor.getName(), null, null, null, true);
        log.info("Vendor created: {} ({})", vendor.getName(), vendor.getVendorCode());
        return mapper.toVendorDTO(vendor);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public VendorDTO updateVendor(String id, UpdateVendorRequest req) {
        Vendor vendor = findOrThrow(id);
        String actor  = currentUserService.getCurrentUserId();
        String old    = vendor.getName();

        vendor.setName(req.getName());
        if (req.getCompanyName()       != null) vendor.setCompanyName(req.getCompanyName());
        if (req.getEmail()             != null) vendor.setEmail(req.getEmail());
        if (req.getPhone()             != null) vendor.setPhone(req.getPhone());
        if (req.getTaxId()             != null) vendor.setTaxId(req.getTaxId());
        if (req.getPaymentTerms()      != null) vendor.setPaymentTerms(req.getPaymentTerms());
        if (req.getPaymentTermsDays()  != null) vendor.setPaymentTermsDays(req.getPaymentTermsDays());
        if (req.getPreferredCurrency() != null) vendor.setPreferredCurrency(req.getPreferredCurrency());
        if (req.getAddressLine1()      != null) vendor.setAddressLine1(req.getAddressLine1());
        if (req.getCity()              != null) vendor.setCity(req.getCity());
        if (req.getState()             != null) vendor.setState(req.getState());
        if (req.getPostalCode()        != null) vendor.setPostalCode(req.getPostalCode());
        if (req.getCountry()           != null) vendor.setCountry(req.getCountry());
        if (req.getCategory()          != null) vendor.setCategory(req.getCategory());
        if (req.getNotes()             != null) vendor.setNotes(req.getNotes());
        vendor.setUpdatedBy(actor);

        vendor = vendorRepository.save(vendor);
        auditService.log(actor, AuditAction.FINANCE_VENDOR_UPDATED, "Vendor", vendor.getId(),
                vendor.getVendorCode(), null, old, vendor.getName(), true);
        return mapper.toVendorDTO(vendor);
    }

    @Transactional
    public VendorDTO changeStatus(String id, VendorStatus newStatus) {
        Vendor vendor = findOrThrow(id);
        String actor  = currentUserService.getCurrentUserId();
        String prev   = vendor.getStatus().name();
        vendor.setStatus(newStatus);
        vendor.setUpdatedBy(actor);
        vendor = vendorRepository.save(vendor);
        auditService.log(actor, AuditAction.FINANCE_VENDOR_STATUS_CHANGED, "Vendor", vendor.getId(),
                vendor.getVendorCode(), null, prev, newStatus.name(), true);
        return mapper.toVendorDTO(vendor);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    public Vendor findOrThrow(String id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Vendor", id));
    }
}
