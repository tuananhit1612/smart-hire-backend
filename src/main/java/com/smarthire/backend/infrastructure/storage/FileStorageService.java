package com.smarthire.backend.infrastructure.storage;

import com.smarthire.backend.core.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private final Path uploadDir;
    private final long maxFileSize;
    private final List<String> allowedImageTypes;
    private final List<String> allowedDocumentTypes;

    public FileStorageService(
            @Value("${app.storage.upload-dir}") String uploadDir,
            @Value("${app.storage.max-file-size}") long maxFileSize,
            @Value("${app.storage.allowed-image-types}") List<String> allowedImageTypes,
            @Value("${app.storage.allowed-document-types}") List<String> allowedDocumentTypes) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize;
        this.allowedImageTypes = allowedImageTypes;
        this.allowedDocumentTypes = allowedDocumentTypes;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadDir);
            log.info("Upload directory initialized: {}", uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadDir, e);
        }
    }

    /**
     * Upload ảnh (avatar, logo, ...).
     * @param file file upload
     * @param subDir thư mục con (vd: "avatars", "logos")
     * @return đường dẫn relative từ upload root (vd: "avatars/uuid.png")
     */
    public String storeImage(MultipartFile file, String subDir) {
        validateFile(file, allowedImageTypes, "Image");
        return storeFile(file, subDir);
    }

    /**
     * Upload tài liệu (CV PDF/DOCX, ...).
     * @param file file upload
     * @param subDir thư mục con (vd: "cv")
     * @return đường dẫn relative từ upload root
     */
    public String storeDocument(MultipartFile file, String subDir) {
        validateFile(file, allowedDocumentTypes, "Document");
        return storeFile(file, subDir);
    }

    /**
     * Upload hồ sơ onboarding (Chấp nhận cả ảnh và tài liệu PDF/DOCX).
     */
    public String storeOnboardingFile(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        if (file.getSize() > maxFileSize) {
            throw new BadRequestException("File size must not exceed " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        boolean isImage = contentType != null && allowedImageTypes.contains(contentType);
        boolean isDoc = contentType != null && allowedDocumentTypes.contains(contentType);

        if (!isImage && !isDoc) {
            throw new BadRequestException("Invalid file type. Allowed: Image (jpeg, png, webp) or Document (pdf, docx)");
        }

        return storeFile(file, subDir);
    }

    /**
     * Xóa file theo đường dẫn relative.
     */
    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            Path filePath = uploadDir.resolve(relativePath).normalize();
            Files.deleteIfExists(filePath);
            log.info("Deleted file: {}", filePath);
        } catch (IOException e) {
            log.warn("Could not delete file: {}", relativePath, e);
        }
    }

    /**
     * Lấy absolute path từ relative path.
     */
    public Path getFilePath(String relativePath) {
        return uploadDir.resolve(relativePath).normalize();
    }

    private void validateFile(MultipartFile file, List<String> allowedTypes, String fileCategory) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(fileCategory + " file is required");
        }

        if (file.getSize() > maxFileSize) {
            throw new BadRequestException(fileCategory + " file size must not exceed "
                    + (maxFileSize / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new BadRequestException("Invalid " + fileCategory.toLowerCase()
                    + " file type. Allowed: " + String.join(", ", allowedTypes));
        }
    }

    private String storeFile(MultipartFile file, String subDir) {
        try {
            Path targetDir = uploadDir.resolve(subDir);
            Files.createDirectories(targetDir);

            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null
                    ? file.getOriginalFilename() : "file");
            String ext = getExtension(originalFilename);
            String newFilename = UUID.randomUUID() + ext;

            Path targetPath = targetDir.resolve(newFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = subDir + "/" + newFilename;
            log.info("Stored file: {}", relativePath);
            return relativePath;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex) : "";
    }
}
