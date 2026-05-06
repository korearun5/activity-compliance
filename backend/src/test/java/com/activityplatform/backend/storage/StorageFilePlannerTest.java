package com.activityplatform.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.activityplatform.backend.common.error.ApplicationException;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StorageFilePlannerTest {
  private final StorageProperties properties = new StorageProperties();
  private final StorageFilePlanner planner = new StorageFilePlanner(properties);

  @Test
  void testPlanNormalizesFilenameContentTypeAndStorageKey() {
    UUID tenantId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();

    StorageFilePlan plan = planner.plan(new FileStorageRequest(
        tenantId,
        "evidence",
        ownerId,
        "photos\\Proof.JPG",
        "image/jpeg; charset=binary",
        12,
        new ByteArrayInputStream("hello".getBytes())
    ));

    assertThat(plan.originalFilename()).isEqualTo("Proof.JPG");
    assertThat(plan.contentType()).isEqualTo("image/jpeg");
    assertThat(plan.sizeBytes()).isEqualTo(12);
    assertThat(plan.storageKey())
        .startsWith(tenantId + "/evidence/" + ownerId + "/")
        .endsWith(".jpg");
  }

  @Test
  void testRejectsMismatchedContentType() {
    assertThatThrownBy(() -> planner.plan(validRequest("proof.jpg", "image/png", 12)))
        .isInstanceOf(ApplicationException.class)
        .hasMessage("Uploaded file type does not match its extension.");
  }

  @Test
  void testRejectsUnsafeFilename() {
    assertThatThrownBy(() -> planner.plan(validRequest("../proof.jpg", "image/jpeg", 12)))
        .isInstanceOf(ApplicationException.class)
        .hasMessage("Uploaded filename is not allowed.");
  }

  @Test
  void testRejectsOversizedUpload() {
    properties.setMaxUploadBytes(4);

    assertThatThrownBy(() -> planner.plan(validRequest("proof.jpg", "image/jpeg", 12)))
        .isInstanceOf(ApplicationException.class)
        .hasMessage("Uploaded file size is not allowed.");
  }

  private FileStorageRequest validRequest(String filename, String contentType, long sizeBytes) {
    return new FileStorageRequest(
        UUID.randomUUID(),
        "evidence",
        UUID.randomUUID(),
        filename,
        contentType,
        sizeBytes,
        new ByteArrayInputStream("hello".getBytes())
    );
  }
}
