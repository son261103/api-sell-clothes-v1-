package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.Exceptions.FileHandlingException;
import com.example.api_sell_clothes_v1.Service.BrandLogoService;
import com.example.api_sell_clothes_v1.Service.BrandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_BRANDS + "/logo")
@RequiredArgsConstructor
public class BrandLogoController {
    private final BrandLogoService brandLogoService;
    private final BrandService brandService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_BRAND')")
    public ResponseEntity<String> uploadLogo(@RequestParam("logo") MultipartFile logoFile) {
        try {
            String logoUrl = brandLogoService.uploadLogo(logoFile);
            return ResponseEntity.ok(logoUrl);
        } catch (FileHandlingException e) {
            log.error("Failed to upload logo: ", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_BRAND')")
    public ResponseEntity<String> updateLogo(
            @RequestParam("logo") MultipartFile newLogoFile,
            @RequestParam("oldUrl") String oldLogoUrl,
            @RequestParam("brandId") Long brandId) {
        try {
            // First update the logo file in Cloudinary
            String newLogoUrl = brandLogoService.updateLogo(newLogoFile, oldLogoUrl);

            // Then update the brand with the new logo URL
            brandService.updateBrandLogo(brandId, newLogoUrl);

            return ResponseEntity.ok(newLogoUrl);
        } catch (FileHandlingException e) {
            log.error("Failed to update logo: ", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update brand with new logo URL: ", e);
            return ResponseEntity.badRequest().body("Logo uploaded but failed to update brand: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAuthority('EDIT_BRAND')")
    public ResponseEntity<ApiResponse> deleteLogo(
            @RequestParam("url") String logoUrl,
            @RequestParam(value = "brandId", required = false) Long brandId) {
        try {
            // Delete the logo from Cloudinary
            brandLogoService.deleteLogo(logoUrl);

            // If brandId is provided, update the brand to remove the logo URL
            if (brandId != null) {
                brandService.updateBrandLogo(brandId, null);
            }

            return ResponseEntity.ok(new ApiResponse(true, "Logo đã được xóa thành công"));
        } catch (FileHandlingException e) {
            log.error("Failed to delete logo: ", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Không thể xóa logo: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update brand after logo deletion: ", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Logo deleted but failed to update brand: " + e.getMessage()));
        }
    }
}