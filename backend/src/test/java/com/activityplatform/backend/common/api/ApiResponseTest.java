package com.activityplatform.backend.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {
  @Test
  void successResponseUsesEnvelopeContract() {
    ApiResponse<String> response = ApiResponse.success("ready");

    assertThat(response.success()).isTrue();
    assertThat(response.data()).isEqualTo("ready");
    assertThat(response.error()).isNull();
  }
}

