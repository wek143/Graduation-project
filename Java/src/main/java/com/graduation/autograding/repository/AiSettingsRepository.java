package com.graduation.autograding.repository;

import com.graduation.autograding.domain.AiSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSettingsRepository extends JpaRepository<AiSettings, Long> {
}
