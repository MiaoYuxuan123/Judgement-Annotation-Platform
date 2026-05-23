/** 任务状态展示映射 */
export function mapAnnotateStatus(taskStatus) {
  if (taskStatus === '可导出' || taskStatus === '待裁定') return { label: '已完成', type: 'done' }
  return { label: '标注中', type: 'progress' }
}

export function mapArbitrateStatus(taskStatus) {
  if (taskStatus === '可导出') return { label: '已裁定', type: 'done' }
  if (taskStatus === '待裁定') return { label: '裁决中', type: 'progress' }
  if (taskStatus === '标注中') return { label: '标注中', type: 'wait' }
  return { label: '等待裁决...', type: 'wait' }
}

/** 标注进度文案 */
export function annotateInfo(detail) {
  const total = detail?.documents?.length || detail?.summary?.documentCount || 0
  if (!total) return '暂无数据'
  const completed = detail?._annotateDone ?? 0
  return `已完成 ${completed} / 共 ${total}`
}

/** 裁决待办文案 */
export function arbitrateInfo(detail) {
  const status = detail?.summary?.status
  if (status === '可导出') return '争议已全部裁定'
  if (status === '标注中') return '等待标注完成...'
  const pending = detail?._disputeCount ?? Math.max(0, (detail?.documents?.length || 1) - 1)
  if (pending <= 0) return '等待裁决...'
  return `待裁决 ${pending} 条争议`
}

/** 参与者：同一任务按角色展开为多行 */
export function buildParticipantRows(tasks, details, userId) {
  const rows = []
  for (const task of tasks) {
    const detail = details[task.taskId]
    const isAnnotator = detail?.annotators?.some((u) => u.id === userId)
    const isReviewer = detail?.reviewer?.id === userId

    if (isAnnotator) {
      const st = mapAnnotateStatus(task.status)
      rows.push({
        key: `${task.taskId}-annotate`,
        taskId: task.taskId,
        taskName: task.taskName,
        role: 'annotate',
        roleLabel: '标注',
        status: st,
        info: annotateInfo(detail),
        infoType: 'normal',
        detail
      })
    }
    if (isReviewer) {
      const st = mapArbitrateStatus(task.status)
      const info = arbitrateInfo(detail)
      rows.push({
        key: `${task.taskId}-arbitrate`,
        taskId: task.taskId,
        taskName: task.taskName,
        role: 'arbitrate',
        roleLabel: '裁定',
        status: st,
        info,
        infoType: task.status === '待裁定' && info.includes('待裁决') ? 'warn' : task.status === '标注中' ? 'muted' : 'normal',
        detail
      })
    }
  }
  return rows
}

/** 参与者操作按钮 */
export function participantAction(row) {
  const { role, detail } = row
  const status = detail?.summary?.status

  if (status === '可导出') {
    return { label: '查看结果/导出', color: 'green', route: `/tasks/${row.taskId}/data` }
  }
  if (role === 'annotate') {
    if (status === '标注中') {
      const hasProgress = row.info && !row.info.startsWith('已完成 0')
      return {
        label: hasProgress ? '继续标注' : '开始标注',
        color: 'orange',
        route: `/tasks/${row.taskId}/data`
      }
    }
    return null
  }
  if (role === 'arbitrate') {
    if (status === '待裁定') {
      return { label: '开始裁决', color: 'orange', route: `/tasks/${row.taskId}/data` }
    }
    return null
  }
  return null
}

/** 创建者操作按钮（仅已裁定/可导出时可操作） */
export function creatorAction(task) {
  if (task.status === '可导出') {
    return { label: '查看结果/导出', color: 'green', route: `/tasks/${task.taskId}/data` }
  }
  return null
}

/** 阶段展示 */
export function stageLabel(status) {
  if (status === '可导出') return '已裁定（可导出）'
  if (status === '待裁定') return '待裁定'
  return '标注中'
}
