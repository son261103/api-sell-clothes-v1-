package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.Entity.Products;
import com.example.api_sell_clothes_v1.Repository.ProductRepository;
import com.example.api_sell_clothes_v1.Repository.ProductVariantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkuGeneratorService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    /**
     * Sinh SKU tự động cho biến thể sản phẩm từ ID sản phẩm
     * Định dạng: [Mã SP]-[Size]-[Màu sắc]
     * Ví dụ: AOT001-S-DEN
     */
    public String generateSku(Long productId, String size, String color) {
        // Tìm sản phẩm theo ID
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));

        // Tạo mã sản phẩm từ tên
        String productCode = generateProductCode(product.getName());

        // Tạo mã size
        String sizeCode = formatSizeCode(size);

        // Tạo mã màu sắc
        String colorCode = formatColorCode(color);

        // Ghép lại thành SKU
        String baseSku = productCode + "-" + sizeCode + "-" + colorCode;

        // Kiểm tra xem SKU đã tồn tại chưa và thêm số nếu cần
        String finalSku = baseSku;
        int counter = 1;

        while (variantRepository.existsBySku(finalSku)) {
            finalSku = baseSku + "-" + counter;
            counter++;
        }

        log.debug("Đã tạo SKU {} cho sản phẩm ID: {}, size: {}, màu: {}", finalSku, productId, size, color);
        return finalSku;
    }

    /**
     * Sinh SKU tự động cho biến thể sản phẩm từ tên sản phẩm (chỉ cho preview)
     * Định dạng: [Mã SP]-[Size]-[Màu sắc]
     * Ví dụ: AOT001-S-DEN
     */
    public String generateSku(String productName, String size, String color) {
        // Tạo mã sản phẩm từ tên (không cần kiểm tra trùng lặp vì đây chỉ là preview)
        String productCode = generateSimpleProductCode(productName);

        // Tạo mã size
        String sizeCode = formatSizeCode(size);

        // Tạo mã màu sắc
        String colorCode = formatColorCode(color);

        // Ghép lại thành SKU
        return productCode + "-" + sizeCode + "-" + colorCode;
    }

    /**
     * Sinh SKU cho nhiều biến thể dựa trên sản phẩm, kích thước và màu sắc
     */
    public String[] generateMultipleSkus(Long productId, String[] sizes, String[] colors) {
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));

        String productCode = generateProductCode(product.getName());
        String[] skus = new String[sizes.length * colors.length];

        int index = 0;
        for (String size : sizes) {
            String sizeCode = formatSizeCode(size);
            for (String color : colors) {
                String colorCode = formatColorCode(color);
                String baseSku = productCode + "-" + sizeCode + "-" + colorCode;

                // Kiểm tra xem SKU đã tồn tại chưa và thêm số nếu cần
                String finalSku = baseSku;
                int counter = 1;

                while (variantRepository.existsBySku(finalSku)) {
                    finalSku = baseSku + "-" + counter;
                    counter++;
                }

                skus[index++] = finalSku;
            }
        }

        return skus;
    }

    /**
     * Tạo mã sản phẩm từ tên (lấy 3 chữ cái đầu và số thứ tự)
     * Phương pháp này bây giờ lấy 3 chữ cái đầu tiên sau khi loại bỏ khoảng trắng
     */
    private String generateProductCode(String productName) {
        // Loại bỏ dấu tiếng Việt
        String normalized = removeDiacritics(productName.toUpperCase());

        // Xóa tất cả khoảng trắng
        String noSpaces = normalized.replaceAll("\\s+", "");

        // Lấy 3 ký tự đầu tiên
        StringBuilder codeBuilder = new StringBuilder();

        if (noSpaces.length() >= 3) {
            codeBuilder.append(noSpaces.substring(0, 3));
        } else {
            codeBuilder.append(noSpaces);
            // Thêm X nếu không đủ 3 ký tự
            while (codeBuilder.length() < 3) {
                codeBuilder.append("X");
            }
        }

        // Thêm số thứ tự để đảm bảo độc nhất
        String baseCode = codeBuilder.toString();

        // Thêm 3 số. Các sản phẩm cùng loại sẽ có cùng chữ và khác số
        long existingCount = variantRepository.countBySkuStartingWith(baseCode);

        // Format số với 3 chữ số và padding 0
        return baseCode + String.format("%03d", existingCount + 1);
    }

    /**
     * Tạo mã sản phẩm đơn giản từ tên (chỉ cho preview) - sửa để đồng bộ với Excel
     */
    private String generateSimpleProductCode(String productName) {
        // Loại bỏ dấu tiếng Việt
        String normalized = removeDiacritics(productName.toUpperCase());

        // Xóa tất cả khoảng trắng
        String noSpaces = normalized.replaceAll("\\s+", "");

        // Lấy 3 ký tự đầu tiên của chuỗi đã bỏ khoảng trắng
        StringBuilder codeBuilder = new StringBuilder();

        if (noSpaces.length() >= 3) {
            codeBuilder.append(noSpaces.substring(0, 3));
        } else {
            codeBuilder.append(noSpaces);
            // Thêm X nếu không đủ 3 ký tự
            while (codeBuilder.length() < 3) {
                codeBuilder.append("X");
            }
        }

        // Thêm 001 mặc định cho preview
        String result = codeBuilder.toString() + "001";
        log.debug("Đã tạo mã sản phẩm: {}", result);
        return result;
    }

    /**
     * Format mã size chuẩn hóa
     */
    private String formatSizeCode(String size) {
        if (size == null || size.isEmpty()) {
            return "OS"; // One Size - mặc định nếu không có kích thước
        }

        // Loại bỏ khoảng trắng và chuyển thành viết hoa
        String sizeCode = size.trim().toUpperCase();

        // Nếu size là một số (như 39, 40, 41, v.v.), giữ nguyên
        if (sizeCode.matches("\\d+")) {
            return sizeCode;
        }

        // Nếu size có nhiều từ (như "Extra Large"), lấy chữ cái đầu
        if (sizeCode.contains(" ")) {
            String[] words = sizeCode.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (!word.isEmpty()) {
                    sb.append(word.charAt(0));
                }
            }
            return sb.toString();
        }

        // Đối với các size thông thường (S, M, L, XL, v.v.)
        return sizeCode;
    }

    /**
     * Format mã màu sắc chuẩn hóa
     */
    private String formatColorCode(String color) {
        if (color == null || color.isEmpty()) {
            return "XXX"; // Mã mặc định nếu không có màu sắc
        }

        // Nếu màu sắc bắt đầu bằng # (mã hex), giữ nguyên dạng ban đầu
        if (color.startsWith("#")) {
            // Trả về toàn bộ mã màu hex
            return color.substring(0, Math.min(color.length(), 4)); // Chỉ lấy tối đa 4 ký tự (bao gồm #)
        }

        // Loại bỏ dấu và khoảng trắng, chuyển thành viết hoa
        String colorCode = removeDiacritics(color.trim()).toUpperCase();

        // Nếu màu sắc có nhiều từ, lấy 3 chữ cái đầu
        if (colorCode.contains(" ")) {
            String[] words = colorCode.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (!word.isEmpty() && sb.length() < 3) {
                    sb.append(word.charAt(0));
                }
            }

            // Nếu không đủ 3 chữ, thêm từ ký tự đầu tiên của từ đầu tiên
            if (sb.length() < 3 && words[0].length() > 1) {
                for (int i = 1; i < words[0].length() && sb.length() < 3; i++) {
                    sb.append(words[0].charAt(i));
                }
            }

            // Vẫn chưa đủ 3 ký tự, thêm X
            while (sb.length() < 3) {
                sb.append("X");
            }

            return sb.toString();
        }

        // Nếu màu sắc chỉ có một từ, lấy 3 chữ cái đầu hoặc toàn bộ nếu có ít hơn 3 chữ cái
        if (colorCode.length() < 3) {
            // Thêm X nếu không đủ 3 ký tự
            StringBuilder sb = new StringBuilder(colorCode);
            while (sb.length() < 3) {
                sb.append("X");
            }
            return sb.toString();
        } else {
            return colorCode.substring(0, 3);
        }
    }

    /**
     * Loại bỏ dấu tiếng Việt
     */
    private String removeDiacritics(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("");
    }
}