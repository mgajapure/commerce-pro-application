package com.commerce_pro_backend.finance.controller;

import com.commerce_pro_backend.common.dto.*;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.enums.VendorInvoiceStatus;
import com.commerce_pro_backend.finance.service.VendorInvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/v1/finance/invoices/vendor")
@RequiredArgsConstructor @Tag(name = "Finance - Vendor Invoices", description = "AP bill lifecycle management")
public class VendorInvoiceController {

    private final VendorInvoiceService vendorInvoiceService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:ap:read')")
    @Operation(summary = "List vendor invoices")
    public ResponseEntity<ApiResponse<PageResponse<VendorInvoiceSummaryDTO>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) VendorInvoiceStatus status,
            @RequestParam(required = false) String vendorId,
            @RequestParam(required = false) String periodId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "dueDate") @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Vendor invoices retrieved",
                vendorInvoiceService.listInvoices(search, status, vendorId, periodId, from, to, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:ap:read')")
    @Operation(summary = "Get vendor invoice by ID")
    public ResponseEntity<ApiResponse<VendorInvoiceDTO>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Vendor invoice retrieved", vendorInvoiceService.getById(id)));
    }

    @GetMapping("/ap/summary")
    @PreAuthorize("hasAuthority('finance:ap:read')")
    @Operation(summary = "AP aging summary")
    public ResponseEntity<ApiResponse<APSummaryDTO>> getAPSummary() {
        return ResponseEntity.ok(ApiResponse.success("AP summary retrieved", vendorInvoiceService.getAPSummary()));
    }

    @GetMapping("/ap/due-soon")
    @PreAuthorize("hasAuthority('finance:ap:read')")
    @Operation(summary = "Bills due within N days")
    public ResponseEntity<ApiResponse<List<VendorInvoiceSummaryDTO>>> getDueSoon(
            @RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(ApiResponse.success("Due-soon invoices retrieved", vendorInvoiceService.getDueSoon(days)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:ap:create')")
    @Operation(summary = "Record new vendor bill")
    public ResponseEntity<ApiResponse<VendorInvoiceDTO>> create(@Valid @RequestBody CreateVendorInvoiceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vendor invoice created", vendorInvoiceService.createInvoice(req)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('finance:ap:approve')")
    @Operation(summary = "Approve vendor invoice for payment", description = "Auto-posts Expense/AP journal entry")
    public ResponseEntity<ApiResponse<VendorInvoiceDTO>> approve(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Vendor invoice approved", vendorInvoiceService.approve(id)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('finance:ap:approve')")
    @Operation(summary = "Reject vendor invoice")
    public ResponseEntity<ApiResponse<VendorInvoiceDTO>> reject(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success("Vendor invoice rejected",
                vendorInvoiceService.reject(id, body.getOrDefault("reason", "Rejected"))));
    }

    @PostMapping("/{id}/schedule-payment")
    @PreAuthorize("hasAuthority('finance:ap:approve')")
    @Operation(summary = "Schedule payment date")
    public ResponseEntity<ApiResponse<VendorInvoiceDTO>> schedulePayment(
            @PathVariable String id, @Valid @RequestBody SchedulePaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Payment scheduled", vendorInvoiceService.schedulePayment(id, req)));
    }

    @PostMapping("/{id}/record-payment")
    @PreAuthorize("hasAuthority('finance:ap:pay')")
    @Operation(summary = "Record payment to vendor", description = "Auto-posts AP/Cash journal entry")
    public ResponseEntity<ApiResponse<VendorInvoiceDTO>> recordPayment(
            @PathVariable String id, @Valid @RequestBody RecordPaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Payment recorded", vendorInvoiceService.recordPayment(id, req)));
    }

    @PostMapping("/{id}/dispute")
    @PreAuthorize("hasAuthority('finance:ap:approve')")
    @Operation(summary = "Mark invoice as disputed")
    public ResponseEntity<ApiResponse<VendorInvoiceDTO>> dispute(
            @PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success("Vendor invoice disputed",
                vendorInvoiceService.markDisputed(id, body != null ? body.get("notes") : null)));
    }
}
