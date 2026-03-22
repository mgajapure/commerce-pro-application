package com.commerce_pro_backend.common.storage;

import com.commerce_pro_backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

/**
 * Controller for file upload/download operations
 */
@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
@Tag(name = "File Storage", description = "File upload and download APIs")
public class FileStorageController {

    private final FileStorageService fileStorageService;

    /**
     * Upload product image
     * POST /api/files/upload/product
     */
    @PostMapping("/upload/product")
    @Operation(summary = "Upload product image", description = "Upload an image file for a product")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProductImage(
            @RequestParam("file") MultipartFile file) {

        fileStorageService.validateImageFile(file);
        String filePath = fileStorageService.storeProductImage(file);

        return ResponseEntity.ok(ApiResponse.success(
                "File uploaded successfully",
                Map.of("fileName", filePath, "url", "/api/files/download/" + filePath)
        ));
    }

    /**
     * Upload generic file
     * POST /api/files/upload
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload file", description = "Upload a generic file")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "") String subdirectory) {

        String filePath = fileStorageService.storeFile(file, subdirectory);

        return ResponseEntity.ok(ApiResponse.success(
                "File uploaded successfully",
                Map.of("fileName", filePath, "url", "/api/files/download/" + filePath)
        ));
    }

    /**
     * Download file
     * GET /api/files/download/{fileName:.+}
     */
    @GetMapping("/download/{fileName:.+}")
    @Operation(summary = "Download file", description = "Download a file by name")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileName,
            HttpServletRequest request) {

        Resource resource = fileStorageService.loadFileAsResource(fileName);

        // Determine content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // Ignore and use default
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * View image inline
     * GET /api/files/view/{fileName:.+}
     */
    @GetMapping("/view/{fileName:.+}")
    @Operation(summary = "View file", description = "View a file inline (for images)")
    public ResponseEntity<Resource> viewFile(
            @PathVariable String fileName,
            HttpServletRequest request) {

        Resource resource = fileStorageService.loadFileAsResource(fileName);

        // Determine content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // Ignore and use default
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    /**
     * Delete file
     * DELETE /api/files/{fileName:.+}
     */
    @DeleteMapping("/{fileName:.+}")
    @Operation(summary = "Delete file", description = "Delete a file by name")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable String fileName) {
        fileStorageService.deleteFile(fileName);
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
    }
}
