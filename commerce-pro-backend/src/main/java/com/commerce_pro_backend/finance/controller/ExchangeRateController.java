package com.commerce_pro_backend.finance.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController @RequestMapping("/v1/finance/exchange-rates")
@RequiredArgsConstructor @Tag(name = "Finance - Exchange Rates", description = "Currency exchange rate management")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:rates:read')")
    @Operation(summary = "List all exchange rates")
    public ResponseEntity<ApiResponse<List<ExchangeRateDTO>>> list() {
        return ResponseEntity.ok(ApiResponse.success("Exchange rates retrieved", exchangeRateService.getAllRates()));
    }

    @GetMapping("/convert")
    @PreAuthorize("hasAuthority('finance:rates:read')")
    @Operation(summary = "Convert amount between currencies")
    public ResponseEntity<ApiResponse<ConversionResultDTO>> convert(
            @RequestParam String from, @RequestParam String to,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success("Conversion result",
                exchangeRateService.convert(from, to, amount)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:rates:manage')")
    @Operation(summary = "Add exchange rate")
    public ResponseEntity<ApiResponse<ExchangeRateDTO>> addRate(@Valid @RequestBody AddExchangeRateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Exchange rate added", exchangeRateService.addRate(req)));
    }
}
