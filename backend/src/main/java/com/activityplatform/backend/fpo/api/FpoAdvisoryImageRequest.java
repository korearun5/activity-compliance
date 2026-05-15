package com.activityplatform.backend.fpo.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FpoAdvisoryImageRequest(
    @NotBlank
    @Size(max = 2048)
    String imageUrl,
    @Size(max = 500)
    String storageKey,
    @Size(max = 255)
    String originalFilename,
    @Size(max = 120)
    String contentType
) {
}
