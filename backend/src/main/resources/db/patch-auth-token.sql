-- 若登录报 auth_token 表不存在，在 judgment_annotation 库执行本脚本
USE `judgment_annotation`;

CREATE TABLE IF NOT EXISTS `auth_token` (
    `token` VARCHAR(128) NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`token`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录令牌';
