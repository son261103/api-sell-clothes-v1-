package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    // Find all addresses for a user
    List<UserAddress> findByUserUserIdOrderByIsDefaultDesc(Long userId);

    // Find specific address for a user
    Optional<UserAddress> findByAddressIdAndUserUserId(Long addressId, Long userId);

    // Find default address for user
    Optional<UserAddress> findByUserUserIdAndIsDefaultTrue(Long userId);

    // Find all default addresses for user (using a different method name to avoid erasure clash)
    List<UserAddress> findAllByUserUserIdAndIsDefaultTrue(Long userId);

    // Get first address for user
    Optional<UserAddress> findFirstByUserUserId(Long userId);

    // Count addresses for user
    long countByUserUserId(Long userId);

    // Check if an address is used in any orders
    boolean existsByAddressId(Long addressId);
}