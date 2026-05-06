package com.activityplatform.backend.storage;

import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class StorageFilePlanner {
  private static final Map<String, Set<String>> ALLOWED_CONTENT_TYPES_BY_EXTENSION = Map.of(
      ".heic", Set.of("image/heic", "image/heif"),
      ".heif", Set.of("image/heic", "image/heif"),
      ".jpeg", Set.of("image/jpeg", "image/jpg"),
      ".jpg", Set.of("image/jpeg", "image/jpg"),
      ".pdf", Set.of("application/pdf"),
      ".png", Set.of("image/png"),
      ".webp", Set.of("image/webp"),
      ".xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  );
  private static final int MAX_ORIGINAL_FILENAME_LENGTH = 255;
  private static final String SAFE_OWNER_TYPE_PATTERN = "^[A-Za-z0-9_-]{1,40}$";

  private final StorageProperties properties;

  public StorageFilePlanner(StorageProperties properties) {
    this.properties = properties;
  }

  public StorageFilePlan plan(FileStorageRequest request) {
    validateOwnership(request);
    validateSize(request);

    String originalFilename = safeOriginalFilename(request.originalFilename());
    String extension = extensionOf(originalFilename);
    String contentType = normalizeContentType(request.contentType());
    Set<String> allowedContentTypes = ALLOWED_CONTENT_TYPES_BY_EXTENSION.get(extension);

    if (allowedContentTypes == null) {
      throw validation("Uploaded file extension is not allowed.");
    }

    if (!allowedContentTypes.contains(contentType)) {
      throw validation("Uploaded file type does not match its extension.");
    }

    String storageKey = Path.of(
        request.tenantId().toString(),
        request.ownerType(),
        request.ownerId().toString(),
        UUID.randomUUID() + extension
    ).toString().replace('\\', '/');

    return new StorageFilePlan(storageKey, originalFilename, contentType, request.sizeBytes());
  }

  private void validateOwnership(FileStorageRequest request) {
    if (request == null || request.tenantId() == null || request.ownerId() == null) {
      throw validation("File ownership metadata is required.");
    }

    if (request.ownerType() == null || !request.ownerType().matches(SAFE_OWNER_TYPE_PATTERN)) {
      throw validation("File owner type is not allowed.");
    }

    if (request.inputStream() == null) {
      throw validation("File content is required.");
    }
  }

  private void validateSize(FileStorageRequest request) {
    if (request.sizeBytes() <= 0 || request.sizeBytes() > properties.getMaxUploadBytes()) {
      throw validation("Uploaded file size is not allowed.");
    }
  }

  private String extensionOf(String filename) {
    int dotIndex = filename.lastIndexOf('.');
    return filename.substring(dotIndex).toLowerCase(Locale.ROOT);
  }

  private String normalizeContentType(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      throw validation("Uploaded file content type is required.");
    }

    return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
  }

  private String safeOriginalFilename(String filename) {
    if (filename == null || filename.isBlank()) {
      throw validation("Uploaded filename is required.");
    }

    String normalized = filename.replace('\\', '/');
    if (normalized.contains("..")) {
      throw validation("Uploaded filename is not allowed.");
    }

    int separatorIndex = normalized.lastIndexOf('/');
    String basename = separatorIndex >= 0 ? normalized.substring(separatorIndex + 1) : normalized;
    basename = basename.trim();

    if (basename.isBlank()
        || basename.length() > MAX_ORIGINAL_FILENAME_LENGTH
        || basename.chars().anyMatch(character -> character < 32)) {
      throw validation("Uploaded filename is not allowed.");
    }

    int dotIndex = basename.lastIndexOf('.');
    if (dotIndex <= 0 || dotIndex == basename.length() - 1) {
      throw validation("Uploaded filename must include an extension.");
    }

    return basename;
  }

  private ApplicationException validation(String message) {
    return new ApplicationException(ErrorCode.VALIDATION_FAILED, message, HttpStatus.BAD_REQUEST);
  }
}
