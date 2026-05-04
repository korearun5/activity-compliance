package com.activityplatform.backend.storage;

public record StoredFile(
    String storageKey,
    String originalFilename,
    String contentType,
    long sizeBytes
) {
}

