package com.graduation.autograding.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TestCaseRequest(
        @NotNull(message = "输入数据不能为空")
        @Size(max = 10000, message = "测试用例输入不能超过 10000 个字符")
        String inputData,
        @NotNull(message = "期望输出不能为空")
        @Size(max = 10000, message = "测试用例期望输出不能超过 10000 个字符")
        String expectedOutput
) {
}
