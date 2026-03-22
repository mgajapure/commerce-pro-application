package com.commerce_pro_backend.payment.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.payment.dto.request.CreateChargebackRequest;
import com.commerce_pro_backend.payment.dto.request.ResolveChargebackRequest;
import com.commerce_pro_backend.payment.dto.request.SubmitEvidenceRequest;
import com.commerce_pro_backend.payment.dto.response.ChargebackDisputeDTO;
import com.commerce_pro_backend.payment.dto.response.ChargebackSummaryDTO;
import com.commerce_pro_backend.payment.enums.ChargebackStatus;
import com.commerce_pro_backend.payment.service.ChargebackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/payments/chargebacks")
@RequiredArgsConstructor
@Tag(name = "Chargebacks & Disputes", description = "Chargeback intake, evidence submission, and resolution tracking")
public class ChargebackController {

    private final ChargebackService service;

    @GetMapping
    @Operation(summary = "List chargebacks and disputes", description = "Filter by status and/or order ID")
    public ResponseEntity<ApiResponse<PageResponse<ChargebackSummaryDTO>>> list(
            @RequestParam(required = false) ChargebackStatus status,
            @RequestParam(required = false) String orderId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Chargebacks retrieved",
                service.list(status, orderId, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get chargeback dispute detail")
    public ResponseEntity<ApiResponse<ChargebackDisputeDTO>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Chargeback retrieved", service.getById(id)));
    }

    @GetMapping("/deadline-alerts")
    @Operation(summary = "List disputes with evidence deadlines within the next 3 days")
    public ResponseEntity<ApiResponse<List<ChargebackSummaryDTO>>> getDeadlineAlerts() {
        return ResponseEntity.ok(ApiResponse.success("Deadline alerts retrieved", service.getDeadlineAlerts()));
    }

    @PostMapping
    @Operation(summary = "Intake a new chargeback / dispute",
               description = "Records a chargeback received from the bank. Updates Order.paymentStatus to CHARGEBACK.")
    public ResponseEntity<ApiResponse<ChargebackDisputeDTO>> intake(@Valid @RequestBody CreateChargebackRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Chargeback recorded", service.intake(req)));
    }

    @PostMapping("/{id}/submit-evidence")
    @Operation(summary = "Submit evidence for a chargeback",
               description = "Upload evidence notes and file paths. Moves status to EVIDENCE_SUBMITTED.")
    public ResponseEntity<ApiResponse<ChargebackDisputeDTO>> submitEvidence(
            @PathVariable String id,
            @Valid @RequestBody SubmitEvidenceRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Evidence submitted", service.submitEvidence(id, req)));
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept the chargeback loss", description = "Merchant concedes without fighting — marks as ACCEPTED")
    public ResponseEntity<ApiResponse<ChargebackDisputeDTO>> accept(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success("Chargeback accepted", service.accept(id, body.get("notes"))));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Record the bank's decision", description = "Mark the dispute as WON or LOST based on the bank's ruling")
    public ResponseEntity<ApiResponse<ChargebackDisputeDTO>> resolve(
            @PathVariable String id,
            @RequestBody ResolveChargebackRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Chargeback resolved", service.resolve(id, req)));
    }
}
