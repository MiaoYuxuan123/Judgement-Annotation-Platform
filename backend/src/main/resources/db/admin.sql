DROP TABLE IF EXISTS `global_document`;
DROP TABLE IF EXISTS `relation_type`;
DROP TABLE IF EXISTS `label_l2`;
DROP TABLE IF EXISTS `label_l1`;
DROP TABLE IF EXISTS `guide_version`;
DROP TABLE IF EXISTS `sys_user`;

CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '用户唯一标识',
    `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码（演示环境可为明文）',
    `real_name` VARCHAR(100) NOT NULL COMMENT '真实姓名',
    `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT 'admin / creator / user',
    `can_create_task` TINYINT NOT NULL DEFAULT 0 COMMENT '1=可创建任务',
    `last_seen` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常 0=禁用',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `guide_version` (
    `id` INT UNSIGNED AUTO_INCREMENT,
    `version_name` VARCHAR(100) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `is_active` TINYINT NOT NULL DEFAULT 0 COMMENT '1=当前启用版本',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指南版本';

CREATE TABLE IF NOT EXISTS `label_l1` (
    `id` INT UNSIGNED AUTO_INCREMENT,
    `guide_version_id` INT UNSIGNED NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `abbr` VARCHAR(10) NOT NULL,
    `description` VARCHAR(200) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_version_abbr` (`guide_version_id`, `abbr`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `label_l2` (
    `id` INT UNSIGNED AUTO_INCREMENT,
    `guide_version_id` INT UNSIGNED NOT NULL,
    `parent_l1_id` INT UNSIGNED NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `abbr` VARCHAR(20) NOT NULL,
    `description` VARCHAR(200) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_parent_l1` (`parent_l1_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `relation_type` (
    `id` INT UNSIGNED AUTO_INCREMENT,
    `guide_version_id` INT UNSIGNED NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `abbr` CHAR(1) NOT NULL,
    `description` VARCHAR(200) DEFAULT NULL,
    `is_binary` TINYINT NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_version_relation_abbr` (`guide_version_id`, `abbr`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `global_document` (
    `id` BIGINT UNSIGNED AUTO_INCREMENT,
    `document_id` VARCHAR(50) NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `file_path` VARCHAR(255) NOT NULL DEFAULT '',
    `file_type` VARCHAR(10) NOT NULL,
    `extracted_text` LONGTEXT NOT NULL,
    `uploaded_by_id` BIGINT UNSIGNED DEFAULT NULL,
    `uploaded_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_document_id` (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
