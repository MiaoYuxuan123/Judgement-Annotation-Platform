CREATE TABLE IF NOT EXISTS `proposition` (
    `id` INT AUTO_INCREMENT,
    `task_id` INT NOT NULL,
    `task_document_id` INT NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `display_id` VARCHAR(20) NOT NULL COMMENT 'P1 / P2',
    `content` TEXT NOT NULL,
    `start_offset` INT NOT NULL,
    `end_offset` INT NOT NULL,
    `label_l1` VARCHAR(10) NOT NULL,
    `label_l2` VARCHAR(30) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=草稿 1=已提交 2=已采纳',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_prop_scope_display` (`task_id`, `task_document_id`, `user_id`, `display_id`),
    KEY `idx_prop_scope` (`task_id`, `task_document_id`, `user_id`),
    KEY `idx_prop_range` (`task_document_id`, `start_offset`, `end_offset`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标注命题表';

CREATE TABLE IF NOT EXISTS `relation` (
    `id` INT AUTO_INCREMENT,
    `task_id` INT NOT NULL,
    `task_document_id` INT NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `display_id` VARCHAR(20) NOT NULL COMMENT 'R1 / R2',
    `type` CHAR(1) NOT NULL COMMENT 'S/A/J/M/I',
    `target_type` CHAR(1) DEFAULT NULL COMMENT 'P/R',
    `target_id` VARCHAR(20) DEFAULT NULL COMMENT '目标 display_id',
    `expression` VARCHAR(500) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=草稿 1=已提交 2=已采纳',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rel_scope_display` (`task_id`, `task_document_id`, `user_id`, `display_id`),
    KEY `idx_rel_scope` (`task_id`, `task_document_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标注关系表';

CREATE TABLE IF NOT EXISTS `relation_member` (
    `id` INT AUTO_INCREMENT,
    `relation_id` INT NOT NULL,
    `source_type` CHAR(1) NOT NULL COMMENT 'P/R',
    `source_id` VARCHAR(20) NOT NULL COMMENT '来源 display_id',
    PRIMARY KEY (`id`),
    KEY `idx_relation_id` (`relation_id`),
    CONSTRAINT `fk_relation_member_relation` FOREIGN KEY (`relation_id`) REFERENCES `relation` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关系成员表';
