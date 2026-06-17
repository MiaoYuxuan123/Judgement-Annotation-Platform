/**
 * 论证图布局覆盖层：与自动布局结果合并，持久化用户手动排版偏好。
 *
 * graphLayout 结构：
 * {
 *   version: 1,
 *   nodePositions: { [nodeId]: { x, y } },
 *   edgeStyles: {
 *     [edgeId]: {
 *       sourceHandle, targetHandle,
 *       waypoints: [{ x, y }],
 *       directed: boolean
 *     }
 *   }
 * }
 */

export const HANDLE_IDS = [
  'top', 'top-left', 'top-right',
  'right', 'bottom-right', 'bottom',
  'bottom-left', 'left'
]

export const EMPTY_LAYOUT = () => ({
  version: 1,
  nodePositions: {},
  edgeStyles: {}
})

export function cloneLayout(layout) {
  if (!layout) return EMPTY_LAYOUT()
  return JSON.parse(JSON.stringify(layout))
}

function parseNodeSize(node) {
  if (String(node.type || '').startsWith('hub-')) {
    const relType = String(node.data?.relType || node.type.replace('hub-', '')).toUpperCase()
    const hubSizes = { S: 14, A: 18, M: 24, J: 24 }
    const size = hubSizes[relType] || 24
    return { width: size, height: size }
  }
  const width = parseFloat(node.style?.width) || parseFloat(node.width) || 58
  const height = parseFloat(node.style?.height) || parseFloat(node.height) || 34
  return { width, height }
}

export function nodeRect(node) {
  const { width, height } = parseNodeSize(node)
  const x = node.position?.x ?? 0
  const y = node.position?.y ?? 0
  return {
    x,
    y,
    width,
    height,
    cx: x + width / 2,
    cy: y + height / 2
  }
}

export function handleConnectionPoint(node, handleId) {
  const rect = nodeRect(node)
  if (String(node?.type || '').startsWith('hub-')) {
    return { x: rect.cx, y: rect.cy }
  }
  const map = {
    top: { x: rect.cx, y: rect.y },
    'top-left': { x: rect.x, y: rect.y },
    'top-right': { x: rect.x + rect.width, y: rect.y },
    right: { x: rect.x + rect.width, y: rect.cy },
    'bottom-right': { x: rect.x + rect.width, y: rect.y + rect.height },
    bottom: { x: rect.cx, y: rect.y + rect.height },
    'bottom-left': { x: rect.x, y: rect.y + rect.height },
    left: { x: rect.x, y: rect.cy }
  }
  return map[handleId] || map.right
}

function pointOnRectToward(rect, toward) {
  const dx = toward.x - rect.cx
  const dy = toward.y - rect.cy
  if (Math.abs(dx) >= Math.abs(dy)) {
    return { x: dx >= 0 ? rect.x + rect.width : rect.x, y: rect.cy }
  }
  return { x: rect.cx, y: dy >= 0 ? rect.y + rect.height : rect.y }
}

/** 节点边框上朝向某点的连接点（与自动布局一致） */
export function borderPointToward(node, toward) {
  if (String(node?.type || '').startsWith('hub-')) {
    const rect = nodeRect(node)
    return { x: rect.cx, y: rect.cy }
  }
  return pointOnRectToward(nodeRect(node), toward)
}

export function orthogonalPath(from, to) {
  if (Math.abs(from.x - to.x) < 0.5 || Math.abs(from.y - to.y) < 0.5) return [from, to]
  const midX = (from.x + to.x) / 2
  return [from, { x: midX, y: from.y }, { x: midX, y: to.y }, to]
}

export function pointsToSvgPath(points) {
  if (!points?.length) return ''
  return points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
}

function resolveEdgeEndpoints(edge, nodeById, edgeStyle = {}) {
  const sourceNode = nodeById.get(edge.source)
  const targetNode = nodeById.get(edge.target)
  if (!sourceNode || !targetNode) {
    return edge.data?.points || []
  }

  const targetCenter = nodeRect(targetNode)
  const sourceCenter = nodeRect(sourceNode)
  const sourceOut = edgeStyle.sourceHandle
    ? handleConnectionPoint(sourceNode, edgeStyle.sourceHandle)
    : pointOnRectToward(nodeRect(sourceNode), targetCenter)
  const targetIn = edgeStyle.targetHandle
    ? handleConnectionPoint(targetNode, edgeStyle.targetHandle)
    : pointOnRectToward(nodeRect(targetNode), sourceCenter)

  if (edgeStyle.waypoints?.length) {
    return [sourceOut, ...edgeStyle.waypoints, targetIn]
  }
  return orthogonalPath(sourceOut, targetIn)
}

function applyEdgeStyle(edge, nodeById, edgeStyle) {
  const points = resolveEdgeEndpoints(edge, nodeById, edgeStyle)
  const directed = edgeStyle.directed ?? edge.data?.directed ?? false
  return {
    ...edge,
    sourceHandle: edgeStyle.sourceHandle || edge.sourceHandle,
    targetHandle: edgeStyle.targetHandle || edge.targetHandle,
    data: {
      ...edge.data,
      points,
      path: pointsToSvgPath(points),
      directed,
      sourceHandle: edgeStyle.sourceHandle,
      targetHandle: edgeStyle.targetHandle,
      waypoints: edgeStyle.waypoints || []
    }
  }
}

export function applyLayoutOverride(layout, override) {
  if (!layout) return { nodes: [], edges: [] }
  if (!override || (!Object.keys(override.nodePositions || {}).length && !Object.keys(override.edgeStyles || {}).length)) {
    return {
      nodes: layout.nodes.map((n) => ({ ...n, position: { ...n.position }, data: { ...n.data } })),
      edges: layout.edges.map((e) => ({
        ...e,
        data: { ...e.data, points: [...(e.data?.points || [])] }
      })),
      bounds: layout.bounds
    }
  }

  const nodeById = new Map()
  const nodes = layout.nodes.map((node) => {
    const pos = override.nodePositions?.[node.id]
    const next = {
      ...node,
      position: pos ? { x: pos.x, y: pos.y } : { ...node.position },
      data: { ...node.data }
    }
    nodeById.set(next.id, next)
    return next
  })

  const edges = layout.edges.map((edge) => {
    const style = override.edgeStyles?.[edge.id] || {}
    return applyEdgeStyle(
      { ...edge, data: { ...edge.data, points: [...(edge.data?.points || [])] } },
      nodeById,
      style
    )
  })

  return { nodes, edges, bounds: layout.bounds }
}

export function extractLayoutOverride(nodes, edges, baseLayout) {
  const override = EMPTY_LAYOUT()
  const baseNodeMap = new Map((baseLayout?.nodes || []).map((n) => [n.id, n]))
  const baseEdgeMap = new Map((baseLayout?.edges || []).map((e) => [e.id, e]))

  nodes.forEach((node) => {
    const base = baseNodeMap.get(node.id)
    if (!base) return
    const dx = Math.round(node.position.x - base.position.x)
    const dy = Math.round(node.position.y - base.position.y)
    if (Math.abs(dx) > 0.5 || Math.abs(dy) > 0.5) {
      override.nodePositions[node.id] = {
        x: Math.round(node.position.x),
        y: Math.round(node.position.y)
      }
    }
  })

  edges.forEach((edge) => {
    const base = baseEdgeMap.get(edge.id)
    if (!base) return
    const style = {}
    if (edge.data?.sourceHandle) style.sourceHandle = edge.data.sourceHandle
    if (edge.data?.targetHandle) style.targetHandle = edge.data.targetHandle
    if (edge.data?.waypoints?.length) style.waypoints = edge.data.waypoints.map((p) => ({ x: p.x, y: p.y }))
    if (edge.data?.directed !== base.data?.directed) style.directed = edge.data.directed

    const basePts = JSON.stringify(base.data?.points || [])
    const curPts = JSON.stringify(edge.data?.points || [])
    if (
      style.sourceHandle ||
      style.targetHandle ||
      style.waypoints?.length ||
      style.directed !== undefined ||
      basePts !== curPts
    ) {
      override.edgeStyles[edge.id] = style
    }
  })

  return override
}

export function refreshEdgesForNode(nodes, edges, nodeId, override) {
  const nodeById = new Map(nodes.map((n) => [n.id, n]))
  const styles = override?.edgeStyles || {}
  return edges.map((edge) => {
    if (edge.source !== nodeId && edge.target !== nodeId) return edge
    const style = styles[edge.id] || {}
    if (style.waypoints?.length) {
      return applyEdgeStyle(edge, nodeById, style)
    }
    const nextStyle = { ...style }
    delete nextStyle.waypoints
    return applyEdgeStyle(edge, nodeById, nextStyle)
  })
}

export function updateNodePosition(override, nodeId, position) {
  const next = cloneLayout(override)
  next.nodePositions[nodeId] = { x: Math.round(position.x), y: Math.round(position.y) }
  return next
}

export function updateEdgeStyle(override, edgeId, patch) {
  const next = cloneLayout(override)
  next.edgeStyles[edgeId] = { ...(next.edgeStyles[edgeId] || {}), ...patch }
  return next
}

export function isLayoutEmpty(layout) {
  if (!layout) return true
  if (layout.version === 2) return !layout.nodes?.length
  return !Object.keys(layout.nodePositions || {}).length && !Object.keys(layout.edgeStyles || {}).length
}

/** 提交保存时：空 v1 不发送，避免覆盖库中 v2 手动图示 */
export function graphLayoutForSave(layout) {
  if (!layout) return null
  if (layout.version === 2 && layout.nodes?.length) return layout
  if (isLayoutEmpty(layout)) return null
  return layout
}

export function layoutSummary(layout) {
  if (!layout) return { nodes: 0, edges: 0 }
  return {
    nodes: Object.keys(layout.nodePositions || {}).length,
    edges: Object.keys(layout.edgeStyles || {}).length
  }
}
