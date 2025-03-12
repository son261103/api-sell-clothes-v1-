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
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZipImportProcessor {

    /**
     * Xử lý file ZIP và trích xuất file Excel và ảnh
     * @param zipFile File ZIP tải lên
     * @return Workbook và Map chứa ảnh theo cấu trúc SKU/tên file
     */
    public ImportData processZipFile(MultipartFile zipFile) throws IOException {
        if (zipFile == null || zipFile.isEmpty()) {
            throw new FileHandlingException("File ZIP không được để trống");
        }

        // Tạo thư mục tạm để giải nén
        Path tempDir = Files.createTempDirectory("product_import_");

        try {
            // Giải nén file ZIP
            unzipFile(zipFile.getInputStream(), tempDir);

            // Tìm file Excel
            Path excelFile = findExcelFile(tempDir);
            if (excelFile == null) {
                throw new FileHandlingException("Không tìm thấy file Excel trong file ZIP");
            }

            // Đọc file Excel
            Workbook workbook = new XSSFWorkbook(Files.newInputStream(excelFile));

            // Tìm thư mục Images
            Path imagesDir = tempDir.resolve("Images");

            // Đọc cấu trúc thư mục ảnh theo SKU
            Map<String, Map<String, byte[]>> imagesBySku = new HashMap<>();

            if (Files.exists(imagesDir) && Files.isDirectory(imagesDir)) {
                Files.list(imagesDir).forEach(skuDir -> {
                    if (Files.isDirectory(skuDir)) {
                        String sku = skuDir.getFileName().toString();
                        Map<String, byte[]> variantImages = new HashMap<>();

                        try {
                            Files.list(skuDir).forEach(imageFile -> {
                                if (Files.isRegularFile(imageFile)) {
                                    String imageName = imageFile.getFileName().toString();
                                    try {
                                        byte[] imageData = Files.readAllBytes(imageFile);
                                        variantImages.put(imageName, imageData);
                                    } catch (IOException e) {
                                        log.error("Lỗi khi đọc file ảnh {}: {}", imageFile, e.getMessage());
                                    }
                                }
                            });

                            if (!variantImages.isEmpty()) {
                                imagesBySku.put(sku, variantImages);
                            }
                        } catch (IOException e) {
                            log.error("Lỗi khi đọc thư mục SKU {}: {}", skuDir, e.getMessage());
                        }
                    }
                });
            } else {
                log.warn("Không tìm thấy thư mục Images trong ZIP");
            }

            return new ImportData(workbook, imagesBySku);

        } catch (IOException e) {
            log.error("Lỗi khi xử lý file ZIP: ", e);
            throw new FileHandlingException("Không thể xử lý file ZIP: " + e.getMessage());
        } finally {
            // Xóa thư mục tạm
            deleteDirectory(tempDir.toFile());
        }
    }

    /**
     * Lớp chứa dữ liệu đã import từ ZIP
     */
    public static class ImportData {
        private final Workbook workbook;
        private final Map<String, Map<String, byte[]>> imagesBySku;

        public ImportData(Workbook workbook, Map<String, Map<String, byte[]>> imagesBySku) {
            this.workbook = workbook;
            this.imagesBySku = imagesBySku;
        }

        public Workbook getWorkbook() {
            return workbook;
        }

        public Map<String, Map<String, byte[]>> getImagesBySku() {
            return imagesBySku;
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
    }

    /**
     * Giải nén file ZIP vào thư mục đích
     */
    private void unzipFile(InputStream zipStream, Path destDir) throws IOException {
        byte[] buffer = new byte[1024];

        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {
                Path newPath = zipSlipProtect(zipEntry, destDir);

                if (zipEntry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    // Tạo thư mục cha nếu chưa tồn tại
                    if (newPath.getParent() != null) {
                        if (Files.notExists(newPath.getParent())) {
                            Files.createDirectories(newPath.getParent());
                        }
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