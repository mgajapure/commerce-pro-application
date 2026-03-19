package com.commerce_pro_backend.customer.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.customer.dto.GroupDTO;
import com.commerce_pro_backend.customer.service.CustomerGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/customer-groups")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Customer Groups", description = "Customer group management API")
public class CustomerGroupController {

    private final CustomerGroupService groupService;

    @GetMapping
    @PreAuthorize("hasAuthority('customer:group:read')")
    @Operation(summary = "List all groups (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<GroupDTO.Response>>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Groups retrieved", groupService.listGroups(pageable)));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('customer:group:read')")
    @Operation(summary = "List active groups (for dropdowns)")
    public ResponseEntity<ApiResponse<List<GroupDTO.Response>>> listActive() {
        return ResponseEntity.ok(ApiResponse.success("Active groups retrieved", groupService.listActiveGroups()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:group:read')")
    @Operation(summary = "Get group by ID")
    public ResponseEntity<ApiResponse<GroupDTO.Response>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Group retrieved", groupService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('customer:group:create')")
    @Operation(summary = "Create group")
    public ResponseEntity<ApiResponse<GroupDTO.Response>> create(@Valid @RequestBody GroupDTO.Request req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Group created", groupService.createGroup(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:group:update')")
    @Operation(summary = "Update group")
    public ResponseEntity<ApiResponse<GroupDTO.Response>> update(@PathVariable String id,
                                                                   @Valid @RequestBody GroupDTO.Request req) {
        return ResponseEntity.ok(ApiResponse.success("Group updated", groupService.updateGroup(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:group:delete')")
    @Operation(summary = "Delete group (fails if members exist)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        groupService.deleteGroup(id);
        return ResponseEntity.ok(ApiResponse.success("Group deleted", null));
    }
}
