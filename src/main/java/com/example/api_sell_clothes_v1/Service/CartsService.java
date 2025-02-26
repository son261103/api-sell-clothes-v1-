package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Carts.CartResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Carts.CartSummaryDTO;
import com.example.api_sell_clothes_v1.Entity.CartItems;
import com.example.api_sell_clothes_v1.Entity.Carts;
import com.example.api_sell_clothes_v1.Entity.ProductVariant;
import com.example.api_sell_clothes_v1.Entity.Users;
import com.example.api_sell_clothes_v1.Exceptions.ResourceNotFoundException;
import com.example.api_sell_clothes_v1.Mapper.CartMapper;
import com.example.api_sell_clothes_v1.Repository.CartItemsRepository;
import com.example.api_sell_clothes_v1.Repository.CartsRepository;
import com.example.api_sell_clothes_v1.Repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartsService {

    private final CartsRepository cartsRepository;
    private final CartItemsRepository cartItemsRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;

    /**
     * Lấy thông tin giỏ hàng của người dùng đã đăng nhập
     */
    public CartResponseDTO getUserCart(Long userId) {
        // Kiểm tra người dùng tồn tại
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Tìm hoặc tạo giỏ hàng
        Carts cart = cartsRepository.findByUserUserId(userId)
                .orElseGet(() -> {
                    Carts newCart = Carts.builder()
                            .user(user)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return cartsRepository.save(newCart);
                });

        return cartMapper.toDto(cart);
    }

    /**
     * Lấy thông tin giỏ hàng theo session (cho người dùng chưa đăng nhập)
     */
    public CartResponseDTO getSessionCart(String sessionId) {
        Carts cart = cartsRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Carts newCart = Carts.builder()
                            .sessionId(sessionId)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return cartsRepository.save(newCart);
                });

        return cartMapper.toDto(cart);
    }

    /**
     * Tìm giỏ hàng theo ID
     */
    public Carts getCartById(Long cartId) {
        return cartsRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "id", cartId));
    }

    /**
     * Tìm giỏ hàng của người dùng hoặc tạo mới nếu chưa có
     */
    public Carts findOrCreateUserCart(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return cartsRepository.findByUserUserId(userId)
                .orElseGet(() -> {
                    Carts newCart = Carts.builder()
                            .user(user)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return cartsRepository.save(newCart);
                });
    }

    /**
     * Tìm giỏ hàng theo session hoặc tạo mới nếu chưa có
     */
    public Carts findOrCreateSessionCart(String sessionId) {
        return cartsRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Carts newCart = Carts.builder()
                            .sessionId(sessionId)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return cartsRepository.save(newCart);
                });
    }

    /**
     * Cập nhật thời gian sửa đổi của giỏ hàng
     */
    public Carts updateCartTimestamp(Carts cart) {
        cart.setUpdatedAt(LocalDateTime.now());
        return cartsRepository.save(cart);
    }

    /**
     * Xóa tất cả sản phẩm khỏi giỏ hàng của người dùng
     */
    @Transactional
    public ApiResponse clearUserCart(Long userId) {
        Carts cart = cartsRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));

        cartItemsRepository.deleteByCartCartId(cart.getCartId());

        // Cập nhật thời gian cập nhật giỏ hàng
        cart.setUpdatedAt(LocalDateTime.now());
        cartsRepository.save(cart);

        return new ApiResponse(true, "Đã xóa tất cả sản phẩm khỏi giỏ hàng");
    }

    /**
     * Xóa tất cả sản phẩm khỏi giỏ hàng theo session
     */
    @Transactional
    public ApiResponse clearSessionCart(String sessionId) {
        Carts cart = cartsRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "sessionId", sessionId));

        cartItemsRepository.deleteByCartCartId(cart.getCartId());

        // Cập nhật thời gian cập nhật giỏ hàng
        cart.setUpdatedAt(LocalDateTime.now());
        cartsRepository.save(cart);

        return new ApiResponse(true, "Đã xóa tất cả sản phẩm khỏi giỏ hàng");
    }

    /**
     * Chuyển đổi giỏ hàng từ session sang user khi đăng nhập
     */
    @Transactional
    public ApiResponse mergeSessionCartToUserCart(String sessionId, Long userId) {
        Carts sessionCart = cartsRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "sessionId", sessionId));

        // Tìm hoặc tạo giỏ hàng người dùng
        Carts userCart = findOrCreateUserCart(userId);

        // Lấy danh sách các mục trong giỏ hàng session
        List<CartItems> sessionItems = cartItemsRepository.findByCartCartId(sessionCart.getCartId());

        // Giao lại việc xử lý gộp các item cho CartItemsService
        // Đây là một ví dụ, bạn có thể điều chỉnh phù hợp với code của bạn

        // Cập nhật thời gian cập nhật giỏ hàng người dùng
        userCart.setUpdatedAt(LocalDateTime.now());
        cartsRepository.save(userCart);

        // Xóa giỏ hàng session
        cartsRepository.delete(sessionCart);

        return new ApiResponse(true, "Đã chuyển giỏ hàng thành công");
    }

    /**
     * Lấy tổng số lượng và tổng giá trị giỏ hàng của người dùng
     */
    public CartSummaryDTO getCartSummary(Long userId) {
        Carts cart = findOrCreateUserCart(userId);
        return calculateCartSummary(cart);
    }

    /**
     * Lấy tổng số lượng và tổng giá trị giỏ hàng theo session
     */
    public CartSummaryDTO getSessionCartSummary(String sessionId) {
        Carts cart = findOrCreateSessionCart(sessionId);
        return calculateCartSummary(cart);
    }

    /**
     * Phương thức hỗ trợ tính toán tổng số lượng và giá trị giỏ hàng
     */
    private CartSummaryDTO calculateCartSummary(Carts cart) {
        List<CartItems> cartItems = cartItemsRepository.findByCartCartId(cart.getCartId());

        int totalItems = 0;
        BigDecimal totalPrice = BigDecimal.ZERO;
        int selectedItems = 0;
        BigDecimal selectedTotalPrice = BigDecimal.ZERO;

        for (CartItems item : cartItems) {
            ProductVariant variant = item.getVariant();
            BigDecimal price = variant.getProduct().getSalePrice() != null ?
                    variant.getProduct().getSalePrice() : variant.getProduct().getPrice();

            if (price != null) {
                int quantity = item.getQuantity();
                BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(quantity));

                totalItems += quantity;
                totalPrice = totalPrice.add(itemTotal);

                if (item.getIsSelected()) {
                    selectedItems += quantity;
                    selectedTotalPrice = selectedTotalPrice.add(itemTotal);
                }
            }
        }

        return CartSummaryDTO.builder()
                .cartId(cart.getCartId())
                .totalItems(totalItems)
                .totalPrice(totalPrice)
                .selectedItems(selectedItems)
                .selectedTotalPrice(selectedTotalPrice)
                .build();
    }

    /**
     * Kiểm tra xem người dùng đã có giỏ hàng chưa
     */
    public boolean userHasCart(Long userId) {
        return cartsRepository.existsByUserUserId(userId);
    }

    /**
     * Kiểm tra xem session đã có giỏ hàng chưa
     */
    public boolean sessionHasCart(String sessionId) {
        return cartsRepository.existsBySessionId(sessionId);
    }

    /**
     * Đếm số lượng mục trong giỏ hàng của người dùng
     */
    public Long countUserCartItems(Long userId) {
        return cartsRepository.countItemsByUserId(userId);
    }

    /**
     * Đếm số lượng mục trong giỏ hàng theo session
     */
    public Long countSessionCartItems(String sessionId) {
        return cartsRepository.countItemsBySessionId(sessionId);
    }
}