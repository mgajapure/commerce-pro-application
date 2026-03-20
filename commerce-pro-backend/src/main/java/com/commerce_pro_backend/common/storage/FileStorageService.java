package com.commerce_pro_backend.common.storage;

import com.commerce_pro_backend.common.exception.ApiException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service for handling file uploads and storage
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    private Path fileStoragePath;

    @PostConstruct
    public void init() {
        this.fileStoragePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStoragePath);
            // Create subdirectories for different file types
            Files.createDirectories(this.fileStoragePath.resolve("products"));
            Files.createDirectories(this.fileStoragePath.resolve("categories"));
            Files.createDirectories(this.fileStoragePath.resolve("brands"));
            Files.createDirectories(this.fileStoragePath.resolve("reports"));
            log.info("File storage initialized at: {}", this.fileStoragePath);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create upload directory", ex);
        }
    }

    /**
     * Store file with a unique name
     */
    public String storeFile(MultipartFile file, String subdirectory) {
        // Normalize file name
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        // Check for invalid characters
        if (originalFileName.contains("..")) {
            throw ApiException.badRequest("Filename contains invalid path sequence: " + originalFileName);
        }

        // Generate unique filename
        String fileExtension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = originalFileName.substring(dotIndex);
        }
        String newFileName = UUID.randomUUID().toString() + fileExtension;

        // Determine target directory
        Path targetPath = fileStoragePath;
        if (StringUtils.hasText(subdirectory)) {
            targetPath = targetPath.resolve(subdirectory);
        }

        try {
            // Copy file to target location
            Path targetLocation = targetPath.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("Stored file: {} -> {}", originalFileName, newFileName);
            
            // Return relative path for storage in database
            return subdirectory != null ? subdirectory + "/" + newFileName : newFileName;
        } catch (IOException ex) {
            throw ApiException.badRequest("Could not store file " + originalFileName);
        }
    }

    /**
     * Store product image
     */
    public String storeProductImage(MultipartFile file) {
        return storeFile(file, "products");
    }

    /**
     * Load file as resource
     */
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = fileStoragePath.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return resource;
            } else {
                throw ApiException.notFound("File", fileName);
            }
        } catch (MalformedURLException ex) {
            throw ApiException.notFound("File", fileName);
        }
    }

    /**
     * Delete file
     */
    public void deleteFile(String fileName) {
        try {
            Path filePath = fileStoragePath.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
            log.info("Deleted file: {}", fileName);
        } catch (IOException ex) {
            log.error("Could not delete file: {}", fileName, ex);
        }
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * Validate file type
     */
    public void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw ApiException.badRequest("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw ApiException.badRequest("Only image files are allowed");
        }

        // Max 10MB
        if (file.getSize() > 10 * 1024 * 1024) {
            throw ApiException.badRequest("File size exceeds 10MB limit");
        }
    }
}
