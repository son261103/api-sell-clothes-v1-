package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.Exceptions.FileHandlingException;
import com.example.api_sell_clothes_v1.Service.BrandLogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(ApiPatternConstants.API_BRANDS + "/logo")
@RequiredArgsConstructor
public class BrandLogoController {
    private final BrandLogoService brandLogoService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_BRAND')")
    public ResponseEntity<String> uploadLogo(@RequestParam("logo") MultipartFile logoFile) {
        try {
            String logoUrl = brandLogoService.uploadLogo(logoFile);
            return ResponseEntity.ok(logoUrl);
        } catch (FileHandlingException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_BRAND')")
    public ResponseEntity<String> updateLogo(
            @RequestParam("logo") MultipartFile newLogoFile,
            @RequestParam("oldUrl") String oldLogoUrl) {
        try {
            String newLogoUrl = brandLogoService.updateLogo(newLogoFile, oldLogoUrl);
            return ResponseEntity.ok(newLogoUrl);
        } catch (FileHandlingException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAuthority('EDIT_BRAND')")
    public ResponseEntity<ApiResponse> deleteLogo(@RequestParam("url") String logoUrl) {
        try {
            brandLogoService.deleteLogo(logoUrl);
            return ResponseEntity.ok(new ApiResponse(true, "Logo đã được xóa thành công"));
        } catch (FileHandlingException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Không thể xóa logo: " + e.getMessage()));
        }
    }
}