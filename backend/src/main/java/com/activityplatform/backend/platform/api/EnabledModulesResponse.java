package com.activityplatform.backend.platform.api;

import java.util.List;

public record EnabledModulesResponse(
    List<String> modules
) {
}
