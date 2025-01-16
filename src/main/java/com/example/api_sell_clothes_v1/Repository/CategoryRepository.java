package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.Categories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Categories, Long> {

    // Tìm category theo ID
    Optional<Categories> findByCategoryId(Long id);

    // Tìm category theo slug
    Optional<Categories> findBySlug(String slug);

    // Tìm category theo tên
    Optional<Categories> findByName(String name);

    // Tìm category cha theo parentId
    Optional<Categories> findByParentId(Long parentId);
}
