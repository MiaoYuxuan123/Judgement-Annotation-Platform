CREATE TABLE IF NOT EXISTS `arbitration_snapshot` (
    `id` INT AUTO_INCREMENT,
    `task_id` INT NOT NULL,
    `task_document_id` INT NOT NULL,
    `arbitrator_id` BIGINT UNSIGNED NOT NULL,
    `adopted_from` VARCHAR(50) DEFAULT NULL,
    `based_on_annotator_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '手动裁定时所基于的标注员ID',
    `final_result` TINYINT NOT NULL DEFAULT 0,
    `arbitrated_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_doc` (`task_id`, `task_document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='裁定快照';
