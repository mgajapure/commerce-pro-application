package com.commerce_pro_backend.analytics.service;

import com.commerce_pro_backend.analytics.dto.*;
import com.commerce_pro_backend.analytics.enums.ExportFormat;
import com.commerce_pro_backend.analytics.enums.ReportType;
import com.commerce_pro_backend.analytics.service.ReportTemplateRegistry.ColumnDef;
import com.commerce_pro_backend.analytics.service.ReportTemplateRegistry.ReportTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Generates report files (CSV, Excel/XLSX, JSON) and writes them to the
 * {@code reports/} sub-directory under the application's upload directory.
 *
 * <p>Files are served through the existing FileStorageController endpoint:
 * {@code GET /api/files/download/reports/{filename}}.
 *
 * <p>Export quality is driven by {@link ReportTemplateRegistry}: every report
 * type has a named template with column definitions (header labels, widths,
 * type hints) so all exports are consistently branded and properly formatted.
 *
 * <h3>Excel output structure (per sheet)</h3>
 * <ol>
 *   <li>Row 1 — Report title  (large bold, indigo text)</li>
 *   <li>Row 2 — Metadata line (generated timestamp, category)</li>
 *   <li>Row 3 — Blank separator</li>
 *   <li>Row 4 — Column headers (bold, light-indigo fill, auto-filter enabled)</li>
 *   <li>Rows 5+ — Data rows (alternating white / very-light-gray)</li>
 * </ol>
 * Rows 1-4 are frozen so the header stays visible while scrolling.
 *
 * <h3>CSV output structure</h3>
 * Two metadata comment lines (prefixed with {@code #}) are written before the
 * header row so the file remains machine-parseable while still carrying context.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExportService {

    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    private final ReportTemplateRegistry templateRegistry;

    private Path reportsDir;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final DateTimeFormatter META_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostConstruct
    public void init() throws IOException {
        reportsDir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("reports");
        Files.createDirectories(reportsDir);
        log.info("Report export directory: {}", reportsDir);
    }

    // ── Public entry points ───────────────────────────────────────────────────

    public ExportResult exportSales(SalesReportDTO report, ExportFormat format, ReportType reportType) {
        List<String[]> rows = report.getRows() == null ? List.of() : report.getRows().stream().map(r -> new String[]{
                safe(r.getDimension()), dec(r.getRevenue()), dec(r.getDiscount()),
                dec(r.getTax()), dec(r.getShipping()), dec(r.getRefunded()),
                str(r.getOrders()), str(r.getUnits()), dec(r.getAov()), dec(r.getSharePct())
        }).toList();
        return write(rows, format, reportType, report);
    }

    public ExportResult exportInventory(InventoryReportDTO report, ExportFormat format, ReportType reportType) {
        List<String[]> rows = report.getStockValueRows() == null ? List.of()
                : report.getStockValueRows().stream().map(r -> new String[]{
                        safe(r.getProductName()), safe(r.getSku()), safe(r.getCategory()),
                        safe(r.getWarehouseName()), str(r.getQuantityOnHand()), str(r.getReserved()),
                        str(r.getAvailable()), dec(r.getUnitCost()), dec(r.getTotalValue()),
                        safe(r.getStockStatus()), safe(r.getBinLocation())
                }).toList();
        return write(rows, format, reportType, report);
    }

    public ExportResult exportInventoryMovement(InventoryReportDTO report, ExportFormat format, ReportType reportType) {
        List<String[]> rows = report.getMovementRows() == null ? List.of()
                : report.getMovementRows().stream().map(r -> new String[]{
                        safe(r.getMovementId()), safe(r.getProductName()), safe(r.getSku()),
                        safe(r.getWarehouseName()), safe(r.getMovementType()),
                        str(r.getQuantity()), str(r.getPreviousQuantity()), str(r.getNewQuantity()),
                        safe(r.getReason()), safe(r.getReference()), safe(r.getCreatedAt()), safe(r.getCreatedBy())
                }).toList();
        return write(rows, format, reportType, report);
    }

    public ExportResult exportInventoryAgeing(InventoryReportDTO report, ExportFormat format, ReportType reportType) {
        List<String[]> rows = report.getAgeingRows() == null ? List.of()
                : report.getAgeingRows().stream().map(r -> new String[]{
                        safe(r.getProductName()), safe(r.getSku()), safe(r.getWarehouseName()),
                        str(r.getQuantityOnHand()), dec(r.getTotalValue()),
                        str(r.getDaysSinceLastMovement()), safe(r.getAgeingBucket()), safe(r.getLastMovementDate())
                }).toList();
        return write(rows, format, reportType, report);
    }

    public ExportResult exportOrders(OrderReportDTO report, ExportFormat format, ReportType reportType) {
        List<String[]> rows = report.getRows() == null ? List.of()
                : report.getRows().stream().map(r -> new String[]{
                        safe(r.getOrderNumber()), safe(r.getCustomerName()), safe(r.getCustomerEmail()),
                        safe(r.getStatus()), safe(r.getFulfillmentStatus()), safe(r.getPaymentStatus()),
                        safe(r.getSource()), dec(r.getTotalAmount()), str(r.getItemCount()),
                        safe(r.getCreatedAt()), safe(r.getShippedAt()), safe(r.getDeliveredAt()),
                        safe(r.getCancellationReason()), safe(r.getCarrierName()), safe(r.getTrackingNumber())
                }).toList();
        return write(rows, format, reportType, report);
    }

    public ExportResult exportCustomers(CustomerReportDTO report, ExportFormat format, ReportType reportType) {
        List<String[]> rows = report.getLtvRows() == null ? List.of()
                : report.getLtvRows().stream().map(r -> new String[]{
                        safe(r.getCustomerId()), safe(r.getCustomerName()), safe(r.getEmail()),
                        safe(r.getTier()), safe(r.getStatus()), str(r.getTotalOrders()),
                        dec(r.getLifetimeSpend()), dec(r.getAverageOrderValue()),
                        safe(r.getFirstOrderAt()), safe(r.getLastOrderAt()), str(r.getDaysSinceLastOrder()),
                        str(r.getLoyaltyPoints()), safe(r.getAcquisitionSource()), safe(r.getSegment())
                }).toList();
        return write(rows, format, reportType, report);
    }

    public ExportResult exportFinancial(FinancialReportDTO report, ExportFormat format, ReportType reportType) {
        List<String[]> rows = report.getRevenueTrend() == null ? List.of()
                : report.getRevenueTrend().stream().map(r -> new String[]{
                        safe(r.getPeriod()), dec(r.getGrossRevenue()), dec(r.getNetRevenue()),
                        dec(r.getCogs()), dec(r.getGrossProfit()), dec(r.getGrossMarginPct()),
                        dec(r.getTax()), dec(r.getRefunds())
                }).toList();
        return write(rows, format, reportType, report);
    }

    public ExportResult exportShipping(ShippingReportDTO report, ExportFormat format, ReportType reportType) {
        List<String[]> rows = report.getRows() == null ? List.of()
                : report.getRows().stream().map(r -> new String[]{
                        safe(r.getShipmentNumber()), safe(r.getOrderNumber()), safe(r.getCarrierName()),
                        safe(r.getTrackingNumber()), safe(r.getStatus()),
                        safe(r.getShippedAt()), safe(r.getEstimatedDeliveryDate()), safe(r.getActualDeliveryDate()),
                        str(r.getDeliveryDays()), str(r.getOnTime()), dec(r.getShippingCost()),
                        safe(r.getRecipientCity()), safe(r.getRecipientCountry())
                }).toList();
        return write(rows, format, reportType, report);
    }

    public ExportResult exportReturns(ReturnReportDTO report, ExportFormat format, ReportType reportType) {
        List<String[]> rows = report.getRows() == null ? List.of()
                : report.getRows().stream().map(r -> new String[]{
                        safe(r.getOrderNumber()), safe(r.getCustomerName()), safe(r.getProductName()),
                        safe(r.getSku()), str(r.getReturnedQuantity()), dec(r.getRefundedAmount()),
                        safe(r.getReason()), safe(r.getOrderStatus()), safe(r.getCreatedAt())
                }).toList();
        return write(rows, format, reportType, report);
    }

    // ── Core write logic ──────────────────────────────────────────────────────

    private ExportResult write(List<String[]> dataRows,
                                ExportFormat format, ReportType reportType, Object reportData) {
        ReportTemplate template = templateRegistry.getTemplate(reportType);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String ext = switch (format) {
            case CSV   -> ".csv";
            case EXCEL -> ".xlsx";
            case JSON  -> ".json";
            default    -> ".csv";
        };
        String baseName   = reportType.name().toLowerCase();
        String fileName   = sanitize(baseName) + "_" + timestamp + ext;
        Path   filePath   = reportsDir.resolve(fileName);

        try {
            switch (format) {
                case CSV   -> writeCsv(filePath, template, dataRows);
                case EXCEL -> writeExcel(filePath, template, dataRows);
                case JSON  -> writeJson(filePath, reportData);
            }
            long size = Files.size(filePath);
            log.info("Exported {} → {} ({} bytes, {} rows)",
                    template.title(), fileName, size, dataRows.size());
            return new ExportResult("reports/" + fileName, fileName, size, dataRows.size());
        } catch (Exception ex) {
            log.error("Export failed for {} ({})", fileName, reportType, ex);
            throw new RuntimeException("Report export failed: " + ex.getMessage(), ex);
        }
    }

    // ── CSV ───────────────────────────────────────────────────────────────────

    private void writeCsv(Path path, ReportTemplate template, List<String[]> rows) throws IOException {
        String generated = LocalDateTime.now().format(META_FMT);
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            // UTF-8 BOM for Excel auto-detect
            bw.write('\uFEFF');
            // Metadata comment lines (hash prefix keeps them machine-parseable)
            bw.write("# " + template.title()); bw.newLine();
            bw.write("# Generated: " + generated + "  |  Category: " + template.category()); bw.newLine();
            // Column header row
            bw.write(toCsvLine(template.columns().stream().map(ColumnDef::header).toArray(String[]::new)));
            bw.newLine();
            // Data rows
            for (String[] row : rows) {
                bw.write(toCsvLine(row));
                bw.newLine();
            }
        }
    }

    private String toCsvLine(String[] cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) sb.append(',');
            String val = cols[i] == null ? "" : cols[i];
            if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.startsWith("#")) {
                sb.append('"').append(val.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(val);
            }
        }
        return sb.toString();
    }

    // ── Excel (OOXML / ZIP) ───────────────────────────────────────────────────
    //
    // Produces a professional .xlsx without an Apache POI dependency by building
    // the ZIP structure manually.  Sheet layout:
    //   Row 1  – Report title        (style 2: large bold indigo)
    //   Row 2  – Generated metadata  (style 4: small gray italic)
    //   Row 3  – blank
    //   Row 4  – Column headers      (style 3: bold + light-indigo fill)
    //   Row 5+ – Data rows           (style 0 / style 5 alternating)
    //
    // Rows 1-4 are frozen.  AutoFilter covers the header row.
    // Column widths are set from template ColumnDef.width().

    private static final int TITLE_STYLE  = 2;
    private static final int META_STYLE   = 4;
    private static final int HEADER_STYLE = 3;
    private static final int ALT_STYLE    = 5;

    private void writeExcel(Path path, ReportTemplate template, List<String[]> dataRows) throws IOException {
        String generated = LocalDateTime.now().format(META_FMT);
        String sheetName = truncate(template.title().replace("Report", "").trim(), 28);

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.BufferedOutputStream(Files.newOutputStream(path)))) {

            // _rels/.rels
            addZipEntry(zos, "_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                "</Relationships>");

            // [Content_Types].xml
            addZipEntry(zos, "[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\"  ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/xl/workbook.xml\"              ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                "<Override PartName=\"/xl/worksheets/sheet1.xml\"     ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                "<Override PartName=\"/xl/styles.xml\"                ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
                "</Types>");

            // xl/_rels/workbook.xml.rels
            addZipEntry(zos, "xl/_rels/workbook.xml.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\"    Target=\"styles.xml\"/>" +
                "</Relationships>");

            // xl/workbook.xml
            addZipEntry(zos, "xl/workbook.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
                "          xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                "<sheets>" +
                "<sheet name=\"" + xmlEsc(sheetName) + "\" sheetId=\"1\" r:id=\"rId1\"/>" +
                "</sheets></workbook>");

            // xl/styles.xml  – 6 cell styles
            addZipEntry(zos, "xl/styles.xml", buildStylesXml());

            // xl/worksheets/sheet1.xml
            addZipEntry(zos, "xl/worksheets/sheet1.xml",
                buildSheetXml(template, dataRows, generated));
        }
    }

    /**
     * Builds styles.xml with 6 named xf (cell-format) entries:
     * <ol start="0">
     *   <li>0 – normal</li>
     *   <li>1 – bold (legacy / inline use)</li>
     *   <li>2 – title: large (14 pt) bold indigo</li>
     *   <li>3 – header: bold, light-indigo fill</li>
     *   <li>4 – metadata: small (9 pt) gray italic</li>
     *   <li>5 – alternate data row: very-light-gray fill</li>
     * </ol>
     */
    private String buildStylesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +

        // fonts (0=normal, 1=bold, 2=title bold indigo 14pt, 3=header bold, 4=meta gray 9pt italic)
        "<fonts count=\"5\">" +
            "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
            "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
            "<font><b/><sz val=\"14\"/><name val=\"Calibri\"/><color rgb=\"FF3730A3\"/></font>" +
            "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/><color rgb=\"FF1E3A5F\"/></font>" +
            "<font><i/><sz val=\"9\"/><name val=\"Calibri\"/><color rgb=\"FF888888\"/></font>" +
        "</fonts>" +

        // fills (0=none, 1=gray125 required, 2=light-indigo header bg, 3=alt-row very-light-gray)
        "<fills count=\"4\">" +
            "<fill><patternFill patternType=\"none\"/></fill>" +
            "<fill><patternFill patternType=\"gray125\"/></fill>" +
            "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE8EAF6\"/></patternFill></fill>" +
            "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF9FAFB\"/></patternFill></fill>" +
        "</fills>" +

        // borders
        "<borders count=\"2\">" +
            "<border><left/><right/><top/><bottom/><diagonal/></border>" +
            "<border>" +
                "<left   style=\"thin\"><color rgb=\"FFD1D5DB\"/></left>" +
                "<right  style=\"thin\"><color rgb=\"FFD1D5DB\"/></right>" +
                "<top    style=\"thin\"><color rgb=\"FFD1D5DB\"/></top>" +
                "<bottom style=\"thin\"><color rgb=\"FFD1D5DB\"/></bottom>" +
                "<diagonal/>" +
            "</border>" +
        "</borders>" +

        "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +

        // cellXfs — the 6 styles used in the sheet
        "<cellXfs count=\"6\">" +
            // 0: normal data
            "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\"/>" +
            // 1: bold (kept for back-compat)
            "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyBorder=\"1\"/>" +
            // 2: title row
            "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>" +
            // 3: column header (bold + indigo bg)
            "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"/>" +
            // 4: metadata row (italic small gray)
            "<xf numFmtId=\"0\" fontId=\"4\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>" +
            // 5: alternate data row
            "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyFill=\"1\" applyBorder=\"1\"/>" +
        "</cellXfs>" +
        "</styleSheet>";
    }

    private String buildSheetXml(ReportTemplate template, List<String[]> dataRows, String generated) {
        List<ColumnDef> cols   = template.columns();
        int             colCnt = cols.size();
        String          lastCol = colName(colCnt - 1);

        StringBuilder ws = new StringBuilder(512 + dataRows.size() * colCnt * 20);
        ws.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        ws.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");

        // ── sheet views (freeze rows 1-4) ─────────────────────────────────────
        ws.append("<sheetViews><sheetView workbookViewId=\"0\">");
        ws.append("<pane ySplit=\"4\" topLeftCell=\"A5\" activePane=\"bottomLeft\" state=\"frozen\"/>");
        ws.append("</sheetView></sheetViews>");

        // ── column widths from template ───────────────────────────────────────
        ws.append("<cols>");
        for (int i = 0; i < colCnt; i++) {
            ws.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
              .append("\" width=\"").append(cols.get(i).width())
              .append("\" customWidth=\"1\"/>");
        }
        ws.append("</cols>");

        // ── sheet data ────────────────────────────────────────────────────────
        ws.append("<sheetData>");

        // Row 1: title
        ws.append("<row r=\"1\">");
        ws.append("<c r=\"A1\" t=\"inlineStr\" s=\"").append(TITLE_STYLE).append("\">")
          .append("<is><t>").append(xmlEsc(template.title())).append("</t></is></c>");
        ws.append("</row>");

        // Row 2: metadata
        String metaText = "Generated: " + generated + "    |    Category: " + template.category()
                        + "    |    Rows: " + dataRows.size();
        ws.append("<row r=\"2\">");
        ws.append("<c r=\"A2\" t=\"inlineStr\" s=\"").append(META_STYLE).append("\">")
          .append("<is><t>").append(xmlEsc(metaText)).append("</t></is></c>");
        ws.append("</row>");

        // Row 3: blank (just an empty row element to reserve space)
        ws.append("<row r=\"3\"/>");

        // Row 4: column headers
        ws.append("<row r=\"4\">");
        for (int i = 0; i < colCnt; i++) {
            String cellRef = colName(i) + "4";
            ws.append("<c r=\"").append(cellRef).append("\" t=\"inlineStr\" s=\"").append(HEADER_STYLE).append("\">")
              .append("<is><t>").append(xmlEsc(cols.get(i).header())).append("</t></is></c>");
        }
        ws.append("</row>");

        // Rows 5+: data
        int rowNum = 5;
        for (String[] row : dataRows) {
            int style = ((rowNum % 2) == 0) ? ALT_STYLE : 0;
            ws.append("<row r=\"").append(rowNum).append("\">");
            for (int i = 0; i < Math.min(row.length, colCnt); i++) {
                String cellRef = colName(i) + rowNum;
                String val     = row[i] != null ? xmlEsc(row[i]) : "";
                ws.append("<c r=\"").append(cellRef).append("\" t=\"inlineStr\" s=\"").append(style).append("\">")
                  .append("<is><t>").append(val).append("</t></is></c>");
            }
            ws.append("</row>");
            rowNum++;
        }

        ws.append("</sheetData>");

        // ── auto-filter on header row ─────────────────────────────────────────
        ws.append("<autoFilter ref=\"A4:").append(lastCol).append("4\"/>");

        ws.append("</worksheet>");
        return ws.toString();
    }

    // ── JSON ──────────────────────────────────────────────────────────────────

    private void writeJson(Path path, Object data) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), data);
    }

    // ── ZIP helpers ───────────────────────────────────────────────────────────

    private void addZipEntry(java.util.zip.ZipOutputStream zos, String name, String content) throws IOException {
        zos.putNextEntry(new java.util.zip.ZipEntry(name));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    // ── Cell reference helpers ────────────────────────────────────────────────

    private String colName(int col) {
        StringBuilder sb = new StringBuilder();
        col++;
        while (col > 0) {
            col--;
            sb.insert(0, (char) ('A' + (col % 26)));
            col /= 26;
        }
        return sb.toString();
    }

    private String xmlEsc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    // ── String helpers ────────────────────────────────────────────────────────

    private String safe(Object o)  { return o != null ? o.toString() : ""; }
    private String dec(java.math.BigDecimal b) { return b != null ? b.toPlainString() : "0"; }
    private String str(Object o)   { return o != null ? o.toString() : ""; }
    private String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9_-]", "_").replaceAll("_+", "_");
    }
    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    // ── Result record ─────────────────────────────────────────────────────────

    public record ExportResult(String relativePath, String fileName, long fileSizeBytes, int rowCount) {}
}
