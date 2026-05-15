package com.graduation.autograding.service;

import com.graduation.autograding.dto.AiDiagnosisResponse;

public interface AiClient {

    AiDiagnosisResponse diagnose(Long submissionId, String submissionStatus, String prompt);
}
