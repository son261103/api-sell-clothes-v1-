package com.example.api_sell_clothes_v1.Service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.*;

/**
 * Dịch vụ đơn giản hóa để tạo file Excel mà không bị lỗi
 */
@Slf4j
@Service
public class SimplifiedExcelService {

    /**
     * Tạo và trả về một file Excel đơn giản trực tiếp, không sử dụng ZIP
     */
    public void generateSimpleExcelFile(HttpServletResponse response) {
        try {
            // Thiết lập response headers
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=template.xlsx");

            // Tạo workbook trong memory
            Workbook workbook = new XSSFWorkbook();

            // Tạo một sheet đơn giản
            Sheet sheet = workbook.createSheet("Sheet1");

            // Tạo header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Cột 1", "Cột 2", "Cột 3"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Thêm một số dữ liệu mẫu
            for (int i = 1; i < 5; i++) {
                Row row = sheet.createRow(i);
                for (int j = 0; j < 3; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue("Dữ liệu " + i + "-" + j);
                }
            }

            // Điều chỉnh độ rộng cột
            for (int i = 0; i < 3; i++) {
                sheet.autoSizeColumn(i);
            }

            // Ghi workbook trực tiếp vào response output stream
            try (OutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            }

            // Đóng workbook sau khi đã ghi xong
            workbook.close();

            log.info("Đã tạo file Excel đơn giản thành công");

        } catch (IOException e) {
            log.error("Lỗi khi tạo file Excel đơn giản: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Tạo một file Excel đơn giản và lưu vào ổ đĩa
     * @return Đường dẫn đến file đã tạo
     */
    public String generateAndSaveExcelFile() {
        String filePath = System.getProperty("java.io.tmpdir") + "/simple_template_" + System.currentTimeMillis() + ".xlsx";

        try {
            // Tạo workbook
            Workbook workbook = new XSSFWorkbook();

            // Tạo một sheet đơn giản
            Sheet sheet = workbook.createSheet("Sheet1");

            // Tạo header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Cột 1", "Cột 2", "Cột 3"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Thêm một số dữ liệu mẫu
            for (int i = 1; i < 5; i++) {
                Row row = sheet.createRow(i);
                for (int j = 0; j < 3; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue("Dữ liệu " + i + "-" + j);
                }
            }

            // Lưu workbook vào file
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
                outputStream.flush();
            }

            // Đóng workbook sau khi đã ghi xong
            workbook.close();

            log.info("Đã tạo và lưu file Excel đơn giản tại: {}", filePath);

            return filePath;

        } catch (IOException e) {
            log.error("Lỗi khi tạo và lưu file Excel đơn giản: ", e);
            return null;
        }
    }

    /**
     * Gửi file có sẵn đến client
     * @param filePath Đường dẫn file cần gửi
     * @param response HttpServletResponse
     */
    public void sendExistingFile(String filePath, HttpServletResponse response) {
        File file = new File(filePath);

        if (!file.exists()) {
            log.error("File không tồn tại: {}", filePath);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            // Thiết lập response headers
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
            response.setHeader("Content-Length", String.valueOf(file.length()));

            // Ghi file vào response
            try (FileInputStream inputStream = new FileInputStream(file);
                 OutputStream outputStream = response.getOutputStream()) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }

            log.info("Đã gửi file thành công: {}", filePath);

        } catch (IOException e) {
            log.error("Lỗi khi gửi file: {}", filePath, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Phương thức kết hợp để tạo và gửi file Excel một cách an toàn
     */
    public void generateAndSendSafeExcel(HttpServletResponse response) {
        // Tạo và lưu file Excel
        String filePath = generateAndSaveExcelFile();

        if (filePath != null) {
            // Kiểm tra file có thể đọc được không
            try {
                try (FileInputStream fis = new FileInputStream(filePath);
                     Workbook testWorkbook = WorkbookFactory.create(fis)) {
                    // Nếu đọc được workbook, file không bị hỏng
                    log.info("Đã xác thực file Excel không bị hỏng");
                }

                // Gửi file đến client
                sendExistingFile(filePath, response);

                // Xóa file tạm sau khi gửi xong
                try {
                    new File(filePath).delete();
                } catch (Exception e) {
                    log.warn("Không thể xóa file tạm: {}", filePath, e);
                }

            } catch (IOException e) {
                log.error("File Excel bị hỏng: {}", filePath, e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                // Xóa file hỏng
                try {
                    new File(filePath).delete();
                } catch (Exception ex) {
                    log.warn("Không thể xóa file tạm: {}", filePath, ex);
                }
            }
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}