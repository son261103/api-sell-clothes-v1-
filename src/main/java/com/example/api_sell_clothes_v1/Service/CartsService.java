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
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    @Transactional
    public CartResponseDTO getUserCart(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID không được null");
        }
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Carts cart = cartsRepository.findByUserUserId(userId)
                .orElseGet(() -> {
                    Carts newCart = Carts.builder()
                            .user(user)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .cartItems(new ArrayList<>()) // Đảm bảo danh sách rỗng
                            .build();
                    return cartsRepository.save(newCart);
                });

        // Đảm bảo cartItems được tải đầy đủ nếu cần
        Hibernate.initialize(cart.getCartItems());
        return cartMapper.toDto(cart);
    }

    /**
     * Lấy thông tin giỏ hàng theo session (cho người dùng chưa đăng nhập)
     */
    @Transactional
    public CartResponseDTO getSessionCart(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID không được null hoặc rỗng");
        }
        Carts cart = cartsRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Carts newCart = Carts.builder()
                            .sessionId(sessionId)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .cartItems(new ArrayList<>()) // Đảm bảo danh sách rỗng
                            .build();
                    return cartsRepository.save(newCart);
                });

        // Đảm bảo cartItems được tải đầy đủ nếu cần
        Hibernate.initialize(cart.getCartItems());
        return cartMapper.toDto(cart);
    }

    /**
     * Tìm giỏ hàng theo ID
     */
    @Transactional
    public Carts getCartById(Long cartId) {
        if (cartId == null) {
            throw new IllegalArgumentException("Cart ID không được null");
        }
        Carts cart = cartsRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "id", cartId));
        Hibernate.initialize(cart.getCartItems()); // Tải danh sách cartItems
        return cart;
    }

    /**
     * Tìm giỏ hàng của người dùng hoặc tạo mới nếu chưa có
     */
    @Transactional
    public Carts findOrCreateUserCart(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID không được null");
        }
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Carts cart = cartsRepository.findByUserUserId(userId)
                .orElseGet(() -> {
                    Carts newCart = Carts.builder()
                            .user(user)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .cartItems(new ArrayList<>()) // Đảm bảo danh sách rỗng
                            .build();
                    return cartsRepository.save(newCart);
                });
        Hibernate.initialize(cart.getCartItems());
        return cart;
    }

    /**
     * Tìm giỏ hàng theo session hoặc tạo mới nếu chưa có
     */
    @Transactional
    public Carts findOrCreateSessionCart(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID không được null hoặc rỗng");
        }
        Carts cart = cartsRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Carts newCart = Carts.builder()
                            .sessionId(sessionId)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .cartItems(new ArrayList<>()) // Đảm bảo danh sách rỗng
                            .build();
                    return cartsRepository.save(newCart);
                });
        Hibernate.initialize(cart.getCartItems());
        return cart;
    }

    /**
     * Cập nhật thời gian sửa đổi của giỏ hàng
     */
    @Transactional
    public Carts updateCartTimestamp(Carts cart) {
        if (cart == null) {
            throw new IllegalArgumentException("Cart không được null");
        }
        cart.setUpdatedAt(LocalDateTime.now());
        return cartsRepository.save(cart);
    }

    /**
     * Xóa tất cả sản phẩm khỏi giỏ hàng của người dùng
     */
    @Transactional
    public ApiResponse clearUserCart(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID không được null");
        }
        Carts cart = cartsRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));

        cart.getCartItems().clear(); // Xóa trực tiếp từ danh sách trong entity
        cart.setUpdatedAt(LocalDateTime.now());
        cartsRepository.save(cart);

        return new ApiResponse(true, "Đã xóa tất cả sản phẩm khỏi giỏ hàng");
    }

    /**
     * Xóa tất cả sản phẩm khỏi giỏ hàng theo session
     */
    @Transactional
    public ApiResponse clearSessionCart(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID không được null hoặc rỗng");
        }
        Carts cart = cartsRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "sessionId", sessionId));

        cart.getCartItems().clear(); // Xóa trực tiếp từ danh sách trong entity
        cart.setUpdatedAt(LocalDateTime.now());
        cartsRepository.save(cart);

        return new ApiResponse(true, "Đã xóa tất cả sản phẩm khỏi giỏ hàng");
    }

    /**
     * Chuyển đổi giỏ hàng từ session sang user khi đăng nhập
     */
    @Transactional
    public ApiResponse mergeSessionCartToUserCart(String sessionId, Long userId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID không được null hoặc rỗng");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID không được null");
        }

        Carts sessionCart = cartsRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "sessionId", sessionId));
        Carts userCart = findOrCreateUserCart(userId);

        // Di chuyển các mục từ session cart sang user cart
        List<CartItems> sessionItems = new ArrayList<>(sessionCart.getCartItems());
        for (CartItems item : sessionItems) {
            item.setCart(userCart);
            userCart.addCartItem(item);
        }
        sessionCart.getCartItems().clear();

        // Cập nhật và lưu cả hai giỏ hàng
        userCart.setUpdatedAt(LocalDateTime.now());
        sessionCart.setUpdatedAt(LocalDateTime.now());
        cartsRepository.save(userCart);
        cartsRepository.delete(sessionCart);

        return new ApiResponse(true, "Đã chuyển giỏ hàng thành công");
    }

    /**
     * Lấy tổng số lượng và tổng giá trị giỏ hàng của người dùng
     */
    @Transactional
    public CartSummaryDTO getCartSummary(Long userId) {
        Carts cart = findOrCreateUserCart(userId);
        return calculateCartSummary(cart);
    }

    /**
     * Lấy tổng số lượng và tổng giá trị giỏ hàng theo session
     */
    @Transactional
    public CartSummaryDTO getSessionCartSummary(String sessionId) {
        Carts cart = findOrCreateSessionCart(sessionId);
        return calculateCartSummary(cart);
    }

    /**
     * Phương thức hỗ trợ tính toán tổng số lượng và giá trị giỏ hàng
     */
    @Transactional
    public CartSummaryDTO calculateCartSummary(Carts cart) {
        List<CartItems> cartItems = cart.getCartItems(); // Lấy trực tiếp từ entity

        int totalItems = 0;
        BigDecimal totalPrice = BigDecimal.ZERO;
        int selectedItems = 0;
        BigDecimal selectedTotalPrice = BigDecimal.ZERO;

        for (CartItems item : cartItems) {
            ProductVariant variant = item.getVariant();
            if (variant != null && variant.getProduct() != null) {
                Hibernate.initialize(variant);
                Hibernate.initialize(variant.getProduct());

                BigDecimal price = variant.getProduct().getSalePrice() != null
                        ? variant.getProduct().getSalePrice()
                        : variant.getProduct().getPrice();

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
        if (userId == null) {
            return false;
        }
        return cartsRepository.existsByUserUserId(userId);
    }

    /**
     * Kiểm tra xem session đã có giỏ hàng chưa
     */
    public boolean sessionHasCart(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }
        return cartsRepository.existsBySessionId(sessionId);
    }

    /**
     * Đếm số lượng mục trong giỏ hàng của người dùng
     */
    @Transactional
    public Long countUserCartItems(Long userId) {
        if (userId == null) {
            return 0L;
        }
        Carts cart = cartsRepository.findByUserUserId(userId).orElse(null);
        return cart != null ? (long) cart.getCartItems().size() : 0L;
    }

    /**
     * Đếm số lượng mục trong giỏ hàng theo session
     */
    @Transactional
    public Long countSessionCartItems(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return 0L;
        }
        Carts cart = cartsRepository.findBySessionId(sessionId).orElse(null);
        return cart != null ? (long) cart.getCartItems().size() : 0L;
    }
}