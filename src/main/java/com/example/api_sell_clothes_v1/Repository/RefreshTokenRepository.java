package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.RefreshTokens;
import com.example.api_sell_clothes_v1.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokens, Long> {
    Optional<RefreshTokens> findByRefreshToken(String token);

    Optional<RefreshTokens> findByUser(Users user);

    int deleteAllByUser(Users user);

    // Nếu muốn xóa theo userId, thêm phương thức này
    @Modifying
    @Query("DELETE FROM RefreshTokens r WHERE r.user.userId = :userId")
    void deleteByUserUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM RefreshTokens r WHERE r.expirationTime < :date")
    int deleteByExpirationTimeBefore(@Param("date") LocalDateTime date);
}