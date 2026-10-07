package com.getlinkdtd.link.service;

import com.getlinkdtd.link.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AvatarStorageService {
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-f0-9-]{36}\\.(png|jpg|webp|gif)$");
    private static final Map<String, MediaType> MEDIA_TYPES = Map.of(
            "png", MediaType.IMAGE_PNG,
            "jpg", MediaType.IMAGE_JPEG,
            "webp", MediaType.parseMediaType("image/webp"),
            "gif", MediaType.IMAGE_GIF);

    private final Path directory;
    private final long maxBytes;

    public AvatarStorageService(
            @Value("${getlink.link.avatar.directory:./data/avatars}") String directory,
            @Value("${getlink.link.avatar.max-bytes:5242880}") long maxBytes) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
        try {
            Files.createDirectories(this.directory);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create avatar storage directory", exception);
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalid("Please choose an image");
        }
        if (file.getSize() > maxBytes) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "AVATAR_TOO_LARGE", "Avatar must be 5 MB or smaller");
        }

        String extension;
        try (InputStream input = file.getInputStream()) {
            extension = extension(input.readNBytes(12));
        } catch (IOException exception) {
            throw invalid("Could not read the uploaded image");
        }
        if (extension == null) {
            throw invalid("Only PNG, JPEG, WebP, and GIF images are allowed");
        }

        String filename = UUID.randomUUID() + "." + extension;
        Path destination = safePath(filename);
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "AVATAR_STORE_FAILED", "Could not store the avatar");
        }
        return filename;
    }

    public StoredAvatar load(String filename) {
        Path path = safePath(filename);
        if (!Files.isRegularFile(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Avatar not found");
        }
        try {
            Resource resource = new UrlResource(path.toUri());
            String extension = filename.substring(filename.lastIndexOf('.') + 1);
            return new StoredAvatar(resource, MEDIA_TYPES.get(extension));
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Avatar not found");
        }
    }

    public void deleteFromPublicUrl(String avatarUrl) {
        String prefix = "/api/links/avatars/";
        if (avatarUrl == null || !avatarUrl.startsWith(prefix)) return;
        String filename = avatarUrl.substring(prefix.length());
        if (!SAFE_FILENAME.matcher(filename).matches()) return;
        try {
            Files.deleteIfExists(safePath(filename));
        } catch (IOException ignored) {
            // A stale avatar can be cleaned up later without failing the profile update.
        }
    }

    private Path safePath(String filename) {
        if (!SAFE_FILENAME.matcher(filename).matches()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Avatar not found");
        }
        Path path = directory.resolve(filename).normalize();
        if (!path.getParent().equals(directory)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Avatar not found");
        }
        return path;
    }

    private String extension(byte[] header) {
        String hex = HexFormat.of().formatHex(header);
        if (hex.startsWith("89504e470d0a1a0a")) return "png";
        if (hex.startsWith("ffd8ff")) return "jpg";
        if (hex.startsWith("474946383761") || hex.startsWith("474946383961")) return "gif";
        if (header.length >= 12 && hex.startsWith("52494646") && hex.substring(16, 24).equals("57454250")) return "webp";
        return null;
    }

    private ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AVATAR", message);
    }

    public record StoredAvatar(Resource resource, MediaType mediaType) {}
}
