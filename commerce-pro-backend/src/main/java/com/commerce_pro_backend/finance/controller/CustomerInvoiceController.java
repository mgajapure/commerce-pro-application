package com.commerce_pro_backend.finance.controller;

import com.commerce_pro_backend.common.dto.*;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.enums.InvoiceStatus;
import com.commerce_pro_backend.finance.service.*;
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
import java.util.List;

@RestController @RequestMapping("/v1/finance/invoices/customer")
@RequiredArgsConstructor @Tag(name = "Finance - Customer Invoices", description = "AR invoice lifecycle")
public class CustomerInvoiceController {

    private final CustomerInvoiceService invoiceService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:invoice:read')")
    @Operation(summary = "List customer invoices")
    public ResponseEntity<ApiResponse<PageResponse<CustomerInvoiceSummaryDTO>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String periodId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Boolean overdue,
            @PageableDefault(size = 20, sort = "createdAt") @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Invoices retrieved",
                invoiceService.listInvoices(search, status, customerId, periodId, from, to, overdue, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:invoice:read')")
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<ApiResponse<CustomerInvoiceDTO>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Invoice retrieved", invoiceService.getById(id)));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAuthority('finance:invoice:read')")
    @Operation(summary = "Get invoices for an order")
    public ResponseEntity<ApiResponse<List<CustomerInvoiceSummaryDTO>>> getByOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.success("Invoices retrieved", invoiceService.getByOrderId(orderId)));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAuthority('finance:invoice:read')")
    @Operation(summary = "Get invoices for a customer")
    public ResponseEntity<ApiResponse<PageResponse<CustomerInvoiceSummaryDTO>>> getByCustomer(
            @PathVariable String customerId,
            @PageableDefault(size = 20) @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Customer invoices retrieved",
                invoiceService.getByCustomer(customerId, pageable)));
    }

    @GetMapping("/ar/summary")
    @PreAuthorize("hasAuthority('finance:ar:read')")
    @Operation(summary = "AR aging summary", description = "Outstanding AR bucketed by days overdue")
    public ResponseEntity<ApiResponse<ARSummaryDTO>> getARSummary() {
        return ResponseEntity.ok(ApiResponse.success("AR summary retrieved", invoiceService.getARSummary()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:invoice:create')")
    @Operation(summary = "Create customer invoice")
    public ResponseEntity<ApiResponse<CustomerInvoiceDTO>> create(
            @Valid @RequestBody CreateCustomerInvoiceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invoice created", invoiceService.createInvoice(req)));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAuthority('finance:invoice:create')")
    @Operation(summary = "Send invoice to customer", description = "Transitions from DRAFT → SENT, auto-posts AR journal entry")
    public ResponseEntity<ApiResponse<CustomerInvoiceDTO>> send(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Invoice sent", invoiceService.sendInvoice(id)));
    }

    @PostMapping("/{id}/record-payment")
    @PreAuthorize("hasAuthority('finance:invoice:payment')")
    @Operation(summary = "Record payment received", description = "Partial or full — auto-posts Cash/AR journal entry")
    public ResponseEntity<ApiResponse<CustomerInvoiceDTO>> recordPayment(
            @PathVariable String id, @Valid @RequestBody RecordPaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Payment recorded", invoiceService.recordPayment(id, req)));
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('finance:invoice:void')")
    @Operation(summary = "Void invoice")
    public ResponseEntity<ApiResponse<CustomerInvoiceDTO>> voidInvoice(
            @PathVariable String id, @Valid @RequestBody VoidInvoiceRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Invoice voided", invoiceService.voidInvoice(id, req)));
    }

    @PostMapping("/{id}/write-off")
    @PreAuthorize("hasAuthority('finance:invoice:void')")
    @Operation(summary = "Write off invoice as bad debt")
    public ResponseEntity<ApiResponse<CustomerInvoiceDTO>> writeOff(
            @PathVariable String id, @Valid @RequestBody VoidInvoiceRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Invoice written off", invoiceService.writeOff(id, req.getReason())));
    }
}
