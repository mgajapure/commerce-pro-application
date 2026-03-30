package com.commerce_pro_backend.finance.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.finance.dto.FinanceDTOs.*;
import com.commerce_pro_backend.finance.entity.*;
import com.commerce_pro_backend.finance.enums.AccountType;
import com.commerce_pro_backend.finance.enums.JournalEntryType;
import com.commerce_pro_backend.finance.mapper.FinanceMapper;
import com.commerce_pro_backend.finance.repository.JournalEntryRepository;
import com.commerce_pro_backend.finance.specification.JournalEntrySpecification;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.service.AuditService;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the journal entry ledger.
 * Provides auto-posting helpers called by other finance services,
 * and manual entry creation with debit/credit balance validation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JournalEntryService {

    private final JournalEntryRepository journalEntryRepository;
    private final FinancialPeriodService periodService;
    private final InvoiceNumberService   invoiceNumberService;
    private final FinanceMapper          mapper;
    private final AuditService           auditService;
    private final CurrentUserService     currentUserService;

    // ── Standard GL Account Codes ─────────────────────────────────────────────
    private static final String ACCOUNT_CASH             = "1000";
    private static final String ACCOUNT_AR               = "1100";
    private static final String ACCOUNT_AP               = "2000";
    private static final String ACCOUNT_REVENUE          = "4000";
    private static final String ACCOUNT_COGS             = "5000";
    private static final String ACCOUNT_EXPENSE          = "6000";
    private static final String ACCOUNT_TAX_PAYABLE      = "2100";
    private static final String ACCOUNT_SHIPPING_REV     = "4100";
    private static final String ACCOUNT_GATEWAY_FEES     = "6100";
    private static final String ACCOUNT_CHARGEBACK_LOSS  = "6200";

    // ── Read ──────────────────────────────────────────────────────────────────

    public PageResponse<JournalEntrySummaryDTO> listEntries(
            String search, JournalEntryType type, String periodId,
            String status, LocalDate from, LocalDate to, Pageable pageable) {

        Specification<JournalEntry> spec = JournalEntrySpecification
                .withFilters(search, type, periodId, status, from, to);
        Page<JournalEntry> page = journalEntryRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream().map(mapper::toJournalEntrySummaryDTO).toList());
    }

    public JournalEntryDTO getById(String id) {
        return mapper.toJournalEntryDTO(findOrThrow(id));
    }

    public List<JournalEntrySummaryDTO> getBySource(String sourceType, String sourceId) {
        return mapper.toJournalEntrySummaryList(
                journalEntryRepository.findBySourceTypeAndSourceId(sourceType, sourceId));
    }

    // ── Manual Entry ──────────────────────────────────────────────────────────

    @Transactional
    public JournalEntryDTO postManualEntry(CreateJournalEntryRequest req) {
        // Validate debit/credit balance — double-entry bookkeeping requirement
        java.math.BigDecimal totalDebits  = req.getLines().stream()
                .filter(l -> l.getAccountType() == com.commerce_pro_backend.finance.enums.AccountType.DEBIT)
                .map(JournalLineRequest::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal totalCredits = req.getLines().stream()
                .filter(l -> l.getAccountType() == com.commerce_pro_backend.finance.enums.AccountType.CREDIT)
                .map(JournalLineRequest::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw com.commerce_pro_backend.common.exception.ApiException.badRequest(
                    "Journal entry is unbalanced: total debits (" + totalDebits +
                    ") must equal total credits (" + totalCredits + ")");
        }

        String actor = currentUserService.getCurrentUserId();

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw ApiException.badRequest("Journal entry is not balanced. Debits: " + totalDebits + ", Credits: " + totalCredits);
        }

        FinancialPeriod period = resolvePeriod(req.getPeriodId(), req.getEntryDate());

        JournalEntry entry = JournalEntry.builder()
                .entryRef(invoiceNumberService.generateJournalEntryRef())
                .period(period)
                .entryType(req.getEntryType())
                .status("POSTED")
                .entryDate(req.getEntryDate())
                .description(req.getDescription())
                .reference(req.getReference())
                .currency(req.getCurrency() != null ? req.getCurrency() : "USD")
                .postedBy(actor)
                .notes(req.getNotes())
                .build();

        List<JournalEntryLine> lines = new ArrayList<>();
        int sort = 0;
        for (JournalLineRequest lr : req.getLines()) {
            lines.add(JournalEntryLine.builder()
                    .journalEntry(entry)
                    .accountType(lr.getAccountType())
                    .accountCode(lr.getAccountCode())
                    .accountName(lr.getAccountName())
                    .amount(lr.getAmount())
                    .currency(lr.getCurrency() != null ? lr.getCurrency() : "USD")
                    .description(lr.getDescription())
                    .sortOrder(sort++)
                    .build());
        }
        entry.setLines(lines);
        entry = journalEntryRepository.save(entry);

        auditService.log(actor, AuditAction.FINANCE_JOURNAL_POSTED, "JournalEntry", entry.getId(),
                entry.getEntryRef(), null, null, null, true);
        log.info("Manual journal entry posted: {}", entry.getEntryRef());
        return mapper.toJournalEntryDTO(entry);
    }

    @Transactional
    public JournalEntryDTO reverseEntry(String id, ReverseEntryRequest req) {
        JournalEntry original = findOrThrow(id);
        if (!"POSTED".equals(original.getStatus())) {
            throw ApiException.badRequest("Only POSTED entries can be reversed. Status: " + original.getStatus());
        }
        String actor = currentUserService.getCurrentUserId();

        // Mark original as REVERSED
        original.setStatus("REVERSED");
        original.setReversedAt(LocalDateTime.now());
        original.setReversedBy(actor);
        original.setReversalReason(req.getReason());
        journalEntryRepository.save(original);

        // Create reversal entry (swap debit/credit)
        JournalEntry reversal = JournalEntry.builder()
                .entryRef(invoiceNumberService.generateJournalEntryRef())
                .period(original.getPeriod())
                .entryType(original.getEntryType())
                .status("POSTED")
                .entryDate(LocalDate.now())
                .description("REVERSAL: " + original.getDescription())
                .reference(original.getEntryRef())
                .currency(original.getCurrency())
                .sourceType(original.getSourceType())
                .sourceId(original.getSourceId())
                .postedBy(actor)
                .notes(req.getReason())
                .build();

        List<JournalEntryLine> reversalLines = new ArrayList<>();
        int sort = 0;
        for (JournalEntryLine ol : original.getLines()) {
            reversalLines.add(JournalEntryLine.builder()
                    .journalEntry(reversal)
                    .accountType(ol.getAccountType() == AccountType.DEBIT ? AccountType.CREDIT : AccountType.DEBIT)
                    .accountCode(ol.getAccountCode()).accountName(ol.getAccountName())
                    .amount(ol.getAmount()).currency(ol.getCurrency())
                    .description("Reversal of: " + ol.getDescription())
                    .sortOrder(sort++)
                    .build());
        }
        reversal.setLines(reversalLines);
        reversal = journalEntryRepository.save(reversal);

        auditService.log(actor, AuditAction.FINANCE_JOURNAL_REVERSED, "JournalEntry", original.getId(),
                original.getEntryRef(), null, "POSTED", "REVERSED", true);
        return mapper.toJournalEntryDTO(reversal);
    }

    // ── Auto-Posting Helpers ──────────────────────────────────────────────────
    // Called by CustomerInvoiceService, VendorInvoiceService, ExpenseService

    /** DR Accounts Receivable / CR Revenue when invoice is sent */
    @Transactional
    public void postInvoiceSent(CustomerInvoice invoice) {
        post(JournalEntryType.ORDER_REVENUE, invoice.getInvoiceNumber(), "CUSTOMER_INVOICE", invoice.getId(),
                invoice.getPeriod(), invoice.getCurrency(),
                "Invoice " + invoice.getInvoiceNumber() + " issued to " + invoice.getCustomerName(),
                List.of(
                    line(AccountType.DEBIT,  ACCOUNT_AR,      "Accounts Receivable", invoice.getTotalAmount()),
                    line(AccountType.CREDIT, ACCOUNT_REVENUE, "Revenue",             invoice.getSubtotal()),
                    line(AccountType.CREDIT, ACCOUNT_TAX_PAYABLE, "Tax Payable",     invoice.getTaxAmount())
                )
        );
    }

    /** DR Cash / CR Accounts Receivable when payment received */
    @Transactional
    public void postPaymentReceived(CustomerInvoice invoice, BigDecimal amount) {
        post(JournalEntryType.AR_RECEIPT, invoice.getInvoiceNumber(), "CUSTOMER_INVOICE", invoice.getId(),
                invoice.getPeriod(), invoice.getCurrency(),
                "Payment received for " + invoice.getInvoiceNumber(),
                List.of(
                    line(AccountType.DEBIT,  ACCOUNT_CASH, "Cash",                  amount),
                    line(AccountType.CREDIT, ACCOUNT_AR,   "Accounts Receivable",   amount)
                )
        );
    }

    /** DR Expense / CR Accounts Payable when vendor invoice approved */
    @Transactional
    public void postVendorInvoiceApproved(VendorInvoice inv) {
        post(JournalEntryType.EXPENSE, inv.getVendorInvoiceNumber(), "VENDOR_INVOICE", inv.getId(),
                inv.getPeriod(), inv.getCurrency(),
                "Vendor bill approved: " + inv.getVendorInvoiceNumber() + " (" + inv.getVendor().getName() + ")",
                List.of(
                    line(AccountType.DEBIT,  ACCOUNT_EXPENSE, "Expense",            inv.getSubtotal()),
                    line(AccountType.DEBIT,  ACCOUNT_TAX_PAYABLE, "Input Tax",      inv.getTaxAmount()),
                    line(AccountType.CREDIT, ACCOUNT_AP,      "Accounts Payable",   inv.getTotalAmount())
                )
        );
    }

    /** DR Accounts Payable / CR Cash when vendor paid */
    @Transactional
    public void postVendorPayment(VendorInvoice inv, BigDecimal amount) {
        post(JournalEntryType.AP_PAYMENT, inv.getVendorInvoiceNumber(), "VENDOR_INVOICE", inv.getId(),
                inv.getPeriod(), inv.getCurrency(),
                "Payment to " + inv.getVendor().getName() + " for " + inv.getVendorInvoiceNumber(),
                List.of(
                    line(AccountType.DEBIT,  ACCOUNT_AP,   "Accounts Payable", amount),
                    line(AccountType.CREDIT, ACCOUNT_CASH, "Cash",             amount)
                )
        );
    }

    /** DR Expense category / CR Cash when expense paid */
    @Transactional
    public void postExpensePaid(Expense expense) {
        post(JournalEntryType.EXPENSE, expense.getExpenseRef(), "EXPENSE", expense.getId(),
                expense.getPeriod(), expense.getCurrency(),
                "Expense: " + expense.getTitle(),
                List.of(
                    line(AccountType.DEBIT,  ACCOUNT_EXPENSE, expense.getCategory() != null
                            ? expense.getCategory().getName() : "Operating Expense", expense.getTotalAmount()),
                    line(AccountType.CREDIT, ACCOUNT_CASH,    "Cash",               expense.getTotalAmount())
                )
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void post(JournalEntryType type, String reference, String sourceType, String sourceId,
                      FinancialPeriod period, String currency, String description,
                      List<JournalEntryLine> lineTemplates) {

        // Skip posting if lines with zero amounts
        boolean allZero = lineTemplates.stream().allMatch(l -> l.getAmount().compareTo(BigDecimal.ZERO) == 0);
        if (allZero) return;

        JournalEntry entry = JournalEntry.builder()
                .entryRef(invoiceNumberService.generateJournalEntryRef())
                .period(period).entryType(type).status("POSTED")
                .entryDate(LocalDate.now()).description(description)
                .reference(reference).currency(currency != null ? currency : "USD")
                .sourceType(sourceType).sourceId(sourceId)
                .postedBy("SYSTEM")
                .build();

        List<JournalEntryLine> lines = new ArrayList<>();
        for (int i = 0; i < lineTemplates.size(); i++) {
            JournalEntryLine t = lineTemplates.get(i);
            if (t.getAmount().compareTo(BigDecimal.ZERO) != 0) {
                t.setJournalEntry(entry);
                t.setSortOrder(i);
                lines.add(t);
            }
        }
        entry.setLines(lines);

        if (!entry.isBalanced()) {
            log.warn("Auto-posting skipped — unbalanced entry for source {}/{}", sourceType, sourceId);
            return;
        }
        journalEntryRepository.save(entry);
        log.debug("Journal entry auto-posted: {} for {}/{}", entry.getEntryRef(), sourceType, sourceId);
    }

    private JournalEntryLine line(AccountType type, String code, String name, BigDecimal amount) {
        return JournalEntryLine.builder()
                .accountType(type).accountCode(code).accountName(name)
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .build();
    }

    private FinancialPeriod resolvePeriod(String periodId, LocalDate date) {
        if (periodId != null && !periodId.isBlank()) {
            FinancialPeriod p = periodService.findOrThrow(periodId);
            if (!p.isPostingAllowed()) {
                throw ApiException.badRequest("Period '" + p.getPeriodName() + "' is " + p.getStatus() + " — postings not allowed");
            }
            return p;
        }
        return periodService.resolvePeriodForDate(date);
    }

    JournalEntry findOrThrow(String id) {
        return journalEntryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("JournalEntry", id));
    }
}
