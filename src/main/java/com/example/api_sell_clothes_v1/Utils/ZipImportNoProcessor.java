package com.example.api_sell_clothes_v1.Utils;

import com.example.api_sell_clothes_v1.Exceptions.FileHandlingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Lớp tiện ích xử lý file ZIP chứa Excel và ảnh thumbnail cho sản phẩm không có biến thể
 */
@Slf4j
public class ZipImportNoProcessor  {

    /**
     * Xử lý file ZIP và trích xuất dữ liệu
     */
    public ImportData processZipFile(MultipartFile zipFile) throws IOException {
        if (zipFile == null || zipFile.isEmpty()) {
            throw new FileHandlingException("File ZIP không được để trống");
        }

        log.info("Bắt đầu xử lý file ZIP: {}, kích thước: {} bytes", zipFile.getOriginalFilename(), zipFile.getSize());

        // Tạo thư mục tạm thời để giải nén
        String tempDir = System.getProperty("java.io.tmpdir") + "/product_import_" + System.nanoTime();
        Path tempPath = Paths.get(tempDir);
        Files.createDirectories(tempPath);

        ImportData importData = new ImportData();
        importData.totalSize = zipFile.getSize();

        try (ZipInputStream zipIn = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                String entryName = entry.getName();
                log.debug("Đang giải nén ZIP entry: {}", entryName);

                // Ghi nhận tất cả đường dẫn file
                if (!entry.isDirectory()) {
                    importData.allFilePaths.add(entryName);
                }

                Path filePath = Paths.get(tempDir, entryName);

                // Tạo thư mục cha nếu chưa tồn tại
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                    continue;
                } else {
                    Files.createDirectories(filePath.getParent());
                }

                // Lưu file vào đĩa
                try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zipIn.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }

                // Nếu là file Excel
                if (entryName.toLowerCase().endsWith(".xlsx") || entryName.toLowerCase().endsWith(".xls")) {
                    log.debug("Đã tìm thấy file Excel trong ZIP tại: {}", filePath);
                    try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                        importData.workbook = WorkbookFactory.create(fis);
                        importData.excelFileName = Paths.get(entryName).getFileName().toString();
                    } catch (Exception e) {
                        log.error("Lỗi khi đọc file Excel từ ZIP: {}", filePath, e);
                    }
                }

                // Đọc file ảnh và lưu vào bộ nhớ
                if (isImageFile(entryName)) {
                    log.debug("Đã tìm thấy file ảnh: {}", entryName);
                    try {
                        byte[] imageData = Files.readAllBytes(filePath);
                        importData.imagesByName.put(entryName, imageData);
                        importData.totalImageCount++;
                    } catch (Exception e) {
                        log.error("Lỗi khi đọc file ảnh: {}", entryName, e);
                    }
                }
            }
        } finally {
            // Xóa thư mục tạm sau khi xử lý xong
            try {
                deleteDirectory(tempPath.toFile());
            } catch (Exception e) {
                log.warn("Không thể xóa thư mục tạm: {}", tempPath, e);
            }
        }

        // Tìm thư mục Images nếu chưa tìm thấy trực tiếp
        if (!importData.hasImagesFolder()) {
            log.info("Không tìm thấy thư mục Images trực tiếp, đang tìm kiếm đệ quy...");
            // Tìm kiếm dựa vào đường dẫn
            boolean foundImagesFolder = importData.allFilePaths.stream()
                    .anyMatch(path -> path.startsWith("Images/") || path.startsWith("images/"));
            if (foundImagesFolder) {
                log.info("Tìm thấy thư mục Images trong cấu trúc đường dẫn");
            } else {
                log.warn("Không tìm thấy thư mục Images trong ZIP");
            }
        }

        // Ghi log chi tiết
        log.info("Đã xử lý xong file ZIP:");
        log.info("- Tổng số file: {}", importData.allFilePaths.size());
        log.info("- File Excel: {}", importData.excelFileName);
        log.info("- Số lượng ảnh: {}", importData.totalImageCount);
        log.info("- Các đường dẫn ảnh: {}",
                importData.allFilePaths.stream()
                        .filter(this::isImageFile)
                        .collect(Collectors.joining(", ")));

        return importData;
    }

    /**
     * Xóa thư mục và tất cả nội dung bên trong
     */
    private boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    /**
     * Kiểm tra nếu file là file ảnh dựa vào phần mở rộng
     */
    private boolean isImageFile(String filePath) {
        String lcPath = filePath.toLowerCase();
        return lcPath.endsWith(".jpg") || lcPath.endsWith(".jpeg") ||
                lcPath.endsWith(".png") || lcPath.endsWith(".gif") ||
                lcPath.endsWith(".webp") || lcPath.endsWith(".bmp");
    }

    /**
     * Lớp chứa dữ liệu đã xử lý từ file ZIP
     */
    public static class ImportData {
        private Workbook workbook;
        private String excelFileName;
        private Map<String, byte[]> imagesByName = new HashMap<>();
        private int totalImageCount = 0;
        private long totalSize = 0;
        private List<String> allFilePaths = new ArrayList<>();

        /**
         * Lấy nội dung file từ đường dẫn
         */
        public byte[] getFileContent(String path) {
            // Thử tìm chính xác
            if (imagesByName.containsKey(path)) {
                return imagesByName.get(path);
            }

            // Thử tìm không phân biệt hoa thường
            String lowercasePath = path.toLowerCase();
            for (Map.Entry<String, byte[]> entry : imagesByName.entrySet()) {
                if (entry.getKey().toLowerCase().equals(lowercasePath)) {
                    return entry.getValue();
                }
            }

            // Thử tìm nếu path chứa trong đường dẫn đầy đủ
            for (Map.Entry<String, byte[]> entry : imagesByName.entrySet()) {
                if (entry.getKey().toLowerCase().contains(lowercasePath)) {
                    return entry.getValue();
                }
            }

            // Thử tìm theo phần cuối của path
            for (Map.Entry<String, byte[]> entry : imagesByName.entrySet()) {
                if (entry.getKey().toLowerCase().endsWith(lowercasePath)) {
                    return entry.getValue();
                }
            }

            // Thử tìm theo tên file (phần cuối cùng của path)
            String fileName = new File(path).getName().toLowerCase();
            for (Map.Entry<String, byte[]> entry : imagesByName.entrySet()) {
                String entryFileName = new File(entry.getKey()).getName().toLowerCase();
                if (entryFileName.equals(fileName)) {
                    return entry.getValue();
                }
            }

            return null;
        }

        /**
         * Format kích thước file để hiển thị
         */
        public String getFormattedSize() {
            if (totalSize < 1024) {
                return totalSize + " bytes";
            } else if (totalSize < 1024 * 1024) {
                return String.format("%.2f KB", totalSize / 1024.0);
            } else if (totalSize < 1024 * 1024 * 1024) {
                return String.format("%.2f MB", totalSize / (1024.0 * 1024));
            } else {
                return String.format("%.2f GB", totalSize / (1024.0 * 1024 * 1024));
            }
        }

        public Workbook getWorkbook() {
            return workbook;
        }

        public String getExcelFileName() {
            return excelFileName;
        }

        public int getTotalImageCount() {
            return totalImageCount;
        }

        public Map<String, byte[]> getImagesBySku() {
            // Phương thức này chỉ để tương thích với code cũ, không thực sự có SKU
            return null;
        }

        public List<String> getAllFilePaths() {
            return allFilePaths;
        }

        public boolean hasImagesFolder() {
            return allFilePaths.stream()
                    .anyMatch(path -> path.startsWith("Images/") || path.startsWith("images/"));
        }
    }
}