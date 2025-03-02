package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.Permissions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permissions, Long> {
    Optional<Permissions> findByCodeName(String codeName);

    List<Permissions> findAllByCodeNameIn(List<String> codeNames);

    List<Permissions> findByGroupName(String groupName);

    boolean existsByName(String name);

    @Query("SELECT p FROM Permissions p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.codeName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.groupName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Permissions> findBySearchCriteria(@Param("search") String search, Pageable pageable);

}
