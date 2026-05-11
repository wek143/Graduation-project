package com.graduation.autograding.service;

import com.graduation.autograding.domain.JudgeCaseResult;
import com.graduation.autograding.domain.Submission;
import com.graduation.autograding.domain.SubmissionStatus;
import com.graduation.autograding.judge.JavaJudgeService;
import com.graduation.autograding.judge.JudgeOutcome;
import com.graduation.autograding.repository.SubmissionRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionJudgeQueueService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionJudgeQueueService.class);

    private final SubmissionRepository submissionRepository;
    private final JavaJudgeService javaJudgeService;
    private final AuditLogService auditLogService;

    public SubmissionJudgeQueueService(SubmissionRepository submissionRepository,
                                       JavaJudgeService javaJudgeService,
                                       AuditLogService auditLogService) {
        this.submissionRepository = submissionRepository;
        this.javaJudgeService = javaJudgeService;
        this.auditLogService = auditLogService;
    }

    @Async("judgeTaskExecutor")
    @Transactional
    public void judgeSubmissionAsync(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("提交记录不存在。"));
        try {
            JudgeOutcome outcome = javaJudgeService.judge(
                    submission.getSourceCode(),
                    submission.getAssignment().getTestCases()
            );

            submission.setClassName(outcome.className());
            submission.setStatus(outcome.status());
            submission.setScore(outcome.score());
            submission.setCompileMessage(outcome.compileMessage());
            submission.setRuntimeMessage(outcome.runtimeMessage());
            submission.clearCaseResults();

            List<JudgeCaseResult> caseResults = outcome.caseResults();
            for (JudgeCaseResult caseResult : caseResults) {
                submission.addCaseResult(caseResult);
            }
            submissionRepository.save(submission);
            auditLogService.record(null, "SUBMISSION_JUDGED", "SUBMISSION", String.valueOf(submissionId),
                    "提交 " + submissionId + " 判题完成，状态：" + outcome.status().name());
        } catch (Exception ex) {
            log.error("判题引擎异常，提交 ID={}，错误：{}", submissionId, ex.getMessage(), ex);
            // 将提交状态标记为 FAILED，避免永久卡在 PENDING
            submission.setStatus(SubmissionStatus.FAILED);
            submission.setRuntimeMessage("判题引擎内部错误，请联系管理员。");
            submissionRepository.save(submission);
        }
    }
}
