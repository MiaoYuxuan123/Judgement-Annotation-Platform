/** 登录后按角色返回默认首页路径 */
export function getDefaultRoute(auth) {
  if (auth.user?.role === 'admin') return '/documents'
  return '/tasks'
}

/** 任务创建者 */
export function isCreator(auth) {
  return auth.user?.canCreateTask === true
}

/** 普通用户：非管理员且不可创建任务 */
export function isParticipant(auth) {
  return auth.user?.role !== 'admin' && !auth.user?.canCreateTask
}

/** 使用任务模块独立框架（创建者 / 参与者） */
export function usesTaskShell(auth) {
  return isCreator(auth) || isParticipant(auth)
}
