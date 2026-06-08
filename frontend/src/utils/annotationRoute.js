/** 裁定「部分修改」模式下的路由 query 是否有效 */
export function isArbitrationMode(query) {
  return query?.mode === 'arbitration'
}

/** 构建 GET /tasks/:taskId/items/:dataId 的 URL（含裁定来源参数） */
export function annotationItemUrl(taskId, dataId, query = {}) {
  let url = `/tasks/${taskId}/items/${dataId}`
  const params = new URLSearchParams()
  if (isArbitrationMode(query) && query.fromUserId) {
    params.set('sourceUserId', query.fromUserId)
  } else if (isArbitrationMode(query) && query.fromFinal === '1') {
    params.set('sourceArbitration', '1')
  }
  const qs = params.toString()
  return qs ? `${url}?${qs}` : url
}

export function annotatePageRoute(taskId, dataId, query = {}) {
  return {
    path: `/annotate/${taskId}/${dataId}`,
    query: { ...query }
  }
}

/** 裁定图示保存后，后续加载应优先读裁定草稿而非标注员来源 */
export function arbitrationDraftQuery(query = {}) {
  const next = { ...query, mode: 'arbitration', fromFinal: '1' }
  delete next.fromUserId
  return next
}

export function graphEditorRoute(taskId, dataId, query = {}) {
  return {
    path: `/annotate/${taskId}/${dataId}/graph`,
    query: { ...query }
  }
}

function hasAnnotationContent(annotation) {
  if (!annotation) return false
  if (annotation.propositions?.length || annotation.relations?.length) return true
  const layout = annotation.graphLayout
  if (layout?.version === 2 && layout.nodes?.length) return true
  if (layout?.version === 1 && layout.nodePositions && Object.keys(layout.nodePositions).length) {
    return true
  }
  return false
}

/** 裁定模式下优先返回已存在的裁定草稿，否则按来源（标注员/最终版）加载 */
export async function fetchAnnotationItem(client, taskId, dataId, query = {}) {
  if (!isArbitrationMode(query)) {
    return client.get(annotationItemUrl(taskId, dataId, query))
  }
  const draftData = await client.get(annotationItemUrl(taskId, dataId, {
    mode: 'arbitration',
    fromFinal: '1'
  }))
  if (hasAnnotationContent(draftData?.annotation)) return draftData
  return client.get(annotationItemUrl(taskId, dataId, query))
}
