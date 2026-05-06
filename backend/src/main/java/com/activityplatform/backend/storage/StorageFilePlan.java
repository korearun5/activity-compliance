package com.activityplatform.backend.storage;

public record StorageFilePlan(
    String storageKey,
    String originalFilename,
    String contentType,
    long sizeBytes
) {
}
