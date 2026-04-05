package com.commerce_pro_backend.analytics.service;

import org.springframework.stereotype.Service;

import java.awt.Color;
import java.util.Map;

/**
 * Provides per-category PDF visual themes and global branding metadata for
 * all report types exported through {@link ReportExportService}.
 *
 * <p>Each report category has a dedicated {@link PdfTheme} that supplies three
 * coordinated colours: the accent colour used for the header bar and column
 * header row, a very-light page-background tint used in the top banner, and an
 * alternating row tint for data rows.
 *
 * <p>Centralising these definitions here means that adding a new category or
 * re-branding requires changes in exactly one place.
 */
@Service
public class ReportPdfTemplateRegistry {

    // ── Per-category colour themes ─────────────────────────────────────────────

    private static final Map<String, PdfTheme> CATEGORY_THEMES = Map.of(
        "Sales",     new PdfTheme(
            new Color(55,  48,  163),   // indigo-800   — accent
            new Color(238, 242, 255),   // indigo-50    — banner bg
            new Color(224, 231, 255)    // indigo-100   — alt row
        ),
        "Inventory", new PdfTheme(
            new Color(13,  148, 136),   // teal-600
            new Color(240, 253, 250),   // teal-50
            new Color(204, 251, 241)    // teal-100
        ),
        "Orders",    new PdfTheme(
            new Color(29,  78,  216),   // blue-700
            new Color(239, 246, 255),   // blue-50
            new Color(219, 234, 254)    // blue-100
        ),
        "Customers", new PdfTheme(
            new Color(109, 40,  217),   // violet-700
            new Color(245, 243, 255),   // violet-50
            new Color(237, 233, 254)    // violet-100
        ),
        "Financial", new PdfTheme(
            new Color(4,   120, 87),    // emerald-700
            new Color(236, 253, 245),   // emerald-50
            new Color(209, 250, 229)    // emerald-100
        ),
        "Shipping",  new PdfTheme(
            new Color(180, 83,  9),     // amber-700
            new Color(255, 251, 235),   // amber-50
            new Color(254, 243, 199)    // amber-100
        ),
        "Returns",   new PdfTheme(
            new Color(190, 18,  60),    // rose-700
            new Color(255, 241, 242),   // rose-50
            new Color(255, 228, 230)    // rose-100
        ),
        "Custom",    new PdfTheme(
            new Color(71,  85,  105),   // slate-600
            new Color(248, 250, 252),   // slate-50
            new Color(241, 245, 249)    // slate-100
        )
    );

    /** Default theme used when no category-specific one is registered. */
    private static final PdfTheme DEFAULT_THEME = new PdfTheme(
        new Color(75, 85, 99),          // gray-600
        new Color(249, 250, 251),       // gray-50
        new Color(243, 244, 246)        // gray-100
    );

    // ── Global branding ────────────────────────────────────────────────────────

    private static final PdfBranding BRANDING = new PdfBranding(
        "Commerce Pro",
        "Analytics & Reporting Suite",
        "Confidential \u2014 Internal Use Only"
    );

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Returns the {@link PdfTheme} for the given category name, falling back to
     * a neutral grey theme if the category is unknown.
     */
    public PdfTheme getTheme(String category) {
        return CATEGORY_THEMES.getOrDefault(category, DEFAULT_THEME);
    }

    /** Returns the global branding constants applied to every exported PDF. */
    public PdfBranding getBranding() {
        return BRANDING;
    }

    // ── Value objects ──────────────────────────────────────────────────────────

    /**
     * Three coordinated colours that define the visual theme of one report category.
     *
     * @param accentColor   Primary brand colour: used for header bar, column-header row.
     * @param bannerBgColor Very-light tint used behind the report banner block.
     * @param altRowColor   Subtle tint applied to every other data row.
     */
    public record PdfTheme(
            Color accentColor,
        Color bannerBgColor,
        Color altRowColor
    ) {}

    /**
     * Static branding copy printed on every exported PDF.
     *
     * @param companyName     Top-left company / product name in the banner.
     * @param suiteName       Sub-title below the company name.
     * @param confidentiality Footer left-hand label.
     */
    public record PdfBranding(
        String companyName,
        String suiteName,
        String confidentiality
    ) {}
}
