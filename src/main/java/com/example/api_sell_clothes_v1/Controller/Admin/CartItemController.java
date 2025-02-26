package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Carts.CartAddItemDTO;
import com.example.api_sell_clothes_v1.DTO.Carts.CartItemDTO;
import com.example.api_sell_clothes_v1.DTO.Carts.CartSelectionDTO;
import com.example.api_sell_clothes_v1.DTO.Carts.CartUpdateQuantityDTO;
import com.example.api_sell_clothes_v1.DTO.Carts.CartResponseDTO;
import com.example.api_sell_clothes_v1.Service.CartItemsService;
import com.example.api_sell_clothes_v1.Service.CartsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_CART_ITEMS)
@RequiredArgsConstructor
public class CartItemController {
    private final CartItemsService cartItemsService;
    private final CartsService cartsService;

    /**
     * Thêm sản phẩm vào giỏ hàng của người dùng đã đăng nhập
     */
    @PostMapping("/add")
    @PreAuthorize("hasAuthority('EDIT_CART')")
    public ResponseEntity<CartItemDTO> addItemToUserCart(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody CartAddItemDTO addItemDTO) {
        return new ResponseEntity<>(
                cartItemsService.addItemToUserCart(userId, addItemDTO),
                HttpStatus.CREATED
        );
    }

    /**
     * Thêm sản phẩm vào giỏ hàng theo session (cho người dùng chưa đăng nhập)
     */
    @PostMapping("/session/{sessionId}/add")
    public ResponseEntity<CartItemDTO> addItemToSessionCart(
            @PathVariable String sessionId,
            @Valid @RequestBody CartAddItemDTO addItemDTO) {
        return new ResponseEntity<>(
                cartItemsService.addItemToSessionCart(sessionId, addItemDTO),
                HttpStatus.CREATED
        );
    }

    /**
     * Cập nhật số lượng một sản phẩm trong giỏ hàng
     */
    @PutMapping("/{cartItemId}/quantity")
    @PreAuthorize("hasAuthority('EDIT_CART')")
    public ResponseEntity<CartItemDTO> updateCartItemQuantity(
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartUpdateQuantityDTO updateDTO) {
        return ResponseEntity.ok(cartItemsService.updateCartItemQuantity(cartItemId, updateDTO.getQuantity()));
    }

    /**
     * Cập nhật trạng thái chọn của một sản phẩm trong giỏ hàng
     */
    @PutMapping("/{cartItemId}/select")
    @PreAuthorize("hasAuthority('EDIT_CART')")
    public ResponseEntity<CartItemDTO> updateCartItemSelection(
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartSelectionDTO selectionDTO) {
        return ResponseEntity.ok(cartItemsService.updateCartItemSelection(cartItemId, selectionDTO.getIsSelected()));
    }

    /**
     * Xóa một sản phẩm khỏi giỏ hàng
     */
    @DeleteMapping("/{cartItemId}")
    @PreAuthorize("hasAuthority('EDIT_CART')")
    public ResponseEntity<ApiResponse> removeCartItem(@PathVariable Long cartItemId) {
        return ResponseEntity.ok(cartItemsService.removeCartItem(cartItemId));
    }

    /**
     * Chọn tất cả sản phẩm trong giỏ hàng
     */
    @PutMapping("/select-all")
    @PreAuthorize("hasAuthority('EDIT_CART')")
    public ResponseEntity<ApiResponse> selectAllCartItems(@RequestAttribute("userId") Long userId) {
        CartResponseDTO cart = cartsService.getUserCart(userId);
        return ResponseEntity.ok(cartItemsService.selectAllCartItems(cart.getCartId()));
    }

    /**
     * Bỏ chọn tất cả sản phẩm trong giỏ hàng
     */
    @PutMapping("/deselect-all")
    @PreAuthorize("hasAuthority('EDIT_CART')")
    public ResponseEntity<ApiResponse> deselectAllCartItems(@RequestAttribute("userId") Long userId) {
        CartResponseDTO cart = cartsService.getUserCart(userId);
        return ResponseEntity.ok(cartItemsService.deselectAllCartItems(cart.getCartId()));
    }

    /**
     * Chọn tất cả sản phẩm trong giỏ hàng theo session
     */
    @PutMapping("/session/{sessionId}/select-all")
    public ResponseEntity<ApiResponse> selectAllSessionCartItems(@PathVariable String sessionId) {
        CartResponseDTO cart = cartsService.getSessionCart(sessionId);
        return ResponseEntity.ok(cartItemsService.selectAllCartItems(cart.getCartId()));
    }

    /**
     * Bỏ chọn tất cả sản phẩm trong giỏ hàng theo session
     */
    @PutMapping("/session/{sessionId}/deselect-all")
    public ResponseEntity<ApiResponse> deselectAllSessionCartItems(@PathVariable String sessionId) {
        CartResponseDTO cart = cartsService.getSessionCart(sessionId);
        return ResponseEntity.ok(cartItemsService.deselectAllCartItems(cart.getCartId()));
    }

    /**
     * Lấy danh sách các mục trong giỏ hàng
     */
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_CART')")
    public ResponseEntity<List<CartItemDTO>> getCartItems(@RequestAttribute("userId") Long userId) {
        CartResponseDTO cart = cartsService.getUserCart(userId);
        return ResponseEntity.ok(cartItemsService.getCartItems(cart.getCartId()));
    }

    /**
     * Lấy danh sách các mục trong giỏ hàng theo session
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<CartItemDTO>> getSessionCartItems(@PathVariable String sessionId) {
        CartResponseDTO cart = cartsService.getSessionCart(sessionId);
        return ResponseEntity.ok(cartItemsService.getCartItems(cart.getCartId()));
    }

    /**
     * Lấy danh sách các mục đã chọn trong giỏ hàng
     */
    @GetMapping("/selected")
    @PreAuthorize("hasAuthority('VIEW_CART')")
    public ResponseEntity<List<CartItemDTO>> getSelectedCartItems(@RequestAttribute("userId") Long userId) {
        CartResponseDTO cart = cartsService.getUserCart(userId);
        return ResponseEntity.ok(cartItemsService.getSelectedCartItems(cart.getCartId()));
    }

    /**
     * Lấy danh sách các mục đã chọn trong giỏ hàng theo session
     */
    @GetMapping("/session/{sessionId}/selected")
    public ResponseEntity<List<CartItemDTO>> getSelectedSessionCartItems(@PathVariable String sessionId) {
        CartResponseDTO cart = cartsService.getSessionCart(sessionId);
        return ResponseEntity.ok(cartItemsService.getSelectedCartItems(cart.getCartId()));
    }

    /**
     * Kiểm tra xem một sản phẩm đã có trong giỏ hàng chưa
     */
    @GetMapping("/check")
    @PreAuthorize("hasAuthority('VIEW_CART')")
    public ResponseEntity<Boolean> isItemInCart(
            @RequestAttribute("userId") Long userId,
            @RequestParam Long variantId) {
        CartResponseDTO cart = cartsService.getUserCart(userId);
        return ResponseEntity.ok(cartItemsService.isItemInCart(cart.getCartId(), variantId));
    }

    /**
     * Kiểm tra xem một sản phẩm đã có trong giỏ hàng session chưa
     */
    @GetMapping("/session/{sessionId}/check")
    public ResponseEntity<Boolean> isItemInSessionCart(
            @PathVariable String sessionId,
            @RequestParam Long variantId) {
        CartResponseDTO cart = cartsService.getSessionCart(sessionId);
        return ResponseEntity.ok(cartItemsService.isItemInCart(cart.getCartId(), variantId));
    }

    /**
     * Đếm số lượng mục trong giỏ hàng
     */
    @GetMapping("/count")
    @PreAuthorize("hasAuthority('VIEW_CART')")
    public ResponseEntity<Long> countCartItems(@RequestAttribute("userId") Long userId) {
        CartResponseDTO cart = cartsService.getUserCart(userId);
        return ResponseEntity.ok(cartItemsService.countCartItems(cart.getCartId()));
    }

    /**
     * Đếm số lượng mục trong giỏ hàng session
     */
    @GetMapping("/session/{sessionId}/count")
    public ResponseEntity<Long> countSessionCartItems(@PathVariable String sessionId) {
        CartResponseDTO cart = cartsService.getSessionCart(sessionId);
        return ResponseEntity.ok(cartItemsService.countCartItems(cart.getCartId()));
    }

    /**
     * Đếm số lượng mục đã chọn trong giỏ hàng
     */
    @GetMapping("/count-selected")
    @PreAuthorize("hasAuthority('VIEW_CART')")
    public ResponseEntity<Long> countSelectedCartItems(@RequestAttribute("userId") Long userId) {
        CartResponseDTO cart = cartsService.getUserCart(userId);
        return ResponseEntity.ok(cartItemsService.countSelectedCartItems(cart.getCartId()));
    }

    /**
     * Đếm số lượng mục đã chọn trong giỏ hàng session
     */
    @GetMapping("/session/{sessionId}/count-selected")
    public ResponseEntity<Long> countSelectedSessionCartItems(@PathVariable String sessionId) {
        CartResponseDTO cart = cartsService.getSessionCart(sessionId);
        return ResponseEntity.ok(cartItemsService.countSelectedCartItems(cart.getCartId()));
    }

    // Note: Phương thức mergeItems không cần endpoint vì đây là logic nội bộ,
    // thường được gọi từ một controller khác (ví dụ: khi người dùng đăng nhập)
}