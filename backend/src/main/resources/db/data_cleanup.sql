-- 清理演示/测试数据（可重复执行）
-- 1. 删除名为 test / test2 的任务
-- 2. 演示任务 1001/1002/1003 内重复文书（保留 task_document.id 最小的一条）
-- 3. 其余任务按 global_doc_id 去重

-- ── 演示任务 1001/1002/1003：删除重复文书的关联数据（保留 id 较小的一条）──

DELETE rm FROM relation_member rm
INNER JOIN argument_relation ar ON rm.relation_id = ar.id
INNER JOIN annotation ann ON ar.annotation_id = ann.id
INNER JOIN task_document td ON ann.document_id = td.id
INNER JOIN task_document td_keep ON td.task_id = td_keep.task_id
  AND td.task_id IN (1001, 1002, 1003)
  AND (
    (td.global_doc_id IS NOT NULL AND td.global_doc_id = td_keep.global_doc_id)
    OR td.file_name = td_keep.file_name
  )
  AND td.id > td_keep.id;

DELETE p FROM proposition p
INNER JOIN annotation ann ON p.annotation_id = ann.id
INNER JOIN task_document td ON ann.document_id = td.id
INNER JOIN task_document td_keep ON td.task_id = td_keep.task_id
  AND td.task_id IN (1001, 1002, 1003)
  AND (
    (td.global_doc_id IS NOT NULL AND td.global_doc_id = td_keep.global_doc_id)
    OR td.file_name = td_keep.file_name
  )
  AND td.id > td_keep.id;

DELETE ar FROM argument_relation ar
INNER JOIN annotation ann ON ar.annotation_id = ann.id
INNER JOIN task_document td ON ann.document_id = td.id
INNER JOIN task_document td_keep ON td.task_id = td_keep.task_id
  AND td.task_id IN (1001, 1002, 1003)
  AND (
    (td.global_doc_id IS NOT NULL AND td.global_doc_id = td_keep.global_doc_id)
    OR td.file_name = td_keep.file_name
  )
  AND td.id > td_keep.id;

DELETE ann FROM annotation ann
INNER JOIN task_document td ON ann.document_id = td.id
INNER JOIN task_document td_keep ON td.task_id = td_keep.task_id
  AND td.task_id IN (1001, 1002, 1003)
  AND (
    (td.global_doc_id IS NOT NULL AND td.global_doc_id = td_keep.global_doc_id)
    OR td.file_name = td_keep.file_name
  )
  AND td.id > td_keep.id;

DELETE s FROM arbitration_snapshot s
INNER JOIN task_document td ON s.task_document_id = td.id
INNER JOIN task_document td_keep ON td.task_id = td_keep.task_id
  AND td.task_id IN (1001, 1002, 1003)
  AND (
    (td.global_doc_id IS NOT NULL AND td.global_doc_id = td_keep.global_doc_id)
    OR td.file_name = td_keep.file_name
  )
  AND td.id > td_keep.id;

DELETE td FROM task_document td
INNER JOIN task_document td_keep ON td.task_id = td_keep.task_id
  AND td.task_id IN (1001, 1002, 1003)
  AND (
    (td.global_doc_id IS NOT NULL AND td.global_doc_id = td_keep.global_doc_id)
    OR td.file_name = td_keep.file_name
  )
  AND td.id > td_keep.id;

-- ── 删除 test / test2 任务 ──

DELETE rm FROM relation_member rm
INNER JOIN argument_relation ar ON rm.relation_id = ar.id
INNER JOIN annotation ann ON ar.annotation_id = ann.id
INNER JOIN task t ON ann.task_id = t.id
WHERE t.title IN ('test', 'test2');

DELETE p FROM proposition p
INNER JOIN annotation ann ON p.annotation_id = ann.id
INNER JOIN task t ON ann.task_id = t.id
WHERE t.title IN ('test', 'test2');

DELETE ar FROM argument_relation ar
INNER JOIN annotation ann ON ar.annotation_id = ann.id
INNER JOIN task t ON ann.task_id = t.id
WHERE t.title IN ('test', 'test2');

DELETE ann FROM annotation ann
INNER JOIN task t ON ann.task_id = t.id
WHERE t.title IN ('test', 'test2');

DELETE s FROM arbitration_snapshot s
INNER JOIN task t ON s.task_id = t.id
WHERE t.title IN ('test', 'test2');

DELETE tm FROM task_member tm
INNER JOIN task t ON tm.task_id = t.id
WHERE t.title IN ('test', 'test2');

DELETE td FROM task_document td
INNER JOIN task t ON td.task_id = t.id
WHERE t.title IN ('test', 'test2');

DELETE FROM task WHERE title IN ('test', 'test2');

-- ── 其余任务：按 global_doc_id 去重 ──

DELETE rm FROM relation_member rm
INNER JOIN argument_relation ar ON rm.relation_id = ar.id
INNER JOIN annotation ann ON ar.annotation_id = ann.id
INNER JOIN task_document td ON ann.document_id = td.id
INNER JOIN task_document td_keep ON td.task_id = td_keep.task_id
  AND td.global_doc_id = td_keep.global_doc_id
  AND td.global_doc_id IS NOT NULL
  AND td.id > td_keep.id;

DELETE p FROM proposition p
INNER JOIN annotation ann ON p.annotation_id = ann.id
INNER JOIN task_document td ON ann.document_id = td.id
INNER JOIN task_document td_keep ON td.task_id = td_keep.task_id
  AND td.global_doc_id = td_keep.global_doc_id
  AND td.global_doc_id IS NOT NULL
  AND td.id > td_keep.id;

DELETE ar FROM argument_relation ar
INNER JOIN annotation ann ON ar.annotation_id = ann.id
INNER JOIN task_document td ON ann.document_id = td.id
INNER JOIN task_document td_keep ON td.task_id = td_keep.task_id
  AND td.global_doc_id = td_keep.global_doc_id
  AND td.global_doc_id IS NOT NULL
  AND td.id > td_keep.id;

DELETE ann FROM annotation ann
INNER JOIN task_document td ON ann.document_id = td.id
INNER JOIN task_document td_keep ON td.task_id = td_keep.task_id
  AND td.global_doc_id = td_keep.global_doc_id
  AND td.global_doc_id IS NOT NULL
  AND td.id > td_keep.id;

DELETE s FROM arbitration_snapshot s
INNER JOIN task_document td ON s.task_document_id = td.id
INNER JOIN task_document td_keep ON td.task_id = td_keep.task_id
  AND td.global_doc_id = td_keep.global_doc_id
  AND td.global_doc_id IS NOT NULL
  AND td.id > td_keep.id;

DELETE td FROM task_document td
INNER JOIN task_document td_keep ON td.task_id = td_keep.task_id
  AND td.global_doc_id = td_keep.global_doc_id
  AND td.global_doc_id IS NOT NULL
  AND td.id > td_keep.id;
