package com.activityplatform.backend.storage;

import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {
  private final StorageFilePlanner filePlanner;
  private final LocalFileStorageProperties properties;

  public LocalFileStorageService(
      StorageFilePlanner filePlanner,
      LocalFileStorageProperties properties
  ) {
    this.filePlanner = filePlanner;
    this.properties = properties;
  }

  @Override
  public StoredFile store(FileStorageRequest request) {
    StorageFilePlan plan = filePlanner.plan(request);
    Path root = Path.of(properties.getRootPath()).toAbsolutePath().normalize();
    Path target = root.resolve(plan.storageKey()).normalize();

    if (!target.startsWith(root)) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_FAILED,
          "Resolved storage path is not allowed.",
          HttpStatus.BAD_REQUEST
      );
    }

    try {
      Files.createDirectories(target.getParent());
      Files.copy(request.inputStream(), target, StandardCopyOption.REPLACE_EXISTING);
      return new StoredFile(
          plan.storageKey(),
          plan.originalFilename(),
          plan.contentType(),
          plan.sizeBytes()
      );
    } catch (IOException exception) {
      throw new ApplicationException(
          ErrorCode.FILE_STORAGE_FAILED,
          "Unable to store uploaded file.",
          HttpStatus.INTERNAL_SERVER_ERROR
      );
    }
  }
}
