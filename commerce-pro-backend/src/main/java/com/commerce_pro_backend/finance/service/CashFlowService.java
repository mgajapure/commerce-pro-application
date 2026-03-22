package com.commerce_pro_backend.finance.service;

import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashFlowService {

    private final CustomerInvoiceRepository customerInvoiceRepository;
    private final VendorInvoiceRepository   vendorInvoiceRepository;

    public CashFlowForecastDTO getForecast() {
        LocalDate today  = LocalDate.now();
        LocalDate day30  = today.plusDays(30);
        LocalDate day60  = today.plusDays(60);
        LocalDate day90  = today.plusDays(90);

        // AR — expected cash in
        BigDecimal arDue30 = customerInvoiceRepository.sumAgingBucket(day30, today);
        BigDecimal arDue60 = customerInvoiceRepository.sumAgingBucket(day60, day30);

        // AP — expected cash out
        BigDecimal apDue30 = vendorInvoiceRepository.sumAgingBucket(day30, today);
        BigDecimal apDue60 = vendorInvoiceRepository.sumAgingBucket(day60, day30);

        BigDecimal net30 = arDue30.subtract(apDue30);
        BigDecimal net60 = net30.add(arDue60).subtract(apDue60);

        List<CashFlowProjectionDTO> projections = new ArrayList<>();
        projections.add(CashFlowProjectionDTO.builder().period("0-30 days")
                .expectedInflows(arDue30).expectedOutflows(apDue30).netCashFlow(net30).build());
        projections.add(CashFlowProjectionDTO.builder().period("31-60 days")
                .expectedInflows(arDue60).expectedOutflows(apDue60).netCashFlow(arDue60.subtract(apDue60)).build());

        return CashFlowForecastDTO.builder()
                .totalArDue30Days(arDue30).totalArDue60Days(arDue60)
                .totalApDue30Days(apDue30).totalApDue60Days(apDue60)
                .projectedNetCash30Days(net30).projectedNetCash60Days(net60)
                .projectedNetCash90Days(net60) // simplified
                .projections(projections)
                .build();
    }
}
