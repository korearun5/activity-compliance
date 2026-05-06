package com.activityplatform.backend.common.api;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
    List<T> content,
    PageMeta page
) {
  public static <T> PageResponse<T> from(Page<T> source) {
    return new PageResponse<>(
        source.getContent(),
        new PageMeta(
            source.getNumber(),
            source.getSize(),
            source.getTotalElements(),
            source.getTotalPages()
        )
    );
  }
}
