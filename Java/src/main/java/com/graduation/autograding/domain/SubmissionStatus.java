package com.graduation.autograding.domain;

public enum SubmissionStatus {
    PENDING,
    ACCEPTED,
    PARTIAL_ACCEPTED,
    FAILED,
    COMPILE_ERROR,
    RUNTIME_ERROR,
    TIME_LIMIT_EXCEEDED
}
