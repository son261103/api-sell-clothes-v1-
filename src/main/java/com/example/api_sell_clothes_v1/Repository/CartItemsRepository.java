package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.CartItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemsRepository extends JpaRepository<CartItems, Long> {
    /**
     * Tìm tất cả các mục trong giỏ hàng của một giỏ hàng cụ thể
     */
    List<CartItems> findByCartCartId(Long cartId);

    /**
     * Lấy tất cả các mục đã được chọn trong giỏ hàng
     */
    List<CartItems> findByCartCartIdAndIsSelectedTrue(Long cartId);

    /**
     * Tìm mục trong giỏ hàng bằng ID giỏ hàng và ID biến thể
     */
    // Sửa từ variantId sang variant.variantId để khớp với tên trường trong ProductVariant
    Optional<CartItems> findByCartCartIdAndVariantVariantId(Long cartId, Long variantId);

    /**
     * Kiểm tra xem một sản phẩm đã có trong giỏ hàng hay chưa
     */
    // Tương tự, sửa lại phương thức này
    boolean existsByCartCartIdAndVariantVariantId(Long cartId, Long variantId);

    /**
     * Cập nhật trạng thái chọn của tất cả các mục trong giỏ hàng
     */
    @Modifying
    @Query("UPDATE CartItems ci SET ci.isSelected = :isSelected WHERE ci.cart.cartId = :cartId")
    int updateAllSelectionStatus(@Param("cartId") Long cartId, @Param("isSelected") Boolean isSelected);

    /**
     * Xóa hết các mục trong giỏ hàng
     */
    @Modifying
    void deleteByCartCartId(Long cartId);

    /**
     * Xóa tất cả các mục đã chọn trong giỏ hàng
     */
    @Modifying
    @Query("DELETE FROM CartItems ci WHERE ci.cart.cartId = :cartId AND ci.isSelected = true")
    void deleteAllSelectedByCartId(@Param("cartId") Long cartId);

    /**
     * Cập nhật số lượng của một mục trong giỏ hàng
     */
    @Modifying
    @Query("UPDATE CartItems ci SET ci.quantity = :quantity WHERE ci.itemId = :itemId")
    int updateQuantity(@Param("itemId") Long itemId, @Param("quantity") Integer quantity);

    /**
     * Đếm số lượng mục trong giỏ hàng
     */
    long countByCartCartId(Long cartId);

    /**
     * Đếm số lượng mục đã chọn trong giỏ hàng
     */
    long countByCartCartIdAndIsSelectedTrue(Long cartId);

    /**
     * Tìm các mục trong giỏ hàng của một người dùng cụ thể
     */
    @Query("SELECT ci FROM CartItems ci WHERE ci.cart.user.userId = :userId")
    List<CartItems> findByUserId(@Param("userId") Long userId);

    /**
     * Tìm các mục trong giỏ hàng của một session cụ thể
     */
    @Query("SELECT ci FROM CartItems ci WHERE ci.cart.sessionId = :sessionId")
    List<CartItems> findBySessionId(@Param("sessionId") String sessionId);
}