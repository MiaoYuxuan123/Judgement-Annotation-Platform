DROP TABLE IF EXISTS `task_document`;
DROP TABLE IF EXISTS `task_member`;
DROP TABLE IF EXISTS `task`;

CREATE TABLE IF NOT EXISTS `task` (
    `id` INT AUTO_INCREMENT,
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT '标注中',
    `creator_id` BIGINT UNSIGNED NOT NULL,
    `guide_version_id` INT UNSIGNED DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `stage_changed_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_creator_id` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `task_member` (
    `id` INT AUTO_INCREMENT,
    `task_id` INT NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `role_in_task` VARCHAR(20) NOT NULL COMMENT '标注员 / 裁定者',
    `assigned_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_user_role` (`task_id`, `user_id`, `role_in_task`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `task_document` (
    `id` INT AUTO_INCREMENT,
    `task_id` INT NOT NULL,
    `source_type` VARCHAR(10) NOT NULL DEFAULT 'GLOBAL',
    `global_doc_id` BIGINT UNSIGNED DEFAULT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `file_path` VARCHAR(255) DEFAULT NULL,
    `extracted_text` LONGTEXT NOT NULL,
    `uploaded_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `status` VARCHAR(20) NOT NULL DEFAULT '待标注',
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
