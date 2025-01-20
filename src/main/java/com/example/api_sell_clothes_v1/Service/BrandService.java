package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Brands.*;
import com.example.api_sell_clothes_v1.DTO.Categories.CategoryCreateDTO;
import com.example.api_sell_clothes_v1.Entity.Brands;
import com.example.api_sell_clothes_v1.Exceptions.FileHandlingException;
import com.example.api_sell_clothes_v1.Mapper.BrandMapper;
import com.example.api_sell_clothes_v1.Repository.BrandRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandService {
    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;
    private final BrandLogoService brandLogoService;

    /**
     * Get all brands with hierarchy information
     */
    @Transactional(readOnly = true)
    public BrandHierarchyDTO getAllBrandsHierarchy() {
        List<Brands> brands = brandRepository.findAllByOrderByNameAsc();
        Object[] stats = brandRepository.getBrandStatistics();

        // Đảm bảo stats không null và chứa 3 phần tử mong đợi
        int totalBrands = stats != null && stats.length > 0 ? ((Long) stats[0]).intValue() : 0;
        int activeBrands = stats != null && stats.length > 1 ? ((Long) stats[1]).intValue() : 0;
        int inactiveBrands = stats != null && stats.length > 2 ? ((Long) stats[2]).intValue() : 0;

        return BrandHierarchyDTO.builder()
                .brands(brandMapper.toDto(brands))
                .totalBrands(totalBrands)
                .activeBrands(activeBrands)
                .inactiveBrands(inactiveBrands)
                .build();
    }


    /**
     * Get brand by ID
     */
    @Transactional(readOnly = true)
    public BrandResponseDTO getBrandById(Long brandId) {
        Brands brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new EntityNotFoundException("Brand does not exist"));
        return brandMapper.toDto(brand);
    }

    /**
     * Get all brands
     */
    @Transactional(readOnly = true)
    public List<BrandResponseDTO> getAllBrands() {
        return brandMapper.toDto(brandRepository.findAll());
    }

    /**
     * Get active brands
     */
    @Transactional(readOnly = true)
    public List<BrandResponseDTO> getActiveBrands() {
        return brandMapper.toDto(brandRepository.findByStatusTrue());
    }

    /**
     * Search brands by name
     */
    @Transactional(readOnly = true)
    public List<BrandResponseDTO> searchBrands(String keyword) {
        return brandMapper.toDto(brandRepository.findByNameContainingIgnoreCase(keyword));
    }

    /**
     * Create new brand with logo
     */
    @Transactional
    public BrandResponseDTO createBrand(BrandCreateDTO createDTO, MultipartFile logoFile) {
        // Validate unique name
        if (brandRepository.existsByNameIgnoreCase(createDTO.getName())) {
            throw new IllegalArgumentException("brand name already exists");
        }

        // Create brand entity
        Brands brand = brandMapper.toEntity(createDTO);

        // Set status to active by default
        brand.setStatus(true);

        // Upload logo if provided
        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                String logoUrl = brandLogoService.uploadLogo(logoFile);
                brand.setLogoUrl(logoUrl);
            } catch (FileHandlingException e) {
                log.error("Failed to upload brand logo: {}", e.getMessage());
                throw new FileHandlingException("Unable to upload brand logo: " + e.getMessage());
            }
        }

        // Save brand
        Brands savedBrand = brandRepository.save(brand);
        log.info("Created new brand with ID: {}", savedBrand.getBrandId());

        return brandMapper.toDto(savedBrand);
    }

    /**
     * Update brand information and logo
     */
    @Transactional
    public BrandResponseDTO updateBrand(Long brandId, BrandUpdateDTO updateDTO, MultipartFile logoFile) {
        // Find existing brand
        Brands existingBrand = brandRepository.findById(brandId)
                .orElseThrow(() -> new EntityNotFoundException("Brand does not exist"));

        // Validate unique name if changed
        if (updateDTO.getName() != null &&
                !updateDTO.getName().isEmpty() &&
                !updateDTO.getName().equalsIgnoreCase(existingBrand.getName()) &&
                brandRepository.existsByNameIgnoreCase(updateDTO.getName())) {
            throw new IllegalArgumentException("brand name already exists");
        }

        // Update logo if provided
        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                String newLogoUrl = brandLogoService.updateLogo(logoFile, existingBrand.getLogoUrl());
                existingBrand.setLogoUrl(newLogoUrl);
            } catch (FileHandlingException e) {
                log.error("Failed to update brand logo: {}", e.getMessage());
                throw new FileHandlingException("Unable to update brand logo: " + e.getMessage());
            }
        }

        // Update only changed fields
        if (updateDTO.getName() != null && !updateDTO.getName().isEmpty()) {
            existingBrand.setName(updateDTO.getName());
        }
        if (updateDTO.getDescription() != null) {
            existingBrand.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getStatus() != null) {
            existingBrand.setStatus(updateDTO.getStatus());
        }

        if (updateDTO.getLogoUrl() != null){
            existingBrand.setLogoUrl(updateDTO.getLogoUrl());
        }

        // Save updated brand
        Brands savedBrand = brandRepository.save(existingBrand);
        log.info("Updated brand with ID: {}", savedBrand.getBrandId());

        return brandMapper.toDto(savedBrand);
    }

    /**
     * Delete brand and its logo
     */
    @Transactional
    public ApiResponse deleteBrand(Long brandId) {
        Brands brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new EntityNotFoundException("Brand does not exist"));

        boolean logoDeletedSuccessfully = true;
        String logoDeleteError = null;

        // Try to delete logo if exists
        if (brand.getLogoUrl() != null && !brand.getLogoUrl().isEmpty()) {
            try {
                brandLogoService.deleteLogo(brand.getLogoUrl());
            } catch (FileHandlingException e) {
                logoDeletedSuccessfully = false;
                logoDeleteError = e.getMessage();
                log.warn("Failed to delete brand logo: {}. Will proceed with brand deletion.", e.getMessage());
            } catch (Exception e) {
                logoDeletedSuccessfully = false;
                logoDeleteError = "Unexpected error while deleting logo";
                log.warn("Unexpected error while deleting brand logo: {}. Will proceed with brand deletion.", e.getMessage());
            }
        }

        try {
            // Delete brand
            brandRepository.delete(brand);
            log.info("Deleted brand with ID: {}", brandId);

            // Return appropriate response based on logo deletion status
            if (logoDeletedSuccessfully) {
                return new ApiResponse(true, "Brand and its logo were successfully removed");
            } else {
                return new ApiResponse(true,
                        String.format("Brand was removed but failed to delete logo: %s", logoDeleteError));
            }
        } catch (DataIntegrityViolationException e) {
            log.error("Failed to delete brand due to data integrity violation: {}", e.getMessage());
            throw new IllegalStateException("Cannot delete brand as it is referenced by other entities");
        } catch (Exception e) {
            log.error("Failed to delete brand: {}", e.getMessage());
            throw new RuntimeException("Failed to delete brand: " + e.getMessage());
        }
    }
    /**
     * Update brand status
     */
    @Transactional
    public ApiResponse updateBrandStatus(Long brandId, Integer status) {
        try {
            // Chỉ chấp nhận trạng thái 0 hoặc 1
            if (status != 0 && status != 1) {
                throw new IllegalArgumentException("Invalid status value. Only 0 (deactive) or 1 (active) are allowed.");
            }

            Brands brand = brandRepository.findById(brandId)
                    .orElseThrow(() -> new EntityNotFoundException("Brand does not exist"));

            // Cập nhật trạng thái
            brand.setStatus(status == 1);
            brandRepository.save(brand);

            String statusMessage = (status == 1) ? "activate" : "deactivate";
            log.info("Updated status for brand ID {} to {}", brandId, statusMessage);

            return new ApiResponse(true, "Successfully " + statusMessage + " brand.");
        } catch (IllegalArgumentException e) {
            log.error("Invalid status value provided: {}", e.getMessage());
            return new ApiResponse(false, "Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error updating brand status: {}", e.getMessage());
            return new ApiResponse(false, "Error updating brand status: " + e.getMessage());
        }
    }


    @Transactional
    public ApiResponse toggleBrandStatus(Long brandId) {
        try {
            Brands brand = brandRepository.findById(brandId)
                    .orElseThrow(() -> new EntityNotFoundException("Brand does not exist"));

            // Nếu đang kích hoạt brand, cần kiểm tra các điều kiện liên quan
            if (!brand.getStatus()) { // Nếu đang kích hoạt brand
                // Giả sử bạn có logic kiểm tra các ràng buộc liên quan (ví dụ: sản phẩm thuộc brand)
                boolean hasActiveProducts = brandRepository.existsByBrandIdAndStatus(brand.getBrandId(), true);
                if (hasActiveProducts) {
                    throw new IllegalArgumentException("Cannot activate brand while there are active products under this brand");
                }
            }

            // Cập nhật trạng thái của brand
            brand.setStatus(!brand.getStatus());
            brandRepository.save(brand);

            String statusMessage = brand.getStatus() ? "activated" : "deactivated";
            log.info("Brand ID {} has been {}", brandId, statusMessage);

            return new ApiResponse(true, "Brand has been " + statusMessage);
        } catch (IllegalArgumentException e) {
            log.error("Invalid operation for brand status: {}", e.getMessage());
            return new ApiResponse(false, "Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error toggling brand status: {}", e.getMessage());
            return new ApiResponse(false, "Error updating brand status: " + e.getMessage());
        }
    }

}