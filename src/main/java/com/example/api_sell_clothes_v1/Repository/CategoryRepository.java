package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.Categories;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Categories, Long> {

    List<Categories> findAllByParentId(Long parentId);


    List<Categories> findAllByParentIdIsNull();

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    Optional<Categories> findByName(String name);

    Optional<Categories> findBySlug(String slug);

    List<Categories> findAllByParentIdAndStatusIsTrue(Long parentId);

    List<Categories> findAllByParentIdIsNullAndStatusIsTrue();

    Page<Categories> findByParentIdIsNullAndStatusAndNameContainingIgnoreCase(
            Boolean status, String search, Pageable pageable);

    Page<Categories> findByParentIdIsNullAndNameContainingIgnoreCase(
            String search, Pageable pageable);

    Page<Categories> findByParentIdIsNullAndStatus(
            Boolean status, Pageable pageable);

    Page<Categories> findByParentIdIsNull(Pageable pageable);

    // For sub categories
    Page<Categories> findByParentIdAndStatusAndNameContainingIgnoreCase(
            Long parentId, Boolean status, String search, Pageable pageable);

    Page<Categories> findByParentIdAndNameContainingIgnoreCase(
            Long parentId, String search, Pageable pageable);

    Page<Categories> findByParentIdAndStatus(
            Long parentId, Boolean status, Pageable pageable);

    Page<Categories> findByParentId(Long parentId, Pageable pageable);
}