package com.commerce_pro_backend.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tax_reports", indexes = {
    @Index(name = "idx_taxrep_period", columnList = "period_id"),
    @Index(name = "idx_taxrep_type",   columnList = "report_type"),
    @Index(name = "idx_taxrep_status", columnList = "status")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TaxReport {

    @Id @UuidGenerator @Column(updatable = false, nullable = false) private String id;
    @Column(name = "report_ref", unique = true, length = 30)  private String reportRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id") private FinancialPeriod period;

    @Column(name = "report_type", length = 30)       private String reportType;   // SALES_TAX, VAT, GST
    @Column(name = "jurisdiction_id", length = 36)   private String jurisdictionId;
    @Column(name = "jurisdiction_name", length = 100) private String jurisdictionName;
    @Column(name = "period_start", nullable = false) private LocalDateTime periodStart;
    @Column(name = "period_end", nullable = false)   private LocalDateTime periodEnd;
    @Column(name = "status", length = 20)            @Builder.Default private String status = "DRAFT";

    @Column(name = "taxable_sales", precision = 19, scale = 4)    @Builder.Default private BigDecimal taxableSales = BigDecimal.ZERO;
    @Column(name = "exempt_sales", precision = 19, scale = 4)     @Builder.Default private BigDecimal exemptSales = BigDecimal.ZERO;
    @Column(name = "total_sales", precision = 19, scale = 4)      @Builder.Default private BigDecimal totalSales = BigDecimal.ZERO;
    @Column(name = "tax_collected", precision = 19, scale = 4)    @Builder.Default private BigDecimal taxCollected = BigDecimal.ZERO;
    @Column(name = "tax_paid_to_vendors", precision = 19, scale = 4) @Builder.Default private BigDecimal taxPaidToVendors = BigDecimal.ZERO;
    @Column(name = "net_tax_payable", precision = 19, scale = 4)  @Builder.Default private BigDecimal netTaxPayable = BigDecimal.ZERO;
    @Column(name = "order_count", nullable = false)               @Builder.Default private Long orderCount = 0L;
    @Column(name = "tax_breakdown", columnDefinition = "TEXT")    private String taxBreakdown; // JSON: {"0.085": 12345.67}

    @Column(name = "generated_at") private LocalDateTime generatedAt;
    @Column(name = "generated_by", length = 36) private String generatedBy;
    @Column(name = "filed_at")     private LocalDateTime filedAt;
    @Column(name = "filed_by", length = 36)     private String filedBy;
    @Column(name = "notes", length = 2000)      private String notes;

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at")                       private LocalDateTime updatedAt;
}
