package com.commerce_pro_backend.order.dto;

import com.commerce_pro_backend.order.enums.OrderSource;
import com.commerce_pro_backend.order.enums.OrderStatus;
import com.commerce_pro_backend.order.enums.PaymentStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class OrderFilterDTO {

    private String search;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private OrderSource source;
    private String customerId;
    private Boolean isFlagged;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdTo;
}
