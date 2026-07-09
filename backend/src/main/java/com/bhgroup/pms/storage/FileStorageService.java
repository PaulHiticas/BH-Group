package com.bhgroup.pms.storage;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.config.AppProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/avif");

    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final AppProperties appProperties;

    public StoredFile storeImage(MultipartFile file, String subfolder) {
        return store(file, subfolder, ALLOWED_IMAGE_TYPES, "Only JPEG, PNG, WEBP or AVIF images are allowed");
    }

    public StoredFile storeDocument(MultipartFile file, String subfolder) {
        return store(file, subfolder, ALLOWED_DOCUMENT_TYPES, "Only PDF, DOC, DOCX or image files are allowed");
    }

    private StoredFile store(MultipartFile file, String subfolder, Set<String> allowedTypes, String errorMessage) {
        if (file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }
        if (!allowedTypes.contains(file.getContentType())) {
            throw new BadRequestException(errorMessage);
        }

        String extension = extractExtension(file.getOriginalFilename());
        String fileKey = subfolder + "/" + UUID.randomUUID() + extension;

        try {
            Path targetPath = resolvePath(fileKey);
            Files.createDirectories(targetPath.getParent());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.error("Failed to store file {}", fileKey, ex);
            throw new BadRequestException("Failed to store file");
        }

        String url = appProperties.getStorage().getPublicBaseUrl() + "/" + fileKey;
        return new StoredFile(fileKey, url);
    }

    public void delete(String fileKey) {
        try {
            Files.deleteIfExists(resolvePath(fileKey));
        } catch (IOException ex) {
            log.warn("Failed to delete file {}", fileKey, ex);
        }
    }

    private Path resolvePath(String fileKey) {
        return Paths.get(appProperties.getStorage().getUploadDir()).resolve(fileKey).normalize();
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }

    public record StoredFile(String fileKey, String url) {
    }
}
