-- V4: Restructure courses table for admin-managed course classes (H2)
-- Remove unique constraint on code
-- Make teacher_id and code nullable

ALTER TABLE courses
    DROP CONSTRAINT IF EXISTS uk_courses_code;

ALTER TABLE courses
    ALTER COLUMN code VARCHAR(50) NULL;

ALTER TABLE courses
    ALTER COLUMN teacher_id BIGINT NULL;

ALTER TABLE courses
    ALTER COLUMN class_name VARCHAR(60) NULL;
