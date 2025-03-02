package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Brands.BrandCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Categories.CategoryCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Brands.BrandHierarchyDTO;
import com.example.api_sell_clothes_v1.DTO.Brands.BrandResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Brands.BrandUpdateDTO;
import com.example.api_sell_clothes_v1.Service.BrandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(ApiPatternConstants.API_BRANDS)
@RequiredArgsConstructor
public class BrandController {
    private final BrandService brandService;
    private final ObjectMapper objectMapper;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('VIEW_BRAND')")
    public ResponseEntity<Page<BrandResponseDTO>> getAllBrands(
            @PageableDefault(page = 0, size = 10, sort = "brandId") Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean status) {
        return ResponseEntity.ok(brandService.getAllBrands(pageable, search, status));
    }

    @GetMapping("/list/active")
    @PreAuthorize("hasAuthority('VIEW_BRAND')")
    public ResponseEntity<List<BrandResponseDTO>> getActiveBrands() {
        List<BrandResponseDTO> brands = brandService.getActiveBrands();
        return ResponseEntity.ok(brands);
    }

    @GetMapping("/view/{id}")
    @PreAuthorize("hasAuthority('VIEW_BRAND')")
    public ResponseEntity<BrandResponseDTO> getBrandById(@PathVariable("id") Long brandId) {
        return ResponseEntity.ok(brandService.getBrandById(brandId));
    }

    /**
     * Create new brand with optional logo
     * Accepts multipart form data with JSON string for brand data
     */
    @PostMapping(value = "/create", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasAuthority('CREATE_BRAND')")
    public ResponseEntity<BrandResponseDTO> createBrand(
            @RequestParam("brand") String brandCreateDTOString,
            @RequestParam(value = "logo", required = false) MultipartFile logoFile) {
        try {
            BrandCreateDTO createDTO = objectMapper.readValue(brandCreateDTOString, BrandCreateDTO.class);
            BrandResponseDTO createdBrand = brandService.createBrand(createDTO, logoFile);
            return new ResponseEntity<>(createdBrand, HttpStatus.CREATED);
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi khi xử lý dữ liệu thương hiệu: " + e.getMessage());
        }
    }

    /**
     * Update brand with optional logo
     * Accepts multipart form data with JSON string for brand data
     */
    @PutMapping(value = "/edit/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasAuthority('EDIT_BRAND')")
    public ResponseEntity<BrandResponseDTO> updateBrand(
            @PathVariable("id") Long brandId,
            @RequestParam("brand") String brandUpdateDTOString,
            @RequestParam(value = "logo", required = false) MultipartFile logoFile) {
        try {
            BrandUpdateDTO updateDTO = objectMapper.readValue(brandUpdateDTOString, BrandUpdateDTO.class);
            BrandResponseDTO updatedBrand = brandService.updateBrand(brandId, updateDTO, logoFile);
            return ResponseEntity.ok(updatedBrand);
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi khi xử lý dữ liệu cập nhật: " + e.getMessage());
        }
    }


    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('DELETE_BRAND')")
    public ResponseEntity<ApiResponse> deleteBrand(@PathVariable("id") Long brandId) {
        return ResponseEntity.ok(brandService.deleteBrand(brandId));
    }

    @GetMapping("/hierarchy")
    @PreAuthorize("hasAuthority('VIEW_BRAND')")
    public ResponseEntity<BrandHierarchyDTO> getBrandHierarchy() {
        return ResponseEntity.ok(brandService.getAllBrandsHierarchy());
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('VIEW_BRAND')")
    public ResponseEntity<List<BrandResponseDTO>> searchBrands(
            @RequestParam(required = false) String keyword
    ) {
        try {
            List<BrandResponseDTO> results = brandService.searchBrands(keyword);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PatchMapping("/status/{id}")
    @PreAuthorize("hasAuthority('EDIT_BRAND')")
    public ResponseEntity<ApiResponse> toggleBrandStatus(@PathVariable("id") Long brandId) {
        ApiResponse response = brandService.toggleBrandStatus(brandId);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }
}