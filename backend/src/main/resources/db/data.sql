USE `judgment_annotation`;

INSERT INTO sys_user (id, username, password_hash, real_name, role, can_create_task, status) VALUES
(1, 'admin', '123456', '系统管理员', 'admin', 0, 1),
(2, 'creator', '123456', '任务创建者', 'creator', 1, 1),
(3, 'annotator1', '123456', '参与者一', 'user', 0, 1),
(4, 'annotator2', '123456', '参与者二', 'user', 0, 1),
(5, 'reviewer', '123456', '参与者三', 'user', 0, 1);

INSERT INTO guide_version (id, version_name, description, is_active, created_at) VALUES
(1, 'V1.0 标准指南', '默认裁判文书论证标签体系', 1, '2026-05-01 00:00:00');

INSERT INTO label_l1 (guide_version_id, name, abbr, description) VALUES
(1, '个别事实判断', 'SF', '关于案件中个别对象的事实判断'),
(1, '一般事实判断', 'GF', '为规范适用提供经验或背景支撑'),
(1, '个别规范判断', 'SM', '体现法院对本案的规范性评价'),
(1, '一般规范判断', 'GM', '构成法律论证的规范基础');

INSERT INTO label_l2 (guide_version_id, parent_l1_id, name, abbr, description) VALUES
(1, 4, '法律条文', 'GM-L', '直接来源于成文法规范'),
(1, 4, '法律解释', 'GM-I', '对法律条文含义的解释'),
(1, 4, '合同及合同解释', 'GM-C', '来源于合同条款'),
(1, 4, '习惯与行业惯例', 'GM-U', '社会习惯、交易习惯'),
(1, 4, '道德与价值观念', 'GM-M', '价值判断、公序良俗'),
(1, 4, '其他规范判断', 'GM-O', '无法稳定归入上述类型');

INSERT INTO relation_type (guide_version_id, name, abbr, description, is_binary) VALUES
(1, '支持关系', 'S', '为另一命题成立提供理由', 1),
(1, '反对关系', 'A', '为另一命题不成立提供理由', 1),
(1, '组合关系', 'J', '多个命题共同构成理由', 0),
(1, '匹配关系', 'M', '规范要件与事实对应', 1),
(1, '同一关系', 'I', '语义上表达同一判断', 1);

INSERT INTO global_document (id, document_id, title, file_name, file_path, file_type, extracted_text, uploaded_by_id) VALUES
(101, 'W101', '合同纠纷一审判决书', '合同纠纷一审判决书.txt', '', 'txt',
 '本院认为，依法成立的合同，对当事人具有法律约束力。当事人应当按照约定全面履行自己的义务。被告未按期支付货款，已经构成违约。原告提交的送货单、对账单能够相互印证，本院予以采信。因此，被告应当向原告支付货款并承担逾期付款责任。', 1),
(102, 'W102', '劳动争议仲裁审查裁定', '劳动争议仲裁审查裁定.txt', '', 'txt',
 '本院认为，劳动者与用人单位建立劳动关系后，双方均应遵守劳动合同约定。现有考勤记录、工资流水可以证明申请人在案涉期间持续提供劳动。公司主张双方不存在劳动关系，但未提交充分反证，本院不予采纳。', 1),
(103, 'W103', '侵权责任纠纷判决书', '侵权责任纠纷判决书.txt', '', 'txt',
 '本院认为，行为人因过错侵害他人民事权益造成损害的，应当承担侵权责任。监控视频显示，被告车辆倒车时未尽到合理注意义务，与原告车辆发生碰撞。事故认定书载明被告承担全部责任，故原告要求赔偿维修费具有事实和法律依据。', 1);

INSERT INTO task (id, title, description, status, creator_id, guide_version_id, created_at, stage_changed_at) VALUES
(1001, '合同法标注演示任务', '标注合同纠纷裁判理由中的事实、规范与关系', '标注中', 2, 1, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
(1002, '侵权责任裁定样例', '展示裁定与导出流程', '可导出', 2, 1, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY),
(1003, '劳动争议裁定演示', '演示裁定界面完整路径', '待裁定', 2, 1, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY);

INSERT INTO task_member (task_id, user_id, role_in_task) VALUES
(1001, 3, '标注员'), (1001, 4, '标注员'), (1001, 5, '裁定者'),
(1002, 3, '标注员'), (1002, 4, '标注员'), (1002, 5, '裁定者'),
(1003, 3, '标注员'), (1003, 4, '标注员'), (1003, 5, '裁定者');

INSERT INTO task_document (task_id, source_type, global_doc_id, file_name, extracted_text, status) VALUES
(1001, 'GLOBAL', 101, '合同纠纷一审判决书.txt',
 (SELECT extracted_text FROM global_document WHERE id = 101), '待标注'),
(1001, 'GLOBAL', 102, '劳动争议仲裁审查裁定.txt',
 (SELECT extracted_text FROM global_document WHERE id = 102), '待标注'),
(1002, 'GLOBAL', 103, '侵权责任纠纷判决书.txt',
 (SELECT extracted_text FROM global_document WHERE id = 103), '已裁定'),
(1003, 'GLOBAL', 102, '劳动争议仲裁审查裁定.txt',
 (SELECT extracted_text FROM global_document WHERE id = 102), '待标注');

-- =============================================================================
-- 演示标注 / 裁定数据（原 DemoDataStore 内存种子，接入 MySQL 后写在此处）
-- 若库中已有任务 1001–1003，可先执行：DELETE FROM proposition WHERE task_id IN (1001,1002,1003);
-- =============================================================================

SET @td_1001_101 := (SELECT id FROM task_document WHERE task_id = 1001 AND global_doc_id = 101 LIMIT 1);
SET @td_1001_102 := (SELECT id FROM task_document WHERE task_id = 1001 AND global_doc_id = 102 LIMIT 1);
SET @td_1002_103 := (SELECT id FROM task_document WHERE task_id = 1002 AND global_doc_id = 103 LIMIT 1);
SET @td_1003_102 := (SELECT id FROM task_document WHERE task_id = 1003 AND global_doc_id = 102 LIMIT 1);

-- ---------- 任务 1001 / 文书 101 / 标注员 3（annotator1）----------
INSERT INTO proposition (task_id, task_document_id, user_id, display_id, content, start_offset, end_offset, label_l1, label_l2, status) VALUES
(1001, @td_1001_101, 3, 'P1', '依法成立的合同', 5, 12, 'GM', 'GM-L', 1),
(1001, @td_1001_101, 3, 'P2', '对当事人具有法律约束力', 13, 24, 'GM', 'GM-I', 1),
(1001, @td_1001_101, 3, 'P3', '被告未按期支付货款', 44, 53, 'SF', NULL, 1),
(1001, @td_1001_101, 3, 'P4', '构成违约', 56, 60, 'SM', NULL, 1),
(1001, @td_1001_101, 3, 'P5', '支付货款并承担逾期付款责任', 97, 110, 'SF', NULL, 1);

INSERT INTO relation (task_id, task_document_id, user_id, display_id, type, target_type, target_id, expression, status) VALUES
(1001, @td_1001_101, 3, 'R1', 'S', 'P', 'P2', 'S(P1,P2)', 1);
SET @r := LAST_INSERT_ID();
INSERT INTO relation_member (relation_id, source_type, source_id) VALUES (@r, 'P', 'P1');

INSERT INTO relation (task_id, task_document_id, user_id, display_id, type, target_type, target_id, expression, status) VALUES
(1001, @td_1001_101, 3, 'R2', 'S', 'P', 'P4', 'S(P3,P4)', 1);
SET @r := LAST_INSERT_ID();
INSERT INTO relation_member (relation_id, source_type, source_id) VALUES (@r, 'P', 'P3');

INSERT INTO relation (task_id, task_document_id, user_id, display_id, type, target_type, target_id, expression, status) VALUES
(1001, @td_1001_101, 3, 'R3', 'A', 'P', 'P4', 'A(P5,P4)', 1);
SET @r := LAST_INSERT_ID();
INSERT INTO relation_member (relation_id, source_type, source_id) VALUES (@r, 'P', 'P5');

-- ---------- 任务 1001 / 文书 101 / 标注员 4（annotator2，部分命题）----------
INSERT INTO proposition (task_id, task_document_id, user_id, display_id, content, start_offset, end_offset, label_l1, label_l2, status) VALUES
(1001, @td_1001_101, 4, 'P1', '依法成立的合同', 5, 12, 'GM', 'GM-L', 1),
(1001, @td_1001_101, 4, 'P3', '被告未按期支付货款', 44, 53, 'SF', NULL, 1),
(1001, @td_1001_101, 4, 'P4', '构成违约', 56, 60, 'SM', NULL, 1);

INSERT INTO relation (task_id, task_document_id, user_id, display_id, type, target_type, target_id, expression, status) VALUES
(1001, @td_1001_101, 4, 'R1', 'S', 'P', 'P4', 'S(P3,P4)', 1);
SET @r := LAST_INSERT_ID();
INSERT INTO relation_member (relation_id, source_type, source_id) VALUES (@r, 'P', 'P3');

-- ---------- 任务 1002 / 文书 103 / 裁定结果（reviewer，五种关系演示）----------
INSERT INTO proposition (task_id, task_document_id, user_id, display_id, content, start_offset, end_offset, label_l1, label_l2, status) VALUES
(1002, @td_1002_103, 5, 'P1', '行为人因过错侵害他人民事权益', 5, 19, 'SF', NULL, 2),
(1002, @td_1002_103, 5, 'P2', '应当承担侵权责任', 25, 33, 'GM', 'GM-L', 2),
(1002, @td_1002_103, 5, 'P3', '被告车辆倒车时未尽到合理注意义务', 41, 57, 'SF', NULL, 2),
(1002, @td_1002_103, 5, 'P4', '与原告车辆发生碰撞', 58, 67, 'SF', NULL, 2),
(1002, @td_1002_103, 5, 'P5', '承担全部责任', 77, 83, 'GM', 'GM-L', 2);

INSERT INTO relation (task_id, task_document_id, user_id, display_id, type, target_type, target_id, expression, status) VALUES
(1002, @td_1002_103, 5, 'R1', 'S', 'P', 'P2', 'S(P1,P2)', 2);
SET @r := LAST_INSERT_ID();
INSERT INTO relation_member (relation_id, source_type, source_id) VALUES (@r, 'P', 'P1');

INSERT INTO relation (task_id, task_document_id, user_id, display_id, type, target_type, target_id, expression, status) VALUES
(1002, @td_1002_103, 5, 'R2', 'A', 'P', 'P2', 'A(P3,P2)', 2);
SET @r := LAST_INSERT_ID();
INSERT INTO relation_member (relation_id, source_type, source_id) VALUES (@r, 'P', 'P3');

INSERT INTO relation (task_id, task_document_id, user_id, display_id, type, target_type, target_id, expression, status) VALUES
(1002, @td_1002_103, 5, 'R3', 'J', 'P', 'P4', 'J(P1,P4)', 2);
SET @r := LAST_INSERT_ID();
INSERT INTO relation_member (relation_id, source_type, source_id) VALUES (@r, 'P', 'P1');

INSERT INTO relation (task_id, task_document_id, user_id, display_id, type, target_type, target_id, expression, status) VALUES
(1002, @td_1002_103, 5, 'R4', 'M', 'P', 'P4', 'M(P2,P4)', 2);
SET @r := LAST_INSERT_ID();
INSERT INTO relation_member (relation_id, source_type, source_id) VALUES (@r, 'P', 'P2');

INSERT INTO relation (task_id, task_document_id, user_id, display_id, type, target_type, target_id, expression, status) VALUES
(1002, @td_1002_103, 5, 'R5', 'I', 'P', 'P5', 'I(P1,P5)', 2);
SET @r := LAST_INSERT_ID();
INSERT INTO relation_member (relation_id, source_type, source_id) VALUES (@r, 'P', 'P1');

INSERT INTO arbitration_snapshot (task_id, task_document_id, arbitrator_id, adopted_from, final_result, arbitrated_at) VALUES
(1002, @td_1002_103, 5, 'MANUAL', 1, NOW() - INTERVAL 1 DAY);

-- ---------- 任务 1003 / 文书 102 / 标注员 3（待裁定对比）----------
INSERT INTO proposition (task_id, task_document_id, user_id, display_id, content, start_offset, end_offset, label_l1, label_l2, status) VALUES
(1003, @td_1003_102, 3, 'P1', '劳动者与用人单位建立劳动关系后，双方均应遵守劳动合同约定', 5, 33, 'GM', 'GM-L', 1),
(1003, @td_1003_102, 3, 'P2', '现有考勤记录、工资流水可以证明申请人在案涉期间持续提供劳动', 34, 62, 'SF', NULL, 1),
(1003, @td_1003_102, 3, 'P3', '公司主张双方不存在劳动关系，但未提交充分反证', 63, 83, 'SF', NULL, 1),
(1003, @td_1003_102, 3, 'P4', '本院不予采纳', 84, 89, 'SM', NULL, 1);

INSERT INTO relation (task_id, task_document_id, user_id, display_id, type, target_type, target_id, expression, status) VALUES
(1003, @td_1003_102, 3, 'R1', 'S', 'P', 'P4', 'S(P1,P4)', 1);
SET @r := LAST_INSERT_ID();
INSERT INTO relation_member (relation_id, source_type, source_id) VALUES (@r, 'P', 'P1');

INSERT INTO relation (task_id, task_document_id, user_id, display_id, type, target_type, target_id, expression, status) VALUES
(1003, @td_1003_102, 3, 'R2', 'S', 'P', 'P3', 'S(P2,P3)', 1);
SET @r := LAST_INSERT_ID();
INSERT INTO relation_member (relation_id, source_type, source_id) VALUES (@r, 'P', 'P2');

INSERT INTO relation (task_id, task_document_id, user_id, display_id, type, target_type, target_id, expression, status) VALUES
(1003, @td_1003_102, 3, 'R3', 'S', 'P', 'P4', 'S(P3,P4)', 1);
SET @r := LAST_INSERT_ID();
INSERT INTO relation_member (relation_id, source_type, source_id) VALUES (@r, 'P', 'P3');

-- ---------- 任务 1003 / 文书 102 / 标注员 4（与 3 有差异，用于裁定对比）----------
INSERT INTO proposition (task_id, task_document_id, user_id, display_id, content, start_offset, end_offset, label_l1, label_l2, status) VALUES
(1003, @td_1003_102, 4, 'P1', '劳动者与用人单位建立劳动关系后，双方均应遵守劳动合同约定', 5, 33, 'GF', NULL, 1),
(1003, @td_1003_102, 4, 'P2', '现有考勤记录、工资流水可以证明申请人在案涉期间持续提供劳动', 34, 62, 'SF', NULL, 1),
(1003, @td_1003_102, 4, 'P3', '公司主张双方不存在劳动关系，但未提交充分反证', 63, 83, 'SF', NULL, 1),
(1003, @td_1003_102, 4, 'P4', '本院不予采纳', 84, 89, 'SM', NULL, 1);

INSERT INTO relation (task_id, task_document_id, user_id, display_id, type, target_type, target_id, expression, status) VALUES
(1003, @td_1003_102, 4, 'R1', 'S', 'P', 'P4', 'S(P2,P4)', 1);
SET @r := LAST_INSERT_ID();
INSERT INTO relation_member (relation_id, source_type, source_id) VALUES (@r, 'P', 'P2');

INSERT INTO relation (task_id, task_document_id, user_id, display_id, type, target_type, target_id, expression, status) VALUES
(1003, @td_1003_102, 4, 'R2', 'A', 'P', 'P4', 'A(P3,P4)', 1);
SET @r := LAST_INSERT_ID();
INSERT INTO relation_member (relation_id, source_type, source_id) VALUES (@r, 'P', 'P3');
