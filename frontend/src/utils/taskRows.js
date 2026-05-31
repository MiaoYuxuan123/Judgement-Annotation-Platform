/**
 * 任务列表与文书状态工具（统一入口）
 *
 * 三阶段：标注中 → 待裁定 → 可导出
 * - 标注员视角：本人提交后 → 待裁定；本人全部提交 → 任务待裁定
 * - 裁定者视角：全员提交后 → 待裁定；裁定确认后 → 可导出
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
    return countSubmittedAnnotators(entry) >= annotatorCount
  }).length
}

// ── 展示文案 ──────────────────────────────────────────

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
  const status = detail?.summary?.status
  if (status === '可导出') return '争议已全部裁定'
  if (status === '标注中') return '等待标注完成...'
  const total = detail?.documents?.length || detail?.summary?.documentCount || 0
  const ready = countDocsReadyForArbitration(detail, review)
  const pending = Math.max(0, total - ready)
  if (pending <= 0) return '等待裁决...'
  return `待裁决 ${pending} 条`
}

// ── 参与者任务列表 ────────────────────────────────────

export function buildParticipantRows(tasks, details, userId) {
  const rows = []
  for (const task of tasks) {
    const detail = details[task.taskId]
    const review = detail?._review
    const isAnnotator = detail?.annotators?.some((u) => u.id === userId)
    const isReviewer = detail?.reviewer?.id === userId

    if (isAnnotator) {
      const personalStage = resolveAnnotatorTaskStage(detail, review, userId)
      rows.push({
        key: `${task.taskId}-annotate`,
        taskId: task.taskId,
        taskName: task.taskName,
        role: 'annotate',
        roleLabel: '标注',
        status: stageDisplay(personalStage),
        personalStage,
        info: annotateInfo(detail, review, userId),
        infoType: 'normal',
        detail
      })
    }
    if (isReviewer) {
      const globalStage = detail?.summary?.status || task.status || '标注中'
      const info = arbitrateInfo(detail, review)
      rows.push({
        key: `${task.taskId}-arbitrate`,
        taskId: task.taskId,
        taskName: task.taskName,
        role: 'arbitrate',
        roleLabel: '裁定',
        status: stageDisplay(globalStage),
        personalStage: globalStage,
        info,
        infoType: globalStage === '待裁定' && info.includes('待裁决') ? 'warn' : globalStage === '标注中' ? 'muted' : 'normal',
        detail
      })
    }
  }
  return rows
}

export function participantAction(row, userId) {
  const { role, detail } = row
  const review = detail?._review
  const globalStatus = detail?.summary?.status

  if (globalStatus === '可导出') {
    return { label: '查看结果/导出', color: 'green', route: `/tasks/${row.taskId}/data` }
  }
  if (role === 'annotate') {
    const personalStage = row.personalStage || resolveAnnotatorTaskStage(detail, review, userId)
    if (personalStage === '标注中') {
      const completed = countAnnotatorSubmittedDocs(detail, review, userId)
      return {
        label: completed > 0 ? '继续标注' : '开始标注',
        color: 'orange',
        route: `/tasks/${row.taskId}/data`
      }
    }
    return null
  }
  if (role === 'arbitrate') {
    if (globalStatus === '待裁定') {
      return { label: '开始裁决', color: 'orange', route: `/tasks/${row.taskId}/data` }
    }
    return null
  }
  return null
}

export function participantRowStage(row) {
  return row.personalStage || row.detail?.summary?.status || '标注中'
}

export function creatorAction(task) {
  if (task.status === '可导出') {
    return { label: '查看结果/导出', color: 'green', route: `/tasks/${task.taskId}/data` }
  }
  return null
}
