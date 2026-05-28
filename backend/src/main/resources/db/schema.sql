DROP TABLE IF EXISTS `arbitration_snapshot`;
DROP TABLE IF EXISTS `relation_member`;
DROP TABLE IF EXISTS `relation`;
DROP TABLE IF EXISTS `proposition`;
DROP TABLE IF EXISTS `annotation`;
DROP TABLE IF EXISTS `argument_relation`;
DROP TABLE IF EXISTS `task_document`;
DROP TABLE IF EXISTS `task_member`;
DROP TABLE IF EXISTS `task`;
DROP TABLE IF EXISTS `global_document`;
DROP TABLE IF EXISTS `relation_type`;
DROP TABLE IF EXISTS `label_l2`;
DROP TABLE IF EXISTS `label_l1`;
DROP TABLE IF EXISTS `guide_version`;
DROP TABLE IF EXISTS `auth_token`;
DROP TABLE IF EXISTS `sys_user`;

-- 用户
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

-- 登录 Token（Bearer）
CREATE TABLE IF NOT EXISTS `auth_token` (
    `token` VARCHAR(128) NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`token`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录令牌';

-- 指南版本
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

-- 全局文书库
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

-- 任务
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

-- 标注命题。与 PropositionMapper / PropositionPo 对齐。
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

-- 标注关系。与 RelationMapper / RelationPo 对齐。
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

-- 关系成员。当前后端按 source_type/source_id 保存来源成员，target 在 relation 表中。
CREATE TABLE IF NOT EXISTS `relation_member` (
    `id` INT AUTO_INCREMENT,
    `relation_id` INT NOT NULL,
    `source_type` CHAR(1) NOT NULL COMMENT 'P/R',
    `source_id` VARCHAR(20) NOT NULL COMMENT '来源 display_id',
    PRIMARY KEY (`id`),
    KEY `idx_relation_id` (`relation_id`),
    CONSTRAINT `fk_relation_member_relation` FOREIGN KEY (`relation_id`) REFERENCES `relation` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关系成员表';


-- 裁定元数据
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


CREATE TABLE IF NOT EXISTS annotation (
                                          id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                          task_id BIGINT UNSIGNED NOT NULL,
                                          document_id BIGINT UNSIGNED NOT NULL,
                                          user_id BIGINT UNSIGNED NOT NULL,
                                          status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    is_final TINYINT NOT NULL DEFAULT 0,
    guide_version_id BIGINT UNSIGNED DEFAULT NULL,
    guide_snapshot JSON DEFAULT NULL,
    submitted_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_annotation_task_doc_user (task_id, document_id, user_id),
    KEY idx_annotation_task (task_id),
    KEY idx_annotation_doc (document_id),
    KEY idx_annotation_user (user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标注结果表';

CREATE TABLE IF NOT EXISTS proposition (
                                           id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                           annotation_id BIGINT UNSIGNED NOT NULL,
                                           display_id VARCHAR(20) NOT NULL,
    sequence_no INT NOT NULL,
    start_pos INT NOT NULL,
    end_pos INT NOT NULL,
    selected_text TEXT NOT NULL,
    label_l1 VARCHAR(10) NOT NULL,
    label_l2 VARCHAR(30) DEFAULT NULL,
    label_path VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_prop_annotation_display (annotation_id, display_id),
    UNIQUE KEY uk_prop_annotation_sequence (annotation_id, sequence_no),
    KEY idx_prop_annotation_range (annotation_id, start_pos, end_pos),
    CONSTRAINT fk_prop_annotation FOREIGN KEY (annotation_id) REFERENCES annotation(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='命题表';

CREATE TABLE IF NOT EXISTS argument_relation (
                                                 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                                 annotation_id BIGINT UNSIGNED NOT NULL,
                                                 display_id VARCHAR(20) NOT NULL,
    sequence_no INT NOT NULL,
    relation_type VARCHAR(10) NOT NULL,
    expression VARCHAR(500) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rel_annotation_display (annotation_id, display_id),
    UNIQUE KEY uk_rel_annotation_sequence (annotation_id, sequence_no),
    KEY idx_rel_annotation_type (annotation_id, relation_type),
    CONSTRAINT fk_rel_annotation FOREIGN KEY (annotation_id) REFERENCES annotation(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关系表';

CREATE TABLE IF NOT EXISTS relation_member (
                                               id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                               relation_id BIGINT UNSIGNED NOT NULL,
                                               member_type VARCHAR(1) NOT NULL,
    proposition_id BIGINT UNSIGNED DEFAULT NULL,
    child_relation_id BIGINT UNSIGNED DEFAULT NULL,
    member_role VARCHAR(10) NOT NULL DEFAULT 'MEMBER',
    member_order INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_member_relation (relation_id),
    KEY idx_member_relation_order (relation_id, member_order),
    KEY idx_member_prop_ref (proposition_id),
    KEY idx_member_rel_ref (child_relation_id),
    CONSTRAINT fk_member_relation FOREIGN KEY (relation_id) REFERENCES argument_relation(id) ON DELETE CASCADE,
    CONSTRAINT fk_member_prop FOREIGN KEY (proposition_id) REFERENCES proposition(id) ON DELETE CASCADE,
    CONSTRAINT fk_member_child_relation FOREIGN KEY (child_relation_id) REFERENCES argument_relation(id) ON DELETE CASCADE,
    CONSTRAINT chk_member_ref CHECK (
(member_type = 'P' AND proposition_id IS NOT NULL AND child_relation_id IS NULL)
    OR
(member_type = 'R' AND proposition_id IS NULL AND child_relation_id IS NOT NULL)
    )
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关系成员表';

