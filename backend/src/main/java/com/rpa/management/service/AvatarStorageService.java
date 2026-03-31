package com.rpa.management.service;

import com.rpa.management.common.exception.BadRequestBusinessException;
import com.rpa.management.common.exception.ResourceNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AvatarStorageService {

    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final String MANAGED_URL_MARKER = "/user/avatar/";

    private final Path storageDir;

    public AvatarStorageService() {
        try {
            this.storageDir = Path.of("storage", "avatars").toAbsolutePath().normalize();
            Files.createDirectories(this.storageDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to initialize avatar storage", ex);
        }
    }

    public String store(MultipartFile file) {
        validate(file);

        String extension = resolveExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + extension;
        Path target = storageDir.resolve(filename).normalize();

        if (!target.startsWith(storageDir)) {
            throw new BadRequestBusinessException("Invalid avatar file path");
        }

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store avatar", ex);
        }
    }

    public Resource load(String filename) {
        try {
            Path file = resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Avatar not found");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("Avatar not found");
        }
    }

    public String probeContentType(String filename) {
        try {
            String contentType = Files.probeContentType(resolve(filename));
            return StringUtils.hasText(contentType) ? contentType : "application/octet-stream";
        } catch (IOException ex) {
            return "application/octet-stream";
        }
    }

    public void deleteManagedAvatar(String avatarUrl) {
        String filename = extractManagedFilename(avatarUrl);
        if (!StringUtils.hasText(filename)) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(filename));
        } catch (IOException ex) {
            // Ignore cleanup failures; the new avatar has already been saved.
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestBusinessException("Avatar file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestBusinessException("Avatar file must be 2MB or smaller");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BadRequestBusinessException("Only image files are supported");
        }
        resolveExtension(file.getOriginalFilename());
    }

    private Path resolve(String filename) {
        Path file = storageDir.resolve(filename).normalize();
        if (!file.startsWith(storageDir)) {
            throw new ResourceNotFoundException("Avatar not found");
        }
        return file;
    }

    private String resolveExtension(String originalFilename) {
        String cleaned = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        String extension = StringUtils.getFilenameExtension(cleaned);
        if (!StringUtils.hasText(extension)) {
            throw new BadRequestBusinessException("Avatar file extension is required");
        }
        String normalized = extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(normalized)) {
            throw new BadRequestBusinessException("Avatar must be JPG, PNG, GIF, or WebP");
        }
        return normalized;
    }

    private String extractManagedFilename(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl) || !avatarUrl.contains(MANAGED_URL_MARKER)) {
            return null;
        }
        String filename = avatarUrl.substring(avatarUrl.lastIndexOf('/') + 1);
        return StringUtils.hasText(filename) ? filename : null;
    }
}
