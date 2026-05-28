-- 仅恢复演示标注数据（用户/任务/文书已存在时执行）
-- 会清空任务 1001–1003 的命题、关系、裁定快照后重新写入（与 data.sql 末尾一致）

USE `judgment_annotation`;

DELETE rm FROM relation_member rm
INNER JOIN relation r ON rm.relation_id = r.id
WHERE r.task_id IN (1001, 1002, 1003);

DELETE FROM relation WHERE task_id IN (1001, 1002, 1003);
DELETE FROM proposition WHERE task_id IN (1001, 1002, 1003);
DELETE FROM arbitration_snapshot WHERE task_id IN (1001, 1002, 1003);

SET @td_1001_101 := (SELECT id FROM task_document WHERE task_id = 1001 AND global_doc_id = 101 LIMIT 1);
SET @td_1001_102 := (SELECT id FROM task_document WHERE task_id = 1001 AND global_doc_id = 102 LIMIT 1);
SET @td_1002_103 := (SELECT id FROM task_document WHERE task_id = 1002 AND global_doc_id = 103 LIMIT 1);
SET @td_1003_102 := (SELECT id FROM task_document WHERE task_id = 1003 AND global_doc_id = 102 LIMIT 1);

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

INSERT INTO proposition (task_id, task_document_id, user_id, display_id, content, start_offset, end_offset, label_l1, label_l2, status) VALUES
(1001, @td_1001_101, 4, 'P1', '依法成立的合同', 5, 12, 'GM', 'GM-L', 1),
(1001, @td_1001_101, 4, 'P3', '被告未按期支付货款', 44, 53, 'SF', NULL, 1),
(1001, @td_1001_101, 4, 'P4', '构成违约', 56, 60, 'SM', NULL, 1);

INSERT INTO relation (task_id, task_document_id, user_id, display_id, type, target_type, target_id, expression, status) VALUES
(1001, @td_1001_101, 4, 'R1', 'S', 'P', 'P4', 'S(P3,P4)', 1);
SET @r := LAST_INSERT_ID();
INSERT INTO relation_member (relation_id, source_type, source_id) VALUES (@r, 'P', 'P3');

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
