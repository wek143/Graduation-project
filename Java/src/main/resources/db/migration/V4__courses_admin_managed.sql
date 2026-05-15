-- V4: Restructure courses table for admin-managed course classes
-- Remove unique constraint on code (code is now optional)
-- Make teacher_id nullable (courses start without a teacher)
-- Make code nullable

ALTER TABLE courses
    DROP CONSTRAINT uk_courses_code;

ALTER TABLE courses
    MODIFY COLUMN code VARCHAR(50) NULL,
    MODIFY COLUMN teacher_id BIGINT NULL,
    MODIFY COLUMN class_name VARCHAR(60) NULL;
