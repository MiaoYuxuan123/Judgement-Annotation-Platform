-- 已有库升级：区分标注员提交与裁定结果，避免同人兼任时互相覆盖
ALTER TABLE `annotation`
    ADD COLUMN `record_type` VARCHAR(20) NOT NULL DEFAULT 'ANNOTATION'
        COMMENT 'ANNOTATION=标注员提交, ARBITRATION=裁定结果'
        AFTER `user_id`;

ALTER TABLE `annotation` DROP INDEX `uk_annotation_task_doc_user`;
ALTER TABLE `annotation`
    ADD UNIQUE KEY `uk_annotation_task_doc_user_type` (`task_id`, `document_id`, `user_id`, `record_type`);
