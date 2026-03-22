package com.commerce_pro_backend.finance.dto;

import com.commerce_pro_backend.finance.enums.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * All Finance module DTOs — requests and responses in a single file.
 * Consistent with the project's DTO organisation pattern.
 */
public class FinanceDTOs {

    // ══════════════════════════════════════════════════════════════════════════
    // REQUEST DTOs
    // ══════════════════════════════════════════════════════════════════════════

    @Data public static class CreatePeriodRequest {
        @NotNull  private Integer fiscalYear;
        @NotNull  private Integer periodNumber;
        @NotBlank private String  periodName;
        @NotNull  private LocalDate startDate;
        @NotNull  private LocalDate endDate;
        private String periodType;
        private String notes;
    }

    @Data public static class CreateVendorRequest {
        @NotBlank               private String name;
        private String          companyName;
        @NotBlank @Email        private String email;
        private String          phone;
        private String          taxId;
        @NotBlank               private String paymentTerms;
        private Integer         paymentTermsDays;
        private String          preferredCurrency;
        private String          addressLine1;
        private String          addressLine2;
        private String          city;
        private String          state;
        private String          postalCode;
        private String          country;
        private String          category;
        private String          notes;
        private String          tags;
    }

    @Data public static class UpdateVendorRequest {
        @NotBlank private String name;
        private String  companyName;
        private String  email;
        private String  phone;
        private String  taxId;
        private String  paymentTerms;
        private Integer paymentTermsDays;
        private String  preferredCurrency;
        private String  addressLine1;
        private String  city;
        private String  state;
        private String  postalCode;
        private String  country;
        private String  category;
        private String  notes;
    }

    @Data public static class CreateCustomerInvoiceRequest {
        @NotBlank               private String customerId;
        private String          orderId;
        @NotNull                private LocalDate dueDate;
        @NotBlank               private String paymentTerms;
        private String          currency;
        private String          taxJurisdictionId;
        private String          notes;
        @NotEmpty               private List<InvoiceItemRequest> items;
    }

    @Data public static class InvoiceItemRequest {
        private String  orderItemId;
        private String  productId;
        @NotBlank       private String description;
        @NotNull @Min(1) private Integer quantity;
        @NotNull @DecimalMin("0.01") private BigDecimal unitPrice;
        private BigDecimal discountAmount;
        private String  taxRateId;
        private Integer sortOrder;
    }

    @Data public static class RecordPaymentRequest {
        @NotNull @DecimalMin("0.01") private BigDecimal amount;
        @NotNull                     private LocalDate  paymentDate;
        private String paymentMethod;
        private String reference;
        private String notes;
    }

    @Data public static class VoidInvoiceRequest {
        @NotBlank private String reason;
    }

    @Data public static class CreateVendorInvoiceRequest {
        @NotBlank               private String vendorId;
        @NotBlank               private String vendorReference;
        @NotNull                private LocalDate dueDate;
        private String          description;
        private String          currency;
        private String          notes;
        private BigDecimal      earlyPaymentDiscountPct;
        private Integer         earlyPaymentDiscountDays;
        @NotEmpty               private List<VendorInvoiceItemRequest> items;
    }

    @Data public static class VendorInvoiceItemRequest {
        @NotBlank       private String description;
        @NotNull @Min(1) private Integer quantity;
        @NotNull @DecimalMin("0.01") private BigDecimal unitPrice;
        private BigDecimal taxRate;
        private String     expenseCategoryId;
        private Integer    sortOrder;
    }

    @Data public static class SchedulePaymentRequest {
        @NotNull private LocalDate scheduledPaymentDate;
        private String notes;
    }

    @Data public static class CreateTaxRateRequest {
        @NotBlank  private String         rateCode;
        @NotBlank  private String         name;
        private String                    description;
        @NotNull   private TaxType        taxType;
        @NotNull @DecimalMin("0") @DecimalMax("1") private BigDecimal rate;
        private String                    jurisdictionId;
        private String                    country;
        private String                    stateProvince;
        private TaxApplicability          applicability;
        private Boolean                   isCompound;
        private Boolean                   isInclusive;
        private LocalDate                 effectiveFrom;
        private LocalDate                 effectiveTo;
    }

    @Data public static class CreateJurisdictionRequest {
        @NotBlank private String jurisdictionCode;
        @NotBlank private String name;
        private String country;
        private String stateProvince;
        private String notes;
    }

    @Data public static class GenerateTaxReportRequest {
        @NotBlank private String periodId;
        @NotBlank private String reportType;
        private String jurisdictionId;
        private String notes;
    }

    @Data public static class CreateExpenseCategoryRequest {
        @NotBlank private String categoryCode;
        @NotBlank private String name;
        private String description;
        private String parentId;
        private String glAccountCode;
        private BigDecimal budgetDefault;
    }

    @Data public static class CreateExpenseRequest {
        @NotBlank              private String title;
        @NotBlank              private String categoryId;
        private String         vendorId;
        @NotNull               private LocalDate expenseDate;
        @NotNull @DecimalMin("0.01") private BigDecimal amount;
        private BigDecimal     taxAmount;
        private String         currency;
        private String         paymentMethod;
        private String         notes;
        private Boolean        isRecurring;
        private String         recurrenceInterval;
    }

    @Data public static class ReviewExpenseRequest {
        private String notes;
    }

    @Data public static class CreateJournalEntryRequest {
        @NotNull   private JournalEntryType entryType;
        @NotNull   private LocalDate entryDate;
        @NotBlank  private String description;
        private String reference;
        private String currency;
        private String periodId;
        private String notes;
        @NotEmpty  private List<JournalLineRequest> lines;
    }

    @Data public static class JournalLineRequest {
        @NotNull   private AccountType accountType;
        @NotBlank  private String accountCode;
        @NotBlank  private String accountName;
        @NotNull @DecimalMin("0.01") private BigDecimal amount;
        private String currency;
        private String description;
        private Integer sortOrder;
    }

    @Data public static class ReverseEntryRequest {
        @NotBlank private String reason;
    }

    @Data public static class CreateBudgetRequest {
        @NotBlank  private String name;
        @NotNull   private Integer fiscalYear;
        private String periodId;
        private String description;
        private String currency;
        @NotEmpty  private List<BudgetLineRequest> lines;
    }

    @Data public static class BudgetLineRequest {
        @NotBlank                    private String lineName;
        private String               categoryId;
        private String               accountCode;
        private String               lineType;
        @NotNull @DecimalMin("0")    private BigDecimal budgetedAmount;
        private String               notes;
        private Integer              sortOrder;
    }

    @Data public static class AddExchangeRateRequest {
        @NotBlank @Size(min=3,max=3) private String baseCurrency;
        @NotBlank @Size(min=3,max=3) private String quoteCurrency;
        @NotNull @DecimalMin("0.000001") private BigDecimal rate;
        @NotNull                     private LocalDate rateDate;
        private String               source;
    }

    @Data public static class FinanceFilterDTO {
        private String search;
        private String status;
        private String customerId;
        private String vendorId;
        private String periodId;
        private LocalDate from;
        private LocalDate to;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RESPONSE DTOs
    // ══════════════════════════════════════════════════════════════════════════

    // ── Financial Period ──────────────────────────────────────────────────────

    @Data @Builder public static class FinancialPeriodDTO {
        private String id;
        private Integer fiscalYear;
        private Integer periodNumber;
        private String periodName;
        private String periodType;
        private LocalDate startDate;
        private LocalDate endDate;
        private PeriodStatus status;
        private LocalDateTime closedAt;
        private String closedBy;
        private LocalDateTime lockedAt;
        private String notes;
        private LocalDateTime createdAt;
    }

    // ── Vendor ────────────────────────────────────────────────────────────────

    @Data @Builder public static class VendorDTO {
        private String id;
        private String vendorCode;
        private String name;
        private String companyName;
        private String email;
        private String phone;
        private String website;
        private String taxId;
        private VendorStatus status;
        private String addressLine1;
        private String city;
        private String state;
        private String country;
        private String paymentTerms;
        private Integer paymentTermsDays;
        private String preferredCurrency;
        private String bankAccountLast4;
        private BigDecimal totalPayable;
        private BigDecimal totalPaid;
        private String category;
        private String notes;
        private String tags;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data @Builder public static class VendorSummaryDTO {
        private String id;
        private String vendorCode;
        private String name;
        private String email;
        private VendorStatus status;
        private String paymentTerms;
        private BigDecimal totalPayable;
        private String category;
        private LocalDateTime createdAt;
    }

    // ── Customer Invoice ──────────────────────────────────────────────────────

    @Data @Builder public static class CustomerInvoiceDTO {
        private String id;
        private String invoiceNumber;
        private String orderId;
        private String orderNumber;
        private String customerId;
        private String customerName;
        private String customerEmail;
        private InvoiceStatus status;
        private LocalDateTime issuedAt;
        private LocalDate dueDate;
        private LocalDateTime paidAt;
        private BigDecimal subtotal;
        private BigDecimal discountAmount;
        private BigDecimal taxAmount;
        private BigDecimal shippingAmount;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private BigDecimal balanceDue;
        private String currency;
        private String paymentTerms;
        private String notes;
        private int agingBucket;
        private boolean overdue;
        private List<CustomerInvoiceItemDTO> items;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data @Builder public static class CustomerInvoiceSummaryDTO {
        private String id;
        private String invoiceNumber;
        private String customerName;
        private String customerEmail;
        private InvoiceStatus status;
        private LocalDate dueDate;
        private BigDecimal totalAmount;
        private BigDecimal balanceDue;
        private String currency;
        private boolean overdue;
        private int agingBucket;
        private LocalDateTime issuedAt;
        private LocalDateTime createdAt;
    }

    @Data @Builder public static class CustomerInvoiceItemDTO {
        private String id;
        private String description;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal discountAmount;
        private BigDecimal taxRate;
        private BigDecimal taxAmount;
        private BigDecimal lineTotal;
        private String sku;
        private Integer sortOrder;
    }

    // ── AR Summary ────────────────────────────────────────────────────────────

    @Data @Builder public static class ARSummaryDTO {
        private BigDecimal totalOutstanding;
        private BigDecimal current;
        private BigDecimal days1To30;
        private BigDecimal days31To60;
        private BigDecimal days61To90;
        private BigDecimal days90Plus;
        private long overdueInvoiceCount;
        private BigDecimal overdueAmount;
        private BigDecimal dsoDays;          // Days Sales Outstanding
        private List<CustomerInvoiceSummaryDTO> overdueInvoices;
    }

    // ── Vendor Invoice ────────────────────────────────────────────────────────

    @Data @Builder public static class VendorInvoiceDTO {
        private String id;
        private String vendorInvoiceNumber;
        private String vendorReference;
        private String vendorId;
        private String vendorName;
        private VendorInvoiceStatus status;
        private LocalDateTime receivedAt;
        private LocalDate dueDate;
        private LocalDate scheduledPaymentDate;
        private LocalDateTime approvedAt;
        private LocalDateTime paidAt;
        private BigDecimal subtotal;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private BigDecimal balanceDue;
        private String currency;
        private BigDecimal earlyPaymentDiscountPct;
        private Integer earlyPaymentDiscountDays;
        private String description;
        private String notes;
        private boolean overdue;
        private int agingBucket;
        private List<VendorInvoiceItemDTO> items;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data @Builder public static class VendorInvoiceSummaryDTO {
        private String id;
        private String vendorInvoiceNumber;
        private String vendorReference;
        private String vendorName;
        private VendorInvoiceStatus status;
        private LocalDate dueDate;
        private BigDecimal totalAmount;
        private BigDecimal balanceDue;
        private String currency;
        private boolean overdue;
        private int agingBucket;
        private LocalDateTime createdAt;
    }

    @Data @Builder public static class VendorInvoiceItemDTO {
        private String id;
        private String description;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal taxRate;
        private BigDecimal taxAmount;
        private BigDecimal lineTotal;
        private Integer sortOrder;
    }

    // ── AP Summary ────────────────────────────────────────────────────────────

    @Data @Builder public static class APSummaryDTO {
        private BigDecimal totalOutstanding;
        private BigDecimal current;
        private BigDecimal days1To30;
        private BigDecimal days31To60;
        private BigDecimal days61To90;
        private BigDecimal days90Plus;
        private long overdueInvoiceCount;
        private BigDecimal overdueAmount;
        private BigDecimal dpoDays;           // Days Payable Outstanding
        private List<VendorInvoiceSummaryDTO> upcomingPayments;
        private List<VendorInvoiceSummaryDTO> overdueInvoices;
    }

    // ── Tax ───────────────────────────────────────────────────────────────────

    @Data @Builder public static class TaxRateDTO {
        private String id;
        private String rateCode;
        private String name;
        private String description;
        private TaxType taxType;
        private BigDecimal rate;
        private String rateDisplay;
        private String jurisdictionId;
        private String country;
        private String stateProvince;
        private TaxApplicability applicability;
        private Boolean isCompound;
        private Boolean isInclusive;
        private Boolean isActive;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private LocalDateTime createdAt;
    }

    @Data @Builder public static class TaxJurisdictionDTO {
        private String id;
        private String jurisdictionCode;
        private String name;
        private String country;
        private String stateProvince;
        private Boolean isActive;
        private LocalDateTime createdAt;
    }

    @Data @Builder public static class TaxReportDTO {
        private String id;
        private String reportRef;
        private String periodId;
        private String periodName;
        private String reportType;
        private String jurisdictionName;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;
        private String status;
        private BigDecimal taxableSales;
        private BigDecimal exemptSales;
        private BigDecimal totalSales;
        private BigDecimal taxCollected;
        private BigDecimal taxPaidToVendors;
        private BigDecimal netTaxPayable;
        private Long orderCount;
        private Map<String, BigDecimal> taxBreakdownMap;
        private LocalDateTime generatedAt;
        private LocalDateTime filedAt;
        private String notes;
        private LocalDateTime createdAt;
    }

    @Data @Builder public static class TaxSummaryDTO {
        private BigDecimal totalTaxCollected;
        private BigDecimal totalTaxPaidToVendors;
        private BigDecimal netTaxLiability;
        private Map<String, BigDecimal> taxByRate;
        private Map<String, BigDecimal> taxByJurisdiction;
        private Long totalOrders;
    }

    // ── Expense ───────────────────────────────────────────────────────────────

    @Data @Builder public static class ExpenseCategoryDTO {
        private String id;
        private String categoryCode;
        private String name;
        private String description;
        private String parentId;
        private String glAccountCode;
        private Boolean isActive;
        private BigDecimal budgetDefault;
        private LocalDateTime createdAt;
    }

    @Data @Builder public static class ExpenseDTO {
        private String id;
        private String expenseRef;
        private String title;
        private String description;
        private String categoryId;
        private String categoryName;
        private ExpenseStatus status;
        private LocalDate expenseDate;
        private String vendorId;
        private String vendorName;
        private BigDecimal amount;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
        private String currency;
        private String paymentMethod;
        private String receiptFilePath;
        private Boolean isRecurring;
        private String recurrenceInterval;
        private String approvedBy;
        private LocalDateTime approvedAt;
        private LocalDateTime paidAt;
        private String notes;
        private String rejectionReason;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data @Builder public static class ExpenseSummaryDTO {
        private String id;
        private String expenseRef;
        private String title;
        private String categoryName;
        private ExpenseStatus status;
        private LocalDate expenseDate;
        private BigDecimal totalAmount;
        private String currency;
        private Boolean isRecurring;
        private LocalDateTime createdAt;
    }

    @Data @Builder public static class ExpenseStatsDTO {
        private BigDecimal totalMTD;
        private BigDecimal totalYTD;
        private long pendingCount;
        private BigDecimal pendingAmount;
        private Map<String, BigDecimal> byCategory;
    }

    // ── Journal Entry ─────────────────────────────────────────────────────────

    @Data @Builder public static class JournalEntryDTO {
        private String id;
        private String entryRef;
        private String periodId;
        private String periodName;
        private JournalEntryType entryType;
        private String status;
        private LocalDate entryDate;
        private String description;
        private String reference;
        private String currency;
        private String sourceType;
        private String sourceId;
        private String postedBy;
        private String reversalReason;
        private String notes;
        private List<JournalEntryLineDTO> lines;
        private LocalDateTime createdAt;
    }

    @Data @Builder public static class JournalEntrySummaryDTO {
        private String id;
        private String entryRef;
        private JournalEntryType entryType;
        private String status;
        private LocalDate entryDate;
        private String description;
        private String reference;
        private BigDecimal totalDebits;
        private LocalDateTime createdAt;
    }

    @Data @Builder public static class JournalEntryLineDTO {
        private String id;
        private AccountType accountType;
        private String accountCode;
        private String accountName;
        private BigDecimal amount;
        private String currency;
        private String description;
        private Integer sortOrder;
    }

    // ── Budget ────────────────────────────────────────────────────────────────

    @Data @Builder public static class BudgetDTO {
        private String id;
        private String name;
        private String description;
        private Integer fiscalYear;
        private String periodId;
        private String periodName;
        private BudgetStatus status;
        private BigDecimal totalRevenueBudget;
        private BigDecimal totalExpenseBudget;
        private BigDecimal totalRevenueActual;
        private BigDecimal totalExpenseActual;
        private BigDecimal netBudget;
        private BigDecimal netActual;
        private BigDecimal netVariance;
        private String currency;
        private LocalDateTime approvedAt;
        private String notes;
        private List<BudgetLineDTO> lines;
        private LocalDateTime createdAt;
    }

    @Data @Builder public static class BudgetLineDTO {
        private String id;
        private String lineName;
        private String categoryId;
        private String categoryName;
        private String lineType;
        private BigDecimal budgetedAmount;
        private BigDecimal actualAmount;
        private BigDecimal variance;
        private BigDecimal variancePct;
        private String notes;
        private Integer sortOrder;
    }

    // ── Revenue Overview ──────────────────────────────────────────────────────

    @Data @Builder public static class RevenueOverviewDTO {
        private BigDecimal grossRevenueToday;
        private BigDecimal grossRevenueMTD;
        private BigDecimal grossRevenueYTD;
        private BigDecimal netRevenueMTD;
        private BigDecimal netRevenueYTD;
        private BigDecimal totalRefundsMTD;
        private BigDecimal totalTaxCollectedMTD;
        private BigDecimal totalShippingRevenueMTD;
        private long ordersToday;
        private long ordersMTD;
        private BigDecimal averageOrderValue;
        private BigDecimal revenueGrowthPct;
        private BigDecimal outstandingAR;
        private BigDecimal overdueAR;
        private BigDecimal outstandingAP;
        private BigDecimal overdueAP;
        private BigDecimal totalExpensesMTD;
        private BigDecimal grossProfitMTD;
        private BigDecimal grossMarginPct;
        private List<RevenueTrendPointDTO> trend;
    }

    @Data @Builder public static class RevenueTrendPointDTO {
        private String period;
        private BigDecimal grossRevenue;
        private BigDecimal netRevenue;
        private BigDecimal expenses;
    }

    // ── P&L ───────────────────────────────────────────────────────────────────

    @Data @Builder public static class ProfitLossDTO {
        private String periodLabel;
        private LocalDate startDate;
        private LocalDate endDate;
        // Revenue
        private BigDecimal grossRevenue;
        private BigDecimal salesReturns;
        private BigDecimal netRevenue;
        // COGS
        private BigDecimal cogs;
        private BigDecimal grossProfit;
        private BigDecimal grossMarginPct;
        // Operating Expenses
        private BigDecimal totalOperatingExpenses;
        private Map<String, BigDecimal> expenseByCategory;
        // Other
        private BigDecimal totalShipping;
        private BigDecimal gatewayFees;
        private BigDecimal chargebacks;
        // Net Income
        private BigDecimal ebitda;
        private BigDecimal totalTax;
        private BigDecimal netIncome;
        private BigDecimal netMarginPct;
        // Comparisons
        private BigDecimal previousPeriodNetIncome;
        private BigDecimal netIncomeChangePct;
        private BigDecimal budgetedNetIncome;
        private BigDecimal budgetVariance;
    }

    // ── Cash Flow ─────────────────────────────────────────────────────────────

    @Data @Builder public static class CashFlowForecastDTO {
        private BigDecimal totalArDue30Days;
        private BigDecimal totalArDue60Days;
        private BigDecimal totalApDue30Days;
        private BigDecimal totalApDue60Days;
        private BigDecimal projectedNetCash30Days;
        private BigDecimal projectedNetCash60Days;
        private BigDecimal projectedNetCash90Days;
        private List<CashFlowProjectionDTO> projections;
    }

    @Data @Builder public static class CashFlowProjectionDTO {
        private String period;
        private BigDecimal expectedInflows;
        private BigDecimal expectedOutflows;
        private BigDecimal netCashFlow;
    }

    // ── Exchange Rate ─────────────────────────────────────────────────────────

    @Data @Builder public static class ExchangeRateDTO {
        private String id;
        private String baseCurrency;
        private String quoteCurrency;
        private BigDecimal rate;
        private LocalDate rateDate;
        private String source;
        private LocalDateTime createdAt;
    }

    @Data @Builder public static class ConversionResultDTO {
        private BigDecimal originalAmount;
        private String fromCurrency;
        private String toCurrency;
        private BigDecimal rate;
        private BigDecimal convertedAmount;
        private LocalDate rateDate;
    }
}
