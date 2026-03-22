package com.commerce_pro_backend.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_rates", indexes = {
    @Index(name = "idx_er_pair_date", columnList = "base_currency, quote_currency, rate_date", unique = true)
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ExchangeRate {

    @Id @UuidGenerator @Column(updatable = false, nullable = false) private String id;
    @Column(name = "base_currency", nullable = false, length = 3)  private String baseCurrency;
    @Column(name = "quote_currency", nullable = false, length = 3) private String quoteCurrency;
    @Column(name = "rate", nullable = false, precision = 19, scale = 6) private BigDecimal rate;
    @Column(name = "rate_date", nullable = false) private LocalDate rateDate;
    @Column(name = "source", length = 50) private String source;  // MANUAL, ECB, OPENEXCHANGE

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "created_by", updatable = false, length = 36)       private String createdBy;
}
