package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Carts.CartAddItemDTO;
import com.example.api_sell_clothes_v1.DTO.Carts.CartItemDTO;
import com.example.api_sell_clothes_v1.Entity.CartItems;
import com.example.api_sell_clothes_v1.Entity.Carts;
import com.example.api_sell_clothes_v1.Entity.ProductVariant;
import com.example.api_sell_clothes_v1.Exceptions.ResourceNotFoundException;
import com.example.api_sell_clothes_v1.Mapper.CartItemMapper;
import com.example.api_sell_clothes_v1.Repository.CartItemsRepository;
import com.example.api_sell_clothes_v1.Repository.ProductVariantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartItemsService {

    private final CartItemsRepository cartItemsRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CartItemMapper cartItemMapper;
    private final CartsService cartsService;

    /**
     * Thêm sản phẩm vào giỏ hàng của người dùng đã đăng nhập
     */
    @Transactional
    public CartItemDTO addItemToUserCart(Long userId, CartAddItemDTO addItemDTO) {
        // Kiểm tra variant tồn tại và còn hàng
        ProductVariant variant = validateVariantForCart(addItemDTO.getVariantId(), addItemDTO.getQuantity());

        // Tìm hoặc tạo giỏ hàng
        Carts cart = cartsService.findOrCreateUserCart(userId);

        // Thêm hoặc cập nhật item trong giỏ hàng
        CartItems cartItem = addOrUpdateCartItem(cart, variant, addItemDTO.getQuantity());

        // Cập nhật thời gian cập nhật giỏ hàng
        cartsService.updateCartTimestamp(cart);

        return cartItemMapper.toDto(cartItem);
    }

    /**
     * Thêm sản phẩm vào giỏ hàng theo session (cho người dùng chưa đăng nhập)
     */
    @Transactional
    public CartItemDTO addItemToSessionCart(String sessionId, CartAddItemDTO addItemDTO) {
        // Kiểm tra variant tồn tại và còn hàng
        ProductVariant variant = validateVariantForCart(addItemDTO.getVariantId(), addItemDTO.getQuantity());

        // Tìm hoặc tạo giỏ hàng
        Carts cart = cartsService.findOrCreateSessionCart(sessionId);

        // Thêm hoặc cập nhật item trong giỏ hàng
        CartItems cartItem = addOrUpdateCartItem(cart, variant, addItemDTO.getQuantity());

        // Cập nhật thời gian cập nhật giỏ hàng
        cartsService.updateCartTimestamp(cart);

        return cartItemMapper.toDto(cartItem);
    }

    /**
     * Phương thức hỗ trợ để kiểm tra variant
     */
    private ProductVariant validateVariantForCart(Long variantId, Integer quantity) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant", "id", variantId));

        if (variant.getStockQuantity() < quantity) {
            throw new IllegalArgumentException("Số lượng sản phẩm vượt quá hàng tồn kho");
        }

        // Kiểm tra sản phẩm và biến thể có hoạt động không
        if (!variant.getStatus() || !variant.getProduct().getStatus()) {
            throw new IllegalArgumentException("Sản phẩm hoặc biến thể không hoạt động");
        }

        return variant;
    }

    /**
     * Phương thức hỗ trợ để thêm hoặc cập nhật mục trong giỏ hàng
     */
    private CartItems addOrUpdateCartItem(Carts cart, ProductVariant variant, Integer quantity) {
        // Kiểm tra xem sản phẩm đã có trong giỏ hàng chưa
        Optional<CartItems> existingCartItem = cartItemsRepository.findByCartCartIdAndVariantVariantId(
                cart.getCartId(), variant.getVariantId());

        CartItems cartItem;
        if (existingCartItem.isPresent()) {
            // Nếu đã có, tăng số lượng
            cartItem = existingCartItem.get();
            int newQuantity = cartItem.getQuantity() + quantity;

            if (newQuantity > variant.getStockQuantity()) {
                throw new IllegalArgumentException("Tổng số lượng sản phẩm vượt quá hàng tồn kho");
            }

            cartItem.setQuantity(newQuantity);
            cartItem.setIsSelected(true); // Đảm bảo sản phẩm được chọn khi thêm vào giỏ hàng
            cartItem.setUpdatedAt(LocalDateTime.now());
        } else {
            // Nếu chưa có, tạo mới
            cartItem = CartItems.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(quantity)
                    .isSelected(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        return cartItemsRepository.save(cartItem);
    }

    /**
     * Cập nhật số lượng một sản phẩm trong giỏ hàng
     */
    @Transactional
    public CartItemDTO updateCartItemQuantity(Long cartItemId, Integer quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
        }

        CartItems cartItem = cartItemsRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", cartItemId));

        // Kiểm tra số lượng tồn kho
        ProductVariant variant = cartItem.getVariant();
        if (variant.getStockQuantity() < quantity) {
            throw new IllegalArgumentException("Số lượng sản phẩm vượt quá hàng tồn kho");
        }

        cartItem.setQuantity(quantity);
        cartItem.setUpdatedAt(LocalDateTime.now());

        // Cập nhật thời gian cập nhật giỏ hàng
        Carts cart = cartItem.getCart();
        cartsService.updateCartTimestamp(cart);

        CartItems updatedCartItem = cartItemsRepository.save(cartItem);
        return cartItemMapper.toDto(updatedCartItem);
    }

    /**
     * Xóa một sản phẩm khỏi giỏ hàng
     */
    @Transactional
    public ApiResponse removeCartItem(Long cartItemId) {
        CartItems cartItem = cartItemsRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", cartItemId));

        // Lấy thông tin giỏ hàng để cập nhật thời gian
        Carts cart = cartItem.getCart();

        cartItemsRepository.delete(cartItem);

        // Cập nhật thời gian cập nhật giỏ hàng
        cartsService.updateCartTimestamp(cart);

        return new ApiResponse(true, "Đã xóa sản phẩm khỏi giỏ hàng");
    }

    /**
     * Cập nhật trạng thái chọn của một sản phẩm trong giỏ hàng
     */
    @Transactional
    public CartItemDTO updateCartItemSelection(Long cartItemId, Boolean isSelected) {
        CartItems cartItem = cartItemsRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", cartItemId));

        cartItem.setIsSelected(isSelected);
        cartItem.setUpdatedAt(LocalDateTime.now());

        // Cập nhật thời gian cập nhật giỏ hàng
        Carts cart = cartItem.getCart();
        cartsService.updateCartTimestamp(cart);

        CartItems updatedCartItem = cartItemsRepository.save(cartItem);
        return cartItemMapper.toDto(updatedCartItem);
    }

    /**
     * Chọn tất cả sản phẩm trong giỏ hàng
     */
    @Transactional
    public ApiResponse selectAllCartItems(Long cartId) {
        Carts cart = cartsService.getCartById(cartId);

        cartItemsRepository.updateAllSelectionStatus(cart.getCartId(), true);

        // Cập nhật thời gian cập nhật giỏ hàng
        cartsService.updateCartTimestamp(cart);

        return new ApiResponse(true, "Đã chọn tất cả sản phẩm trong giỏ hàng");
    }

    /**
     * Bỏ chọn tất cả sản phẩm trong giỏ hàng
     */
    @Transactional
    public ApiResponse deselectAllCartItems(Long cartId) {
        Carts cart = cartsService.getCartById(cartId);

        cartItemsRepository.updateAllSelectionStatus(cart.getCartId(), false);

        // Cập nhật thời gian cập nhật giỏ hàng
        cartsService.updateCartTimestamp(cart);

        return new ApiResponse(true, "Đã bỏ chọn tất cả sản phẩm trong giỏ hàng");
    }

    /**
     * Lấy danh sách các mục đã chọn trong giỏ hàng
     */
    public List<CartItemDTO> getSelectedCartItems(Long cartId) {
        List<CartItems> selectedItems = cartItemsRepository.findByCartCartIdAndIsSelectedTrue(cartId);
        return selectedItems.stream()
                .map(cartItemMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả các mục trong giỏ hàng
     */
    public List<CartItemDTO> getCartItems(Long cartId) {
        List<CartItems> cartItems = cartItemsRepository.findByCartCartId(cartId);
        return cartItems.stream()
                .map(cartItemMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Gộp các mục từ giỏ hàng session vào giỏ hàng người dùng
     */
    @Transactional
    public void mergeItems(Carts userCart, List<CartItems> sessionItems) {
        for (CartItems sessionItem : sessionItems) {
            Optional<CartItems> existingUserItem = cartItemsRepository.findByCartCartIdAndVariantVariantId(
                    userCart.getCartId(), sessionItem.getVariant().getVariantId());

            if (existingUserItem.isPresent()) {
                // Nếu sản phẩm đã tồn tại, cộng số lượng
                CartItems userItem = existingUserItem.get();
                int newQuantity = userItem.getQuantity() + sessionItem.getQuantity();

                // Kiểm tra số lượng tồn kho
                ProductVariant variant = sessionItem.getVariant();
                if (variant.getStockQuantity() < newQuantity) {
                    // Nếu vượt quá, đặt số lượng tối đa là số lượng tồn kho
                    newQuantity = variant.getStockQuantity();
                }

                userItem.setQuantity(newQuantity);
                userItem.setIsSelected(true); // Đảm bảo sản phẩm được chọn
                userItem.setUpdatedAt(LocalDateTime.now());
                cartItemsRepository.save(userItem);
            } else {
                // Nếu sản phẩm chưa tồn tại, thêm mới vào giỏ hàng người dùng
                CartItems newUserItem = CartItems.builder()
                        .cart(userCart)
                        .variant(sessionItem.getVariant())
                        .quantity(sessionItem.getQuantity())
                        .isSelected(sessionItem.getIsSelected())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                cartItemsRepository.save(newUserItem);
            }
        }
    }

    /**
     * Kiểm tra xem một sản phẩm đã có trong giỏ hàng chưa
     */
    public boolean isItemInCart(Long cartId, Long variantId) {
        return cartItemsRepository.existsByCartCartIdAndVariantVariantId(cartId, variantId);
    }

    /**
     * Tìm mục trong giỏ hàng bằng cartId và variantId
     */
    public Optional<CartItems> findCartItem(Long cartId, Long variantId) {
        return cartItemsRepository.findByCartCartIdAndVariantVariantId(cartId, variantId);
    }

    /**
     * Đếm số lượng mục trong giỏ hàng
     */
    public long countCartItems(Long cartId) {
        return cartItemsRepository.countByCartCartId(cartId);
    }

    /**
     * Đếm số lượng mục đã chọn trong giỏ hàng
     */
    public long countSelectedCartItems(Long cartId) {
        return cartItemsRepository.countByCartCartIdAndIsSelectedTrue(cartId);
    }
}