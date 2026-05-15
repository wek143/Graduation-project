-- V1: Initial schema (H2 compatible, idempotent)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    full_name VARCHAR(80) NULL,
    class_name VARCHAR(60) NULL,
    role VARCHAR(20) NOT NULL,
    active BIT NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS courses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NULL,
    name VARCHAR(100) NOT NULL,
    term VARCHAR(40) NOT NULL,
    class_name VARCHAR(60) NULL,
    active BIT NOT NULL DEFAULT TRUE,
    teacher_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_courses_teacher FOREIGN KEY (teacher_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    deadline DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    grading_policy VARCHAR(20) NULL,
    max_submissions INTEGER NULL,
    late_submission_allowed BIT NULL,
    teacher_id BIGINT NOT NULL,
    course_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_assignments_teacher FOREIGN KEY (teacher_id) REFERENCES users (id),
    CONSTRAINT fk_assignments_course FOREIGN KEY (course_id) REFERENCES courses (id)
);

CREATE TABLE IF NOT EXISTS test_cases (
    id BIGINT NOT NULL AUTO_INCREMENT,
    input_data LONGTEXT NOT NULL,
    expected_output LONGTEXT NOT NULL,
    assignment_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_test_cases_assignment FOREIGN KEY (assignment_id) REFERENCES assignments (id)
);

CREATE TABLE IF NOT EXISTS submissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    source_code LONGTEXT NOT NULL,
    class_name VARCHAR(100) NOT NULL,
    status VARCHAR(40) NOT NULL,
    score INTEGER NOT NULL,
    compile_message LONGTEXT NULL,
    runtime_message LONGTEXT NULL,
    submitted_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_submissions_assignment FOREIGN KEY (assignment_id) REFERENCES assignments (id),
    CONSTRAINT fk_submissions_student FOREIGN KEY (student_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS judge_case_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    case_order INTEGER NOT NULL,
    input_data LONGTEXT NOT NULL,
    expected_output LONGTEXT NOT NULL,
    actual_output LONGTEXT NOT NULL,
    passed BIT NOT NULL,
    error_message LONGTEXT NULL,
    submission_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_judge_case_results_submission FOREIGN KEY (submission_id) REFERENCES submissions (id)
);

CREATE TABLE IF NOT EXISTS course_enrollments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    enrolled_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_course_enrollments_course_student UNIQUE (course_id, student_id),
    CONSTRAINT fk_course_enrollments_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT fk_course_enrollments_student FOREIGN KEY (student_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_id BIGINT NULL,
    actor_username VARCHAR(50) NOT NULL,
    action VARCHAR(60) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id VARCHAR(60) NULL,
    summary VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS auth_tokens (
    token VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    issued_at DATETIME(6) NOT NULL,
    PRIMARY KEY (token),
    CONSTRAINT fk_auth_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_assignments_teacher_id ON assignments (teacher_id);
CREATE INDEX IF NOT EXISTS idx_assignments_course_id ON assignments (course_id);
CREATE INDEX IF NOT EXISTS idx_submissions_assignment_id ON submissions (assignment_id);
CREATE INDEX IF NOT EXISTS idx_submissions_student_id ON submissions (student_id);
CREATE INDEX IF NOT EXISTS idx_submissions_assignment_student ON submissions (assignment_id, student_id);
CREATE INDEX IF NOT EXISTS idx_course_enrollments_course_id ON course_enrollments (course_id);
CREATE INDEX IF NOT EXISTS idx_course_enrollments_student_id ON course_enrollments (student_id);
CREATE INDEX IF NOT EXISTS idx_auth_tokens_user_id ON auth_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_auth_tokens_expires_at ON auth_tokens (expires_at);
