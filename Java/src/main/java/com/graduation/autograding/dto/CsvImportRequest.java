package com.graduation.autograding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CsvImportRequest(
        @NotBlank(message = "CSV 内容不能为空")
        @Size(max = 500000, message = "CSV 内容不能超过 500000 个字符（约 500KB）")
        String csvContent
) {
}
