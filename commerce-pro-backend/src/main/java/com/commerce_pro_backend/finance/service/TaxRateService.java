package com.commerce_pro_backend.finance.service;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.entity.*;
import com.commerce_pro_backend.finance.enums.TaxApplicability;
import com.commerce_pro_backend.finance.mapper.FinanceMapper;
import com.commerce_pro_backend.finance.repository.*;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.service.AuditService;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxRateService {

    private final TaxRateRepository         taxRateRepository;
    private final TaxJurisdictionRepository jurisdictionRepository;
    private final FinanceMapper             mapper;
    private final AuditService             auditService;
    private final CurrentUserService       currentUserService;

    public List<TaxRateDTO> getAllRates() { return mapper.toTaxRateDTOList(taxRateRepository.findAll()); }
    public List<TaxRateDTO> getActiveRates() { return mapper.toTaxRateDTOList(taxRateRepository.findByIsActiveTrue()); }
    public TaxRateDTO getById(String id) { return mapper.toTaxRateDTO(findRateOrThrow(id)); }

    public List<TaxJurisdictionDTO> getAllJurisdictions() { return mapper.toJurisdictionDTOList(jurisdictionRepository.findAll()); }
    public TaxJurisdictionDTO getJurisdictionById(String id) {
        return mapper.toJurisdictionDTO(jurisdictionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("TaxJurisdiction", id)));
    }

    @Transactional
    public TaxRateDTO createRate(CreateTaxRateRequest req) {
        // Validate date range: effectiveTo must be after effectiveFrom when both are set
        if (req.getEffectiveFrom() != null && req.getEffectiveTo() != null
                && !req.getEffectiveTo().isAfter(req.getEffectiveFrom())) {
            throw com.commerce_pro_backend.common.exception.ApiException.badRequest(
                    "effectiveTo must be after effectiveFrom");
        }

        if (taxRateRepository.findByRateCode(req.getRateCode()).isPresent()) {
            throw ApiException.conflict("Tax rate code '" + req.getRateCode() + "' already exists");
        }
        String actor = currentUserService.getCurrentUserId();
        TaxRate rate = TaxRate.builder()
                .rateCode(req.getRateCode()).name(req.getName()).description(req.getDescription())
                .taxType(req.getTaxType()).rate(req.getRate())
                .jurisdictionId(req.getJurisdictionId()).country(req.getCountry()).stateProvince(req.getStateProvince())
                .applicability(req.getApplicability() != null ? req.getApplicability() : TaxApplicability.ALL)
                .isCompound(req.getIsCompound() != null ? req.getIsCompound() : false)
                .isInclusive(req.getIsInclusive() != null ? req.getIsInclusive() : false)
                .isActive(true).effectiveFrom(req.getEffectiveFrom()).effectiveTo(req.getEffectiveTo())
                .createdBy(actor).updatedBy(actor).build();
        rate = taxRateRepository.save(rate);
        auditService.log(actor, AuditAction.FINANCE_TAX_RATE_CREATED, "TaxRate", rate.getId(),
                rate.getRateCode(), null, null, null, true);
        return mapper.toTaxRateDTO(rate);
    }

    @Transactional
    public TaxRateDTO updateRate(String id, CreateTaxRateRequest req) {
        TaxRate rate = findRateOrThrow(id);
        String actor = currentUserService.getCurrentUserId();
        rate.setName(req.getName()); rate.setDescription(req.getDescription());
        rate.setTaxType(req.getTaxType()); rate.setRate(req.getRate());
        if (req.getCountry() != null) rate.setCountry(req.getCountry());
        if (req.getStateProvince() != null) rate.setStateProvince(req.getStateProvince());
        if (req.getApplicability() != null) rate.setApplicability(req.getApplicability());
        if (req.getEffectiveFrom() != null) rate.setEffectiveFrom(req.getEffectiveFrom());
        if (req.getEffectiveTo()   != null) rate.setEffectiveTo(req.getEffectiveTo());
        rate.setUpdatedBy(actor);
        rate = taxRateRepository.save(rate);
        auditService.log(actor, AuditAction.FINANCE_TAX_RATE_UPDATED, "TaxRate", rate.getId(), rate.getRateCode(), null, null, null, true);
        return mapper.toTaxRateDTO(rate);
    }

    @Transactional
    public TaxRateDTO activate(String id) {
        TaxRate rate = findRateOrThrow(id);
        rate.setIsActive(true); rate.setUpdatedBy(currentUserService.getCurrentUserId());
        return mapper.toTaxRateDTO(taxRateRepository.save(rate));
    }

    @Transactional
    public TaxRateDTO deactivate(String id) {
        TaxRate rate = findRateOrThrow(id);
        String actor = currentUserService.getCurrentUserId();
        rate.setIsActive(false); rate.setUpdatedBy(actor);
        rate = taxRateRepository.save(rate);
        auditService.log(actor, AuditAction.FINANCE_TAX_RATE_DEACTIVATED, "TaxRate", rate.getId(), rate.getRateCode(), null, "ACTIVE", "INACTIVE", true);
        return mapper.toTaxRateDTO(rate);
    }

    @Transactional
    public TaxJurisdictionDTO createJurisdiction(CreateJurisdictionRequest req) {
        if (jurisdictionRepository.findByJurisdictionCode(req.getJurisdictionCode()).isPresent()) {
            throw ApiException.conflict("Jurisdiction code '" + req.getJurisdictionCode() + "' already exists");
        }
        String actor = currentUserService.getCurrentUserId();
        TaxJurisdiction j = TaxJurisdiction.builder()
                .jurisdictionCode(req.getJurisdictionCode()).name(req.getName())
                .country(req.getCountry()).stateProvince(req.getStateProvince())
                .notes(req.getNotes()).isActive(true).createdBy(actor).build();
        return mapper.toJurisdictionDTO(jurisdictionRepository.save(j));
    }

    private TaxRate findRateOrThrow(String id) {
        return taxRateRepository.findById(id).orElseThrow(() -> ApiException.notFound("TaxRate", id));
    }
}
