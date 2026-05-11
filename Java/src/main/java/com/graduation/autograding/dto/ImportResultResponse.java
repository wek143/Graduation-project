package com.graduation.autograding.dto;

import java.util.List;

public record ImportResultResponse(
        String importType,
        int createdCount,
        int skippedCount,
        List<String> details
) {
}
