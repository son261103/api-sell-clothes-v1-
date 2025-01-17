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

        return BrandHierarchyDTO.builder()
                .brands(brandMapper.toDto(brands))
                .totalBrands(((Long) stats[0]).intValue())
                .activeBrands(((Long) stats[1]).intValue())
                .inactiveBrands(((Long) stats[2]).intValue())
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
    public ApiResponse updateBrandStatus(Long brandId, Boolean status) {
        try {
            Brands brand = brandRepository.findById(brandId)
                    .orElseThrow(() -> new EntityNotFoundException("Brand does not exist"));

            brand.setStatus(status);
            brandRepository.save(brand);

            String statusMessage = status ? "activate" : "disable";
            log.info("Updated status for brand ID {} to {}", brandId, status);

            return new ApiResponse(true, "Already " + statusMessage + " successful brand");
        } catch (Exception e) {
            log.error("Error updating brand status: {}", e.getMessage());
            return new ApiResponse(false, "Error updating brand status: " + e.getMessage());
        }
    }
}