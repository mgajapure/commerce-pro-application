package com.commerce_pro_backend.customer.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CustomerFilterDTO {
    private String search;
    private String status;
    private String tier;
    private String groupId;
    private Boolean isBlacklisted;
    private Boolean isFraudFlagged;
    private Boolean marketingOptIn;
    private String tag;
    private String acquisitionSource;
    private BigDecimal minLifetimeSpend;
    private BigDecimal maxLifetimeSpend;
    private Integer minTotalOrders;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) private LocalDateTime createdFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) private LocalDateTime createdTo;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) private LocalDateTime lastOrderFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) private LocalDateTime lastOrderTo;
}
