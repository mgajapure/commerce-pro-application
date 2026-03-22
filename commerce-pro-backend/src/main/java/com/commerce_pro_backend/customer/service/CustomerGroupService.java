package com.commerce_pro_backend.customer.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.customer.dto.GroupDTO;
import com.commerce_pro_backend.customer.entity.CustomerGroup;
import com.commerce_pro_backend.customer.mapper.CustomerMapper;
import com.commerce_pro_backend.customer.repository.CustomerGroupRepository;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.service.AuditService;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerGroupService {

    private final CustomerGroupRepository groupRepo;
    private final CustomerMapper          mapper;
    private final AuditService            auditService;
    private final CurrentUserService      currentUserService;

    public PageResponse<GroupDTO.Response> listGroups(Pageable pageable) {
        Page<CustomerGroup> page = groupRepo.findAll(pageable);
        List<GroupDTO.Response> dtos = page.getContent().stream()
                .map(mapper::toGroupResponse).collect(Collectors.toList());
        return PageResponse.from(page, dtos);
    }

    public List<GroupDTO.Response> listActiveGroups() {
        return groupRepo.findByIsActiveTrue(Pageable.unpaged()).getContent()
                .stream().map(mapper::toGroupResponse).collect(Collectors.toList());
    }

    public GroupDTO.Response getById(String id) {
        return mapper.toGroupResponse(findGroup(id));
    }

    @Transactional
    public GroupDTO.Response createGroup(GroupDTO.Request req) {
        String actorId = currentUserService.getCurrentUserId();
        if (groupRepo.existsByName(req.getName()))
            throw ApiException.conflict("A group named '" + req.getName() + "' already exists");

        CustomerGroup g = CustomerGroup.builder()
                .name(req.getName()).description(req.getDescription())
                .colorHex(req.getColorHex() != null ? req.getColorHex() : "#6366F1")
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .discountPercentage(req.getDiscountPercentage())
                .internalNotes(req.getInternalNotes())
                .createdBy(actorId).updatedBy(actorId)
                .build();

        CustomerGroup saved = groupRepo.save(g);
        auditService.log(actorId, AuditAction.CUSTOMER_GROUP_CREATED,
                "CUSTOMER_GROUP", saved.getId(), saved.getName(), null, null, null, true);
        return mapper.toGroupResponse(saved);
    }

    @Transactional
    public GroupDTO.Response updateGroup(String id, GroupDTO.Request req) {
        String actorId = currentUserService.getCurrentUserId();
        CustomerGroup g = findGroup(id);
        if (!g.getName().equals(req.getName()) && groupRepo.existsByNameAndIdNot(req.getName(), id))
            throw ApiException.conflict("Group name '" + req.getName() + "' is already taken");

        g.setName(req.getName()); g.setDescription(req.getDescription());
        if (req.getColorHex() != null)          g.setColorHex(req.getColorHex());
        if (req.getIsActive() != null)          g.setIsActive(req.getIsActive());
        if (req.getDiscountPercentage() != null) g.setDiscountPercentage(req.getDiscountPercentage());
        if (req.getInternalNotes() != null)     g.setInternalNotes(req.getInternalNotes());
        g.setUpdatedBy(actorId);

        CustomerGroup saved = groupRepo.save(g);
        auditService.log(actorId, AuditAction.CUSTOMER_GROUP_UPDATED,
                "CUSTOMER_GROUP", saved.getId(), saved.getName(), null, null, null, true);
        return mapper.toGroupResponse(saved);
    }

    @Transactional
    public void deleteGroup(String id) {
        String actorId = currentUserService.getCurrentUserId();
        CustomerGroup g = findGroup(id);
        if (g.getMemberCount() != null && g.getMemberCount() > 0)
            throw ApiException.conflict("Cannot delete group '" + g.getName() + "' — it has " + g.getMemberCount() + " members. Reassign them first.");
        groupRepo.delete(g);
        auditService.log(actorId, AuditAction.CUSTOMER_GROUP_DELETED,
                "CUSTOMER_GROUP", id, g.getName(), null, null, null, true);
    }

    private CustomerGroup findGroup(String id) {
        return groupRepo.findById(id).orElseThrow(() -> ApiException.notFound("CustomerGroup", id));
    }
}
