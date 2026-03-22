package com.commerce_pro_backend.common.data;

import com.commerce_pro_backend.customer.entity.Customer;
import com.commerce_pro_backend.customer.repository.CustomerRepository;
import com.commerce_pro_backend.finance.entity.*;
import com.commerce_pro_backend.finance.enums.*;
import com.commerce_pro_backend.finance.repository.*;
import com.commerce_pro_backend.finance.service.InvoiceNumberService;
import com.commerce_pro_backend.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds all Finance module reference data and sample records from JSON files.
 * Runs after the main DataInitializer (@Order 9+).
 *
 * JSON sources (src/main/resources/seed/):
 *   tax-jurisdictions.json, tax-rates.json, expense-categories.json,
 *   vendors.json, customer-invoices.json, vendor-invoices.json,
 *   expenses.json, budget.json, exchange-rates.json
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class FinanceDataSeeder {

    private final SeedDataLoader              seedDataLoader;
    private final FinancialPeriodRepository   periodRepository;
    private final TaxJurisdictionRepository   jurisdictionRepository;
    private final TaxRateRepository           taxRateRepository;
    private final ExpenseCategoryRepository   categoryRepository;
    private final VendorRepository            vendorRepository;
    private final CustomerInvoiceRepository   customerInvoiceRepository;
    private final VendorInvoiceRepository     vendorInvoiceRepository;
    private final ExpenseRepository           expenseRepository;
    private final BudgetRepository            budgetRepository;
    private final ExchangeRateRepository      exchangeRateRepository;
    private final InvoiceNumberService        invoiceNumberService;
    private final CustomerRepository          customerRepository;
    private final OrderRepository             orderRepository;

    // ── Periods ───────────────────────────────────────────────────────────────

    @Bean
    @Profile("dev")
    @Order(9)
    CommandLineRunner seedFinancialPeriods() {
        return args -> {
            if (periodRepository.count() > 0) {
                log.info("[FinanceSeeder] Financial periods already seeded — skipping");
                return;
            }
            LocalDate now  = LocalDate.now();
            LocalDate prev = now.minusMonths(1);

            periodRepository.save(FinancialPeriod.builder()
                    .fiscalYear(prev.getYear()).periodNumber(prev.getMonthValue())
                    .periodName(prev.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + prev.getYear())
                    .periodType("MONTHLY")
                    .startDate(prev.withDayOfMonth(1))
                    .endDate(prev.withDayOfMonth(prev.lengthOfMonth()))
                    .status(PeriodStatus.CLOSED)
                    .closedAt(LocalDateTime.now().minusDays(5))
                    .closedBy("SYSTEM").createdBy("SYSTEM")
                    .build());

            periodRepository.save(FinancialPeriod.builder()
                    .fiscalYear(now.getYear()).periodNumber(now.getMonthValue())
                    .periodName(now.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + now.getYear())
                    .periodType("MONTHLY")
                    .startDate(now.withDayOfMonth(1))
                    .endDate(now.withDayOfMonth(now.lengthOfMonth()))
                    .status(PeriodStatus.OPEN)
                    .createdBy("SYSTEM")
                    .build());

            log.info("[FinanceSeeder] Seeded 2 financial periods");
        };
    }

    // ── Tax Jurisdictions & Rates ─────────────────────────────────────────────

    @Bean
    @Profile("dev")
    @Order(10)
    CommandLineRunner seedTaxData() {
        return args -> {
            if (taxRateRepository.count() > 0) {
                log.info("[FinanceSeeder] Tax rates already seeded — skipping");
                return;
            }

            // Jurisdictions
            JsonNode jurJson = seedDataLoader.loadTree("seed/tax-jurisdictions.json");
            Map<String, String> codeToId = new HashMap<>();
            for (JsonNode j : jurJson) {
                TaxJurisdiction saved = jurisdictionRepository.save(TaxJurisdiction.builder()
                        .jurisdictionCode(j.get("code").asText())
                        .name(j.get("name").asText())
                        .country(j.get("country").asText())
                        .stateProvince(j.has("stateProvince") && !j.get("stateProvince").isNull() ? j.get("stateProvince").asText() : null)
                        .isActive(true).createdBy("SYSTEM").build());
                codeToId.put(saved.getJurisdictionCode(), saved.getId());
            }
            log.info("[FinanceSeeder] Seeded {} tax jurisdictions", jurJson.size());

            // Tax Rates
            JsonNode ratesJson = seedDataLoader.loadTree("seed/tax-rates.json");
            for (JsonNode r : ratesJson) {
                String jurCode = r.has("jurisdictionCode") && !r.get("jurisdictionCode").isNull() ? r.get("jurisdictionCode").asText() : null;
                taxRateRepository.save(TaxRate.builder()
                        .rateCode(r.get("rateCode").asText())
                        .name(r.get("name").asText())
                        .taxType(TaxType.valueOf(r.get("taxType").asText()))
                        .rate(new BigDecimal(r.get("rate").asText()))
                        .country(r.get("country").asText())
                        .stateProvince(r.has("stateProvince") && !r.get("stateProvince").isNull() ? r.get("stateProvince").asText() : null)
                        .jurisdictionId(jurCode != null ? codeToId.get(jurCode) : null)
                        .applicability(TaxApplicability.ALL)
                        .isCompound(false).isInclusive(false).isActive(true)
                        .createdBy("SYSTEM").updatedBy("SYSTEM").build());
            }
            log.info("[FinanceSeeder] Seeded {} tax rates", ratesJson.size());
        };
    }

    // ── Expense Categories ────────────────────────────────────────────────────

    @Bean
    @Profile("dev")
    @Order(11)
    CommandLineRunner seedExpenseCategories() {
        return args -> {
            if (categoryRepository.count() > 0) {
                log.info("[FinanceSeeder] Expense categories already seeded — skipping");
                return;
            }
            JsonNode catsJson = seedDataLoader.loadTree("seed/expense-categories.json");
            for (JsonNode c : catsJson) {
                categoryRepository.save(ExpenseCategory.builder()
                        .categoryCode(c.get("categoryCode").asText())
                        .name(c.get("name").asText())
                        .glAccountCode(c.get("glAccountCode").asText())
                        .isActive(true)
                        .budgetDefault(new BigDecimal(c.get("budgetDefault").asText()))
                        .createdBy("SYSTEM").build());
            }
            log.info("[FinanceSeeder] Seeded {} expense categories", catsJson.size());
        };
    }

    // ── Vendors ───────────────────────────────────────────────────────────────

    @Bean
    @Profile("dev")
    @Order(12)
    CommandLineRunner seedVendors() {
        return args -> {
            if (vendorRepository.count() > 0) {
                log.info("[FinanceSeeder] Vendors already seeded — skipping");
                return;
            }
            JsonNode vendorsJson = seedDataLoader.loadTree("seed/vendors.json");
            for (JsonNode v : vendorsJson) {
                vendorRepository.save(Vendor.builder()
                        .vendorCode(invoiceNumberService.generateVendorCode())
                        .name(v.get("name").asText())
                        .email(v.get("email").asText())
                        .status(VendorStatus.ACTIVE)
                        .paymentTerms(v.get("paymentTerms").asText())
                        .paymentTermsDays(v.get("paymentTermsDays").asInt())
                        .preferredCurrency(v.get("preferredCurrency").asText())
                        .category(v.get("category").asText())
                        .createdBy("SYSTEM").updatedBy("SYSTEM").build());
            }
            log.info("[FinanceSeeder] Seeded {} vendors", vendorsJson.size());
        };
    }

    // ── Customer Invoices ─────────────────────────────────────────────────────

    @Bean
    @Profile("dev")
    @Order(13)
    CommandLineRunner seedCustomerInvoices() {
        return args -> {
            if (customerInvoiceRepository.count() > 0) {
                log.info("[FinanceSeeder] Customer invoices already seeded — skipping");
                return;
            }
            FinancialPeriod current = periodRepository.findCurrentOpenPeriod().orElse(null);
            LocalDate today = LocalDate.now();

            List<Customer> customers = customerRepository.findAll(PageRequest.of(0, 15)).getContent();
            List<com.commerce_pro_backend.order.entity.Order> orders = orderRepository.findAll();

            JsonNode invoicesJson = seedDataLoader.loadTree("seed/customer-invoices.json");
            int count = 0;

            for (JsonNode inv : invoicesJson) {
                int ci = inv.get("customerIndex").asInt();
                BigDecimal totalAmt = new BigDecimal(inv.get("totalAmount").asText());
                BigDecimal taxRate  = new BigDecimal(inv.get("taxRate").asText());
                BigDecimal taxAmt   = totalAmt.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
                InvoiceStatus status = InvoiceStatus.valueOf(inv.get("status").asText());
                boolean paid = inv.get("paid").asBoolean();

                // Resolve customer — use real DB customer or fallback
                String custId, custName, custEmail;
                if (ci < customers.size()) {
                    Customer c = customers.get(ci);
                    custId = c.getId();
                    custName = c.getFirstName() + " " + c.getLastName();
                    custEmail = c.getEmail();
                } else {
                    custId = "CUST-" + String.format("%03d", ci + 1);
                    custName = "Customer " + (ci + 1);
                    custEmail = "customer" + (ci + 1) + "@example.com";
                }

                // Resolve dates
                LocalDate issued = today.minusDays(inv.get("issuedDaysAgo").asInt());
                LocalDate due;
                if (inv.has("dueDaysAgo")) {
                    due = today.minusDays(inv.get("dueDaysAgo").asInt());
                } else {
                    due = today.plusDays(inv.get("dueDaysFromNow").asInt());
                }

                // Try to link to a real order
                String orderId = findOrderId(orders, custId, count);

                BigDecimal subtotal = totalAmt.subtract(taxAmt);
                BigDecimal paidAmt = paid ? totalAmt
                        : (status == InvoiceStatus.PARTIALLY_PAID
                            ? totalAmt.multiply(new BigDecimal("0.4")).setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO);

                CustomerInvoice.CustomerInvoiceBuilder builder = CustomerInvoice.builder()
                        .invoiceNumber(invoiceNumberService.generateInvoiceNumber())
                        .customerId(custId).customerName(custName).customerEmail(custEmail)
                        .period(current).status(status)
                        .issuedAt(issued.atTime(9, 0)).dueDate(due)
                        .paidAt(paid ? due.atTime(10, 0) : null)
                        .subtotal(subtotal).discountAmount(BigDecimal.ZERO)
                        .taxAmount(taxAmt).shippingAmount(BigDecimal.ZERO)
                        .totalAmount(totalAmt).paidAmount(paidAmt)
                        .balanceDue(totalAmt.subtract(paidAmt).max(BigDecimal.ZERO))
                        .currency("USD").paymentTerms("Net 30")
                        .createdBy("SYSTEM").updatedBy("SYSTEM");

                if (orderId != null) builder.orderId(orderId);
                customerInvoiceRepository.save(builder.build());
                count++;
            }
            log.info("[FinanceSeeder] Seeded {} customer invoices", count);
        };
    }

    // ── Vendor Invoices ───────────────────────────────────────────────────────

    @Bean
    @Profile("dev")
    @Order(14)
    CommandLineRunner seedVendorInvoices() {
        return args -> {
            if (vendorInvoiceRepository.count() > 0) {
                log.info("[FinanceSeeder] Vendor invoices already seeded — skipping");
                return;
            }
            List<Vendor> vendors = vendorRepository.findAll();
            if (vendors.isEmpty()) {
                log.warn("[FinanceSeeder] No vendors found — skipping vendor invoice seed");
                return;
            }
            FinancialPeriod current = periodRepository.findCurrentOpenPeriod().orElse(null);
            LocalDate today = LocalDate.now();

            JsonNode invJson = seedDataLoader.loadTree("seed/vendor-invoices.json");
            int count = 0;

            for (JsonNode inv : invJson) {
                String vendorMatch = inv.get("vendorName").asText();
                Vendor vendor = vendors.stream()
                        .filter(v -> v.getName().contains(vendorMatch))
                        .findFirst().orElse(vendors.get(0));

                BigDecimal totalAmt = new BigDecimal(inv.get("totalAmount").asText());
                BigDecimal taxAmt = totalAmt.multiply(new BigDecimal("0.1")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal subtotal = totalAmt.subtract(taxAmt);
                VendorInvoiceStatus status = VendorInvoiceStatus.valueOf(inv.get("status").asText());
                boolean paid = status == VendorInvoiceStatus.PAID;
                BigDecimal paidAmt = paid ? totalAmt : BigDecimal.ZERO;

                LocalDate received = today.minusDays(inv.get("receivedDaysAgo").asInt());
                LocalDate due = inv.has("dueDaysAgo")
                        ? today.minusDays(inv.get("dueDaysAgo").asInt())
                        : today.plusDays(inv.get("dueDaysFromNow").asInt());

                String rejectReason = inv.has("rejectReason") && !inv.get("rejectReason").isNull()
                        ? inv.get("rejectReason").asText() : null;

                vendorInvoiceRepository.save(VendorInvoice.builder()
                        .vendorInvoiceNumber(invoiceNumberService.generateVendorInvoiceNumber())
                        .vendorReference(inv.get("vendorReference").asText())
                        .vendor(vendor).period(current)
                        .status(status).receivedAt(received.atTime(10, 0)).dueDate(due)
                        .subtotal(subtotal).taxAmount(taxAmt).totalAmount(totalAmt)
                        .paidAmount(paidAmt).balanceDue(totalAmt.subtract(paidAmt).max(BigDecimal.ZERO))
                        .paidAt(paid ? due.atTime(11, 0) : null)
                        .approvedAt(status == VendorInvoiceStatus.APPROVED || paid ? received.plusDays(2).atTime(14, 0) : null)
                        .approvedBy(status == VendorInvoiceStatus.APPROVED || paid ? "SYSTEM" : null)
                        .rejectionReason(rejectReason).currency("USD")
                        .createdBy("SYSTEM").updatedBy("SYSTEM").build());
                count++;
            }
            log.info("[FinanceSeeder] Seeded {} vendor invoices", count);
        };
    }

    // ── Expenses ──────────────────────────────────────────────────────────────

    @Bean
    @Profile("dev")
    @Order(15)
    CommandLineRunner seedExpenses() {
        return args -> {
            if (expenseRepository.count() > 0) {
                log.info("[FinanceSeeder] Expenses already seeded — skipping");
                return;
            }
            List<ExpenseCategory> cats = categoryRepository.findAll();
            if (cats.isEmpty()) {
                log.warn("[FinanceSeeder] No expense categories — skipping expense seed");
                return;
            }
            FinancialPeriod current = periodRepository.findCurrentOpenPeriod().orElse(null);
            LocalDate today = LocalDate.now();

            JsonNode expJson = seedDataLoader.loadTree("seed/expenses.json");
            int count = 0;

            for (JsonNode e : expJson) {
                String catCode = e.get("categoryCode").asText();
                ExpenseCategory cat = cats.stream()
                        .filter(c -> c.getCategoryCode().equals(catCode))
                        .findFirst().orElse(cats.get(0));

                BigDecimal amt = new BigDecimal(e.get("amount").asText());
                LocalDate date = today.minusDays(e.get("daysAgo").asInt());
                ExpenseStatus status = ExpenseStatus.valueOf(e.get("status").asText());
                String title = e.get("title").asText();
                String rejectReason = e.has("rejectReason") && !e.get("rejectReason").isNull()
                        ? e.get("rejectReason").asText() : null;

                expenseRepository.save(Expense.builder()
                        .expenseRef(invoiceNumberService.generateExpenseRef())
                        .title(title).category(cat).period(current)
                        .expenseDate(date).status(status)
                        .amount(amt).taxAmount(BigDecimal.ZERO).totalAmount(amt)
                        .currency("USD").paymentMethod("CREDIT_CARD")
                        .approvedAt(status == ExpenseStatus.APPROVED || status == ExpenseStatus.PAID
                                ? date.plusDays(1).atTime(10, 0) : null)
                        .approvedBy(status == ExpenseStatus.APPROVED || status == ExpenseStatus.PAID ? "SYSTEM" : null)
                        .paidAt(status == ExpenseStatus.PAID ? date.plusDays(2).atTime(15, 0) : null)
                        .rejectionReason(rejectReason)
                        .isRecurring(title.toLowerCase().contains("subscription"))
                        .recurrenceInterval(title.toLowerCase().contains("subscription") ? "MONTHLY" : null)
                        .createdBy("SYSTEM").updatedBy("SYSTEM").build());
                count++;
            }
            log.info("[FinanceSeeder] Seeded {} expenses", count);
        };
    }

    // ── Budget ────────────────────────────────────────────────────────────────

    @Bean
    @Profile("dev")
    @Order(16)
    CommandLineRunner seedBudget() {
        return args -> {
            if (budgetRepository.count() > 0) {
                log.info("[FinanceSeeder] Budgets already seeded — skipping");
                return;
            }
            int year = LocalDate.now().getYear();
            FinancialPeriod period = periodRepository.findCurrentOpenPeriod().orElse(null);
            List<ExpenseCategory> cats = categoryRepository.findAll();

            JsonNode budgetJson = seedDataLoader.loadTree("seed/budget.json");

            Budget budget = Budget.builder()
                    .name("FY" + year + " " + budgetJson.get("name").asText())
                    .description(budgetJson.get("description").asText() + " for fiscal year " + year)
                    .fiscalYear(year).period(period)
                    .status(BudgetStatus.APPROVED)
                    .totalRevenueBudget(new BigDecimal(budgetJson.get("totalRevenueBudget").asText()))
                    .totalExpenseBudget(new BigDecimal(budgetJson.get("totalExpenseBudget").asText()))
                    .currency(budgetJson.get("currency").asText())
                    .approvedAt(LocalDateTime.now().minusDays(30))
                    .approvedBy("SYSTEM")
                    .notes(budgetJson.get("notes").asText())
                    .createdBy("SYSTEM").updatedBy("SYSTEM")
                    .build();

            List<BudgetLine> lines = new ArrayList<>();
            for (JsonNode lineNode : budgetJson.get("lines")) {
                String catCode = lineNode.has("categoryCode") && !lineNode.get("categoryCode").isNull()
                        ? lineNode.get("categoryCode").asText() : null;
                String catId = null;
                if (catCode != null) {
                    catId = cats.stream()
                            .filter(c -> c.getCategoryCode().equals(catCode))
                            .findFirst().map(ExpenseCategory::getId).orElse(null);
                    if (catId == null) continue; // skip if category not found
                }
                lines.add(BudgetLine.builder()
                        .budget(budget)
                        .lineName(lineNode.get("lineName").asText())
                        .lineType(lineNode.get("lineType").asText())
                        .accountCode(lineNode.get("accountCode").asText())
                        .categoryId(catId)
                        .budgetedAmount(new BigDecimal(lineNode.get("budgetedAmount").asText()))
                        .actualAmount(BigDecimal.ZERO).variance(BigDecimal.ZERO)
                        .variancePct(BigDecimal.ZERO)
                        .sortOrder(lineNode.get("sortOrder").asInt())
                        .build());
            }
            budget.setLines(lines);
            budgetRepository.save(budget);
            log.info("[FinanceSeeder] Seeded 1 approved annual budget for FY{}", year);
        };
    }

    // ── Exchange Rates ────────────────────────────────────────────────────────

    @Bean
    @Profile("dev")
    @Order(17)
    CommandLineRunner seedExchangeRates() {
        return args -> {
            if (exchangeRateRepository.count() > 0) {
                log.info("[FinanceSeeder] Exchange rates already seeded — skipping");
                return;
            }
            LocalDate today = LocalDate.now();
            JsonNode ratesJson = seedDataLoader.loadTree("seed/exchange-rates.json");
            for (JsonNode r : ratesJson) {
                exchangeRateRepository.save(ExchangeRate.builder()
                        .baseCurrency(r.get("baseCurrency").asText())
                        .quoteCurrency(r.get("quoteCurrency").asText())
                        .rate(new BigDecimal(r.get("rate").asText()))
                        .rateDate(today)
                        .source(r.get("source").asText())
                        .createdBy("SYSTEM").build());
            }
            log.info("[FinanceSeeder] Seeded {} exchange rates", ratesJson.size());
        };
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String findOrderId(List<com.commerce_pro_backend.order.entity.Order> orders, String customerId, int fallbackIndex) {
        return orders.stream()
                .filter(o -> customerId.equals(o.getCustomerId()))
                .findFirst()
                .map(com.commerce_pro_backend.order.entity.Order::getId)
                .orElse(fallbackIndex < orders.size() ? orders.get(fallbackIndex).getId() : null);
    }
}
