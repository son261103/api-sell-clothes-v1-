package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Brands.*;
import com.example.api_sell_clothes_v1.Entity.Brands;
import com.example.api_sell_clothes_v1.Exceptions.FileHandlingException;
import com.example.api_sell_clothes_v1.Mapper.BrandMapper;
import com.example.api_sell_clothes_v1.Repository.BrandRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        Brands brand = findBrandById(brandId);
        return brandMapper.toDto(brand);
    }

    /**
     * Get all brands with pagination and filters
     */
    @Transactional(readOnly = true)
    public Page<BrandResponseDTO> getAllBrands(Pageable pageable, String search, Boolean status) {
        Page<Brands> brandsPage;

        if (search != null && !search.trim().isEmpty()) {
            if (status != null) {
                brandsPage = brandRepository.findByStatusAndNameContainingIgnoreCase(
                        status, search.trim(), pageable);
            } else {
                brandsPage = brandRepository.findByNameContainingIgnoreCase(
                        search.trim(), pageable);
            }
        } else {
            if (status != null) {
                brandsPage = brandRepository.findByStatus(status, pageable);
            } else {
                brandsPage = brandRepository.findAll(pageable);
            }
        }

        return brandsPage.map(brandMapper::toDto);
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
     * Create new brand
     */
    @Transactional
    public BrandResponseDTO createBrand(BrandCreateDTO createDTO, MultipartFile logoFile) {
        validateUniqueBrandName(createDTO.getName());

        Brands brand = brandMapper.toEntity(createDTO);
        brand.setStatus(true);

        // Save brand first to get an ID
        Brands savedBrand = brandRepository.save(brand);

        // Then handle logo if provided
        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                String logoUrl = brandLogoService.uploadLogoWithBrandId(logoFile, savedBrand.getBrandId());
                savedBrand.setLogoUrl(logoUrl);
                savedBrand = brandRepository.save(savedBrand);
            } catch (Exception e) {
                log.error("Failed to handle logo for new brand: ", e);
                // Continue with brand creation even if logo upload fails
            }
        }

        log.info("Created new brand with ID: {}", savedBrand.getBrandId());
        return brandMapper.toDto(savedBrand);
    }

    /**
     * Update brand information
     */
    @Transactional
    public BrandResponseDTO updateBrand(Long brandId, BrandUpdateDTO updateDTO, MultipartFile logoFile) {
        Brands existingBrand = findBrandById(brandId);

        if (isNameChanged(existingBrand.getName(), updateDTO.getName())) {
            validateUniqueBrandName(updateDTO.getName());
        }

        // Update fields first
        updateBrandFields(existingBrand, updateDTO);
        Brands updatedBrand = brandRepository.save(existingBrand);

        // Then handle logo separately if provided
        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                String newLogoUrl = brandLogoService.updateLogoWithBrandId(
                        logoFile,
                        updatedBrand.getLogoUrl(),
                        updatedBrand.getBrandId()
                );
                updatedBrand.setLogoUrl(newLogoUrl);
                updatedBrand = brandRepository.save(updatedBrand);
            } catch (Exception e) {
                log.error("Failed to handle logo update: ", e);
                // Continue with brand update even if logo update fails
            }
        }

        log.info("Updated brand with ID: {}", updatedBrand.getBrandId());
        return brandMapper.toDto(updatedBrand);
    }

    /**
     * Update brand logo
     */
    @Transactional
    public BrandResponseDTO updateBrandLogo(Long brandId, String logoUrl) {
        Brands brand = findBrandById(brandId);
        brand.setLogoUrl(logoUrl);
        Brands savedBrand = brandRepository.save(brand);
        log.info("Updated logo for brand ID: {} with URL: {}", brandId, logoUrl);
        return brandMapper.toDto(savedBrand);
    }

    /**
     * Delete brand
     */
    @Transactional
    public ApiResponse deleteBrand(Long brandId) {
        Brands brand = findBrandById(brandId);

        boolean logoDeletedSuccessfully = true;
        String logoDeleteError = null;

        if (brand.getLogoUrl() != null && !brand.getLogoUrl().isEmpty()) {
            try {
                brandLogoService.deleteLogoWithBrandId(brand.getLogoUrl(), brand.getBrandId());
            } catch (Exception e) {
                logoDeletedSuccessfully = false;
                logoDeleteError = e.getMessage();
                log.warn("Failed to delete brand logo: {}. Will proceed with brand deletion.", e.getMessage());
            }
        }

        try {
            brandRepository.delete(brand);
            log.info("Deleted brand with ID: {}", brandId);

            return createDeleteResponse(logoDeletedSuccessfully, logoDeleteError);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Cannot delete brand as it is referenced by other entities");
        }
    }

    /**
     * Update brand status
     */
    @Transactional
    public ApiResponse updateBrandStatus(Long brandId, Integer status) {
        validateStatusValue(status);

        Brands brand = findBrandById(brandId);
        brand.setStatus(status == 1);
        brandRepository.save(brand);

        String statusMessage = (status == 1) ? "activate" : "deactivate";
        log.info("Updated status for brand ID {} to {}", brandId, statusMessage);

        return new ApiResponse(true, "Successfully " + statusMessage + " brand.");
    }

    /**
     * Toggle brand status
     */
    @Transactional
    public ApiResponse toggleBrandStatus(Long brandId) {
        Brands brand = findBrandById(brandId);

        if (!brand.getStatus()) {
            validateBrandActivation(brand);
        }

        brand.setStatus(!brand.getStatus());
        brandRepository.save(brand);

        String statusMessage = brand.getStatus() ? "activated" : "deactivated";
        log.info("Brand ID {} has been {}", brandId, statusMessage);

        return new ApiResponse(true, "Brand has been " + statusMessage);
    }

    // Helper methods
    private Brands findBrandById(Long brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new EntityNotFoundException("Brand does not exist"));
    }

    private void validateUniqueBrandName(String name) {
        if (brandRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Brand name already exists");
        }
    }

    private boolean isNameChanged(String oldName, String newName) {
        return newName != null && !newName.isEmpty() && !newName.equalsIgnoreCase(oldName);
    }

    private void updateBrandFields(Brands brand, BrandUpdateDTO updateDTO) {
        if (updateDTO.getName() != null && !updateDTO.getName().isEmpty()) {
            brand.setName(updateDTO.getName());
        }
        if (updateDTO.getDescription() != null) {
            brand.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getStatus() != null) {
            brand.setStatus(updateDTO.getStatus());
        }
        // We handle logoUrl separately through the logo upload process
    }

    private void validateStatusValue(Integer status) {
        if (status != 0 && status != 1) {
            throw new IllegalArgumentException("Invalid status value. Only 0 (deactive) or 1 (active) are allowed.");
        }
    }

    private void validateBrandActivation(Brands brand) {
        boolean hasActiveProducts = brandRepository.existsByBrandIdAndStatus(brand.getBrandId(), true);
        if (hasActiveProducts) {
            throw new IllegalArgumentException("Cannot activate brand while there are active products under this brand");
        }
    }

    private ApiResponse createDeleteResponse(boolean logoDeletedSuccessfully, String logoDeleteError) {
        if (logoDeletedSuccessfully) {
            return new ApiResponse(true, "Brand and its logo were successfully removed");
        } else {
            return new ApiResponse(true,
                    String.format("Brand was removed but failed to delete logo: %s", logoDeleteError));
        }
    }
}