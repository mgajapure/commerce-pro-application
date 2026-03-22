package com.commerce_pro_backend.customer.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.customer.dto.*;
import com.commerce_pro_backend.customer.entity.*;
import com.commerce_pro_backend.customer.enums.*;
import com.commerce_pro_backend.customer.mapper.CustomerMapper;
import com.commerce_pro_backend.customer.repository.*;
import com.commerce_pro_backend.customer.specification.CustomerSpecification;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.service.AuditService;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository        customerRepo;
    private final CustomerGroupRepository   groupRepo;
    private final CustomerMapper            mapper;
    private final AuditService              auditService;
    private final CurrentUserService        currentUserService;

    // ── List & Search ─────────────────────────────────────────────────────

    public PageResponse<CustomerSummaryDTO> listCustomers(CustomerFilterDTO filter, Pageable pageable) {
        Page<Customer> page = customerRepo.findAll(CustomerSpecification.withFilters(filter), pageable);
        return PageResponse.from(page, mapper.toSummaryDTOList(page.getContent()));
    }

    public CustomerResponseDTO getById(String id) {
        return mapper.toResponseDTO(findCustomer(id));
    }

    public CustomerStatsDTO getStats() {
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();

        Map<String, Long> byTier = customerRepo.countGroupedByTier().stream()
                .collect(Collectors.toMap(a -> ((CustomerTier) a[0]).name(), a -> (Long) a[1]));
        Map<String, Long> byStatus = customerRepo.countGroupedByStatus().stream()
                .collect(Collectors.toMap(a -> ((CustomerStatus) a[0]).name(), a -> (Long) a[1]));

        return CustomerStatsDTO.builder()
                .totalCustomers(customerRepo.count())
                .activeCustomers(customerRepo.countByStatus(CustomerStatus.ACTIVE))
                .newCustomersToday(customerRepo.countCreatedSince(today))
                .newCustomersThisMonth(customerRepo.countCreatedSince(monthStart))
                .blacklistedCustomers(customerRepo.countBlacklisted())
                .fraudFlaggedCustomers(customerRepo.countActiveFraudFlagged())
                .byTier(byTier)
                .byStatus(byStatus)
                .totalLifetimeRevenue(customerRepo.sumTotalLifetimeRevenue())
                .averageLifetimeValue(customerRepo.avgLifetimeValue())
                .loyaltyPointsOutstanding(customerRepo.sumActiveLoyaltyPoints())
                .build();
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

    @Transactional
    public CustomerResponseDTO createCustomer(CustomerRequestDTO req) {
        String actorId = currentUserService.getCurrentUserId();

        if (customerRepo.existsByEmail(req.getEmail()))
            throw ApiException.conflict("A customer with email '" + req.getEmail() + "' already exists");

        CustomerGroup group = resolveGroup(req.getGroupId());

        Customer customer = Customer.builder()
                .firstName(req.getFirstName()).lastName(req.getLastName())
                .email(req.getEmail()).phone(req.getPhone())
                .secondaryPhone(req.getSecondaryPhone()).dateOfBirth(req.getDateOfBirth())
                .gender(req.getGender()).avatarUrl(req.getAvatarUrl())
                .companyName(req.getCompanyName()).taxId(req.getTaxId())
                .preferredCurrency(req.getPreferredCurrency() != null ? req.getPreferredCurrency() : "USD")
                .preferredLanguage(req.getPreferredLanguage() != null ? req.getPreferredLanguage() : "en")
                .marketingOptIn(req.getMarketingOptIn() != null ? req.getMarketingOptIn() : false)
                .smsOptIn(req.getSmsOptIn() != null ? req.getSmsOptIn() : false)
                .acquisitionSource(req.getAcquisitionSource())
                .group(group).internalNotes(req.getInternalNotes())
                .linkedUserId(req.getLinkedUserId())
                .referralCode(UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase())
                .createdBy(actorId).updatedBy(actorId)
                .build();

        Customer saved = customerRepo.save(customer);
        if (group != null) groupRepo.refreshMemberCount(group.getId());

        auditService.log(actorId, AuditAction.CUSTOMER_CREATED,
                "CUSTOMER", saved.getId(), saved.getEmail(), null, null, null, true);
        return mapper.toResponseDTO(saved);
    }

    @Transactional
    public CustomerResponseDTO updateCustomer(String id, CustomerRequestDTO req) {
        String actorId = currentUserService.getCurrentUserId();
        Customer c = findCustomer(id);

        if (!c.getEmail().equals(req.getEmail()) && customerRepo.existsByEmailAndIdNot(req.getEmail(), id))
            throw ApiException.conflict("Email '" + req.getEmail() + "' is already taken");

        String oldGroupId = c.getGroup() != null ? c.getGroup().getId() : null;
        CustomerGroup newGroup = resolveGroup(req.getGroupId());

        c.setFirstName(req.getFirstName()); c.setLastName(req.getLastName());
        c.setEmail(req.getEmail()); c.setPhone(req.getPhone());
        c.setSecondaryPhone(req.getSecondaryPhone()); c.setDateOfBirth(req.getDateOfBirth());
        c.setGender(req.getGender()); c.setAvatarUrl(req.getAvatarUrl());
        c.setCompanyName(req.getCompanyName()); c.setTaxId(req.getTaxId());
        if (req.getPreferredCurrency() != null) c.setPreferredCurrency(req.getPreferredCurrency());
        if (req.getPreferredLanguage() != null) c.setPreferredLanguage(req.getPreferredLanguage());
        if (req.getMarketingOptIn() != null) c.setMarketingOptIn(req.getMarketingOptIn());
        if (req.getSmsOptIn() != null) c.setSmsOptIn(req.getSmsOptIn());
        c.setAcquisitionSource(req.getAcquisitionSource());
        c.setGroup(newGroup); c.setInternalNotes(req.getInternalNotes());
        if (req.getLinkedUserId() != null) c.setLinkedUserId(req.getLinkedUserId());
        c.setUpdatedBy(actorId);

        Customer saved = customerRepo.save(c);
        if (oldGroupId != null) groupRepo.refreshMemberCount(oldGroupId);
        if (newGroup != null)   groupRepo.refreshMemberCount(newGroup.getId());

        auditService.log(actorId, AuditAction.CUSTOMER_UPDATED,
                "CUSTOMER", saved.getId(), saved.getEmail(), null, null, null, true);
        return mapper.toResponseDTO(saved);
    }

    @Transactional
    public void deleteCustomer(String id) {
        String actorId = currentUserService.getCurrentUserId();
        Customer c = findCustomer(id);
        String groupId = c.getGroup() != null ? c.getGroup().getId() : null;
        customerRepo.delete(c);
        if (groupId != null) groupRepo.refreshMemberCount(groupId);
        auditService.log(actorId, AuditAction.CUSTOMER_DELETED,
                "CUSTOMER", id, c.getEmail(), null, null, null, true);
    }

    // ── Status ────────────────────────────────────────────────────────────

    @Transactional
    public CustomerResponseDTO setStatus(String id, String statusStr) {
        String actorId = currentUserService.getCurrentUserId();
        Customer c = findCustomer(id);
        CustomerStatus newStatus = CustomerStatus.valueOf(statusStr);
        String old = c.getStatus().name();
        c.setStatus(newStatus);
        c.setUpdatedBy(actorId);
        customerRepo.save(c);
        auditService.log(actorId, AuditAction.CUSTOMER_STATUS_CHANGED,
                "CUSTOMER", id, c.getEmail(), null, old, newStatus.name(), true);
        return mapper.toResponseDTO(c);
    }

    // ── Tier ─────────────────────────────────────────────────────────────

    @Transactional
    public CustomerResponseDTO evaluateAndUpdateTier(String id) {
        String actorId = currentUserService.getCurrentUserId();
        Customer c = findCustomer(id);
        CustomerTier evaluated = c.evaluateTier();
        if (evaluated != c.getTier()) {
            String old = c.getTier().name();
            c.setTier(evaluated);
            c.setUpdatedBy(actorId);
            customerRepo.save(c);
            auditService.log(actorId, AuditAction.CUSTOMER_TIER_CHANGED,
                    "CUSTOMER", id, c.getEmail(), null, old, evaluated.name(), true);
        }
        return mapper.toResponseDTO(c);
    }

    // ── Blacklist ─────────────────────────────────────────────────────────

    @Transactional
    public CustomerResponseDTO blacklist(String id, BlacklistDTO.BlacklistRequest req) {
        String actorId = currentUserService.getCurrentUserId();
        Customer c = findCustomer(id);
        c.setIsBlacklisted(true);
        c.setBlacklistReason(req.getReason());
        c.setBlacklistedAt(LocalDateTime.now());
        c.setBlacklistedBy(actorId);
        c.setStatus(CustomerStatus.BLACKLISTED);
        c.setUpdatedBy(actorId);
        customerRepo.save(c);
        auditService.log(actorId, AuditAction.CUSTOMER_BLACKLISTED,
                "CUSTOMER", id, c.getEmail(), null, null, req.getReason(), true);
        return mapper.toResponseDTO(c);
    }

    @Transactional
    public CustomerResponseDTO unblacklist(String id, String reason) {
        String actorId = currentUserService.getCurrentUserId();
        Customer c = findCustomer(id);
        c.setIsBlacklisted(false);
        c.setBlacklistReason(null);
        c.setBlacklistedAt(null);
        c.setBlacklistedBy(null);
        c.setStatus(CustomerStatus.ACTIVE);
        c.setUpdatedBy(actorId);
        customerRepo.save(c);
        auditService.log(actorId, AuditAction.CUSTOMER_UNBLACKLISTED,
                "CUSTOMER", id, c.getEmail(), null, null, reason, true);
        return mapper.toResponseDTO(c);
    }

    // ── Fraud ─────────────────────────────────────────────────────────────

    @Transactional
    public CustomerResponseDTO flagFraud(String id, BlacklistDTO.FraudFlagRequest req) {
        String actorId = currentUserService.getCurrentUserId();
        Customer c = findCustomer(id);
        c.setIsFraudFlagged(true);
        c.setFraudReason(req.getReason());
        c.setFraudEvidence(req.getEvidence());
        c.setFraudFlaggedAt(LocalDateTime.now());
        c.setFraudFlaggedBy(actorId);
        c.setFraudResolved(false);
        c.setUpdatedBy(actorId);
        customerRepo.save(c);
        auditService.log(actorId, AuditAction.CUSTOMER_FRAUD_FLAGGED,
                "CUSTOMER", id, c.getEmail(), null, null, req.getReason(), true);
        return mapper.toResponseDTO(c);
    }

    @Transactional
    public CustomerResponseDTO resolveFraud(String id, BlacklistDTO.FraudResolveRequest req) {
        String actorId = currentUserService.getCurrentUserId();
        Customer c = findCustomer(id);
        c.setFraudResolved(true);
        c.setFraudResolution(req.getResolution());
        c.setFraudResolvedAt(LocalDateTime.now());
        c.setFraudResolvedBy(actorId);
        c.setUpdatedBy(actorId);
        customerRepo.save(c);
        auditService.log(actorId, AuditAction.CUSTOMER_FRAUD_RESOLVED,
                "CUSTOMER", id, c.getEmail(), null, null, req.getResolution(), true);
        return mapper.toResponseDTO(c);
    }

    // ── Loyalty points ────────────────────────────────────────────────────

    @Transactional
    public CustomerResponseDTO adjustLoyaltyPoints(String id, LoyaltyAdjustDTO req) {
        String actorId = currentUserService.getCurrentUserId();
        Customer c = findCustomer(id);
        int before = c.getLoyaltyPoints();
        int after  = Math.max(0, before + req.getPoints());
        c.setLoyaltyPoints(after);
        c.setUpdatedBy(actorId);
        customerRepo.save(c);
        auditService.log(actorId, AuditAction.CUSTOMER_LOYALTY_ADJUSTED,
                "CUSTOMER", id, c.getEmail(), null, String.valueOf(before), String.valueOf(after), true);
        return mapper.toResponseDTO(c);
    }

    // ── Tags ──────────────────────────────────────────────────────────────

    @Transactional
    public CustomerResponseDTO updateTags(String id, TagsUpdateDTO req) {
        String actorId = currentUserService.getCurrentUserId();
        Customer c = findCustomer(id);
        c.getTags().clear();
        if (req.getTags() != null) c.getTags().addAll(req.getTags());
        c.setUpdatedBy(actorId);
        return mapper.toResponseDTO(customerRepo.save(c));
    }

    // ── Helper ────────────────────────────────────────────────────────────

    public Customer findCustomer(String id) {
        return customerRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Customer", id));
    }

    private CustomerGroup resolveGroup(String groupId) {
        if (groupId == null || groupId.isBlank()) return null;
        return groupRepo.findById(groupId)
                .orElseThrow(() -> ApiException.notFound("CustomerGroup", groupId));
    }
}
