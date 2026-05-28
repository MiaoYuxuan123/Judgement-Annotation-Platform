-- 在 DataGrip / MySQL Workbench 中选中当前项目库后执行本脚本
-- 用于补齐「你们原 DDL」与「当前后端代码」之间的字段差异
-- 若某条报 Duplicate column name，说明该列已存在，可忽略继续执行后面的语句

-- ========== proposition 表 ==========
ALTER TABLE `proposition`
    ADD COLUMN `task_document_id` INT NOT NULL DEFAULT 0 COMMENT '任务内文书条目ID' AFTER `task_id`;

ALTER TABLE `proposition`
    ADD INDEX `idx_task_doc_user` (`task_id`, `task_document_id`, `user_id`);

-- ========== relation 表 ==========
ALTER TABLE `relation`
    ADD COLUMN `task_id` INT NOT NULL DEFAULT 0 AFTER `id`;

ALTER TABLE `relation`
    ADD COLUMN `task_document_id` INT NOT NULL DEFAULT 0 AFTER `task_id`;

ALTER TABLE `relation`
    ADD COLUMN `user_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER `task_document_id`;

ALTER TABLE `relation`
    ADD INDEX `idx_task_doc_user` (`task_id`, `task_document_id`, `user_id`);

-- ========== guide_version（若无 is_active）==========
ALTER TABLE `guide_version`
    ADD COLUMN `is_active` TINYINT NOT NULL DEFAULT 0 COMMENT '1=当前启用版本' AFTER `description`;

UPDATE `guide_version` SET `is_active` = 1 WHERE `id` = 1;

-- ========== auth_token（登录用，若已存在会报错可忽略）==========
CREATE TABLE IF NOT EXISTS `auth_token` (
    `token` VARCHAR(128) NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`token`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录令牌';

-- ========== arbitration_snapshot（裁定用，若已存在会跳过）==========
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
