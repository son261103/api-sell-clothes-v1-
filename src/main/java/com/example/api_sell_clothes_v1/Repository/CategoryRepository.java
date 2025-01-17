package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.Categories;
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
}