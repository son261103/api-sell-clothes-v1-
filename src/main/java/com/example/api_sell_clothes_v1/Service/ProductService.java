package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Products.*;
import com.example.api_sell_clothes_v1.Entity.Brands;
import com.example.api_sell_clothes_v1.Entity.Categories;
import com.example.api_sell_clothes_v1.Entity.Products;
import com.example.api_sell_clothes_v1.Exceptions.ResourceNotFoundException;
import com.example.api_sell_clothes_v1.Mapper.ProductMapper;
import com.example.api_sell_clothes_v1.Repository.BrandRepository;
import com.example.api_sell_clothes_v1.Repository.CategoryRepository;
import com.example.api_sell_clothes_v1.Repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;
    private final ProductThumbnailService thumbnailService;

    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getAllProducts(
            Pageable pageable,
            String search,
            Long categoryId,
            Long brandId,
            Boolean status,
            BigDecimal minPrice,
            BigDecimal maxPrice) {

        Page<Products> productsPage;

        if (search != null && !search.trim().isEmpty()) {
            // If search keyword is provided, use the search functionality
            productsPage = productRepository.searchProducts(
                    search.trim(),
                    minPrice,
                    maxPrice,
                    categoryId,
                    brandId,
                    status,
                    pageable
            );
        } else if (categoryId != null || brandId != null || status != null) {
            // If any filter is provided but no search keyword
            productsPage = productRepository.findByFilters(
                    categoryId,
                    brandId,
                    status,
                    pageable
            );
        } else {
            // If no filters are provided, return all products
            productsPage = productRepository.findAll(pageable);
        }

        // Map to DTO and return
        return productsPage.map(productMapper::toDto);
    }

    /**
     * Get product hierarchy information including statistics
     */
    @Transactional(readOnly = true)
    public ProductHierarchyDTO getProductsHierarchy() {
        List<Products> products = productRepository.findAll();
        long activeCount = productRepository.countActiveProducts();
        long inactiveCount = productRepository.countInactiveProducts();

        return ProductHierarchyDTO.builder()
                .products(productMapper.toDto(products))
                .totalProducts(products.size())
                .activeProducts((int) activeCount)
                .inactiveProducts((int) inactiveCount)
                .build();
    }

    /**
     * Create new product with thumbnail
     */
    @Transactional
    public ProductResponseDTO createProduct(ProductCreateDTO createDTO, MultipartFile thumbnailFile) {
        // Validate category
        Categories category = categoryRepository.findById(createDTO.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getStatus()) {
            throw new IllegalArgumentException("Cannot create product with inactive category");
        }

        // Validate brand
        Brands brand = brandRepository.findById(createDTO.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));

        if (!brand.getStatus()) {
            throw new IllegalArgumentException("Cannot create product with inactive brand");
        }

        // Handle thumbnail upload
        String thumbnailUrl = null;
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            thumbnailUrl = thumbnailService.uploadThumbnail(thumbnailFile);
            createDTO.setThumbnail(thumbnailUrl);
        }

        // Set default status if not provided
        if (createDTO.getStatus() == null) {
            createDTO.setStatus(true);
        }

        // Create product entity
        Products product = productMapper.toEntity(createDTO);
        product.setCategory(category);
        product.setBrand(brand);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        // Save product
        try {
            Products savedProduct = productRepository.save(product);
            log.info("Created new product with ID: {}", savedProduct.getProductId());
            return productMapper.toDto(savedProduct);
        } catch (Exception e) {
            // If product creation fails, delete the uploaded thumbnail
            if (thumbnailUrl != null) {
                thumbnailService.deleteThumbnail(thumbnailUrl);
            }
            throw e;
        }
    }

    /**
     * Update product with thumbnail
     */
    @Transactional
    public ProductResponseDTO updateProduct(Long productId, ProductUpdateDTO updateDTO, MultipartFile thumbnailFile) {
        // Find existing product
        Products existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        // Update category only if provided
        if (updateDTO.getCategoryId() != null) {
            Categories category = categoryRepository.findById(updateDTO.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

            if (!category.getStatus()) {
                throw new IllegalArgumentException("Cannot update product with inactive category");
            }
            existingProduct.setCategory(category);
        }

        // Update brand only if provided
        if (updateDTO.getBrandId() != null) {
            Brands brand = brandRepository.findById(updateDTO.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));

            if (!brand.getStatus()) {
                throw new IllegalArgumentException("Cannot update product with inactive brand");
            }
            existingProduct.setBrand(brand);
        }

        // Handle thumbnail update
        String oldThumbnailUrl = existingProduct.getThumbnail();
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            String newThumbnailUrl = thumbnailService.updateThumbnail(thumbnailFile, oldThumbnailUrl);
            updateDTO.setThumbnail(newThumbnailUrl);
        } else {
            // Keep the old thumbnail if no new one is provided
            updateDTO.setThumbnail(oldThumbnailUrl);
        }

        // Update only non-null fields from DTO
        if (updateDTO.getName() != null) {
            existingProduct.setName(updateDTO.getName());
        }
        if (updateDTO.getDescription() != null) {
            existingProduct.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getSlug() != null) {
            existingProduct.setSlug(updateDTO.getSlug());
        }
        if (updateDTO.getSlug() != null) {
            existingProduct.setSlug(updateDTO.getSlug());
        }
        if (updateDTO.getPrice() != null) {
            existingProduct.setPrice(updateDTO.getPrice());
        }
        if (updateDTO.getSalePrice() != null) {
            existingProduct.setSalePrice(updateDTO.getSalePrice());
        }
        if (updateDTO.getStatus() != null) {
            existingProduct.setStatus(updateDTO.getStatus());
        }
        if (updateDTO.getThumbnail() != null) {
            existingProduct.setThumbnail(updateDTO.getThumbnail());
        }

        existingProduct.setUpdatedAt(LocalDateTime.now());

        // Save updated product
        Products savedProduct = productRepository.save(existingProduct);
        log.info("Updated product with ID: {}", savedProduct.getProductId());

        return productMapper.toDto(savedProduct);
    }

    /**
     * Delete product and its thumbnail
     */
    @Transactional
    public ApiResponse deleteProduct(Long productId) {
        try {
            Products product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found"));

            // Delete thumbnail if exists
            if (product.getThumbnail() != null && !product.getThumbnail().isEmpty()) {
                thumbnailService.deleteThumbnail(product.getThumbnail());
            }

            productRepository.delete(product);
            log.info("Deleted product with ID: {}", productId);

            return new ApiResponse(true, "Product successfully deleted");
        } catch (DataIntegrityViolationException e) {
            log.error("Failed to delete product due to data integrity violation: {}", e.getMessage());
            throw new IllegalStateException("Cannot delete product as it is referenced by other entities");
        }
    }

    /**
     * Get product by ID
     */
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductById(Long productId) {
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
        return productMapper.toDto(product);
    }

    /**
     * Get product by slug
     */
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductBySlug(String slug) {
        Products product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
        return productMapper.toDto(product);
    }

    /**
     * Get products by category
     */
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByCategory(Long categoryId) {
        List<Products> products = productRepository.findByCategoryIdIncludingSubCategories(categoryId);
        return productMapper.toDto(products);
    }

    /**
     * Get products by brand
     */
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByBrand(Long brandId) {
        List<Products> products = productRepository.findByBrandBrandId(brandId);
        return productMapper.toDto(products);
    }

    /**
     * Get filtered products with pagination
     */
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getFilteredProducts(Long categoryId, Long brandId, Boolean status, Pageable pageable) {
        Page<Products> productsPage = productRepository.findByFilters(categoryId, brandId, status, pageable);
        return productsPage.map(productMapper::toDto);
    }

    /**
     * Toggle product status
     */
    @Transactional
    public ApiResponse toggleProductStatus(Long productId) {
        try {
            Products product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found"));

            // Check category and brand status before activating product
            if (!product.getStatus()) { // If we're activating the product
                if (!product.getCategory().getStatus()) {
                    throw new IllegalArgumentException("Cannot activate product when category is inactive");
                }
                if (!product.getBrand().getStatus()) {
                    throw new IllegalArgumentException("Cannot activate product when brand is inactive");
                }
            }

            product.setStatus(!product.getStatus());
            product.setUpdatedAt(LocalDateTime.now());
            productRepository.save(product);

            String statusMessage = product.getStatus() ? "activated" : "deactivated";
            log.info("Product ID {} has been {}", productId, statusMessage);

            return new ApiResponse(true, "Product has been " + statusMessage);
        } catch (Exception e) {
            log.error("Error toggling product status: {}", e.getMessage());
            return new ApiResponse(false, "Error updating product status: " + e.getMessage());
        }
    }

    /**
     * Search products with various filters
     */
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> searchProducts(
            String keyword,
            Double minPrice,
            Double maxPrice,
            Long categoryId,
            Long brandId,
            Boolean status,
            Pageable pageable) {

        // Convert Double to BigDecimal for price comparison
        BigDecimal minPriceBD = minPrice != null ? BigDecimal.valueOf(minPrice) : null;
        BigDecimal maxPriceBD = maxPrice != null ? BigDecimal.valueOf(maxPrice) : null;

        Page<Products> productsPage = productRepository.searchProducts(
                keyword, minPriceBD, maxPriceBD, categoryId, brandId, status, pageable);
        return productsPage.map(productMapper::toDto);
    }

    /**
     * Get products on sale
     */
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getProductsOnSale(Pageable pageable) {
        Page<Products> productsPage = productRepository.findBySalePriceIsNotNullAndStatusIsTrue(pageable);
        return productsPage.map(productMapper::toDto);
    }

    /**
     * Get latest products
     */
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getLatestProducts(int limit, Pageable pageable) {
        // Override page size if limit is provided
        if (limit > 0) {
            pageable = PageRequest.of(pageable.getPageNumber(), limit, pageable.getSort());
        }
        Page<Products> productsPage = productRepository.findByStatusIsTrueOrderByCreatedAtDesc(pageable);
        return productsPage.map(productMapper::toDto);
    }

    /**
     * Get featured products
     */
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getFeaturedProducts(int limit) {
        List<Products> products = productRepository.findFeaturedProducts(limit);
        return productMapper.toDto(products);
    }

    /**
     * Get related products
     */
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getRelatedProducts(Long productId, int limit) {
        Products currentProduct = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        List<Products> relatedProducts = productRepository
                .findRelatedProducts(productId, currentProduct.getCategory().getCategoryId(),
                        currentProduct.getBrand().getBrandId(), limit);

        return productMapper.toDto(relatedProducts);
    }
}