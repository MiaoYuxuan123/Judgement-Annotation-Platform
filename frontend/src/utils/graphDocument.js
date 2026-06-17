/**
 * 论证图文档模型 v2：用户从零绘制的完整画布状态。
 *
 * {
 *   version: 2,
 *   nodes: VueFlowNode[],
 *   edges: VueFlowEdge[]
 * }
 */

import { layoutArgumentGraphWithElk } from './argumentGraphElkLayout'
import {
  borderPointToward,
  cloneLayout,
  handleConnectionPoint,
  nodeRect,
  pointsToSvgPath,
  orthogonalPath
} from './graphLayoutOverride'

export const HANDLE_IDS = [
  'top', 'top-left', 'top-right',
  'right', 'bottom-right', 'bottom',
  'bottom-left', 'left'
]

export const CARDINAL_HANDLE_IDS = ['top', 'right', 'bottom', 'left']

export const HUB_HANDLE_IDS = ['center-source', 'center-target']

export function isHubNodeType(type) {
  return String(type || '').startsWith('hub-')
}

export function getNodeHandles(node) {
  return isHubNodeType(node?.type) ? HUB_HANDLE_IDS : HANDLE_IDS
}

/** 在画布坐标中查找最近的节点连接点 */
export function findNearestHandle(nodes, point, options = {}) {
  const { excludeNodeId = '', threshold = 28 } = options
  let best = null
  for (const node of nodes || []) {
    if (!node || node.id === excludeNodeId) continue
    for (const handleId of getNodeHandles(node)) {
      const anchor = handleConnectionPoint(node, handleId)
      const dist = Math.hypot(anchor.x - point.x, anchor.y - point.y)
      if (dist > threshold) continue
      if (!best || dist < best.dist) {
        best = { nodeId: node.id, handleId, dist, point: anchor }
      }
    }
  }
  return best
}

const PROP_W = 60
const PROP_H = 34
const HUB_SIZE = { S: 14, A: 18, M: 24, J: 24 }

export const EMPTY_DOCUMENT = () => ({
  version: 2,
  nodes: [],
  edges: []
})

export function cloneDocument(doc) {
  if (!doc) return EMPTY_DOCUMENT()
  return JSON.parse(JSON.stringify(doc))
}

export function isDocumentEmpty(doc) {
  return !doc?.nodes?.length && !doc?.edges?.length
}

export function hubTypeForRelation(relType) {
  const t = String(relType || 'S').toUpperCase()
  return `hub-${t.toLowerCase()}`
}

export function nextPropId(nodes) {
  const nums = nodes
    .filter((n) => n.type === 'prop')
    .map((n) => Number(String(n.data?.label || n.id).replace(/^P/, '')) || 0)
  const max = nums.length ? Math.max(...nums) : 0
  return `P${max + 1}`
}

export function nextRelationId(nodes) {
  const nums = nodes
    .filter((n) => String(n.type || '').startsWith('hub-'))
    .map((n) => Number(String(n.data?.relKey || n.id).replace(/^hub-R|^R/, '').replace(/\D/g, '')) || 0)
  const fromEdges = nodes // fallback
  void fromEdges
  const hubNums = nodes
    .filter((n) => n.data?.relKey)
    .map((n) => Number(String(n.data.relKey).replace(/^R/, '')) || 0)
  const max = hubNums.length ? Math.max(...hubNums) : 0
  return `R${max + 1}`
}

export function nextEdgeId(edges) {
  const nums = (edges || []).map((e) => Number(String(e.id).replace(/^e/, '')) || 0)
  return `e${(nums.length ? Math.max(...nums) : 0) + 1}`
}

export function createPropNode(id, x, y, label = id, extraData = {}) {
  const width = Math.max(PROP_W, String(label).length * 10 + 24)
  return {
    id,
    type: 'prop',
    position: { x: Math.round(x), y: Math.round(y) },
    data: { label, stableId: id, ...extraData },
    style: { width: `${width}px`, height: `${PROP_H}px` }
  }
}

export function createPropNodeFromProposition(proposition, x, y) {
  const id = proposition.propId
  return createPropNode(id, x, y, id, {
    stableId: id,
    tag: proposition.tag,
    text: proposition.text
  })
}

/** 画布上已存在的命题 ID（含同一关系成员） */
export function getPropMemberIds(node) {
  if (node?.type !== 'prop') return []
  if (node.data?.identityMembers?.length) return [...node.data.identityMembers]
  const label = String(node.data?.label || node.id || '')
  if (label.includes('/')) {
    return label.split('/').map((part) => part.trim()).filter(Boolean)
  }
  return [node.data?.stableId || node.id]
}

export function isIdentityPropNode(node) {
  return node?.type === 'prop' && getPropMemberIds(node).length > 1
}

export function formatIdentityLabel(memberIds) {
  return memberIds.join(' / ')
}

function sortPropMemberIds(memberIds) {
  return [...memberIds].sort((a, b) => {
    const na = Number(String(a).replace(/^P/i, '')) || 0
    const nb = Number(String(b).replace(/^P/i, '')) || 0
    if (na !== nb) return na - nb
    return String(a).localeCompare(String(b))
  })
}

function dedupeEdges(edges) {
  const seen = new Set()
  return edges.filter((edge) => {
    const key = [
      edge.source,
      edge.target,
      edge.sourceHandle || '',
      edge.targetHandle || '',
      edge.data?.directed ? '1' : '0'
    ].join('|')
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

/** 将多个命题节点合并为同一关系（矩形内 P1 / P2 图示） */
export function mergePropNodesAsIdentity(nodes, edges, nodeIds) {
  const idSet = new Set(nodeIds || [])
  const selected = (nodes || []).filter((node) => idSet.has(node.id) && node.type === 'prop')
  if (selected.length < 2) return null

  const memberSet = new Set()
  selected.forEach((node) => {
    getPropMemberIds(node).forEach((id) => memberSet.add(id))
  })
  const members = sortPropMemberIds([...memberSet])
  const label = formatIdentityLabel(members)
  const mergedId = members[0]
  const removeIds = new Set(selected.map((node) => node.id))

  const rects = selected.map((node) => nodeRect(node))
  const minX = Math.min(...rects.map((rect) => rect.x))
  const minY = Math.min(...rects.map((rect) => rect.y))
  const maxX = Math.max(...rects.map((rect) => rect.x + rect.width))
  const maxY = Math.max(...rects.map((rect) => rect.y + rect.height))
  const width = Math.max(PROP_W, label.length * 10 + 24)
  const position = {
    x: Math.round((minX + maxX) / 2 - width / 2),
    y: Math.round((minY + maxY) / 2 - PROP_H / 2)
  }

  const idRemap = new Map(selected.map((node) => [node.id, mergedId]))
  const mergedTags = [...new Set(selected.map((node) => node.data?.tag).filter(Boolean))]
  const mergedTexts = selected.map((node) => node.data?.text).filter(Boolean)

  const mergedNode = createPropNode(mergedId, position.x, position.y, label, {
    stableId: mergedId,
    identityMembers: members,
    ...(mergedTags.length ? { tag: mergedTags.join('; ') } : {}),
    ...(mergedTexts.length ? { text: mergedTexts.join(' / ') } : {})
  })

  const nextEdges = dedupeEdges(
    (edges || [])
      .map((edge) => {
        const source = idRemap.get(edge.source) || edge.source
        const target = idRemap.get(edge.target) || edge.target
        if (source === target) return null
        return { ...edge, source, target }
      })
      .filter(Boolean)
  )

  const nextNodes = [
    ...(nodes || []).filter((node) => !removeIds.has(node.id) && node.id !== mergedId),
    mergedNode
  ]

  return {
    nodes: nextNodes,
    edges: rebuildEdgePaths(nextNodes, nextEdges),
    mergedNodeId: mergedId
  }
}

export function getCanvasPropIds(nodes) {
  const ids = new Set()
  for (const node of nodes || []) {
    if (node.type !== 'prop') continue
    getPropMemberIds(node).forEach((id) => ids.add(id))
  }
  return ids
}

/** 为新节点计算网格位置，避免与已有节点重叠（默认同排水平放置） */
export function computeGridPositions(existingNodes, count, anchor = null) {
  if (!count) return []
  const positions = []
  const gapX = 108
  const gapY = 72
  const cols = Math.min(4, Math.max(1, Math.ceil(Math.sqrt(count))))

  let baseX = 80
  let baseY = 80
  if (anchor) {
    baseX = anchor.x
    baseY = anchor.y
  } else if (existingNodes?.length) {
    const maxY = existingNodes.reduce((max, n) => {
      const h = parseFloat(n.style?.height) || PROP_H
      return Math.max(max, n.position.y + h)
    }, 0)
    baseX = existingNodes.reduce((min, n) => Math.min(min, n.position.x), existingNodes[0].position.x)
    baseY = maxY + 48
  }

  for (let i = 0; i < count; i += 1) {
    const col = i % cols
    const row = Math.floor(i / cols)
    positions.push({ x: baseX + col * gapX, y: baseY + row * gapY })
  }
  return positions
}

export function translateWaypoints(waypoints, dx, dy) {
  if (!waypoints?.length || (!dx && !dy)) return waypoints || []
  return waypoints.map((p) => ({ x: p.x + dx, y: p.y + dy }))
}

/** 节点拖动时，同步平移关联连线的折点 */
export function applyNodeDragDelta(nodes, edges, deltas) {
  if (!deltas?.size) return edges

  return edges.map((edge) => {
    const sourceDelta = deltas.get(edge.source)
    const targetDelta = deltas.get(edge.target)
    if (!sourceDelta && !targetDelta) return edge

    let dx = 0
    let dy = 0
    if (sourceDelta && targetDelta) {
      dx = sourceDelta.dx
      dy = sourceDelta.dy
    } else if (sourceDelta) {
      dx = sourceDelta.dx
      dy = sourceDelta.dy
    } else {
      dx = targetDelta.dx
      dy = targetDelta.dy
    }

    const waypoints = edge.data?.waypoints || []
    if (!waypoints.length) return edge

    return {
      ...edge,
      data: {
        ...edge.data,
        waypoints: translateWaypoints(waypoints, dx, dy)
      }
    }
  })
}

export function updateEdgesAfterNodeMove(nodes, edges, deltas) {
  return rebuildEdgePaths(nodes, applyNodeDragDelta(nodes, edges, deltas))
}

export const ORTHO_SNAP_THRESHOLD = 8

/** 折点靠近相邻线段方向时吸附为水平/竖直 */
export function snapOrthogonalPoint(point, anchors, threshold = ORTHO_SNAP_THRESHOLD) {
  let { x, y } = point
  let snapH = false
  let snapV = false

  for (const anchor of anchors) {
    if (!anchor) continue
    if (Math.abs(y - anchor.y) <= threshold) {
      y = anchor.y
      snapH = true
    }
    if (Math.abs(x - anchor.x) <= threshold) {
      x = anchor.x
      snapV = true
    }
  }

  let hint = ''
  if (snapH && snapV) hint = 'both'
  else if (snapH) hint = 'horizontal'
  else if (snapV) hint = 'vertical'

  return { x, y, snapH, snapV, hint }
}

export function snapBendInPath(points, bendIndex, point, threshold = ORTHO_SNAP_THRESHOLD) {
  if (!points?.length || bendIndex <= 0 || bendIndex >= points.length - 1) {
    return snapOrthogonalPoint(point, [], threshold)
  }
  return snapOrthogonalPoint(point, [points[bendIndex - 1], points[bendIndex + 1]], threshold)
}

function collectNodeAlignAnchors(nodes, excludeIds = new Set()) {
  const anchors = []
  for (const node of nodes) {
    if (excludeIds.has(node.id)) continue
    const r = nodeRect(node)
    if (isHubNode(node)) {
      anchors.push({ x: r.cx, y: r.cy })
      continue
    }
    anchors.push(
      { x: r.x, y: r.y },
      { x: r.cx, y: r.y },
      { x: r.x + r.width, y: r.y },
      { x: r.x, y: r.cy },
      { x: r.cx, y: r.cy },
      { x: r.x + r.width, y: r.cy },
      { x: r.x, y: r.y + r.height },
      { x: r.cx, y: r.y + r.height },
      { x: r.x + r.width, y: r.y + r.height }
    )
  }
  return anchors
}

/** 拖动时不与已连线节点互相吸附，避免关系节点被相邻命题“锁死” */
export function getSnapAnchorExcludeIds(movingNodes, edges = []) {
  const excludeIds = new Set((movingNodes || []).map((node) => node.id))
  for (const edge of edges || []) {
    if (excludeIds.has(edge.source)) excludeIds.add(edge.target)
    if (excludeIds.has(edge.target)) excludeIds.add(edge.source)
  }
  return excludeIds
}

function isHubNode(node) {
  return String(node?.type || '').startsWith('hub-')
}

/** 单轴上只取距离最近且在阈值内的吸附，避免小节点多参考点互相覆盖 */
function findBestAxisSnap(refs, applyFns, anchorValues, threshold) {
  let best = null
  for (const anchorValue of anchorValues) {
    for (let i = 0; i < refs.length; i += 1) {
      const dist = Math.abs(refs[i] - anchorValue)
      if (dist > threshold) continue
      if (!best || dist < best.dist) {
        best = { dist, value: applyFns[i](anchorValue) }
      }
    }
  }
  return best
}

/** 拖动节点时与附近节点水平/竖直对齐 */
export function snapNodePosition(node, position, allNodes, excludeIds = new Set(), threshold = ORTHO_SNAP_THRESHOLD) {
  const rect = nodeRect({ ...node, position })
  const { width, height } = rect
  const anchors = collectNodeAlignAnchors(allNodes, excludeIds)
  const hub = isHubNode(node)

  let x = position.x
  let y = position.y
  let snapH = false
  let snapV = false

  const applyX = hub
    ? [(anchorX) => anchorX - width / 2]
    : [
      (anchorX) => anchorX,
      (anchorX) => anchorX - width / 2,
      (anchorX) => anchorX - width
    ]
  const applyY = hub
    ? [(anchorY) => anchorY - height / 2]
    : [
      (anchorY) => anchorY,
      (anchorY) => anchorY - height / 2,
      (anchorY) => anchorY - height
    ]

  const xRefs = hub ? [x + width / 2] : [x, x + width / 2, x + width]
  const yRefs = hub ? [y + height / 2] : [y, y + height / 2, y + height]
  const anchorXs = anchors.map((anchor) => anchor.x)
  const anchorYs = anchors.map((anchor) => anchor.y)

  const bestX = findBestAxisSnap(xRefs, applyX, anchorXs, threshold)
  const bestY = findBestAxisSnap(yRefs, applyY, anchorYs, threshold)

  if (bestX) {
    x = bestX.value
    snapV = true
  }
  if (bestY) {
    y = bestY.value
    snapH = true
  }

  let hint = ''
  if (snapH && snapV) hint = 'both'
  else if (snapH) hint = 'horizontal'
  else if (snapV) hint = 'vertical'

  return { x: Math.round(x), y: Math.round(y), hint }
}

/** 多选拖动时按首个节点吸附，整组同步偏移 */
export function snapNodesGroupPositions(movingNodes, allNodes, threshold = ORTHO_SNAP_THRESHOLD, edges = []) {
  if (!movingNodes?.length) return { positions: new Map(), hint: '' }

  const excludeIds = getSnapAnchorExcludeIds(movingNodes, edges)
  const lead = movingNodes[0]
  const snapped = snapNodePosition(lead, lead.position, allNodes, excludeIds, threshold)
  const dx = snapped.x - lead.position.x
  const dy = snapped.y - lead.position.y

  const positions = new Map()
  if (Math.abs(dx) < 0.01 && Math.abs(dy) < 0.01) {
    movingNodes.forEach((node) => positions.set(node.id, { ...node.position }))
    return { positions, hint: '' }
  }

  movingNodes.forEach((node) => {
    positions.set(node.id, {
      x: Math.round(node.position.x + dx),
      y: Math.round(node.position.y + dy)
    })
  })
  return { positions, hint: snapped.hint }
}

export function createHubNode(relType, relKey, x, y) {
  const type = hubTypeForRelation(relType)
  const size = HUB_SIZE[String(relType).toUpperCase()] || 24
  const id = `hub-${relKey}`
  return {
    id,
    type,
    position: { x: Math.round(x), y: Math.round(y) },
    data: { label: relType === 'J' || relType === 'M' ? '+' : '', hubKind: type, relKey, relType: String(relType).toUpperCase() },
    style: { width: `${size}px`, height: `${size}px` }
  }
}

export function createEdge(id, source, target, sourceHandle, targetHandle, extra = {}) {
  return {
    id,
    source,
    target,
    sourceHandle: sourceHandle || 'right',
    targetHandle: targetHandle || 'left',
    type: 'polyline',
    data: {
      points: [],
      directed: extra.directed ?? false,
      relKey: extra.relKey || '',
      ...extra
    }
  }
}

/** 线段中点（draw.io 风格：拖拽中点生成折点） */
export function segmentMidpoints(points) {
  if (!points || points.length < 2) return []
  const mids = []
  for (let i = 0; i < points.length - 1; i += 1) {
    mids.push({
      segmentIndex: i,
      x: (points[i].x + points[i + 1].x) / 2,
      y: (points[i].y + points[i + 1].y) / 2
    })
  }
  return mids
}

/** 在线段中点插入折点并开始拖拽 */
export function insertBendAtSegment(points, segmentIndex) {
  const pts = points.map((p) => ({ ...p }))
  const a = pts[segmentIndex]
  const b = pts[segmentIndex + 1]
  const mid = { x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 }
  pts.splice(segmentIndex + 1, 0, mid)
  return { points: pts, bendIndex: segmentIndex + 1 }
}

export function moveBendPoint(points, bendIndex, pos) {
  const pts = points.map((p) => ({ ...p }))
  if (bendIndex <= 0 || bendIndex >= pts.length - 1) return pts
  pts[bendIndex] = { x: pos.x, y: pos.y }
  return pts
}

export function removeBendPoint(points, bendIndex) {
  if (points.length <= 2) return points
  if (bendIndex <= 0 || bendIndex >= points.length - 1) return points
  return points.filter((_, i) => i !== bendIndex)
}

/** 根据连线路径端点推断节点连接点（自动布局导入 v2 时使用） */
export function inferHandleIdFromDirection(node, towardPoint) {
  if (isHubNodeType(node?.type)) return 'center-source'
  if (!node || !towardPoint) return 'right'
  const rect = nodeRect(node)
  const dx = towardPoint.x - rect.cx
  const dy = towardPoint.y - rect.cy
  if (Math.abs(dx) >= Math.abs(dy)) {
    return dx >= 0 ? 'right' : 'left'
  }
  return dy >= 0 ? 'bottom' : 'top'
}

export function inferHandleId(node, point) {
  if (!node || !point) return 'right'
  if (isHubNodeType(node.type)) return 'center-source'
  const handles = getNodeHandles(node)
  let bestId = handles[0]
  let bestDist = Infinity
  for (const handleId of handles) {
    const anchor = handleConnectionPoint(node, handleId)
    const dist = Math.hypot(anchor.x - point.x, anchor.y - point.y)
    if (dist < bestDist) {
      bestDist = dist
      bestId = handleId
    }
  }
  return bestId
}

function isNearHubCenter(node, point) {
  if (!node || !point || !isHubNodeType(node.type)) return false
  const rect = nodeRect(node)
  const r = Math.min(rect.width, rect.height) / 2
  return Math.hypot(point.x - rect.cx, point.y - rect.cy) <= r + 2
}

function hubHandleForEnd(end = 'source') {
  return end === 'target' ? 'center-target' : 'center-source'
}

function inferHandleForEndpoint(node, point, otherPoint, end = 'source') {
  if (!node || !point) return 'right'
  if (isHubNodeType(node.type)) return hubHandleForEnd(end)
  return inferHandleId(node, point)
}

function normalizeEdgePathPoints(edge, nodeById) {
  const raw = edge.data?.points || []
  if (raw.length < 2) return raw.map((p) => ({ ...p }))

  const points = raw.map((p) => ({ ...p }))
  const sourceNode = nodeById.get(edge.source)
  const targetNode = nodeById.get(edge.target)

  if (sourceNode && isHubNodeType(sourceNode.type)) {
    points[0] = handleConnectionPoint(sourceNode, 'center-source')
  }
  if (targetNode && isHubNodeType(targetNode.type)) {
    points[points.length - 1] = handleConnectionPoint(targetNode, 'center-target')
  }

  return points
}

function handleMatchesPoint(node, handleId, point, tolerance = 12) {
  if (!node || !handleId || !point) return false
  const anchor = handleConnectionPoint(node, handleId)
  return Math.hypot(anchor.x - point.x, anchor.y - point.y) <= tolerance
}

/** 从已有路径点补全/校正连线两端连接点（仅导入或缺失 handle 时使用） */
export function ensureEdgeHandles(edge, nodeById) {
  const points = normalizeEdgePathPoints(edge, nodeById)
  if (!points?.length) return edge

  const sourceNode = nodeById.get(edge.source)
  const targetNode = nodeById.get(edge.target)
  if (!sourceNode || !targetNode) return edge

  let sourceHandle = edge.sourceHandle || edge.data?.sourceHandle
  let targetHandle = edge.targetHandle || edge.data?.targetHandle
  const staleDefaults = sourceHandle === 'right' && targetHandle === 'left' && (
    !handleMatchesPoint(sourceNode, sourceHandle, points[0])
    || !handleMatchesPoint(targetNode, targetHandle, points[points.length - 1])
  )

  if (!sourceHandle || staleDefaults) {
    sourceHandle = inferHandleForEndpoint(sourceNode, points[0], points[points.length - 1], 'source')
  }
  if (!targetHandle || staleDefaults) {
    targetHandle = inferHandleForEndpoint(targetNode, points[points.length - 1], points[0], 'target')
  }

  return {
    ...edge,
    sourceHandle,
    targetHandle,
    data: {
      ...edge.data,
      points,
      sourceHandle,
      targetHandle
    }
  }
}

export function resolveEdgePoints(edge, nodeById) {
  const sourceNode = nodeById.get(edge.source)
  const targetNode = nodeById.get(edge.target)
  if (!sourceNode || !targetNode) return edge.data?.points || []

  const sourceHandle = edge.sourceHandle || edge.data?.sourceHandle
  const targetHandle = edge.targetHandle || edge.data?.targetHandle
  const sourceOut = sourceHandle
    ? handleConnectionPoint(sourceNode, sourceHandle)
    : handleConnectionPoint(sourceNode, 'right')
  const targetIn = targetHandle
    ? handleConnectionPoint(targetNode, targetHandle)
    : handleConnectionPoint(targetNode, 'left')

  const inner = Array.isArray(edge.data?.waypoints)
    ? edge.data.waypoints.map((p) => ({ ...p }))
    : (edge.data?.points?.length > 2 ? edge.data.points.slice(1, -1).map((p) => ({ ...p })) : [])

  if (inner.length) return [sourceOut, ...inner, targetIn]
  return [sourceOut, targetIn]
}

/** @param {{ inferHandles?: boolean }} options - inferHandles 仅用于导入/迁移，编辑中应传 false */
export function rebuildEdgePaths(nodes, edges, options = {}) {
  const { inferHandles = false } = options
  const nodeById = new Map(nodes.map((n) => [n.id, n]))
  return edges.map((edge) => {
    const working = inferHandles ? ensureEdgeHandles(edge, nodeById) : edge
    const points = resolveEdgePoints(working, nodeById)
    return {
      ...working,
      sourceHandle: working.sourceHandle,
      targetHandle: working.targetHandle,
      data: {
        ...working.data,
        points,
        path: pointsToSvgPath(points),
        waypoints: points.length > 2 ? points.slice(1, -1) : [],
        sourceHandle: working.sourceHandle,
        targetHandle: working.targetHandle
      }
    }
  })
}

/** 重连端点吸附：优先当前端所在节点上的连接点，再考虑其他节点 */
export function findReconnectHandleSnap(nodes, flowPoint, edge, end, options = {}) {
  const withEndAwareHubHandle = (snap) => {
    if (!snap) return snap
    const node = (nodes || []).find((item) => item.id === snap.nodeId)
    if (!isHubNodeType(node?.type)) return snap
    return {
      ...snap,
      handleId: hubHandleForEnd(end),
      point: handleConnectionPoint(node, hubHandleForEnd(end))
    }
  }
  const anchorNodeId = end === 'source' ? edge.source : edge.target
  const otherNodeId = end === 'source' ? edge.target : edge.source
  const anchorNode = (nodes || []).find((node) => node.id === anchorNodeId)
  if (anchorNode) {
    const local = findNearestHandle([anchorNode], flowPoint, { threshold: options.localThreshold ?? 56 })
    if (local) return withEndAwareHubHandle(local)
  }
  return withEndAwareHubHandle(findNearestHandle(nodes, flowPoint, {
    excludeNodeId: otherNodeId,
    threshold: options.remoteThreshold ?? 40
  }))
}


export function decorateForEditor(nodes, edges, options = {}) {
  const { editable = true, tool = 'select', edgeReconnect = false } = options
  const showHandles = editable && (tool === 'connect' || edgeReconnect)
  const canSelect = editable && (tool === 'select' || tool === 'pan')
  return {
    nodes: nodes.map((node) => {
      const isSelected = !!node.selected
      return {
        ...node,
        zIndex: isHubNodeType(node.type) ? 20 : node.zIndex,
        draggable: canSelect,
        selectable: canSelect,
        selected: isSelected,
        data: {
          ...node.data,
          editable: canSelect,
          showHandles,
          selected: isSelected,
          highlighted: isSelected
        }
      }
    }),
    edges: edges.map((edge) => {
      const isSelected = !!edge.selected
      return {
        ...edge,
        selectable: canSelect,
        selected: isSelected,
        data: {
          ...edge.data,
          editable,
          selected: isSelected
        }
      }
    })
  }
}

/** 从标注数据生成初始文档 */
export async function importFromAnnotation(propositions, relations) {
  if (!propositions?.length) return EMPTY_DOCUMENT()
  const layout = await layoutArgumentGraphWithElk(propositions, relations || [])
  const nodes = layout.nodes.map((n) => ({
    ...n,
    data: { ...n.data }
  }))
  const nodeById = new Map(nodes.map((n) => [n.id, n]))
  const edges = layout.edges.map((e) => {
    const points = e.data?.points || []
    return ensureEdgeHandles({
      ...e,
      type: 'polyline',
      data: {
        ...e.data,
        waypoints: points.length > 2 ? points.slice(1, -1).map((p) => ({ ...p })) : []
      }
    }, nodeById)
  })
  return {
    version: 2,
    nodes,
    edges: rebuildEdgePaths(nodes, edges)
  }
}

/**
 * 将手工编辑后的 v2 画布与最新标注数据同步：
 * - 当前命题/关系重新生成一份自动图，确保新增关系一定出现；
 * - 已存在的节点保留手动位置、尺寸和编辑器里的节点数据；
 * - 已存在的边保留手动连接点、折点和箭头设置；
 * - 已从标注中删除的命题/关系不再保留，避免旧图示覆盖新数据。
 */
export async function mergeDocumentWithAnnotation(existingDoc, propositions, relations) {
  const base = await importFromAnnotation(propositions, relations)
  if (existingDoc?.version !== 2 || !existingDoc?.nodes?.length) return base

  const validPropIds = new Set((propositions || []).map((p) => p.propId))
  const baseNodeIds = new Set(base.nodes.map((node) => node.id))
  const baseEdgeIds = new Set(base.edges.map((edge) => edge.id))
  const existingNodeById = new Map((existingDoc.nodes || []).map((node) => [node.id, node]))
  const existingEdgeById = new Map((existingDoc.edges || []).map((edge) => [edge.id, edge]))

  const nodes = base.nodes.map((node) => {
    const existing = existingNodeById.get(node.id)
    if (!existing) return node
    return {
      ...node,
      position: { ...(existing.position || node.position) },
      style: { ...(node.style || {}), ...(existing.style || {}) },
      data: {
        ...node.data,
        ...existing.data,
        stableId: node.data?.stableId,
        tag: node.data?.tag,
        text: node.data?.text,
        relKey: node.data?.relKey,
        relType: node.data?.relType,
        hubKind: node.data?.hubKind
      }
    }
  })

  ;(existingDoc.nodes || []).forEach((node) => {
    if (baseNodeIds.has(node.id)) return
    if (node.type === 'prop') {
      const members = getPropMemberIds(node)
      if (members.length > 1 && members.every((id) => validPropIds.has(id))) nodes.push(node)
    }
  })

  const nodeIds = new Set(nodes.map((node) => node.id))
  const edges = base.edges.map((edge) => {
    const existing = existingEdgeById.get(edge.id)
    if (!existing) return edge
    return {
      ...edge,
      source: nodeIds.has(existing.source) ? existing.source : edge.source,
      target: nodeIds.has(existing.target) ? existing.target : edge.target,
      sourceHandle: existing.sourceHandle || edge.sourceHandle,
      targetHandle: existing.targetHandle || edge.targetHandle,
      data: {
        ...edge.data,
        ...existing.data,
        relKey: edge.data?.relKey,
        directed: existing.data?.directed ?? edge.data?.directed ?? false,
        waypoints: existing.data?.waypoints || []
      }
    }
  })

  ;(existingDoc.edges || []).forEach((edge) => {
    if (baseEdgeIds.has(edge.id)) return
    if (!nodeIds.has(edge.source) || !nodeIds.has(edge.target)) return
    if (edge.data?.relKey) return
    const source = nodes.find((node) => node.id === edge.source)
    const target = nodes.find((node) => node.id === edge.target)
    if (isHubNodeType(source?.type) || isHubNodeType(target?.type)) return
    edges.push(edge)
  })

  return {
    version: 2,
    nodes,
    edges: rebuildEdgePaths(nodes, edges)
  }
}

export function removeRelationsFromDocument(doc, relationIds = []) {
  if (doc?.version !== 2 || !relationIds?.length) return doc
  const relSet = new Set(relationIds.map(String))
  const removedNodeIds = new Set(
    (doc.nodes || [])
      .filter((node) => node.data?.relKey && relSet.has(String(node.data.relKey)))
      .map((node) => node.id)
  )
  const nodes = (doc.nodes || []).filter((node) => !removedNodeIds.has(node.id))
  const nodeIds = new Set(nodes.map((node) => node.id))
  const edges = (doc.edges || []).filter((edge) => {
    if (!nodeIds.has(edge.source) || !nodeIds.has(edge.target)) return false
    if (edge.data?.relKey && relSet.has(String(edge.data.relKey))) return false
    return true
  })
  return {
    ...doc,
    nodes,
    edges: rebuildEdgePaths(nodes, edges)
  }
}

/** v1 layoutOverride 迁移到 v2 */
export async function migrateLayoutToDocument(graphLayout, propositions, relations) {
  if (graphLayout?.version === 2 && graphLayout.nodes?.length) {
    const doc = cloneDocument(graphLayout)
    return {
      ...doc,
      edges: rebuildEdgePaths(doc.nodes, doc.edges, { inferHandles: true })
    }
  }
  const base = await importFromAnnotation(propositions, relations)
  if (!graphLayout || graphLayout.version !== 1) return base

  const nodeById = new Map(base.nodes.map((n) => [n.id, n]))
  base.nodes = base.nodes.map((node) => {
    const pos = graphLayout.nodePositions?.[node.id]
    return pos ? { ...node, position: { x: pos.x, y: pos.y } } : node
  })

  base.edges = base.edges.map((edge) => {
    const style = graphLayout.edgeStyles?.[edge.id] || {}
    return {
      ...edge,
      sourceHandle: style.sourceHandle || edge.sourceHandle,
      targetHandle: style.targetHandle || edge.targetHandle,
      data: {
        ...edge.data,
        waypoints: style.waypoints || edge.data?.waypoints || [],
        directed: style.directed ?? edge.data?.directed ?? false,
        sourceHandle: style.sourceHandle,
        targetHandle: style.targetHandle
      }
    }
  })

  return { version: 2, nodes: base.nodes, edges: rebuildEdgePaths(base.nodes, base.edges) }
}

export function documentSummary(doc) {
  const propCount = (doc?.nodes || []).filter((n) => n.type === 'prop').length
  const hubCount = (doc?.nodes || []).filter((n) => String(n.type || '').startsWith('hub-')).length
  return {
    props: propCount,
    hubs: hubCount,
    edges: (doc?.edges || []).length
  }
}

export { pointsToSvgPath, orthogonalPath, cloneLayout }
