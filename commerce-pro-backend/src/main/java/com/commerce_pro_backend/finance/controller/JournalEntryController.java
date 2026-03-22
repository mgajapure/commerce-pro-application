package com.commerce_pro_backend.finance.controller;

import com.commerce_pro_backend.common.dto.*;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.enums.JournalEntryType;
import com.commerce_pro_backend.finance.service.JournalEntryService;
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

@RestController @RequestMapping("/v1/finance/journal")
@RequiredArgsConstructor @Tag(name = "Finance - Journal", description = "Double-entry journal entry ledger")
public class JournalEntryController {

    private final JournalEntryService journalEntryService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:journal:read')")
    @Operation(summary = "List journal entries")
    public ResponseEntity<ApiResponse<PageResponse<JournalEntrySummaryDTO>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) JournalEntryType entryType,
            @RequestParam(required = false) String periodId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "entryDate") @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Journal entries retrieved",
                journalEntryService.listEntries(search, entryType, periodId, status, from, to, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:journal:read')")
    @Operation(summary = "Get journal entry by ID with all lines")
    public ResponseEntity<ApiResponse<JournalEntryDTO>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Journal entry retrieved", journalEntryService.getById(id)));
    }

    @GetMapping("/source/{sourceType}/{sourceId}")
    @PreAuthorize("hasAuthority('finance:journal:read')")
    @Operation(summary = "Get journal entries for a source document")
    public ResponseEntity<ApiResponse<List<JournalEntrySummaryDTO>>> getBySource(
            @PathVariable String sourceType, @PathVariable String sourceId) {
        return ResponseEntity.ok(ApiResponse.success("Journal entries retrieved",
                journalEntryService.getBySource(sourceType, sourceId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:journal:post')")
    @Operation(summary = "Post manual journal entry", description = "Requires balanced debit and credit totals")
    public ResponseEntity<ApiResponse<JournalEntryDTO>> post(@Valid @RequestBody CreateJournalEntryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Journal entry posted", journalEntryService.postManualEntry(req)));
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasAuthority('finance:journal:post')")
    @Operation(summary = "Reverse a posted journal entry")
    public ResponseEntity<ApiResponse<JournalEntryDTO>> reverse(
            @PathVariable String id, @Valid @RequestBody ReverseEntryRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Journal entry reversed", journalEntryService.reverseEntry(id, req)));
    }
}
