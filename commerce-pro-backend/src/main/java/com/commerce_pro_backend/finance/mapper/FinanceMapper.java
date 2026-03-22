package com.commerce_pro_backend.finance.mapper;

import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.entity.*;
import com.commerce_pro_backend.finance.enums.AccountType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Manual mapper — consistent with OrderMapper, FulfillmentMapper, CustomerMapper.
 */
@Component
public class FinanceMapper {

    // ── FinancialPeriod ───────────────────────────────────────────────────────

    public FinancialPeriodDTO toPeriodDTO(FinancialPeriod p) {
        if (p == null) return null;
        return FinancialPeriodDTO.builder()
                .id(p.getId()).fiscalYear(p.getFiscalYear()).periodNumber(p.getPeriodNumber())
                .periodName(p.getPeriodName()).periodType(p.getPeriodType())
                .startDate(p.getStartDate()).endDate(p.getEndDate())
                .status(p.getStatus()).closedAt(p.getClosedAt()).closedBy(p.getClosedBy())
                .lockedAt(p.getLockedAt()).notes(p.getNotes()).createdAt(p.getCreatedAt())
                .build();
    }

    public List<FinancialPeriodDTO> toPeriodDTOList(List<FinancialPeriod> list) {
        return list.stream().map(this::toPeriodDTO).toList();
    }

    // ── Vendor ────────────────────────────────────────────────────────────────

    public VendorDTO toVendorDTO(Vendor v) {
        if (v == null) return null;
        return VendorDTO.builder()
                .id(v.getId()).vendorCode(v.getVendorCode()).name(v.getName())
                .companyName(v.getCompanyName()).email(v.getEmail()).phone(v.getPhone())
                .website(v.getWebsite()).taxId(v.getTaxId()).status(v.getStatus())
                .addressLine1(v.getAddressLine1()).city(v.getCity()).state(v.getState())
                .country(v.getCountry()).paymentTerms(v.getPaymentTerms())
                .paymentTermsDays(v.getPaymentTermsDays()).preferredCurrency(v.getPreferredCurrency())
                .bankAccountLast4(v.getBankAccountLast4()).totalPayable(v.getTotalPayable())
                .totalPaid(v.getTotalPaid()).category(v.getCategory()).notes(v.getNotes())
                .tags(v.getTags()).createdAt(v.getCreatedAt()).updatedAt(v.getUpdatedAt())
                .build();
    }

    public VendorSummaryDTO toVendorSummaryDTO(Vendor v) {
        if (v == null) return null;
        return VendorSummaryDTO.builder()
                .id(v.getId()).vendorCode(v.getVendorCode()).name(v.getName())
                .email(v.getEmail()).status(v.getStatus()).paymentTerms(v.getPaymentTerms())
                .totalPayable(v.getTotalPayable()).category(v.getCategory()).createdAt(v.getCreatedAt())
                .build();
    }

    public List<VendorSummaryDTO> toVendorSummaryList(List<Vendor> list) {
        return list.stream().map(this::toVendorSummaryDTO).toList();
    }

    // ── CustomerInvoice ───────────────────────────────────────────────────────

    public CustomerInvoiceDTO toCustomerInvoiceDTO(CustomerInvoice inv) {
        if (inv == null) return null;
        List<CustomerInvoiceItemDTO> items = inv.getItems() != null
                ? inv.getItems().stream().map(this::toInvoiceItemDTO).toList() : List.of();
        return CustomerInvoiceDTO.builder()
                .id(inv.getId()).invoiceNumber(inv.getInvoiceNumber())
                .orderId(inv.getOrderId()).orderNumber(inv.getOrderNumber())
                .customerId(inv.getCustomerId()).customerName(inv.getCustomerName())
                .customerEmail(inv.getCustomerEmail()).status(inv.getStatus())
                .issuedAt(inv.getIssuedAt()).dueDate(inv.getDueDate()).paidAt(inv.getPaidAt())
                .subtotal(inv.getSubtotal()).discountAmount(inv.getDiscountAmount())
                .taxAmount(inv.getTaxAmount()).shippingAmount(inv.getShippingAmount())
                .totalAmount(inv.getTotalAmount()).paidAmount(inv.getPaidAmount())
                .balanceDue(inv.getBalanceDue()).currency(inv.getCurrency())
                .paymentTerms(inv.getPaymentTerms()).notes(inv.getNotes())
                .agingBucket(inv.computeAgingBucket()).overdue(inv.isOverdue())
                .items(items).createdAt(inv.getCreatedAt()).updatedAt(inv.getUpdatedAt())
                .build();
    }

    public CustomerInvoiceSummaryDTO toCustomerInvoiceSummaryDTO(CustomerInvoice inv) {
        if (inv == null) return null;
        return CustomerInvoiceSummaryDTO.builder()
                .id(inv.getId()).invoiceNumber(inv.getInvoiceNumber())
                .customerName(inv.getCustomerName()).customerEmail(inv.getCustomerEmail())
                .status(inv.getStatus()).dueDate(inv.getDueDate())
                .totalAmount(inv.getTotalAmount()).balanceDue(inv.getBalanceDue())
                .currency(inv.getCurrency()).overdue(inv.isOverdue())
                .agingBucket(inv.computeAgingBucket())
                .issuedAt(inv.getIssuedAt()).createdAt(inv.getCreatedAt())
                .build();
    }

    public List<CustomerInvoiceSummaryDTO> toCustomerInvoiceSummaryList(List<CustomerInvoice> list) {
        return list.stream().map(this::toCustomerInvoiceSummaryDTO).toList();
    }

    private CustomerInvoiceItemDTO toInvoiceItemDTO(CustomerInvoiceItem item) {
        return CustomerInvoiceItemDTO.builder()
                .id(item.getId()).description(item.getDescription())
                .quantity(item.getQuantity()).unitPrice(item.getUnitPrice())
                .discountAmount(item.getDiscountAmount()).taxRate(item.getTaxRate())
                .taxAmount(item.getTaxAmount()).lineTotal(item.getLineTotal())
                .sku(item.getSku()).sortOrder(item.getSortOrder())
                .build();
    }

    // ── VendorInvoice ─────────────────────────────────────────────────────────

    public VendorInvoiceDTO toVendorInvoiceDTO(VendorInvoice inv) {
        if (inv == null) return null;
        List<VendorInvoiceItemDTO> items = inv.getItems() != null
                ? inv.getItems().stream().map(this::toVendorItemDTO).toList() : List.of();
        return VendorInvoiceDTO.builder()
                .id(inv.getId()).vendorInvoiceNumber(inv.getVendorInvoiceNumber())
                .vendorReference(inv.getVendorReference())
                .vendorId(inv.getVendor() != null ? inv.getVendor().getId() : null)
                .vendorName(inv.getVendor() != null ? inv.getVendor().getName() : null)
                .status(inv.getStatus()).receivedAt(inv.getReceivedAt())
                .dueDate(inv.getDueDate()).scheduledPaymentDate(inv.getScheduledPaymentDate())
                .approvedAt(inv.getApprovedAt()).paidAt(inv.getPaidAt())
                .subtotal(inv.getSubtotal()).taxAmount(inv.getTaxAmount())
                .totalAmount(inv.getTotalAmount()).paidAmount(inv.getPaidAmount())
                .balanceDue(inv.getBalanceDue()).currency(inv.getCurrency())
                .earlyPaymentDiscountPct(inv.getEarlyPaymentDiscountPct())
                .earlyPaymentDiscountDays(inv.getEarlyPaymentDiscountDays())
                .description(inv.getDescription()).notes(inv.getNotes())
                .overdue(inv.isOverdue()).agingBucket(inv.computeAgingBucket())
                .items(items).createdAt(inv.getCreatedAt()).updatedAt(inv.getUpdatedAt())
                .build();
    }

    public VendorInvoiceSummaryDTO toVendorInvoiceSummaryDTO(VendorInvoice inv) {
        if (inv == null) return null;
        return VendorInvoiceSummaryDTO.builder()
                .id(inv.getId()).vendorInvoiceNumber(inv.getVendorInvoiceNumber())
                .vendorReference(inv.getVendorReference())
                .vendorName(inv.getVendor() != null ? inv.getVendor().getName() : null)
                .status(inv.getStatus()).dueDate(inv.getDueDate())
                .totalAmount(inv.getTotalAmount()).balanceDue(inv.getBalanceDue())
                .currency(inv.getCurrency()).overdue(inv.isOverdue())
                .agingBucket(inv.computeAgingBucket()).createdAt(inv.getCreatedAt())
                .build();
    }

    public List<VendorInvoiceSummaryDTO> toVendorInvoiceSummaryList(List<VendorInvoice> list) {
        return list.stream().map(this::toVendorInvoiceSummaryDTO).toList();
    }

    private VendorInvoiceItemDTO toVendorItemDTO(VendorInvoiceItem item) {
        return VendorInvoiceItemDTO.builder()
                .id(item.getId()).description(item.getDescription())
                .quantity(item.getQuantity()).unitPrice(item.getUnitPrice())
                .taxRate(item.getTaxRate()).taxAmount(item.getTaxAmount())
                .lineTotal(item.getLineTotal()).sortOrder(item.getSortOrder())
                .build();
    }

    // ── TaxRate ───────────────────────────────────────────────────────────────

    public TaxRateDTO toTaxRateDTO(TaxRate t) {
        if (t == null) return null;
        return TaxRateDTO.builder()
                .id(t.getId()).rateCode(t.getRateCode()).name(t.getName())
                .description(t.getDescription()).taxType(t.getTaxType())
                .rate(t.getRate()).rateDisplay(t.getRateDisplay())
                .jurisdictionId(t.getJurisdictionId()).country(t.getCountry())
                .stateProvince(t.getStateProvince()).applicability(t.getApplicability())
                .isCompound(t.getIsCompound()).isInclusive(t.getIsInclusive())
                .isActive(t.getIsActive()).effectiveFrom(t.getEffectiveFrom())
                .effectiveTo(t.getEffectiveTo()).createdAt(t.getCreatedAt())
                .build();
    }

    public List<TaxRateDTO> toTaxRateDTOList(List<TaxRate> list) {
        return list.stream().map(this::toTaxRateDTO).toList();
    }

    public TaxJurisdictionDTO toJurisdictionDTO(TaxJurisdiction j) {
        if (j == null) return null;
        return TaxJurisdictionDTO.builder()
                .id(j.getId()).jurisdictionCode(j.getJurisdictionCode())
                .name(j.getName()).country(j.getCountry()).stateProvince(j.getStateProvince())
                .isActive(j.getIsActive()).createdAt(j.getCreatedAt())
                .build();
    }

    public List<TaxJurisdictionDTO> toJurisdictionDTOList(List<TaxJurisdiction> list) {
        return list.stream().map(this::toJurisdictionDTO).toList();
    }

    // ── TaxReport ─────────────────────────────────────────────────────────────

    public TaxReportDTO toTaxReportDTO(TaxReport r) {
        if (r == null) return null;
        return TaxReportDTO.builder()
                .id(r.getId()).reportRef(r.getReportRef())
                .periodId(r.getPeriod() != null ? r.getPeriod().getId() : null)
                .periodName(r.getPeriod() != null ? r.getPeriod().getPeriodName() : null)
                .reportType(r.getReportType()).jurisdictionName(r.getJurisdictionName())
                .periodStart(r.getPeriodStart()).periodEnd(r.getPeriodEnd()).status(r.getStatus())
                .taxableSales(r.getTaxableSales()).exemptSales(r.getExemptSales())
                .totalSales(r.getTotalSales()).taxCollected(r.getTaxCollected())
                .taxPaidToVendors(r.getTaxPaidToVendors()).netTaxPayable(r.getNetTaxPayable())
                .orderCount(r.getOrderCount()).generatedAt(r.getGeneratedAt())
                .filedAt(r.getFiledAt()).notes(r.getNotes()).createdAt(r.getCreatedAt())
                .build();
    }

    public List<TaxReportDTO> toTaxReportDTOList(List<TaxReport> list) {
        return list.stream().map(this::toTaxReportDTO).toList();
    }

    // ── ExpenseCategory ───────────────────────────────────────────────────────

    public ExpenseCategoryDTO toExpenseCategoryDTO(ExpenseCategory c) {
        if (c == null) return null;
        return ExpenseCategoryDTO.builder()
                .id(c.getId()).categoryCode(c.getCategoryCode()).name(c.getName())
                .description(c.getDescription()).parentId(c.getParentId())
                .glAccountCode(c.getGlAccountCode()).isActive(c.getIsActive())
                .budgetDefault(c.getBudgetDefault()).createdAt(c.getCreatedAt())
                .build();
    }

    public List<ExpenseCategoryDTO> toExpenseCategoryDTOList(List<ExpenseCategory> list) {
        return list.stream().map(this::toExpenseCategoryDTO).toList();
    }

    // ── Expense ───────────────────────────────────────────────────────────────

    public ExpenseDTO toExpenseDTO(Expense e) {
        if (e == null) return null;
        return ExpenseDTO.builder()
                .id(e.getId()).expenseRef(e.getExpenseRef()).title(e.getTitle())
                .description(e.getDescription())
                .categoryId(e.getCategory() != null ? e.getCategory().getId() : null)
                .categoryName(e.getCategory() != null ? e.getCategory().getName() : null)
                .status(e.getStatus()).expenseDate(e.getExpenseDate())
                .vendorId(e.getVendorId()).vendorName(e.getVendorName())
                .amount(e.getAmount()).taxAmount(e.getTaxAmount()).totalAmount(e.getTotalAmount())
                .currency(e.getCurrency()).paymentMethod(e.getPaymentMethod())
                .receiptFilePath(e.getReceiptFilePath()).isRecurring(e.getIsRecurring())
                .recurrenceInterval(e.getRecurrenceInterval()).approvedBy(e.getApprovedBy())
                .approvedAt(e.getApprovedAt()).paidAt(e.getPaidAt()).notes(e.getNotes())
                .rejectionReason(e.getRejectionReason())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    public ExpenseSummaryDTO toExpenseSummaryDTO(Expense e) {
        if (e == null) return null;
        return ExpenseSummaryDTO.builder()
                .id(e.getId()).expenseRef(e.getExpenseRef()).title(e.getTitle())
                .categoryName(e.getCategory() != null ? e.getCategory().getName() : null)
                .status(e.getStatus()).expenseDate(e.getExpenseDate())
                .totalAmount(e.getTotalAmount()).currency(e.getCurrency())
                .isRecurring(e.getIsRecurring()).createdAt(e.getCreatedAt())
                .build();
    }

    public List<ExpenseSummaryDTO> toExpenseSummaryList(List<Expense> list) {
        return list.stream().map(this::toExpenseSummaryDTO).toList();
    }

    // ── JournalEntry ──────────────────────────────────────────────────────────

    public JournalEntryDTO toJournalEntryDTO(JournalEntry j) {
        if (j == null) return null;
        List<JournalEntryLineDTO> lines = j.getLines() != null
                ? j.getLines().stream().map(this::toJournalLineDTO).toList() : List.of();
        return JournalEntryDTO.builder()
                .id(j.getId()).entryRef(j.getEntryRef())
                .periodId(j.getPeriod() != null ? j.getPeriod().getId() : null)
                .periodName(j.getPeriod() != null ? j.getPeriod().getPeriodName() : null)
                .entryType(j.getEntryType()).status(j.getStatus()).entryDate(j.getEntryDate())
                .description(j.getDescription()).reference(j.getReference())
                .currency(j.getCurrency()).sourceType(j.getSourceType()).sourceId(j.getSourceId())
                .postedBy(j.getPostedBy()).reversalReason(j.getReversalReason()).notes(j.getNotes())
                .lines(lines).createdAt(j.getCreatedAt())
                .build();
    }

    public JournalEntrySummaryDTO toJournalEntrySummaryDTO(JournalEntry j) {
        if (j == null) return null;
        BigDecimal totalDebits = j.getLines() != null ? j.getLines().stream()
                .filter(l -> l.getAccountType() == AccountType.DEBIT)
                .map(JournalEntryLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
        return JournalEntrySummaryDTO.builder()
                .id(j.getId()).entryRef(j.getEntryRef()).entryType(j.getEntryType())
                .status(j.getStatus()).entryDate(j.getEntryDate()).description(j.getDescription())
                .reference(j.getReference()).totalDebits(totalDebits).createdAt(j.getCreatedAt())
                .build();
    }

    public List<JournalEntrySummaryDTO> toJournalEntrySummaryList(List<JournalEntry> list) {
        return list.stream().map(this::toJournalEntrySummaryDTO).toList();
    }

    private JournalEntryLineDTO toJournalLineDTO(JournalEntryLine l) {
        return JournalEntryLineDTO.builder()
                .id(l.getId()).accountType(l.getAccountType()).accountCode(l.getAccountCode())
                .accountName(l.getAccountName()).amount(l.getAmount()).currency(l.getCurrency())
                .description(l.getDescription()).sortOrder(l.getSortOrder())
                .build();
    }

    // ── Budget ────────────────────────────────────────────────────────────────

    public BudgetDTO toBudgetDTO(Budget b, BigDecimal revenueActual, BigDecimal expenseActual) {
        if (b == null) return null;
        List<BudgetLineDTO> lines = b.getLines() != null
                ? b.getLines().stream().map(this::toBudgetLineDTO).toList() : List.of();
        BigDecimal netBudget = (b.getTotalRevenueBudget() != null ? b.getTotalRevenueBudget() : BigDecimal.ZERO)
                .subtract(b.getTotalExpenseBudget() != null ? b.getTotalExpenseBudget() : BigDecimal.ZERO);
        BigDecimal netActual = (revenueActual != null ? revenueActual : BigDecimal.ZERO)
                .subtract(expenseActual != null ? expenseActual : BigDecimal.ZERO);
        return BudgetDTO.builder()
                .id(b.getId()).name(b.getName()).description(b.getDescription())
                .fiscalYear(b.getFiscalYear())
                .periodId(b.getPeriod() != null ? b.getPeriod().getId() : null)
                .periodName(b.getPeriod() != null ? b.getPeriod().getPeriodName() : null)
                .status(b.getStatus()).totalRevenueBudget(b.getTotalRevenueBudget())
                .totalExpenseBudget(b.getTotalExpenseBudget())
                .totalRevenueActual(revenueActual).totalExpenseActual(expenseActual)
                .netBudget(netBudget).netActual(netActual)
                .netVariance(netActual.subtract(netBudget))
                .currency(b.getCurrency()).approvedAt(b.getApprovedAt()).notes(b.getNotes())
                .lines(lines).createdAt(b.getCreatedAt())
                .build();
    }

    private BudgetLineDTO toBudgetLineDTO(BudgetLine l) {
        return BudgetLineDTO.builder()
                .id(l.getId()).lineName(l.getLineName()).categoryId(l.getCategoryId())
                .lineType(l.getLineType()).budgetedAmount(l.getBudgetedAmount())
                .actualAmount(l.getActualAmount()).variance(l.getVariance())
                .variancePct(l.getVariancePct()).notes(l.getNotes()).sortOrder(l.getSortOrder())
                .build();
    }

    // ── ExchangeRate ──────────────────────────────────────────────────────────

    public ExchangeRateDTO toExchangeRateDTO(ExchangeRate r) {
        if (r == null) return null;
        return ExchangeRateDTO.builder()
                .id(r.getId()).baseCurrency(r.getBaseCurrency()).quoteCurrency(r.getQuoteCurrency())
                .rate(r.getRate()).rateDate(r.getRateDate()).source(r.getSource())
                .createdAt(r.getCreatedAt())
                .build();
    }

    public List<ExchangeRateDTO> toExchangeRateDTOList(List<ExchangeRate> list) {
        return list.stream().map(this::toExchangeRateDTO).toList();
    }
}
