-- Production schema init (idempotent: safe to re-run, does not drop data)
-- Used by server ~/judgment-platform/init.sql on first MySQL container start.

CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '用户唯一标识',
    `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码（演示环境可为明文）',
    `real_name` VARCHAR(100) NOT NULL COMMENT '真实姓名',
    `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT 'admin / creator / user',
    `can_create_task` TINYINT NOT NULL DEFAULT 0 COMMENT '1=可创建任务',
    `last_seen` DATETIME DEFAULT NULL COMMENT '最后活跃时间',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '1=在线 0=离线',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `guide_version` (
    `id` INT UNSIGNED AUTO_INCREMENT,
    `version_name` VARCHAR(100) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指南版本';

CREATE TABLE IF NOT EXISTS `label_l1` (
    `id` INT UNSIGNED AUTO_INCREMENT,
    `guide_version_id` INT UNSIGNED NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `abbr` VARCHAR(10) NOT NULL,
    `description` TEXT DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_version_abbr` (`guide_version_id`, `abbr`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一级标签';

CREATE TABLE IF NOT EXISTS `label_l2` (
    `id` INT UNSIGNED AUTO_INCREMENT,
    `guide_version_id` INT UNSIGNED NOT NULL,
    `parent_l1_id` INT UNSIGNED NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `abbr` VARCHAR(20) NOT NULL,
    `description` TEXT DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_parent_l1` (`parent_l1_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二级标签';

CREATE TABLE IF NOT EXISTS `relation_type` (
    `id` INT UNSIGNED AUTO_INCREMENT,
    `guide_version_id` INT UNSIGNED NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `abbr` CHAR(1) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `is_binary` TINYINT NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_version_relation_abbr` (`guide_version_id`, `abbr`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关系类型';

CREATE TABLE IF NOT EXISTS `global_document` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT,
    `title` VARCHAR(255) NOT NULL,
    `file_name` VARCHAR(255) NOT NULL DEFAULT '',
    `file_type` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '文书类型，用户自定义，如 民事判决书',
    `extracted_text` LONGTEXT NOT NULL,
    `uploaded_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局文书库';

CREATE TABLE IF NOT EXISTS `task` (
    `id` INT AUTO_INCREMENT,
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `status` ENUM('标注中', '待裁定', '可导出') NOT NULL DEFAULT '标注中',
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

CREATE TABLE IF NOT EXISTS `annotation` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT UNSIGNED NOT NULL,
    `document_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `record_type` VARCHAR(20) NOT NULL DEFAULT 'ANNOTATION' COMMENT 'ANNOTATION=标注员提交, ARBITRATION=裁定结果',
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    `is_final` TINYINT NOT NULL DEFAULT 0,
    `guide_version_id` BIGINT UNSIGNED DEFAULT NULL,
    `guide_snapshot` JSON DEFAULT NULL,
    `submitted_at` DATETIME DEFAULT NULL,
    `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '裁定不予采纳理由',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_annotation_task_doc_user_type` (`task_id`, `document_id`, `user_id`, `record_type`),
    KEY `idx_annotation_task` (`task_id`),
    KEY `idx_annotation_doc` (`document_id`),
    KEY `idx_annotation_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标注结果表';

CREATE TABLE IF NOT EXISTS `proposition` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `annotation_id` BIGINT UNSIGNED NOT NULL,
    `display_id` VARCHAR(20) NOT NULL,
    `sequence_no` INT NOT NULL,
    `start_pos` INT NOT NULL,
    `end_pos` INT NOT NULL,
    `selected_text` TEXT NOT NULL,
    `label_l1` VARCHAR(10) NOT NULL,
    `label_l2` VARCHAR(30) DEFAULT NULL,
    `label_path` VARCHAR(50) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_prop_annotation_display` (`annotation_id`, `display_id`),
    UNIQUE KEY `uk_prop_annotation_sequence` (`annotation_id`, `sequence_no`),
    KEY `idx_prop_annotation_range` (`annotation_id`, `start_pos`, `end_pos`),
    CONSTRAINT `fk_prop_annotation`
        FOREIGN KEY (`annotation_id`) REFERENCES `annotation` (`id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='命题表';

CREATE TABLE IF NOT EXISTS `argument_relation` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `annotation_id` BIGINT UNSIGNED NOT NULL,
    `display_id` VARCHAR(20) NOT NULL,
    `sequence_no` INT NOT NULL,
    `relation_type` VARCHAR(10) NOT NULL,
    `expression` VARCHAR(500) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rel_annotation_display` (`annotation_id`, `display_id`),
    UNIQUE KEY `uk_rel_annotation_sequence` (`annotation_id`, `sequence_no`),
    KEY `idx_rel_annotation_type` (`annotation_id`, `relation_type`),
    CONSTRAINT `fk_rel_annotation`
        FOREIGN KEY (`annotation_id`) REFERENCES `annotation` (`id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关系表';

CREATE TABLE IF NOT EXISTS `relation_member` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `relation_id` BIGINT UNSIGNED NOT NULL,
    `member_type` VARCHAR(1) NOT NULL,
    `proposition_id` BIGINT UNSIGNED DEFAULT NULL,
    `child_relation_id` BIGINT UNSIGNED DEFAULT NULL,
    `member_role` VARCHAR(10) NOT NULL DEFAULT 'MEMBER',
    `member_order` INT NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_member_relation` (`relation_id`),
    KEY `idx_member_relation_order` (`relation_id`, `member_order`),
    KEY `idx_member_prop_ref` (`proposition_id`),
    KEY `idx_member_rel_ref` (`child_relation_id`),
    CONSTRAINT `fk_member_relation`
        FOREIGN KEY (`relation_id`) REFERENCES `argument_relation` (`id`)
        ON DELETE CASCADE,
    CONSTRAINT `fk_member_prop`
        FOREIGN KEY (`proposition_id`) REFERENCES `proposition` (`id`)
        ON DELETE CASCADE,
    CONSTRAINT `fk_member_child_relation`
        FOREIGN KEY (`child_relation_id`) REFERENCES `argument_relation` (`id`)
        ON DELETE CASCADE,
    CONSTRAINT `chk_member_ref` CHECK (
        (`member_type` = 'P' AND `proposition_id` IS NOT NULL AND `child_relation_id` IS NULL)
        OR
        (`member_type` = 'R' AND `proposition_id` IS NULL AND `child_relation_id` IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关系成员表';

CREATE TABLE IF NOT EXISTS `arbitration_snapshot` (
    `id` INT AUTO_INCREMENT,
    `task_id` INT NOT NULL,
    `task_document_id` INT NOT NULL,
    `arbitrator_id` BIGINT UNSIGNED NOT NULL,
    `adopted_from` VARCHAR(50) DEFAULT NULL,
    `final_result` TINYINT NOT NULL DEFAULT 0,
    `arbitrated_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_doc` (`task_id`, `task_document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='裁定快照';
