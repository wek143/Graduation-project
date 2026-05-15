CREATE TABLE ai_settings (
    id BIGINT NOT NULL,
    enabled BIT NOT NULL,
    base_url VARCHAR(200) NOT NULL,
    api_key VARCHAR(200) NULL,
    model VARCHAR(100) NOT NULL,
    timeout_seconds INTEGER NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;
