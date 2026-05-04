package com.activityplatform.backend.storage;

import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LocalFileStorageService implements FileStorageService {
  private final LocalFileStorageProperties properties;

  public LocalFileStorageService(LocalFileStorageProperties properties) {
    this.properties = properties;
  }

  @Override
  public StoredFile store(FileStorageRequest request) {
    validate(request);

    String extension = extensionOf(request.originalFilename());
    String storageKey = Path.of(
        request.tenantId().toString(),
        request.ownerType(),
        request.ownerId().toString(),
        UUID.randomUUID() + extension
    ).toString().replace('\\', '/');
    Path target = Path.of(properties.getRootPath()).resolve(storageKey).normalize();

    try {
      Files.createDirectories(target.getParent());
      Files.copy(request.inputStream(), target, StandardCopyOption.REPLACE_EXISTING);
      return new StoredFile(
          storageKey,
          request.originalFilename(),
          request.contentType(),
          request.sizeBytes()
      );
    } catch (IOException exception) {
      throw new ApplicationException(
          ErrorCode.FILE_STORAGE_FAILED,
          "Unable to store uploaded file.",
          HttpStatus.INTERNAL_SERVER_ERROR
      );
    }
  }

  private void validate(FileStorageRequest request) {
    if (request.sizeBytes() <= 0 || request.sizeBytes() > properties.getMaxUploadBytes()) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_FAILED,
          "Uploaded file size is not allowed.",
          HttpStatus.BAD_REQUEST
      );
    }
  }

  private String extensionOf(String filename) {
    if (filename == null || filename.isBlank()) {
      return "";
    }

    int dotIndex = filename.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex == filename.length() - 1) {
      return "";
    }

    return filename.substring(dotIndex).replaceAll("[^A-Za-z0-9.]", "");
  }
}

