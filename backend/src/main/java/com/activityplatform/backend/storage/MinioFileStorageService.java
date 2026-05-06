package com.activityplatform.backend.storage;

import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "minio")
public class MinioFileStorageService implements FileStorageService {
  private final StorageFilePlanner filePlanner;
  private final MinioClient minioClient;
  private final MinioStorageProperties properties;
  private volatile boolean bucketReady;

  public MinioFileStorageService(
      StorageFilePlanner filePlanner,
      MinioClient minioClient,
      MinioStorageProperties properties
  ) {
    this.filePlanner = filePlanner;
    this.minioClient = minioClient;
    this.properties = properties;
  }

  @Override
  public StoredFile store(FileStorageRequest request) {
    StorageFilePlan plan = filePlanner.plan(request);
    ensureBucketReady();

    try {
      minioClient.putObject(PutObjectArgs.builder()
          .bucket(properties.getBucket().trim())
          .object(plan.storageKey())
          .stream(request.inputStream(), plan.sizeBytes(), -1L)
          .contentType(plan.contentType())
          .build());
      return new StoredFile(
          plan.storageKey(),
          plan.originalFilename(),
          plan.contentType(),
          plan.sizeBytes()
      );
    } catch (Exception exception) {
      throw storageFailed();
    }
  }

  private void ensureBucketReady() {
    if (bucketReady || !properties.isCreateBucketIfMissing()) {
      return;
    }

    synchronized (this) {
      if (bucketReady) {
        return;
      }

      try {
        String bucket = properties.getBucket().trim();
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
            .bucket(bucket)
            .build());
        if (!exists) {
          minioClient.makeBucket(MakeBucketArgs.builder()
              .bucket(bucket)
              .build());
        }
        bucketReady = true;
      } catch (Exception exception) {
        throw storageFailed();
      }
    }
  }

  private ApplicationException storageFailed() {
    return new ApplicationException(
        ErrorCode.FILE_STORAGE_FAILED,
        "Unable to store file in MinIO.",
        HttpStatus.INTERNAL_SERVER_ERROR
    );
  }
}
