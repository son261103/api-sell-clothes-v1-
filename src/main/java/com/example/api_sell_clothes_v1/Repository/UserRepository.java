package com.example.api_sell_clothes_v1.Repository;


import com.example.api_sell_clothes_v1.Entity.Users;
import com.example.api_sell_clothes_v1.Enums.Status.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    @Query("SELECT u FROM Users u WHERE u.username = :loginId OR u.email = :loginId OR u.phone = :loginId")
    Optional<Users> findByLoginId(@Param("loginId") String loginId);

    @Query("SELECT MAX(u.userId) FROM Users u")
    Optional<Long> findMaxUserId();

    Optional<Users> findByUsername(String username);

    Optional<Users> findByEmail(String email);

    Optional<Users> findByPhone(String phone);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM Users u WHERE u.status = :status AND " +
            "(LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Users> findByStatusAndSearchCriteria(@Param("status") UserStatus status,
                                              @Param("search") String search,
                                              Pageable pageable);

    @Query("SELECT u FROM Users u WHERE " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Users> findBySearchCriteria(@Param("search") String search, Pageable pageable);

    Page<Users> findByStatus(UserStatus status, Pageable pageable);
}
