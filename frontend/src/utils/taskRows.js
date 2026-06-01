/**
 * 任务列表与文书状态工具（统一入口）
 *
 * 三阶段：标注中 → 待裁定 → 可导出
 * - 标注员视角：本人提交后 → 待裁定
 * - 裁定者视角：每条文书全员提交后可单独进入「待裁定」；任务列表仅展示「查看详情」
 */

// ── 状态判定 ──────────────────────────────────────────

export function normalizeDocStatus(status) {
  if (!status || status === '待标注' || status === '已标注') return '标注中'
  if (status === '已裁定') return '可导出'
  if (status === '标注中' || status === '待裁定' || status === '可导出') return status
  return '标注中'
}

export function isDocExportable(docEntry) {
  if (!docEntry) return false
  const final = docEntry.finalResult
  return Boolean(
    final &&
      typeof final === 'object' &&
      final.propositions &&
      final.finalResult !== false
  )
}

export function isAnnotatorSubmitted(reviewEntry, userId) {
  if (!reviewEntry || userId == null) return false
  const mine = reviewEntry.annotatorResults?.find((r) => r.userId === userId)
  return Boolean(mine && (mine.propositions?.length || mine.relations?.length) && !mine.draft)
}

function countSubmittedAnnotators(reviewEntry) {
  return (reviewEntry?.annotatorResults || []).filter(
    (r) => (r.propositions?.length || r.relations?.length) && !r.draft
  ).length
}

/** 单篇文书阶段（数据列表用） */
export function resolveDocStage(reviewEntry, options = {}) {
  const {
    annotatorCount = 0,
    documentStatus = '',
    viewerRole = 'annotator',
    userId = null
  } = options

  const stored = normalizeDocStatus(documentStatus)
  if (stored === '可导出' || isDocExportable(reviewEntry)) return '可导出'

  if (stored === '待裁定') {
    if (viewerRole === 'annotator') {
      return isAnnotatorSubmitted(reviewEntry, userId) ? '待裁定' : '标注中'
    }
    return '待裁定'
  }

  if (viewerRole === 'annotator') {
    return isAnnotatorSubmitted(reviewEntry, userId) ? '待裁定' : '标注中'
  }

  const final = reviewEntry?.finalResult
  if (final && typeof final === 'object' && final.propositions) {
    return '待裁定'
  }

  const allAnnotatorsDone = annotatorCount > 0 && countSubmittedAnnotators(reviewEntry) >= annotatorCount
  if (allAnnotatorsDone) return '待裁定'

  return '标注中'
}

/** 标注员在整个任务下的阶段（任务目录用） */
export function resolveAnnotatorTaskStage(taskDetail, review, userId) {
  if (taskDetail?.summary?.status === '可导出') return '可导出'

  const docs = taskDetail?.documents || []
  if (!docs.length || userId == null) return '标注中'

  const reviewDocs = review?.documents || []
  const allSubmitted = docs.every((doc) => {
    const entry = reviewDocs.find((d) => d.document.id === doc.id)
    return isAnnotatorSubmitted(entry, userId)
  })

  return allSubmitted ? '待裁定' : '标注中'
}

function countAnnotatorSubmittedDocs(taskDetail, review, userId) {
  const docs = taskDetail?.documents || []
  if (!docs.length || userId == null) return 0
  const reviewDocs = review?.documents || []
  return docs.filter((doc) => {
    const entry = reviewDocs.find((d) => d.document.id === doc.id)
    return isAnnotatorSubmitted(entry, userId)
  }).length
}

function countDocsReadyForArbitration(taskDetail, review) {
  const docs = taskDetail?.documents || []
  const annotatorCount = taskDetail?.annotators?.length || 0
  if (!docs.length || !annotatorCount) return 0
  const reviewDocs = review?.documents || []
  return docs.filter((doc) => {
    const entry = reviewDocs.find((d) => d.document.id === doc.id)
    return resolveDocStage(entry, {
      annotatorCount,
      documentStatus: doc.status,
      viewerRole: 'reviewer'
    }) === '待裁定'
  }).length
}

function countExportableDocs(taskDetail, review) {
  const docs = taskDetail?.documents || []
  const annotatorCount = taskDetail?.annotators?.length || 0
  if (!docs.length) return 0
  const reviewDocs = review?.documents || []
  return docs.filter((doc) => {
    const entry = reviewDocs.find((d) => d.document.id === doc.id)
    return resolveDocStage(entry, {
      annotatorCount,
      documentStatus: doc.status,
      viewerRole: 'reviewer'
    }) === '可导出'
  }).length
}

/** 裁定者在整个任务下的阶段（按每条文书独立汇总） */
export function resolveReviewerTaskStage(taskDetail, review) {
  const docs = taskDetail?.documents || []
  if (!docs.length) return taskDetail?.summary?.status || '标注中'

  const annotatorCount = taskDetail?.annotators?.length || 0
  const reviewDocs = review?.documents || []
  const stages = docs.map((doc) => {
    const entry = reviewDocs.find((d) => d.document.id === doc.id)
    return resolveDocStage(entry, {
      annotatorCount,
      documentStatus: doc.status,
      viewerRole: 'reviewer'
    })
  })

  if (stages.every((s) => s === '可导出')) return '可导出'
  if (stages.some((s) => s === '待裁定')) return '待裁定'
  if (stages.some((s) => s === '可导出')) return '待裁定'
  return '标注中'
}

// ── 展示文案 ──────────────────────────────────────────

function combineParticipantStage(...stages) {
  const order = { 标注中: 0, 待裁定: 1, 可导出: 2 }
  return stages.reduce((earliest, stage) =>
    (order[stage] ?? 0) < (order[earliest] ?? 0) ? stage : earliest
  )
}

/** 当前访问者在任务中的身份列表（可含标注 + 裁定） */
export function resolveTaskViewerRoles(detail, user) {
  const userId = user?.id
  const roles = []

  if (detail?.annotators?.some((u) => u.id === userId)) {
    roles.push({ role: 'annotate', roleLabel: '标注', roleClass: 'role-annotate' })
  }
  if (detail?.reviewer?.id === userId) {
    roles.push({ role: 'arbitrate', roleLabel: '裁定', roleClass: 'role-arbitrate' })
  }
  if (!roles.length && user?.canCreateTask) {
    roles.push({ role: 'creator', roleLabel: '创建者', roleClass: 'role-creator' })
  }
  if (!roles.length && user?.role === 'admin') {
    roles.push({ role: 'admin', roleLabel: '管理员', roleClass: 'role-creator' })
  }
  if (!roles.length) {
    roles.push({ role: 'annotate', roleLabel: '标注', roleClass: 'role-annotate' })
  }
  return roles
}

/** 当前访问者在任务中的主身份（兼容旧用法） */
export function resolveTaskViewerRole(detail, user) {
  return resolveTaskViewerRoles(detail, user)[0]
}

/** 三阶段对应的标签样式 */
export function stageDisplay(stage) {
  if (stage === '可导出') return { label: '可导出', type: 'done' }
  if (stage === '待裁定') return { label: '待裁定', type: 'progress' }
  return { label: '标注中', type: 'wait' }
}

export function stageLabel(stage) {
  return stageDisplay(stage).label
}

export function annotateInfo(detail, review, userId) {
  const total = detail?.documents?.length || detail?.summary?.documentCount || 0
  if (!total) return '暂无数据'
  const completed = countAnnotatorSubmittedDocs(detail, review, userId)
  return `已完成 ${completed} / 共 ${total}`
}

export function arbitrateInfo(detail, review) {
  const stage = resolveReviewerTaskStage(detail, review)
  if (stage === '可导出') return '争议已全部裁定'

  const total = detail?.documents?.length || detail?.summary?.documentCount || 0
  const pending = countDocsReadyForArbitration(detail, review)
  const exportable = countExportableDocs(detail, review)

  if (pending > 0) return `待裁决 ${pending} 条`
  if (exportable > 0 && exportable < total) return `已裁定 ${exportable} / 共 ${total}`
  return '等待标注完成...'
}

// ── 参与者任务列表 ────────────────────────────────────

export function buildParticipantRows(tasks, details, userId) {
  const rows = []
  for (const task of tasks) {
    const detail = details[task.taskId]
    const review = detail?._review
    const isAnnotator = detail?.annotators?.some((u) => u.id === userId)
    const isReviewer = detail?.reviewer?.id === userId
    if (!isAnnotator && !isReviewer) continue

    const roles = []
    const stages = []
    const infoParts = []
    let infoType = 'normal'

    if (isAnnotator) {
      roles.push({ role: 'annotate', roleLabel: '标注' })
      const personalStage = resolveAnnotatorTaskStage(detail, review, userId)
      stages.push(personalStage)
      infoParts.push(`标注 ${annotateInfo(detail, review, userId)}`)
    }
    if (isReviewer) {
      roles.push({ role: 'arbitrate', roleLabel: '裁定' })
      const reviewerStage = resolveReviewerTaskStage(detail, review)
      stages.push(reviewerStage)
      const info = arbitrateInfo(detail, review)
      infoParts.push(`裁定 ${info}`)
      if (reviewerStage === '待裁定' && info.includes('待裁决')) {
        infoType = 'warn'
      } else if (reviewerStage === '标注中' && infoType !== 'warn') {
        infoType = 'muted'
      }
    }

    const personalStage = combineParticipantStage(...stages)
    rows.push({
      key: String(task.taskId),
      taskId: task.taskId,
      taskName: task.taskName,
      roles,
      status: stageDisplay(personalStage),
      personalStage,
      info: infoParts.join('；'),
      infoType,
      detail
    })
  }
  return rows
}

export function participantAction(row) {
  return { label: '查看详情', color: 'green', route: `/tasks/${row.taskId}/data` }
}

export function participantRowStage(row) {
  return row.personalStage || row.detail?.summary?.status || '标注中'
}

export function creatorAction(task) {
  return { label: '查看详情', color: 'green', route: `/tasks/${task.taskId}/data` }
}
