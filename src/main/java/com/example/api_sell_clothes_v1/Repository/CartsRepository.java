package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.Carts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartsRepository extends JpaRepository<Carts, Long> {
    /**
     * Tìm giỏ hàng của người dùng đã đăng nhập
     */
    Optional<Carts> findByUserUserId(Long userId);

    /**
     * Tìm giỏ hàng bằng session ID (cho người dùng chưa đăng nhập)
     */
    Optional<Carts> findBySessionId(String sessionId);

    /**
     * Kiểm tra xem một user đã có giỏ hàng chưa
     */
    boolean existsByUserUserId(Long userId);

    /**
     * Kiểm tra xem một session đã có giỏ hàng chưa
     */
    boolean existsBySessionId(String sessionId);

    /**
     * Đếm số lượng sản phẩm trong giỏ hàng của một người dùng
     */
    @Query("SELECT COUNT(ci) FROM CartItems ci WHERE ci.cart.user.userId = :userId")
    Long countItemsByUserId(@Param("userId") Long userId);

    /**
     * Đếm số lượng sản phẩm trong giỏ hàng của một session
     */
    @Query("SELECT COUNT(ci) FROM CartItems ci WHERE ci.cart.sessionId = :sessionId")
    Long countItemsBySessionId(@Param("sessionId") String sessionId);

    /**
     * Xóa giỏ hàng theo session ID
     */
    void deleteBySessionId(String sessionId);
}