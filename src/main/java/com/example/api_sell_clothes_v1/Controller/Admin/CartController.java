package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Carts.CartItemDTO;
import com.example.api_sell_clothes_v1.DTO.Carts.CartResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Carts.CartSummaryDTO;
import com.example.api_sell_clothes_v1.Service.CartsService;
import com.example.api_sell_clothes_v1.Service.CartItemsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_CARTS)
@RequiredArgsConstructor
public class CartController {
    private final CartsService cartsService;
    private final CartItemsService cartItemsService;

    /**
     * Lấy thông tin giỏ hàng của người dùng đã đăng nhập
     */
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_CART')")
    public ResponseEntity<CartResponseDTO> getUserCart(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(cartsService.getUserCart(userId));
    }

    /**
     * Lấy thông tin giỏ hàng theo session (cho người dùng chưa đăng nhập)
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<CartResponseDTO> getSessionCart(@PathVariable String sessionId) {
        return ResponseEntity.ok(cartsService.getSessionCart(sessionId));
    }

    /**
     * Lấy tổng số lượng và tổng giá trị giỏ hàng của người dùng
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('VIEW_CART')")
    public ResponseEntity<CartSummaryDTO> getCartSummary(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(cartsService.getCartSummary(userId));
    }

    /**
     * Lấy tổng số lượng và tổng giá trị giỏ hàng theo session
     */
    @GetMapping("/session/{sessionId}/summary")
    public ResponseEntity<CartSummaryDTO> getSessionCartSummary(@PathVariable String sessionId) {
        return ResponseEntity.ok(cartsService.getSessionCartSummary(sessionId));
    }

    /**
     * Xóa tất cả sản phẩm khỏi giỏ hàng của người dùng
     */
    @DeleteMapping("/clear")
    @PreAuthorize("hasAuthority('EDIT_CART')")
    public ResponseEntity<ApiResponse> clearUserCart(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(cartsService.clearUserCart(userId));
    }

    /**
     * Xóa tất cả sản phẩm khỏi giỏ hàng theo session
     */
    @DeleteMapping("/session/{sessionId}/clear")
    public ResponseEntity<ApiResponse> clearSessionCart(@PathVariable String sessionId) {
        return ResponseEntity.ok(cartsService.clearSessionCart(sessionId));
    }

    /**
     * Chuyển đổi giỏ hàng từ session sang user khi đăng nhập
     */
    @PostMapping("/session/{sessionId}/merge")
    @PreAuthorize("hasAuthority('CHECKOUT_CART')")
    public ResponseEntity<ApiResponse> mergeSessionCartToUserCart(
            @PathVariable String sessionId,
            @RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(cartsService.mergeSessionCartToUserCart(sessionId, userId));
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
     * Đếm số lượng sản phẩm trong giỏ hàng của người dùng
     */
    @GetMapping("/count")
    @PreAuthorize("hasAuthority('VIEW_CART')")
    public ResponseEntity<Long> countUserCartItems(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(cartsService.countUserCartItems(userId));
    }

    /**
     * Đếm số lượng sản phẩm trong giỏ hàng theo session
     */
    @GetMapping("/session/{sessionId}/count")
    public ResponseEntity<Long> countSessionCartItems(@PathVariable String sessionId) {
        return ResponseEntity.ok(cartsService.countSessionCartItems(sessionId));
    }
}