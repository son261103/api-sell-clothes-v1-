package com.example.api_sell_clothes_v1.Utils;

import com.example.api_sell_clothes_v1.Exceptions.FileHandlingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZipImportProcessor {
    private String lastExcelFileName; // Lưu tên file Excel cuối cùng đã tìm thấy
    private boolean hasExcelFile; // Đánh dấu có tìm thấy file Excel không
    private boolean hasImagesFolder; // Đánh dấu có thư mục Images không

    /**
     * Xử lý file ZIP và trích xuất file Excel và ảnh
     * @param zipFile File ZIP tải lên
     * @return Workbook và Map chứa ảnh theo cấu trúc SKU/tên file
     */
    public ImportData processZipFile(MultipartFile zipFile) throws IOException {
        if (zipFile == null || zipFile.isEmpty()) {
            throw new FileHandlingException("File ZIP không được để trống");
        }

        // Reset các biến trạng thái
        hasExcelFile = false;
        hasImagesFolder = false;
        lastExcelFileName = "";

        // Tạo thư mục tạm để giải nén
        Path tempDir = Files.createTempDirectory("product_import_");

        try {
            // Giải nén file ZIP
            unzipFile(zipFile.getInputStream(), tempDir);

            // Tìm file Excel
            Path excelFile = findExcelFile(tempDir);
            if (excelFile == null) {
                log.warn("Không tìm thấy file Excel trong file ZIP");
                return new ImportData(null, new HashMap<>(), "", zipFile.getSize());
            }

            // Đánh dấu đã tìm thấy file Excel
            hasExcelFile = true;

            // Lưu tên file Excel
            lastExcelFileName = excelFile.getFileName().toString();

            // Đọc file Excel
            Workbook workbook = new XSSFWorkbook(Files.newInputStream(excelFile));

            // Tìm thư mục Images - Sử dụng phương thức tìm kiếm đệ quy
            Path imagesDir = findImagesDir(tempDir);

            // Đọc cấu trúc thư mục ảnh theo SKU
            Map<String, Map<String, byte[]>> imagesBySku = new HashMap<>();
            Map<String, byte[]> allFiles = new HashMap<>(); // Lưu tất cả các file để dễ tìm kiếm

            if (imagesDir != null && Files.isDirectory(imagesDir)) {
                hasImagesFolder = true;
                log.info("Tìm thấy thư mục Images tại: {}", imagesDir);

                // Duyệt qua tất cả các file trong thư mục Images và lưu vào map
                Files.walk(imagesDir)
                        .filter(Files::isRegularFile)
                        .forEach(file -> {
                            try {
                                Path relativePath = imagesDir.relativize(file);
                                String pathString = relativePath.toString().replace('\\', '/');
                                byte[] fileContent = Files.readAllBytes(file);
                                allFiles.put(pathString, fileContent);

                                // Nếu là file trong thư mục SKU (không phải thumbnails)
                                Path parent = file.getParent();
                                if (parent != null && !parent.getFileName().toString().equals("thumbnails")) {
                                    String sku = parent.getFileName().toString();
                                    imagesBySku.computeIfAbsent(sku, k -> new HashMap<>());
                                    imagesBySku.get(sku).put(file.getFileName().toString(), fileContent);
                                }
                            } catch (IOException e) {
                                log.error("Lỗi khi đọc file: {}", file, e);
                            }
                        });
            } else {
                log.warn("Không tìm thấy thư mục Images trong ZIP");
            }

            return new ImportData(workbook, imagesBySku, allFiles, lastExcelFileName, zipFile.getSize());

        } catch (IOException e) {
            log.error("Lỗi khi xử lý file ZIP: ", e);
            throw new FileHandlingException("Không thể xử lý file ZIP: " + e.getMessage());
        } finally {
            // Xóa thư mục tạm
            deleteDirectory(tempDir.toFile());
        }
    }

    /**
     * Tìm thư mục Images trong thư mục giải nén - hỗ trợ tìm kiếm đệ quy
     */
    private Path findImagesDir(Path tempDir) throws IOException {
        // Trước tiên thử tìm trực tiếp
        Path directPath = tempDir.resolve("Images");
        if (Files.exists(directPath) && Files.isDirectory(directPath)) {
            return directPath;
        }

        // Nếu không tìm thấy, tìm kiếm đệ quy
        log.info("Không tìm thấy thư mục Images trực tiếp, đang tìm kiếm đệ quy...");
        return Files.walk(tempDir, 3) // Giới hạn độ sâu tìm kiếm để tránh tìm quá sâu
                .filter(path -> path.getFileName().toString().equals("Images") && Files.isDirectory(path))
                .findFirst()
                .orElse(null);
    }

    /**
     * Lớp chứa dữ liệu đã import từ ZIP
     */
    public static class ImportData {
        private final Workbook workbook;
        private final Map<String, Map<String, byte[]>> imagesBySku;
        private final Map<String, byte[]> allFiles; // Lưu tất cả các file (bao gồm cả thumbnails)
        private final String excelFileName;
        private final long totalSize;  // Tổng kích thước của file ZIP

        public ImportData(Workbook workbook, Map<String, Map<String, byte[]>> imagesBySku, String excelFileName, long totalSize) {
            this(workbook, imagesBySku, new HashMap<>(), excelFileName, totalSize);
        }

        public ImportData(Workbook workbook, Map<String, Map<String, byte[]>> imagesBySku,
                          Map<String, byte[]> allFiles, String excelFileName, long totalSize) {
            this.workbook = workbook;
            this.imagesBySku = imagesBySku;
            this.allFiles = allFiles;
            this.excelFileName = excelFileName;
            this.totalSize = totalSize;
        }

        public Workbook getWorkbook() {
            return workbook;
        }

        public Map<String, Map<String, byte[]>> getImagesBySku() {
            return imagesBySku;
        }

        // Alias cho getImagesBySku() để tương thích với code hiện tại
        public Map<String, Map<String, byte[]>> getSkuImageMap() {
            return imagesBySku;
        }

        // Trả về tên file Excel
        public String getExcelFileName() {
            return excelFileName;
        }

        // Trả về tổng kích thước
        public long getTotalSize() {
            return totalSize;
        }

        // Trả về kích thước được định dạng
        public String getFormattedSize() {
            DecimalFormat df = new DecimalFormat("#.##");
            double sizeInMB = (double) totalSize / (1024 * 1024);
            return df.format(sizeInMB) + " MB";
        }

        // Trả về tổng số lượng ảnh
        public int getTotalImageCount() {
            int count = 0;
            for (Map<String, byte[]> skuImages : imagesBySku.values()) {
                count += skuImages.size();
            }
            return count;
        }

        // Kiểm tra xem có thư mục nào không có ảnh chính
        public List<String> getSkusWithoutMainImage() {
            List<String> skusWithoutMain = new ArrayList<>();
            for (Map.Entry<String, Map<String, byte[]>> entry : imagesBySku.entrySet()) {
                if (!entry.getValue().containsKey("main.jpg")) {
                    skusWithoutMain.add(entry.getKey());
                }
            }
            return skusWithoutMain;
        }

        // Trả về danh sách SKU dưới dạng thông tin thư mục
        public List<Map<String, Object>> getSkuList() {
            List<Map<String, Object>> skuFolders = new ArrayList<>();

            for (Map.Entry<String, Map<String, byte[]>> entry : imagesBySku.entrySet()) {
                String sku = entry.getKey();
                Map<String, byte[]> images = entry.getValue();

                Map<String, Object> skuInfo = new HashMap<>();
                skuInfo.put("sku", sku);
                skuInfo.put("hasMainImage", images.containsKey("main.jpg"));

                // Đếm số lượng ảnh phụ (không phải main.jpg)
                int secondaryCount = 0;
                List<String> imageNames = new ArrayList<>();
                for (String imageName : images.keySet()) {
                    imageNames.add(imageName);
                    if (!imageName.equals("main.jpg")) {
                        secondaryCount++;
                    }
                }

                skuInfo.put("secondaryImageCount", secondaryCount);
                skuInfo.put("imageNames", imageNames);

                skuFolders.add(skuInfo);
            }

            return skuFolders;
        }

        /**
         * Lấy ảnh chính của biến thể theo SKU
         */
        public byte[] getMainImageForSku(String sku) {
            Map<String, byte[]> images = imagesBySku.get(sku);
            if (images != null) {
                return images.get("main.jpg");
            }
            return null;
        }

        /**
         * Lấy danh sách ảnh phụ của biến thể theo SKU
         */
        public List<byte[]> getSecondaryImagesForSku(String sku) {
            Map<String, byte[]> images = imagesBySku.get(sku);
            if (images != null) {
                List<byte[]> secondaryImages = new ArrayList<>();
                for (Map.Entry<String, byte[]> entry : images.entrySet()) {
                    String imageName = entry.getKey();
                    // Chỉ lấy ảnh phụ (không phải main.jpg)
                    if (!imageName.equals("main.jpg")) {
                        secondaryImages.add(entry.getValue());
                    }
                }
                return secondaryImages;
            }
            return Collections.emptyList();
        }

        /**
         * Kiểm tra xem có ảnh cho SKU không
         */
        public boolean hasImagesForSku(String sku) {
            return imagesBySku.containsKey(sku);
        }

        /**
         * Kiểm tra xem có file Excel không
         */
        public boolean hasExcelFile() {
            return workbook != null;
        }

        /**
         * Kiểm tra xem có thư mục Images không
         */
        public boolean hasImagesFolder() {
            return !imagesBySku.isEmpty();
        }

        /**
         * Trả về nội dung file theo đường dẫn tương đối
         * @param filePath Đường dẫn tương đối của file (ví dụ: thumbnails/image.jpg)
         * @return Nội dung file hoặc null nếu không tìm thấy
         */
        public byte[] getFileContent(String filePath) {
            // Chuẩn hóa đường dẫn
            String normalizedPath = filePath;
            if (normalizedPath.startsWith("/")) {
                normalizedPath = normalizedPath.substring(1);
            }

            // Tìm file trong map allFiles
            if (allFiles.containsKey(normalizedPath)) {
                return allFiles.get(normalizedPath);
            }

            // Nếu path có "Images/" ở đầu, thử tìm không có prefix này
            if (normalizedPath.startsWith("Images/")) {
                String withoutPrefix = normalizedPath.substring(7);
                if (allFiles.containsKey(withoutPrefix)) {
                    return allFiles.get(withoutPrefix);
                }
            } else {
                // Thử thêm prefix "Images/"
                String withPrefix = "Images/" + normalizedPath;
                if (allFiles.containsKey(withPrefix)) {
                    return allFiles.get(withPrefix);
                }
            }

            // Tìm tất cả file có chứa tên file cuối cùng
            String fileName = new File(normalizedPath).getName();
            for (Map.Entry<String, byte[]> entry : allFiles.entrySet()) {
                if (entry.getKey().endsWith(fileName)) {
                    return entry.getValue();
                }
            }

            // Không tìm thấy
            return null;
        }

        /**
         * Lấy tất cả các tên file trong thư mục thumbnails
         */
        public List<String> getThumbnailFileNames() {
            List<String> thumbnailFiles = new ArrayList<>();
            for (String path : allFiles.keySet()) {
                if (path.contains("thumbnails/")) {
                    thumbnailFiles.add(path);
                }
            }
            return thumbnailFiles;
        }

        /**
         * Lấy danh sách tất cả các tên file trong ZIP
         */
        public Set<String> getAllFilePaths() {
            return allFiles.keySet();
        }
    }

    /**
     * Giải nén file ZIP vào thư mục đích
     */
    private void unzipFile(InputStream zipStream, Path destDir) throws IOException {
        byte[] buffer = new byte[1024];

        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {
                // In ra thông tin entry để debug
                log.debug("Đang giải nén ZIP entry: {}", zipEntry.getName());

                Path newPath = zipSlipProtect(zipEntry, destDir);

                if (zipEntry.isDirectory()) {
                    Files.createDirectories(newPath);

                    // Kiểm tra nếu là thư mục Images
                    if (newPath.getFileName().toString().equalsIgnoreCase("Images")) {
                        hasImagesFolder = true;
                        log.debug("Đã tìm thấy thư mục Images trong ZIP tại: {}", newPath);
                    }
                } else {
                    // Tạo thư mục cha nếu chưa tồn tại
                    if (newPath.getParent() != null) {
                        if (Files.notExists(newPath.getParent())) {
                            Files.createDirectories(newPath.getParent());
                        }
                    }

                    // Kiểm tra nếu là file Excel
                    if (newPath.toString().toLowerCase().endsWith(".xlsx")) {
                        hasExcelFile = true;
                        lastExcelFileName = newPath.getFileName().toString();
                        log.debug("Đã tìm thấy file Excel: {}", newPath);
                    }

                    // Ghi file
                    try (FileOutputStream fos = new FileOutputStream(newPath.toFile())) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }

                zipEntry = zis.getNextEntry();
            }

            zis.closeEntry();
        }
    }

    /**
     * Bảo vệ chống zip slip vulnerability
     */
    private Path zipSlipProtect(ZipEntry zipEntry, Path targetDir) throws IOException {
        Path targetDirResolved = targetDir.resolve(zipEntry.getName());

        // Kiểm tra path traversal
        Path normalizePath = targetDirResolved.normalize();
        if (!normalizePath.startsWith(targetDir)) {
            throw new IOException("Zip entry tràn khỏi thư mục mục tiêu: " + zipEntry.getName());
        }

        return normalizePath;
    }

    /**
     * Tìm file Excel trong thư mục đã giải nén
     */
    private Path findExcelFile(Path dir) throws IOException {
        // Tìm file .xlsx
        Optional<Path> excelFile = Files.walk(dir)
                .filter(path -> path.toString().toLowerCase().endsWith(".xlsx"))
                .findFirst();

        return excelFile.orElse(null);
    }

    /**
     * Xóa thư mục đệ quy
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
}