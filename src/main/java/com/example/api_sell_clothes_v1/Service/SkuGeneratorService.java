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
     * Sinh SKU tự động cho biến thể sản phẩm
     * Định dạng: [Mã SP]-[Size]-[Màu sắc]
     * Ví dụ: ATN001-S-DEN
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
     * Tạo mã sản phẩm từ tên (lấy 3 chữ cái đầu của mỗi từ và số thứ tự)
     */
    private String generateProductCode(String productName) {
        // Loại bỏ dấu tiếng Việt
        String normalized = removeDiacritics(productName.toUpperCase());

        // Tách thành các từ và lấy chữ cái đầu
        String[] words = normalized.split("\\s+");
        StringBuilder codeBuilder = new StringBuilder();

        // Lấy các chữ cái đầu của mỗi từ (tối đa 3 chữ)
        int count = 0;
        for (String word : words) {
            if (!word.isEmpty() && count < 3) {
                codeBuilder.append(word.charAt(0));
                count++;
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
     * Format mã size chuẩn hóa
     */
    private String formatSizeCode(String size) {
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
        // Loại bỏ dấu và khoảng trắng, chuyển thành viết hoa
        String colorCode = removeDiacritics(color.trim()).toUpperCase();

        // Nếu màu sắc có nhiều từ, lấy 3 chữ cái đầu
        if (colorCode.contains(" ")) {
            String[] words = colorCode.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (!word.isEmpty()) {
                    sb.append(word.charAt(0));
                }
            }
            return sb.toString();
        }

        // Nếu màu sắc chỉ có một từ, lấy 3 chữ cái đầu hoặc toàn bộ nếu có ít hơn 3 chữ cái
        return colorCode.length() > 3 ? colorCode.substring(0, 3) : colorCode;
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