/**
 * 构建指南版本只读页的跳转路由，携带明确的返回上下文（避免 router.back 回到错误页面）。
 */
export function buildGuideViewRoute(configId, returnContext = {}) {
  const query = {
    returnPath: returnContext.returnPath || '/tasks',
    expand: '1'
  }
  if (returnContext.taskId != null && returnContext.taskId !== '') {
    query.taskId = String(returnContext.taskId)
  }
  if (returnContext.rowKey != null && returnContext.rowKey !== '') {
    query.rowKey = String(returnContext.rowKey)
  }
  return {
    path: `/configs/versions/${configId}/view`,
    query
  }
}

/** 从指南版本页返回到来源页面 */
export function navigateBackFromGuide(router, route) {
  const returnPath = route.query.returnPath
  if (returnPath) {
    const query = {}
    if (route.query.taskId) query.taskId = route.query.taskId
    if (route.query.rowKey) query.rowKey = route.query.rowKey
    if (route.query.expand) query.expand = route.query.expand
    router.push({ path: returnPath, query })
    return
  }
  router.back()
}

/** 同步任务目录 URL，保证返回时展开正确的任务 */
export function syncTasksRoute(router, taskId, rowKey = null, expanded = true) {
  const query = {}
  if (taskId != null) {
    query.taskId = String(taskId)
    if (expanded) query.expand = '1'
  }
  if (rowKey != null) {
    query.rowKey = String(rowKey)
  }
  router.replace({ path: '/tasks', query })
}

export function tasksReturnRoute(taskId, rowKey = null) {
  const query = {}
  if (taskId != null) {
    query.taskId = String(taskId)
    query.expand = '1'
  }
  if (rowKey != null) {
    query.rowKey = String(rowKey)
  }
  return { path: '/tasks', query }
}
