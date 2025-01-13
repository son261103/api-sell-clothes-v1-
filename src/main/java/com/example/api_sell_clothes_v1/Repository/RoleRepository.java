package com.example.api_sell_clothes_v1.Repository;


import com.example.api_sell_clothes_v1.Entity.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Roles, Long> {
    Optional<Roles> findByName(String name);

    boolean existsByName(String name);
}
