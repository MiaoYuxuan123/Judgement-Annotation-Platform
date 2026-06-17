CREATE TABLE IF NOT EXISTS `task` (
    `id` INT AUTO_INCREMENT,
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `status` ENUM('标注中', '待裁定', '可导出') NOT NULL DEFAULT '标注中',
    `creator_id` BIGINT UNSIGNED NOT NULL,
    `guide_version_id` INT UNSIGNED DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `stage_changed_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deadline` DATETIME DEFAULT NULL COMMENT '任务截止时间',
    PRIMARY KEY (`id`),
    KEY `idx_creator_id` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `task_member` (
    `id` INT AUTO_INCREMENT,
    `task_id` INT NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `role_in_task` ENUM('标注员', '裁定者') NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_user_role` (`task_id`, `user_id`, `role_in_task`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `task_document` (
    `id` INT AUTO_INCREMENT,
    `task_id` INT NOT NULL,
    `source_type` ENUM('UPLOAD', 'RECREATE', 'GLOBAL') NOT NULL DEFAULT 'GLOBAL',
    `global_doc_id` BIGINT UNSIGNED DEFAULT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `file_path` VARCHAR(500) DEFAULT NULL,
    `extracted_text` LONGTEXT DEFAULT NULL,
    `uploaded_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `status` VARCHAR(20) NOT NULL DEFAULT '标注中' COMMENT '文书阶段：标注中/待裁定/可导出',
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_global_doc_id` (`global_doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `message` (
    `id`               BIGINT AUTO_INCREMENT,
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '接收者',
    `type`             VARCHAR(20) NOT NULL COMMENT 'TASK / ARBITRATION / SUBMISSION',
    `title`            VARCHAR(200) NOT NULL COMMENT '消息标题',
    `content`          TEXT COMMENT '消息正文',
    `task_id`          INT COMMENT '关联任务',
    `task_document_id` INT COMMENT '关联文书（可空）',
    `data_id`          INT COMMENT '文书API ID（用于跳转）',
    `is_read`          TINYINT NOT NULL DEFAULT 0 COMMENT '0=未读 1=已读',
    `created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_read` (`user_id`, `is_read`),
    INDEX `idx_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';
