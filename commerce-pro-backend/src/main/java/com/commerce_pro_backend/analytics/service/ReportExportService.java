package com.commerce_pro_backend.analytics.service;

import com.commerce_pro_backend.analytics.dto.*;
import com.commerce_pro_backend.analytics.enums.ExportFormat;
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
 * reports/ sub-directory under the application's upload directory.
 * Files are served through the existing FileStorageController endpoint:
 *   GET /api/files/download/reports/{filename}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExportService {

    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    private Path reportsDir;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @PostConstruct
    public void init() throws IOException {
        reportsDir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("reports");
        Files.createDirectories(reportsDir);
        log.info("Report export directory: {}", reportsDir);
    }

    // ── Public entry point ────────────────────────────────────────────────────

    public ExportResult exportSales(SalesReportDTO report, ExportFormat format, String baseName) {
        List<String[]> headers = List.<String[]>of(
                new String[]{"Period/Dimension","Revenue","Discount","Tax","Shipping","Refunded","Orders","Units","AOV","Share%"});
        List<String[]> rows = report.getRows() == null ? List.of() : report.getRows().stream().map(r -> new String[]{
                safe(r.getDimension()), dec(r.getRevenue()), dec(r.getDiscount()),
                dec(r.getTax()), dec(r.getShipping()), dec(r.getRefunded()),
                str(r.getOrders()), str(r.getUnits()), dec(r.getAov()), dec(r.getSharePct())
        }).toList();
        return write(headers, rows, format, baseName, report);
    }

    public ExportResult exportInventory(InventoryReportDTO report, ExportFormat format, String baseName) {
        List<String[]> headers = List.<String[]>of(new String[]{
                "Product","SKU","Category","Warehouse","Qty On Hand","Reserved","Available",
                "Unit Cost","Total Value","Status","Bin"});
        List<String[]> rows = report.getStockValueRows() == null ? List.of()
                : report.getStockValueRows().stream().map(r -> new String[]{
                        safe(r.getProductName()), safe(r.getSku()), safe(r.getCategory()),
                        safe(r.getWarehouseName()), str(r.getQuantityOnHand()), str(r.getReserved()),
                        str(r.getAvailable()), dec(r.getUnitCost()), dec(r.getTotalValue()),
                        safe(r.getStockStatus()), safe(r.getBinLocation())
                }).toList();
        return write(headers, rows, format, baseName, report);
    }

    public ExportResult exportInventoryMovement(InventoryReportDTO report, ExportFormat format, String baseName) {
        List<String[]> headers = List.<String[]>of(new String[]{
                "Movement ID","Product","SKU","Warehouse","Type","Qty","Prev Qty","New Qty","Reason","Reference","Date","Created By"});
        List<String[]> rows = report.getMovementRows() == null ? List.of()
                : report.getMovementRows().stream().map(r -> new String[]{
                        safe(r.getMovementId()), safe(r.getProductName()), safe(r.getSku()),
                        safe(r.getWarehouseName()), safe(r.getMovementType()),
                        str(r.getQuantity()), str(r.getPreviousQuantity()), str(r.getNewQuantity()),
                        safe(r.getReason()), safe(r.getReference()), safe(r.getCreatedAt()), safe(r.getCreatedBy())
                }).toList();
        return write(headers, rows, format, baseName, report);
    }

    public ExportResult exportInventoryAgeing(InventoryReportDTO report, ExportFormat format, String baseName) {
        List<String[]> headers = List.<String[]>of(new String[]{
                "Product","SKU","Warehouse","Qty","Total Value","Days Since Last Movement","Ageing Bucket","Last Movement"});
        List<String[]> rows = report.getAgeingRows() == null ? List.of()
                : report.getAgeingRows().stream().map(r -> new String[]{
                        safe(r.getProductName()), safe(r.getSku()), safe(r.getWarehouseName()),
                        str(r.getQuantityOnHand()), dec(r.getTotalValue()),
                        str(r.getDaysSinceLastMovement()), safe(r.getAgeingBucket()), safe(r.getLastMovementDate())
                }).toList();
        return write(headers, rows, format, baseName, report);
    }

    public ExportResult exportOrders(OrderReportDTO report, ExportFormat format, String baseName) {
        List<String[]> headers = List.<String[]>of(new String[]{
                "Order Number","Customer","Email","Status","Fulfillment","Payment",
                "Source","Total","Items","Created At","Shipped At","Delivered At",
                "Cancellation Reason","Carrier","Tracking"});
        List<String[]> rows = report.getRows() == null ? List.of()
                : report.getRows().stream().map(r -> new String[]{
                        safe(r.getOrderNumber()), safe(r.getCustomerName()), safe(r.getCustomerEmail()),
                        safe(r.getStatus()), safe(r.getFulfillmentStatus()), safe(r.getPaymentStatus()),
                        safe(r.getSource()), dec(r.getTotalAmount()), str(r.getItemCount()),
                        safe(r.getCreatedAt()), safe(r.getShippedAt()), safe(r.getDeliveredAt()),
                        safe(r.getCancellationReason()), safe(r.getCarrierName()), safe(r.getTrackingNumber())
                }).toList();
        return write(headers, rows, format, baseName, report);
    }

    public ExportResult exportCustomers(CustomerReportDTO report, ExportFormat format, String baseName) {
        List<String[]> headers = List.<String[]>of(new String[]{
                "Customer ID","Name","Email","Tier","Status","Total Orders",
                "Lifetime Spend","AOV","First Order","Last Order","Days Since Last Order",
                "Loyalty Points","Acquisition Source","Segment"});
        List<String[]> rows = report.getLtvRows() == null ? List.of()
                : report.getLtvRows().stream().map(r -> new String[]{
                        safe(r.getCustomerId()), safe(r.getCustomerName()), safe(r.getEmail()),
                        safe(r.getTier()), safe(r.getStatus()), str(r.getTotalOrders()),
                        dec(r.getLifetimeSpend()), dec(r.getAverageOrderValue()),
                        safe(r.getFirstOrderAt()), safe(r.getLastOrderAt()), str(r.getDaysSinceLastOrder()),
                        str(r.getLoyaltyPoints()), safe(r.getAcquisitionSource()), safe(r.getSegment())
                }).toList();
        return write(headers, rows, format, baseName, report);
    }

    public ExportResult exportFinancial(FinancialReportDTO report, ExportFormat format, String baseName) {
        List<String[]> headers = List.<String[]>of(new String[]{
                "Period","Gross Revenue","Net Revenue","COGS","Gross Profit","Margin%","Tax","Refunds"});
        List<String[]> rows = report.getRevenueTrend() == null ? List.of()
                : report.getRevenueTrend().stream().map(r -> new String[]{
                        safe(r.getPeriod()), dec(r.getGrossRevenue()), dec(r.getNetRevenue()),
                        dec(r.getCogs()), dec(r.getGrossProfit()), dec(r.getGrossMarginPct()),
                        dec(r.getTax()), dec(r.getRefunds())
                }).toList();
        return write(headers, rows, format, baseName, report);
    }

    public ExportResult exportShipping(ShippingReportDTO report, ExportFormat format, String baseName) {
        List<String[]> headers = List.<String[]>of(new String[]{
                "Shipment Number","Order Number","Carrier","Tracking","Status",
                "Shipped At","Est. Delivery","Actual Delivery","Delivery Days","On Time","Cost","City","Country"});
        List<String[]> rows = report.getRows() == null ? List.of()
                : report.getRows().stream().map(r -> new String[]{
                        safe(r.getShipmentNumber()), safe(r.getOrderNumber()), safe(r.getCarrierName()),
                        safe(r.getTrackingNumber()), safe(r.getStatus()),
                        safe(r.getShippedAt()), safe(r.getEstimatedDeliveryDate()), safe(r.getActualDeliveryDate()),
                        str(r.getDeliveryDays()), str(r.getOnTime()), dec(r.getShippingCost()),
                        safe(r.getRecipientCity()), safe(r.getRecipientCountry())
                }).toList();
        return write(headers, rows, format, baseName, report);
    }

    public ExportResult exportReturns(ReturnReportDTO report, ExportFormat format, String baseName) {
        List<String[]> headers = List.<String[]>of(new String[]{
                "Order Number","Customer","Product","SKU","Returned Qty","Refunded Amount","Reason","Status","Date"});
        List<String[]> rows = report.getRows() == null ? List.of()
                : report.getRows().stream().map(r -> new String[]{
                        safe(r.getOrderNumber()), safe(r.getCustomerName()), safe(r.getProductName()),
                        safe(r.getSku()), str(r.getReturnedQuantity()), dec(r.getRefundedAmount()),
                        safe(r.getReason()), safe(r.getOrderStatus()), safe(r.getCreatedAt())
                }).toList();
        return write(headers, rows, format, baseName, report);
    }

    // ── Core write logic ──────────────────────────────────────────────────────

    private ExportResult write(List<String[]> headers, List<String[]> dataRows,
                                ExportFormat format, String baseName, Object reportData) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String ext = switch (format) {
            case CSV   -> ".csv";
            case EXCEL -> ".xlsx";
            case JSON  -> ".json";
            default    -> ".csv";
        };
        String fileName = sanitize(baseName) + "_" + timestamp + ext;
        Path filePath   = reportsDir.resolve(fileName);

        try {
            switch (format) {
                case CSV   -> writeCsv(filePath, headers, dataRows);
                case EXCEL -> writeExcel(filePath, headers, dataRows);
                case JSON  -> writeJson(filePath, reportData);
            }
            long size = Files.size(filePath);
            log.info("Exported report {} ({} bytes)", fileName, size);
            return new ExportResult("reports/" + fileName, fileName, size, dataRows.size());
        } catch (Exception ex) {
            log.error("Export failed for {}", fileName, ex);
            throw new RuntimeException("Report export failed: " + ex.getMessage(), ex);
        }
    }

    // ── CSV ──────────────────────────────────────────────────────────────────

    private void writeCsv(Path path, List<String[]> headers, List<String[]> rows) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            // UTF-8 BOM for Excel compatibility
            bw.write('\uFEFF');
            for (String[] header : headers) {
                bw.write(toCsvLine(header));
                bw.newLine();
            }
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
            if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                sb.append('"').append(val.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(val);
            }
        }
        return sb.toString();
    }

    // ── Excel (pure-Java OOXML, no POI dependency required — uses ZIP-based approach) ────
    // We use Apache Commons CSV is not available, so we produce a proper XLSX via
    // constructing the ZIP manually with inline XML. This avoids adding POI to build.gradle.

    private void writeExcel(Path path, List<String[]> headers, List<String[]> rows) throws IOException {
        // Build minimal XLSX (ZIP with XML parts)
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
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                    "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                    "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
                    "</Types>");

            // xl/_rels/workbook.xml.rels
            addZipEntry(zos, "xl/_rels/workbook.xml.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                    "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" +
                    "</Relationships>");

            // xl/workbook.xml
            addZipEntry(zos, "xl/workbook.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                    "<sheets><sheet name=\"Report\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");

            // xl/styles.xml (header bold style = xf index 1)
            addZipEntry(zos, "xl/styles.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
                    "<fonts count=\"2\"><font><sz val=\"11\"/></font><font><b/><sz val=\"11\"/></font></fonts>" +
                    "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills>" +
                    "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
                    "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
                    "<cellXfs count=\"2\">" +
                    "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
                    "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
                    "</cellXfs></styleSheet>");

            // xl/worksheets/sheet1.xml
            StringBuilder ws = new StringBuilder();
            ws.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
            ws.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
            ws.append("<sheetData>");
            int rowNum = 1;
            for (String[] header : headers) {
                ws.append(xmlRow(header, rowNum++, true));
            }
            for (String[] row : rows) {
                ws.append(xmlRow(row, rowNum++, false));
            }
            ws.append("</sheetData></worksheet>");
            addZipEntry(zos, "xl/worksheets/sheet1.xml", ws.toString());
        }
    }

    private void addZipEntry(java.util.zip.ZipOutputStream zos, String name, String content) throws IOException {
        zos.putNextEntry(new java.util.zip.ZipEntry(name));
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        zos.write(bytes);
        zos.closeEntry();
    }

    private String xmlRow(String[] cols, int rowNum, boolean bold) {
        StringBuilder sb = new StringBuilder("<row r=\"").append(rowNum).append("\">");
        for (int i = 0; i < cols.length; i++) {
            String cellRef = colName(i) + rowNum;
            String val = cols[i] != null ? xmlEsc(cols[i]) : "";
            sb.append("<c r=\"").append(cellRef).append("\" t=\"inlineStr\"");
            if (bold) sb.append(" s=\"1\"");
            sb.append("><is><t>").append(val).append("</t></is></c>");
        }
        sb.append("</row>");
        return sb.toString();
    }

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
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    // ── JSON ─────────────────────────────────────────────────────────────────

    private void writeJson(Path path, Object data) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), data);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String safe(Object o)   { return o != null ? o.toString() : ""; }
    private String dec(java.math.BigDecimal b) { return b != null ? b.toPlainString() : "0"; }
    private String str(Object o)    { return o != null ? o.toString() : ""; }
    private String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9_-]", "_").replaceAll("_+", "_");
    }

    // ── Result DTO ────────────────────────────────────────────────────────────

    public record ExportResult(String relativePath, String fileName, long fileSizeBytes, int rowCount) {}
}
