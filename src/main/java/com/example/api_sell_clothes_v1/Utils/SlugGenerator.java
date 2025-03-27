package com.example.api_sell_clothes_v1.Utils;

import org.springframework.stereotype.Component;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Tiện ích tạo slug URL từ chuỗi văn bản
 * Chuyển đổi tiếng Việt có dấu thành không dấu và thay thế khoảng trắng bằng dấu gạch ngang
 */
@Component
public class SlugGenerator {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTIPLE_DASHES = Pattern.compile("-+");

    /**
     * Tạo slug từ chuỗi đầu vào
     * @param input Chuỗi đầu vào (có thể chứa dấu tiếng Việt và khoảng trắng)
     * @return Chuỗi slug đã chuẩn hóa (không dấu, thay khoảng trắng bằng dấu gạch ngang)
     */
    public String generateSlug(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // Chuyển thành chữ thường
        String result = input.toLowerCase(Locale.ROOT);

        // Loại bỏ dấu tiếng Việt
        result = Normalizer.normalize(result, Normalizer.Form.NFD);
        result = NONLATIN.matcher(result).replaceAll("");

        // Thay thế khoảng trắng bằng dấu gạch ngang
        result = WHITESPACE.matcher(result).replaceAll("-");

        // Loại bỏ các dấu gạch ngang liên tiếp
        result = MULTIPLE_DASHES.matcher(result).replaceAll("-");

        // Cắt bỏ dấu gạch ngang ở đầu và cuối chuỗi nếu có
        if (result.startsWith("-")) {
            result = result.substring(1);
        }
        if (result.endsWith("-")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    /**
     * Kiểm tra xem một chuỗi có phải là slug hợp lệ không
     * @param slug Chuỗi cần kiểm tra
     * @return true nếu là slug hợp lệ, false nếu không
     */
    public boolean isValidSlug(String slug) {
        if (slug == null || slug.isEmpty()) {
            return false;
        }

        // Slug hợp lệ chỉ chứa chữ cái thường, số và dấu gạch ngang
        return slug.equals(generateSlug(slug));
    }
}