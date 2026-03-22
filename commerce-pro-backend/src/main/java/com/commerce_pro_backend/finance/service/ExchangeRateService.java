package com.commerce_pro_backend.finance.service;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.entity.ExchangeRate;
import com.commerce_pro_backend.finance.mapper.FinanceMapper;
import com.commerce_pro_backend.finance.repository.ExchangeRateRepository;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.service.AuditService;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeRateService {

    private final ExchangeRateRepository rateRepository;
    private final FinanceMapper          mapper;
    private final AuditService          auditService;
    private final CurrentUserService    currentUserService;

    public List<ExchangeRateDTO> getAllRates() {
        return mapper.toExchangeRateDTOList(rateRepository.findAll());
    }

    public List<ExchangeRateDTO> getLatestForPair(String base, String quote) {
        return mapper.toExchangeRateDTOList(rateRepository.findLatestRate(base, quote));
    }

    public ConversionResultDTO convert(String from, String to, BigDecimal amount) {
        if (from.equalsIgnoreCase(to)) {
            return ConversionResultDTO.builder()
                    .originalAmount(amount).fromCurrency(from).toCurrency(to)
                    .rate(BigDecimal.ONE).convertedAmount(amount).rateDate(LocalDate.now()).build();
        }
        List<ExchangeRate> rates = rateRepository.findLatestRate(from.toUpperCase(), to.toUpperCase());
        if (rates.isEmpty()) throw ApiException.notFound("ExchangeRate", from + "/" + to);
        ExchangeRate rate = rates.get(0);
        BigDecimal converted = amount.multiply(rate.getRate()).setScale(4, RoundingMode.HALF_UP);
        return ConversionResultDTO.builder()
                .originalAmount(amount).fromCurrency(from).toCurrency(to)
                .rate(rate.getRate()).convertedAmount(converted).rateDate(rate.getRateDate()).build();
    }

    @Transactional
    public ExchangeRateDTO addRate(AddExchangeRateRequest req) {
        String actor = currentUserService.getCurrentUserId();
        if (rateRepository.existsByBaseCurrencyAndQuoteCurrencyAndRateDate(
                req.getBaseCurrency().toUpperCase(), req.getQuoteCurrency().toUpperCase(), req.getRateDate())) {
            throw ApiException.conflict("Rate for " + req.getBaseCurrency() + "/" + req.getQuoteCurrency() + " on " + req.getRateDate() + " already exists");
        }
        ExchangeRate rate = ExchangeRate.builder()
                .baseCurrency(req.getBaseCurrency().toUpperCase())
                .quoteCurrency(req.getQuoteCurrency().toUpperCase())
                .rate(req.getRate()).rateDate(req.getRateDate())
                .source(req.getSource() != null ? req.getSource() : "MANUAL")
                .createdBy(actor).build();
        rate = rateRepository.save(rate);
        auditService.log(actor, AuditAction.FINANCE_EXCHANGE_RATE_ADDED, "ExchangeRate", rate.getId(),
                rate.getBaseCurrency() + "/" + rate.getQuoteCurrency(), null, null, null, true);
        return mapper.toExchangeRateDTO(rate);
    }
}
