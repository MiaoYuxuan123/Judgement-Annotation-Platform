INSERT INTO sys_user (id, username, password_hash, real_name, role, can_create_task, status)
VALUES
    (1, 'admin', '123456', '超级管理员', 'ADMIN', FALSE, 0),
    (2, 'creator', '123456', '任务创建者', 'USER', TRUE, 0),
    (3, 'annotator1', '123456', '标注员一', 'USER', FALSE, 0),
    (4, 'annotator2', '123456', '标注员二', 'USER', FALSE, 0),
    (5, 'reviewer', '123456', '裁定者', 'USER', FALSE, 0);

INSERT INTO guide_version (id, version_name, description, is_active, created_by)
VALUES (1, '中文裁判文书标注指南', '测试指南版本', TRUE, 1);

INSERT INTO label_l1 (id, guide_version_id, name, abbr, description)
VALUES
    (1, 1, '总命题', 'GM', '总命题标签'),
    (2, 1, '子命题', 'SM', '子命题标签'),
    (3, 1, '事实', 'SF', '事实标签'),
    (4, 1, '规则', 'GF', '规则标签');

INSERT INTO label_l2 (guide_version_id, parent_l1_id, name, abbr, description)
VALUES
    (1, 1, '法律规则', 'GM-L', '法律规则'),
    (1, 1, '事实认定', 'GM-I', '事实认定');

INSERT INTO relation_type (guide_version_id, name, abbr, description, is_binary)
VALUES
    (1, '支持关系', 'S', '支持关系', 1),
    (1, '组合关系', 'J', '组合关系', 0),
    (1, '匹配关系', 'M', '匹配关系', 1),
    (1, '反对关系', 'A', '反对关系', 1),
    (1, '同一关系', 'I', '同一关系', 0);

INSERT INTO global_document (id, title, file_name, file_type, extracted_text)
VALUES
    (101, '买卖合同纠纷判决书', 'contract.txt', 'TXT', '依法成立的合同，自成立时生效。被告未按期付款，应承担违约责任。');

INSERT INTO task (id, title, description, status, creator_id, guide_version_id)
VALUES
    (1001, '合同纠纷标注任务', '用于测试的完整标注流程', '标注中', 2, 1);

INSERT INTO task_member (task_id, user_id, role_in_task)
VALUES
    (1001, 3, '标注员'),
    (1001, 4, '标注员'),
    (1001, 5, '裁定者');

INSERT INTO task_document (id, task_id, source_type, global_doc_id, file_name, extracted_text, status)
VALUES
    (1, 1001, 'GLOBAL', 101, 'contract.txt', NULL, '标注中');
