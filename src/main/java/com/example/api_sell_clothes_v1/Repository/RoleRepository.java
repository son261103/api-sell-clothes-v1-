package com.example.api_sell_clothes_v1.Repository;


import com.example.api_sell_clothes_v1.Entity.Roles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Roles, Long> {
    Optional<Roles> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT r FROM Roles r WHERE " +
            "LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Roles> findBySearchCriteria(@Param("search") String search, Pageable pageable);
}
