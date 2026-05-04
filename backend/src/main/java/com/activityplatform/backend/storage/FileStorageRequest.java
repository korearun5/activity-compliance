package com.activityplatform.backend.storage;

import java.io.InputStream;
import java.util.UUID;

public record FileStorageRequest(
    UUID tenantId,
    String ownerType,
    UUID ownerId,
    String originalFilename,
    String contentType,
    long sizeBytes,
    InputStream inputStream
) {
}

