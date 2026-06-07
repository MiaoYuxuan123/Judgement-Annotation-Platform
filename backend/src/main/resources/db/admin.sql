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
    `abbr` VARCHAR(20) NOT NULL,
    `description` VARCHAR(200) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_version_abbr` (`guide_version_id`, `abbr`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一级标签';

CREATE TABLE IF NOT EXISTS `label_l2` (
    `id` INT UNSIGNED AUTO_INCREMENT,
    `guide_version_id` INT UNSIGNED NOT NULL,
    `parent_l1_id` INT UNSIGNED NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `abbr` VARCHAR(20) NOT NULL,
    `description` VARCHAR(200) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_parent_l1` (`parent_l1_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二级标签';

CREATE TABLE IF NOT EXISTS `relation_type` (
    `id` INT UNSIGNED AUTO_INCREMENT,
    `guide_version_id` INT UNSIGNED NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `abbr` VARCHAR(20) NOT NULL,
    `description` VARCHAR(200) DEFAULT NULL,
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