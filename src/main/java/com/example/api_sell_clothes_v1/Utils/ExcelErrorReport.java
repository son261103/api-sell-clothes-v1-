package com.example.api_sell_clothes_v1.Utils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class để quản lý và báo cáo lỗi khi import Excel
 */
@Slf4j
public class ExcelErrorReport {

    @Getter
    private final Workbook workbook;

    // Map lưu trữ lỗi theo sheet và dòng
    private final Map<String, Map<Integer, List<String>>> errors = new TreeMap<>();

    // Đếm số lỗi
    @Getter
    private int totalErrors = 0;

    // Danh sách sheet cần xử lý
    private final List<String> sheetNames = new ArrayList<>();

    // Cell style cho dòng lỗi
    private CellStyle errorStyle;

    // Cell style cho comment lỗi
    private CellStyle commentStyle;

    public ExcelErrorReport(Workbook workbook) {
        this.workbook = workbook;
        initStyles();

        // Lưu tên các sheet để xử lý
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet.getSheetName().equals("Sản Phẩm") || sheet.getSheetName().equals("Biến Thể")) {
                sheetNames.add(sheet.getSheetName());
                errors.put(sheet.getSheetName(), new TreeMap<>());
            }
        }

        // Thêm sheet báo cáo lỗi nếu chưa có
        if (workbook.getSheet("Báo Cáo Lỗi") == null) {
            workbook.createSheet("Báo Cáo Lỗi");
        }
    }

    /**
     * Khởi tạo các style cho báo cáo lỗi
     */
    private void initStyles() {
        // Style cho dòng lỗi - nền đỏ nhạt
        errorStyle = workbook.createCellStyle();
        if (workbook instanceof XSSFWorkbook) {
            XSSFCellStyle xssfCellStyle = (XSSFCellStyle) errorStyle;
            xssfCellStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 255, (byte) 200, (byte) 200}, null));
        } else {
            errorStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        }
        errorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Style cho comment lỗi - chữ đỏ, in đậm
        commentStyle = workbook.createCellStyle();
        Font commentFont = workbook.createFont();
        commentFont.setColor(IndexedColors.RED.getIndex());
        commentFont.setBold(true);
        commentStyle.setFont(commentFont);
    }

    /**
     * Thêm lỗi cho một dòng cụ thể trong sheet
     */
    public void addError(String sheetName, int rowIndex, String errorMessage) {
        if (!errors.containsKey(sheetName)) {
            errors.put(sheetName, new TreeMap<>());
        }

        Map<Integer, List<String>> sheetErrors = errors.get(sheetName);
        if (!sheetErrors.containsKey(rowIndex)) {
            sheetErrors.put(rowIndex, new ArrayList<>());
        }

        sheetErrors.get(rowIndex).add(errorMessage);
        totalErrors++;
    }

    /**
     * Đánh dấu tất cả các dòng lỗi trong workbook
     */
    public void markErrors() {
        // Đánh dấu lỗi trong từng sheet
        for (String sheetName : sheetNames) {
            if (errors.containsKey(sheetName)) {
                markSheetErrors(sheetName, errors.get(sheetName));
            }
        }

        // Tạo báo cáo tổng hợp lỗi
        createErrorReport();
    }

    /**
     * Đánh dấu các dòng lỗi trong một sheet
     */
    private void markSheetErrors(String sheetName, Map<Integer, List<String>> rowErrors) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) return;

        // Thêm cột "Lỗi" vào cuối nếu chưa có
        Row headerRow = sheet.getRow(0);
        int lastCellNum = headerRow.getLastCellNum();
        Cell errorHeaderCell = headerRow.createCell(lastCellNum);
        errorHeaderCell.setCellValue("Lỗi");
        errorHeaderCell.setCellStyle(commentStyle);

        // Đánh dấu từng dòng lỗi
        for (Map.Entry<Integer, List<String>> entry : rowErrors.entrySet()) {
            int rowIndex = entry.getKey();
            List<String> errorMessages = entry.getValue();

            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            // Tô màu cho toàn bộ dòng
            for (int i = 0; i < row.getLastCellNum(); i++) {
                Cell cell = row.getCell(i);
                if (cell == null) {
                    cell = row.createCell(i);
                }
                cell.setCellStyle(errorStyle);
            }

            // Thêm thông báo lỗi vào cột cuối
            Cell errorMessageCell = row.createCell(lastCellNum);
            errorMessageCell.setCellValue(String.join("; ", errorMessages));
            errorMessageCell.setCellStyle(commentStyle);
        }

        // Tự động điều chỉnh độ rộng cột lỗi
        sheet.autoSizeColumn(lastCellNum);
    }

    /**
     * Tạo sheet báo cáo lỗi chi tiết
     */
    private void createErrorReport() {
        Sheet reportSheet = workbook.getSheet("Báo Cáo Lỗi");
        workbook.setSheetOrder("Báo Cáo Lỗi", 0); // Di chuyển sheet báo cáo lên đầu

        // Tạo tiêu đề
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Tạo hàng tiêu đề
        Row titleRow = reportSheet.createRow(0);
        Cell titleCell = titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BÁO CÁO LỖI IMPORT DỮ LIỆU");
        titleCell.setCellStyle(headerStyle);

        // Gộp ô tiêu đề
        reportSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        // Tạo hàng header
        Row headerRow = reportSheet.createRow(2);
        String[] headers = {"Sheet", "Dòng", "Ô", "Mô tả lỗi"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Thêm dữ liệu báo cáo
        int rowNum = 3;
        for (String sheetName : errors.keySet()) {
            Map<Integer, List<String>> sheetErrors = errors.get(sheetName);
            for (Map.Entry<Integer, List<String>> entry : sheetErrors.entrySet()) {
                int errorRowIndex = entry.getKey();
                for (String errorMessage : entry.getValue()) {
                    Row row = reportSheet.createRow(rowNum++);

                    Cell sheetCell = row.createCell(0);
                    sheetCell.setCellValue(sheetName);

                    Cell rowCell = row.createCell(1);
                    rowCell.setCellValue(errorRowIndex + 1); // +1 vì Excel hiển thị từ 1

                    Cell cellRefCell = row.createCell(2);
                    cellRefCell.setCellValue("Dòng " + (errorRowIndex + 1));

                    Cell messageCell = row.createCell(3);
                    messageCell.setCellValue(errorMessage);
                }
            }
        }

        // Thêm thông tin tổng hợp
        Row summaryRow = reportSheet.createRow(rowNum + 1);
        Cell summaryLabelCell = summaryRow.createCell(0);
        summaryLabelCell.setCellValue("Tổng số lỗi:");
        summaryLabelCell.setCellStyle(headerStyle);

        Cell summaryValueCell = summaryRow.createCell(1);
        summaryValueCell.setCellValue(totalErrors);

        // Điều chỉnh độ rộng cột
        for (int i = 0; i < headers.length; i++) {
            reportSheet.autoSizeColumn(i);
        }
    }

    /**
     * Chuyển workbook thành mảng byte
     */
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        return outputStream.toByteArray();
    }

    /**
     * Kiểm tra xem có lỗi nào không
     */
    public boolean hasErrors() {
        return totalErrors > 0;
    }

    /**
     * Đếm số dòng có lỗi
     */
    public int getErrorRowCount() {
        int count = 0;
        for (Map<Integer, List<String>> sheetErrors : errors.values()) {
            count += sheetErrors.size();
        }
        return count;
    }
}