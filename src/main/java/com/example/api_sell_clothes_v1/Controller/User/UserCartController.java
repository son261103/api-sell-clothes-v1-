package com.example.api_sell_clothes_v1.Controller.User;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Carts.*;
import com.example.api_sell_clothes_v1.Service.CartItemsService;
import com.example.api_sell_clothes_v1.Service.CartsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_PUBLIC + "/cart")
@RequiredArgsConstructor
public class UserCartController {

    private final CartsService cartsService;
    private final CartItemsService cartItemsService;

    @GetMapping
    public ResponseEntity<CartResponseDTO> getUserCart(@RequestHeader(value = "X-User-Id", required = true) Long userId) {
        try {
            CartResponseDTO cart = cartsService.getUserCart(userId);
            if (cart == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            log.error("Lỗi khi lấy giỏ hàng cho userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<CartSummaryDTO> getCartSummary(@RequestHeader(value = "X-User-Id", required = true) Long userId) {
        try {
            CartSummaryDTO summary = cartsService.getCartSummary(userId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Lỗi khi lấy tổng quan giỏ hàng cho userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addItemToUserCart(
            @RequestHeader(value = "X-User-Id", required = true) Long userId,
            @Valid @RequestBody CartAddItemDTO addItemDTO) {
        try {
            if (addItemDTO.getVariantId() == null || addItemDTO.getQuantity() <= 0) {
                log.warn("Dữ liệu không hợp lệ khi thêm vào giỏ hàng: variantId hoặc quantity không hợp lệ");
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "VariantId và quantity phải hợp lệ"));
            }
            CartItemDTO item = cartItemsService.addItemToUserCart(userId, addItemDTO);
            return new ResponseEntity<>(item, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi thêm sản phẩm vào giỏ hàng: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi server khi thêm sản phẩm vào giỏ hàng cho userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi server: " + e.getMessage()));
        }
    }

    @PutMapping("/item/{cartItemId}/quantity")
    public ResponseEntity<CartItemDTO> updateCartItemQuantity(
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartUpdateQuantityDTO updateDTO) {
        try {
            CartItemDTO item = cartItemsService.updateCartItemQuantity(cartItemId, updateDTO.getQuantity());
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật số lượng sản phẩm trong giỏ hàng: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PutMapping("/item/{cartItemId}/select")
    public ResponseEntity<CartItemDTO> updateCartItemSelection(
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartSelectionDTO selectionDTO) {
        try {
            CartItemDTO item = cartItemsService.updateCartItemSelection(cartItemId, selectionDTO.getIsSelected());
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật trạng thái chọn sản phẩm: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @DeleteMapping("/item/{cartItemId}")
    public ResponseEntity<ApiResponse> removeCartItem(@PathVariable Long cartItemId) {
        try {
            ApiResponse response = cartItemsService.removeCartItem(cartItemId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi khi xóa sản phẩm khỏi giỏ hàng: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi xóa sản phẩm: " + e.getMessage()));
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse> clearUserCart(@RequestHeader(value = "X-User-Id", required = true) Long userId) {
        try {
            ApiResponse response = cartsService.clearUserCart(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi khi xóa toàn bộ giỏ hàng người dùng: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi xóa giỏ hàng: " + e.getMessage()));
        }
    }

    @PutMapping("/select-all")
    public ResponseEntity<ApiResponse> selectAllUserCartItems(@RequestHeader(value = "X-User-Id", required = true) Long userId) {
        try {
            CartResponseDTO cart = cartsService.getUserCart(userId);
            ApiResponse response = cartItemsService.selectAllCartItems(cart.getCartId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi khi chọn tất cả sản phẩm trong giỏ hàng người dùng: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi chọn tất cả sản phẩm: " + e.getMessage()));
        }
    }

    @PutMapping("/deselect-all")
    public ResponseEntity<ApiResponse> deselectAllUserCartItems(@RequestHeader(value = "X-User-Id", required = true) Long userId) {
        try {
            CartResponseDTO cart = cartsService.getUserCart(userId);
            ApiResponse response = cartItemsService.deselectAllCartItems(cart.getCartId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi khi bỏ chọn tất cả sản phẩm trong giỏ hàng người dùng: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi bỏ chọn tất cả sản phẩm: " + e.getMessage()));
        }
    }

    @GetMapping("/items")
    public ResponseEntity<List<CartItemDTO>> getUserCartItems(@RequestHeader(value = "X-User-Id", required = true) Long userId) {
        try {
            CartResponseDTO cart = cartsService.getUserCart(userId);
            List<CartItemDTO> items = cartItemsService.getCartItems(cart.getCartId());
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách mục trong giỏ hàng người dùng: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/selected")
    public ResponseEntity<List<CartItemDTO>> getSelectedUserCartItems(@RequestHeader(value = "X-User-Id", required = true) Long userId) {
        try {
            CartResponseDTO cart = cartsService.getUserCart(userId);
            List<CartItemDTO> items = cartItemsService.getSelectedCartItems(cart.getCartId());
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách mục đã chọn trong giỏ hàng người dùng: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countUserCartItems(@RequestHeader(value = "X-User-Id", required = true) Long userId) {
        try {
            Long count = cartsService.countUserCartItems(userId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Lỗi khi đếm số lượng sản phẩm trong giỏ hàng người dùng: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}