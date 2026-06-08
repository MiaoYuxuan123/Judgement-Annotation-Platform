-- 标注结果
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
    `layout_json` JSON DEFAULT NULL COMMENT '论证图布局覆盖',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_annotation_task_doc_user_type` (`task_id`, `document_id`, `user_id`, `record_type`),
    KEY `idx_annotation_task` (`task_id`),
    KEY `idx_annotation_doc` (`document_id`),
    KEY `idx_annotation_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标注结果表';

-- 命题
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

-- 关系
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

-- 关系成员
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
