/**
 * 裁判文书论证图生成器。
 *
 * 数据来源与图示语义：
 * - proposition.displayId -> 矩形命题节点。
 * - argument_relation.displayId / relationType -> 关系表达式和关系枢纽节点。
 * - relation_member.memberOrder -> 关系成员顺序。
 * - relation_member.memberType=P/R -> 成员引用命题或内层关系。
 *
 * 绘图规则：
 * - S：理由 -> ● -> 结论，第二段带箭头。
 * - A：反对方 -> ○ -> 被反对方，第二段带箭头。
 * - M：个别判断 -> ⊕ -> 一般判断，第二段带箭头；若个别判断本身是 J，则复用 J 的 ⊕。
 * - J：多个成员无向汇聚到 ⊕；二元横排，多元默认上方汇聚，若成员已在主链中则贴近该成员侧边展开。
 * - I：不画关系圆，把同一组命题合并为同一个矩形内的 P1 / P2。
 */

const PROP_H = 34
const PROP_MIN_W = 58
const HUB_SIZE = { S: 14, A: 18, M: 24, J: 24 }
const GAP = {
  inline: 42,
  relation: 76,
  member: 54,
  row: 72,
  verticalRelation: 58,
  component: 92,
  padding: 48
}

function relationType(rel) {
  return String(rel?.type || 'S').toUpperCase()
}

function relationId(rel, index) {
  return rel?.relId || `R${index + 1}`
}

function relationKey(rel, index) {
  return relationId(rel, index)
}

function relationMembers(rel) {
  return (rel?.members?.length ? rel.members : [rel?.source, rel?.target]).filter(Boolean)
}

function propLabel(p) {
  return p?.propId || `P${p?.sequenceNo || ''}`
}

function propBoxSize(label) {
  return { width: Math.max(PROP_MIN_W, String(label).length * 10 + 24), height: PROP_H }
}

class UnionFind {
  constructor(ids) {
    this.parent = new Map(ids.map((id) => [id, id]))
  }

  find(id) {
    if (!this.parent.has(id)) this.parent.set(id, id)
    let root = id
    while (this.parent.get(root) !== root) root = this.parent.get(root)
    let cursor = id
    while (this.parent.get(cursor) !== cursor) {
      const next = this.parent.get(cursor)
      this.parent.set(cursor, root)
      cursor = next
    }
    return root
  }

  union(a, b) {
    const ra = this.find(a)
    const rb = this.find(b)
    if (ra !== rb) this.parent.set(rb, ra)
  }
}

function createIdentityGroups(propositions, relations) {
  const propIds = propositions.map((p) => p.propId)
  const propSet = new Set(propIds)
  const uf = new UnionFind(propIds)

  relations
    .filter((rel) => relationType(rel) === 'I')
    .forEach((rel) => {
      const ids = relationMembers(rel).filter((id) => propSet.has(id))
      for (let i = 1; i < ids.length; i += 1) uf.union(ids[0], ids[i])
    })

  return uf
}

function collectVisiblePropIds(relations) {
  const visible = new Set()
  const relMap = new Map(relations.map((rel, index) => [relationKey(rel, index), rel]))
  const visit = (memberId, seen = new Set()) => {
    if (String(memberId).startsWith('P')) {
      visible.add(memberId)
      return
    }
    if (seen.has(memberId)) return
    const rel = relMap.get(memberId)
    if (!rel) return
    seen.add(memberId)
    relationMembers(rel).forEach((id) => visit(id, seen))
  }
  relations.forEach((rel, index) => visit(relationKey(rel, index)))
  return visible
}

function filterGraphRelations(relations) {
  const relIdSet = new Set(relations.map((rel, index) => relationKey(rel, index)))
  return relations.filter((rel) => {
    const type = relationType(rel)
    const members = relationMembers(rel)
    if (type === 'I') return members.filter((id) => String(id).startsWith('P')).length >= 2
    if (type === 'J') return members.length >= 2
    if (['S', 'A', 'M'].includes(type)) return members.length >= 2
    return members.some((id) => relIdSet.has(id) || String(id).startsWith('P'))
  })
}

function flowNode(id, type, label, x, y, width, height, extraData = {}) {
  return {
    id,
    type,
    position: { x, y },
    data: { label, hubKind: type, ...extraData },
    style: { width: `${width}px`, height: `${height}px` },
    draggable: false,
    selectable: false,
    connectable: false
  }
}

function pointsToSvgPath(points) {
  if (!points.length) return ''
  return points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
}

function flowEdge(id, source, target, directed, points) {
  return {
    id,
    source,
    target,
    type: 'elk-orthogonal',
    animated: false,
    selectable: false,
    data: {
      points,
      path: pointsToSvgPath(points),
      directed
    }
  }
}

function rectAt(x, y, width, height) {
  return {
    x,
    y,
    width,
    height,
    cx: x + width / 2,
    cy: y + height / 2
  }
}

function pointOnRectToward(rect, toward) {
  const dx = toward.x - rect.cx
  const dy = toward.y - rect.cy
  if (Math.abs(dx) >= Math.abs(dy)) {
    return {
      x: dx >= 0 ? rect.x + rect.width : rect.x,
      y: rect.cy
    }
  }
  return {
    x: rect.cx,
    y: dy >= 0 ? rect.y + rect.height : rect.y
  }
}

function orthogonal(from, to) {
  if (Math.abs(from.x - to.x) < 0.5 || Math.abs(from.y - to.y) < 0.5) return [from, to]
  const midX = (from.x + to.x) / 2
  return [from, { x: midX, y: from.y }, { x: midX, y: to.y }, to]
}

function translatePoint(p, dx, dy) {
  return { x: p.x + dx, y: p.y + dy }
}

function translateUnit(unit, dx, dy) {
  const translateAttach = (attach) => ({
    ...attach,
    point: translatePoint(attach.point, dx, dy),
    rect: attach.rect
      ? rectAt(attach.rect.x + dx, attach.rect.y + dy, attach.rect.width, attach.rect.height)
      : null
  })

  return {
    ...unit,
    nodes: unit.nodes.map((n) => ({
      ...n,
      position: { x: n.position.x + dx, y: n.position.y + dy }
    })),
    edges: unit.edges.map((e) => ({
      ...e,
      data: {
        ...e.data,
        points: e.data.points.map((p) => translatePoint(p, dx, dy)),
        path: pointsToSvgPath(e.data.points.map((p) => translatePoint(p, dx, dy)))
      }
    })),
    attach: translateAttach(unit.attach),
    sourceAttach: unit.sourceAttach ? translateAttach(unit.sourceAttach) : undefined
  }
}

function mergeUnits(units) {
  return {
    nodes: units.flatMap((u) => u.nodes),
    edges: units.flatMap((u) => u.edges)
  }
}

function nodeRect(node) {
  const width = parseFloat(node.style.width) || PROP_MIN_W
  const height = parseFloat(node.style.height) || PROP_H
  return rectAt(node.position.x, node.position.y, width, height)
}

function mergeDuplicatePropNodes(nodes, edges) {
  const nextNodes = nodes.map((node) => ({
    ...node,
    position: { ...node.position },
    data: { ...node.data }
  }))
  const nextEdges = edges.map((edge) => ({
    ...edge,
    data: {
      ...edge.data,
      points: [...(edge.data?.points || [])]
    }
  }))

  const canonicalByStableId = new Map()
  const duplicateToCanonical = new Map()
  const canonicalRects = new Map()
  const removed = new Set()
  const nodeById = new Map(nextNodes.map((node) => [node.id, node]))

  nextNodes.forEach((node) => {
    if (node.type !== 'prop' || !node.data?.stableId) return
    const stableId = node.data.stableId
    if (!canonicalByStableId.has(stableId)) {
      canonicalByStableId.set(stableId, node.id)
      canonicalRects.set(node.id, nodeRect(node))
      return
    }
    duplicateToCanonical.set(node.id, canonicalByStableId.get(stableId))
    removed.add(node.id)
  })

  if (!duplicateToCanonical.size) return { nodes: nextNodes, edges: nextEdges }

  const canonicalIds = new Set(duplicateToCanonical.values())
  const adjacency = new Map()
  nextEdges.forEach((edge) => {
    if (!adjacency.has(edge.source)) adjacency.set(edge.source, [])
    if (!adjacency.has(edge.target)) adjacency.set(edge.target, [])
    adjacency.get(edge.source).push(edge.target)
    adjacency.get(edge.target).push(edge.source)
  })

  const shiftedStarts = new Set()
  const shiftComponent = (startId, dx) => {
    if (!startId || removed.has(startId) || canonicalIds.has(startId) || Math.abs(dx) < 0.5) return
    const key = `${startId}:${Math.round(dx)}`
    if (shiftedStarts.has(key)) return
    shiftedStarts.add(key)

    const queue = [startId]
    const component = new Set()
    while (queue.length) {
      const id = queue.shift()
      if (component.has(id) || removed.has(id) || canonicalIds.has(id)) continue
      component.add(id)
      ;(adjacency.get(id) || []).forEach((next) => {
        if (!removed.has(next) && !canonicalIds.has(next)) queue.push(next)
      })
    }

    component.forEach((id) => {
      const node = nodeById.get(id)
      if (node) node.position.x += dx
    })

    nextEdges.forEach((edge) => {
      if (!component.has(edge.source) && !component.has(edge.target)) return
      edge.data.points = edge.data.points.map((p) => ({ x: p.x + dx, y: p.y }))
      edge.data.path = pointsToSvgPath(edge.data.points)
    })
  }

  nextEdges.forEach((edge) => {
    if (!duplicateToCanonical.has(edge.source)) return
    const canonical = nodeById.get(duplicateToCanonical.get(edge.source))
    const target = nodeById.get(edge.target)
    if (!canonical || !target || removed.has(edge.target)) return
    shiftComponent(edge.target, nodeRect(canonical).cx - nodeRect(target).cx)
  })

  const reroutedEdges = nextEdges.map((edge) => {
    const source = duplicateToCanonical.get(edge.source) || edge.source
    const target = duplicateToCanonical.get(edge.target) || edge.target
    const points = [...(edge.data?.points || [])]
    const sourceChanged = duplicateToCanonical.has(edge.source)
    const targetChanged = duplicateToCanonical.has(edge.target)

    if (sourceChanged && points.length >= 2) {
      const rect = canonicalRects.get(source)
      if (rect) points[0] = pointOnRectToward(rect, points[points.length - 1])
    }
    if (targetChanged && points.length >= 2) {
      const rect = canonicalRects.get(target)
      if (rect) points[points.length - 1] = pointOnRectToward(rect, points[0])
    }
    const nextPoints = sourceChanged || targetChanged ? orthogonal(points[0], points[points.length - 1]) : points

    return {
      ...edge,
      source,
      target,
      data: {
        ...edge.data,
        points: nextPoints,
        path: pointsToSvgPath(nextPoints)
      }
    }
  })

  return {
    nodes: nextNodes.filter((node) => !removed.has(node.id)),
    edges: reroutedEdges
  }
}

function boundsFromNodes(nodes) {
  let maxX = 0
  let maxY = 0
  nodes.forEach((n) => {
    const w = parseFloat(n.style.width) || PROP_MIN_W
    const h = parseFloat(n.style.height) || PROP_H
    maxX = Math.max(maxX, n.position.x + w)
    maxY = Math.max(maxY, n.position.y + h)
  })
  return {
    width: Math.max(maxX + GAP.padding, 360),
    height: Math.max(maxY + GAP.padding, 240)
  }
}

function buildManualGraph(propositions = [], relations = []) {
  const rels = filterGraphRelations(relations || [])
  const visiblePropIds = collectVisiblePropIds(rels)
  const visibleProps = (propositions || []).filter((p) => visiblePropIds.has(p.propId))
  if (!visibleProps.length && !rels.length) {
    return { nodes: [], edges: [], bounds: { width: 400, height: 220 } }
  }

  const propMap = new Map(visibleProps.map((p) => [p.propId, p]))
  const relMap = new Map(rels.map((rel, index) => [relationKey(rel, index), rel]))
  const identityUf = createIdentityGroups(visibleProps, rels)
  const groupedProps = new Map()
  visibleProps.forEach((p) => {
    const root = identityUf.find(p.propId)
    if (!groupedProps.has(root)) groupedProps.set(root, [])
    groupedProps.get(root).push(p)
  })

  const referencedRelations = new Set()
  rels.forEach((rel) => {
    relationMembers(rel).forEach((id) => {
      if (String(id).startsWith('R')) referencedRelations.add(id)
    })
  })

  const ctx = { nextNode: 1, nextEdge: 1, propMap, relMap, identityUf, groupedProps }

  function uniqueNodeId(prefix, stableId) {
    return `${prefix}-${stableId}-${ctx.nextNode++}`
  }

  function uniqueEdgeId(prefix, stableId) {
    return `${prefix}-${stableId}-${ctx.nextEdge++}`
  }

  function makePropUnit(propId) {
    const root = ctx.identityUf.find(propId)
    const items = [...(ctx.groupedProps.get(root) || [ctx.propMap.get(propId)])]
      .filter(Boolean)
      .sort((a, b) => (a.sequenceNo || 0) - (b.sequenceNo || 0))
    const label = items.map(propLabel).join(' / ')
    const size = propBoxSize(label)
    const nodeId = uniqueNodeId('prop', root)
    const rect = rectAt(0, 0, size.width, size.height)
    return {
      width: size.width,
      height: size.height,
      nodes: [flowNode(nodeId, 'prop', label, 0, 0, size.width, size.height, { stableId: root })],
      edges: [],
      attach: { nodeId, point: { x: rect.cx, y: rect.cy }, rect }
    }
  }

  function makeHub(kind, key) {
    const size = HUB_SIZE[kind]
    const hubKind = kind === 'S' ? 'hub-s' : kind === 'A' ? 'hub-a' : kind === 'J' ? 'hub-j' : 'hub-m'
    const nodeId = uniqueNodeId(`hub-${kind.toLowerCase()}`, key)
    const rect = rectAt(0, 0, size, size)
    return {
      width: size,
      height: size,
      nodes: [flowNode(nodeId, hubKind, kind === 'S' || kind === 'A' ? '' : '+', 0, 0, size, size, { relKey: key, relType: kind })],
      edges: [],
      attach: { nodeId, point: { x: rect.cx, y: rect.cy }, rect }
    }
  }

  function memberUnit(memberId, stack) {
    if (String(memberId).startsWith('P')) {
      if (!ctx.propMap.has(memberId)) return null
      return makePropUnit(memberId)
    }
    if (String(memberId).startsWith('R')) {
      return makeRelationUnit(memberId, stack)
    }
    return null
  }

  function makeJUnit(key, rel, stack) {
    const members = relationMembers(rel).map((id) => memberUnit(id, stack)).filter(Boolean)
    if (members.length < 2) return null

    const hub = makeHub('J', key)
    const isBinary = members.length === 2

    if (isBinary) {
      const left = translateUnit(members[0], 0, 0)
      const hubX = left.width + GAP.inline
      const hubY = Math.max(0, (left.height - hub.height) / 2)
      const placedHub = translateUnit(hub, hubX, hubY)
      const rightX = hubX + hub.width + GAP.inline
      const rightY = Math.max(0, (left.height - members[1].height) / 2)
      const right = translateUnit(members[1], rightX, rightY)
      const rowHeight = Math.max(left.height, right.height, hub.height)

      const leftOut = pointOnRectToward(left.attach.rect, placedHub.attach.point)
      const rightIn = pointOnRectToward(right.attach.rect, placedHub.attach.point)
      const edges = [
        flowEdge(uniqueEdgeId('j-in', key), left.attach.nodeId, placedHub.attach.nodeId, false, [leftOut, placedHub.attach.point]),
        flowEdge(uniqueEdgeId('j-out', key), placedHub.attach.nodeId, right.attach.nodeId, false, [placedHub.attach.point, rightIn])
      ]
      const merged = mergeUnits([left, placedHub, right])
      return {
        width: rightX + right.width,
        height: rowHeight,
        nodes: merged.nodes,
        edges: [...merged.edges, ...edges],
        attach: placedHub.attach
      }
    }

    let x = 0
    const rowUnits = members.map((unit) => {
      const placed = translateUnit(unit, x, 0)
      x += unit.width + GAP.member
      return placed
    })
    const rowWidth = x - GAP.member
    const rowHeight = Math.max(...rowUnits.map((u) => u.height))
    const hubX = rowWidth / 2 - hub.width / 2
    const hubY = rowHeight + GAP.row
    const placedHub = translateUnit(hub, hubX, hubY)
    const busY = hubY + hub.height / 2

    const jEdges = rowUnits.map((unit) => {
      const from = pointOnRectToward(unit.attach.rect, { x: unit.attach.point.x, y: busY })
      const to = placedHub.attach.point
      const points = Math.abs(from.x - to.x) < 0.5
        ? [from, to]
        : [from, { x: from.x, y: busY }, { x: to.x, y: busY }, to]
      return flowEdge(uniqueEdgeId('j-member', key), unit.attach.nodeId, placedHub.attach.nodeId, false, points)
    })
    const merged = mergeUnits([...rowUnits, placedHub])
    return {
      width: Math.max(rowWidth, hubX + hub.width),
      height: hubY + hub.height,
      nodes: merged.nodes,
      edges: [...merged.edges, ...jEdges],
      attach: placedHub.attach
    }
  }

  function makeDirectedUnit(key, rel, stack) {
    const type = relationType(rel)
    const members = relationMembers(rel)
    const source = memberUnit(members[0], stack)
    const target = memberUnit(members[1], stack)
    if (!source || !target) return null

    if (type === 'M' && relationType(ctx.relMap.get(members[0])) === 'J') {
      return makeMergedMatchUnit(key, source, target)
    }

    return makeVerticalDirectedUnit(key, type, source, target)
  }

  function makeMergedMatchUnit(key, source, target) {
    const width = Math.max(source.width, target.width)
    const sourceX = (width - source.width) / 2
    const placedSource = translateUnit(source, sourceX, 0)
    const targetX = placedSource.attach.point.x - target.width / 2
    const targetY = Math.max(placedSource.height + GAP.verticalRelation, placedSource.attach.point.y + GAP.verticalRelation + target.height / 2)
    const placedTarget = translateUnit(target, targetX, targetY)
    const hubOut = placedSource.attach.rect
      ? { x: placedSource.attach.point.x, y: placedSource.attach.rect.y + placedSource.attach.rect.height }
      : placedSource.attach.point
    const targetIn = placedTarget.attach.rect
      ? pointOnRectToward(placedTarget.attach.rect, hubOut)
      : placedTarget.attach.point
    const edge = flowEdge(
      uniqueEdgeId('M-merged-out', key),
      placedSource.attach.nodeId,
      placedTarget.attach.nodeId,
      true,
      orthogonal(hubOut, targetIn)
    )
    const merged = mergeUnits([placedSource, placedTarget])
    return {
      width: Math.max(width, targetX + target.width),
      height: targetY + target.height,
      nodes: merged.nodes,
      edges: [...merged.edges, edge],
      attach: placedTarget.attach,
      sourceAttach: placedTarget.attach
    }
  }

  function makeVerticalDirectedUnit(key, type, source, target) {
    const hub = makeHub(type, key)
    const width = Math.max(source.width, target.width, hub.width)
    const sourceX = (width - source.width) / 2
    const hubX = (width - hub.width) / 2
    const targetX = (width - target.width) / 2
    const placedSource = translateUnit(source, sourceX, 0)
    const hubY = source.height + GAP.verticalRelation
    const placedHub = translateUnit(hub, hubX, hubY)
    const targetY = hubY + hub.height + GAP.verticalRelation
    const placedTarget = translateUnit(target, targetX, targetY)

    const placedSourceAttach = placedSource.sourceAttach || placedSource.attach
    const sourceOut = placedSourceAttach.rect
      ? pointOnRectToward(placedSourceAttach.rect, placedHub.attach.point)
      : placedSourceAttach.point
    const targetIn = placedTarget.attach.rect
      ? pointOnRectToward(placedTarget.attach.rect, placedHub.attach.point)
      : placedTarget.attach.point
    const hubOut = { x: placedHub.attach.point.x, y: placedHub.attach.rect.y + placedHub.attach.rect.height }

    const edges = [
      flowEdge(uniqueEdgeId(`${type}-in`, key), placedSourceAttach.nodeId, placedHub.attach.nodeId, false, orthogonal(sourceOut, placedHub.attach.point)),
      flowEdge(uniqueEdgeId(`${type}-out`, key), placedHub.attach.nodeId, placedTarget.attach.nodeId, true, orthogonal(hubOut, targetIn))
    ]
    const merged = mergeUnits([placedSource, placedHub, placedTarget])
    return {
      width,
      height: targetY + target.height,
      nodes: merged.nodes,
      edges: [...merged.edges, ...edges],
      attach: placedHub.attach,
      sourceAttach: placedTarget.attach
    }
  }

  function makeRelationUnit(key, stack = new Set()) {
    if (stack.has(key)) return null
    const rel = ctx.relMap.get(key)
    if (!rel) return null
    const nextStack = new Set(stack)
    nextStack.add(key)
    const type = relationType(rel)
    if (type === 'I') {
      const propId = relationMembers(rel).find((id) => String(id).startsWith('P'))
      return propId ? makePropUnit(propId) : null
    }
    if (type === 'J') return makeJUnit(key, rel, nextStack)
    if (['S', 'A', 'M'].includes(type)) return makeDirectedUnit(key, rel, nextStack)
    return null
  }

  let topKeys = rels
    .map((rel, index) => relationKey(rel, index))
    .filter((key) => relationType(ctx.relMap.get(key)) !== 'I' && !referencedRelations.has(key))

  if (!topKeys.length) {
    topKeys = rels
      .map((rel, index) => relationKey(rel, index))
      .filter((key) => relationType(ctx.relMap.get(key)) !== 'I')
  }

  const units = []
  let y = GAP.padding
  topKeys.forEach((key) => {
    const unit = makeRelationUnit(key)
    if (!unit) return
    units.push(translateUnit(unit, GAP.padding, y))
    y += unit.height + GAP.component
  })

  const onlyIdentity = rels.filter((rel) => relationType(rel) === 'I')
  if (!units.length && onlyIdentity.length) {
    onlyIdentity.forEach((rel) => {
      const propId = relationMembers(rel).find((id) => String(id).startsWith('P'))
      if (!propId) return
      const unit = makePropUnit(propId)
      units.push(translateUnit(unit, GAP.padding, y))
      y += unit.height + GAP.component
    })
  }

  const merged = mergeUnits(units)
  const deduped = mergeDuplicatePropNodes(merged.nodes, merged.edges)
  const optimizedOnce = optimizeDirectedHubs(deduped.nodes, deduped.edges)
  const optimized = optimizeDirectedHubs(optimizedOnce.nodes, optimizedOnce.edges)
  const branched = optimizeSharedSourceBranches(optimized.nodes, optimized.edges)
  const combined = optimizeAnchoredCombinationHubs(branched.nodes, branched.edges)
  const directTargets = optimizeDirectHubTargets(combined.nodes, combined.edges)
  const reoptimizedOnce = optimizeDirectedHubs(directTargets.nodes, directTargets.edges)
  const reoptimized = optimizeDirectedHubs(reoptimizedOnce.nodes, reoptimizedOnce.edges)
  const finalBranches = optimizeSharedSourceBranches(reoptimized.nodes, reoptimized.edges)
  const targetBranches = optimizeSharedTargetBranches(finalBranches.nodes, finalBranches.edges)
  const compacted = compactDisconnectedComponents(targetBranches.nodes, targetBranches.edges)
  const normalized = normalizeGraph(compacted.nodes, compacted.edges)
  return {
    ...normalized,
    bounds: boundsFromNodes(normalized.nodes)
  }
}

function compactDisconnectedComponents(nodes, edges) {
  if (nodes.length < 2) return { nodes, edges }

  const nextNodes = nodes.map((node) => ({
    ...node,
    position: { ...node.position },
    data: { ...node.data }
  }))
  const nextEdges = edges.map((edge) => ({
    ...edge,
    data: {
      ...edge.data,
      points: [...(edge.data?.points || [])]
    }
  }))
  const nodeById = new Map(nextNodes.map((node) => [node.id, node]))
  const adjacency = new Map(nextNodes.map((node) => [node.id, []]))

  nextEdges.forEach((edge) => {
    if (!adjacency.has(edge.source) || !adjacency.has(edge.target)) return
    adjacency.get(edge.source).push(edge.target)
    adjacency.get(edge.target).push(edge.source)
  })

  const visited = new Set()
  const components = []
  nextNodes.forEach((node) => {
    if (visited.has(node.id)) return
    const queue = [node.id]
    const ids = []
    visited.add(node.id)
    while (queue.length) {
      const id = queue.shift()
      ids.push(id)
      ;(adjacency.get(id) || []).forEach((next) => {
        if (visited.has(next)) return
        visited.add(next)
        queue.push(next)
      })
    }
    const rects = ids.map((id) => nodeRect(nodeById.get(id)))
    components.push({
      ids: new Set(ids),
      minY: Math.min(...rects.map((r) => r.y)),
      maxY: Math.max(...rects.map((r) => r.y + r.height))
    })
  })

  if (components.length < 2) return { nodes: nextNodes, edges: nextEdges }

  components.sort((a, b) => a.minY - b.minY)
  const componentGap = 36
  let cursorY = components[0].minY
  const shiftByNode = new Map()

  components.forEach((component, index) => {
    if (index === 0) {
      cursorY = component.maxY + componentGap
      return
    }
    const dy = cursorY - component.minY
    component.ids.forEach((id) => shiftByNode.set(id, dy))
    cursorY += component.maxY - component.minY + componentGap
  })

  nextNodes.forEach((node) => {
    const dy = shiftByNode.get(node.id) || 0
    node.position.y += dy
  })

  nextEdges.forEach((edge) => {
    const sourceDy = shiftByNode.get(edge.source) || 0
    const targetDy = shiftByNode.get(edge.target) || 0
    const dy = sourceDy === targetDy ? sourceDy : 0
    if (!dy) return
    edge.data.points = edge.data.points.map((p) => ({ x: p.x, y: p.y + dy }))
    edge.data.path = pointsToSvgPath(edge.data.points)
  })

  return { nodes: nextNodes, edges: nextEdges }
}

function rerouteEdgeBetweenNodes(edge, nodeById) {
  const source = nodeById.get(edge.source)
  const target = nodeById.get(edge.target)
  if (!source || !target) return
  const sourcePoint = connectionPoint(source, nodeCenter(target))
  const targetPoint = connectionPoint(target, nodeCenter(source))
  edge.data.points = orthogonal(sourcePoint, targetPoint)
  edge.data.path = pointsToSvgPath(edge.data.points)
}

function optimizeAnchoredCombinationHubs(nodes, edges) {
  const nextNodes = nodes.map((node) => ({
    ...node,
    position: { ...node.position },
    data: { ...node.data }
  }))
  const nextEdges = edges.map((edge) => ({
    ...edge,
    data: {
      ...edge.data,
      points: [...(edge.data?.points || [])]
    }
  }))
  const nodeById = new Map(nextNodes.map((node) => [node.id, node]))
  const directedTargets = new Set()
  const degree = new Map(nextNodes.map((node) => [node.id, 0]))

  nextEdges.forEach((edge) => {
    degree.set(edge.source, (degree.get(edge.source) || 0) + 1)
    degree.set(edge.target, (degree.get(edge.target) || 0) + 1)
    if (edge.data?.directed) directedTargets.add(edge.target)
  })

  nextNodes
    .filter((node) => node.type === 'hub-j')
    .forEach((hub) => {
      const memberEdges = nextEdges.filter((edge) => (
        !edge.data?.directed &&
        (edge.source === hub.id || edge.target === hub.id)
      ))
      if (memberEdges.length < 2) return

      const members = memberEdges
        .map((edge) => nodeById.get(edge.source === hub.id ? edge.target : edge.source))
        .filter(Boolean)
      const anchor = members.find((node) => node.type === 'prop' && directedTargets.has(node.id))
      if (!anchor) return

      const anchorRect = nodeRect(anchor)
      const hubRect = nodeRect(hub)
      const hubCenter = {
        x: anchorRect.x + anchorRect.width + GAP.inline + hubRect.width / 2,
        y: anchorRect.cy
      }
      moveNodeCenter(hub, hubCenter)

      const movableMembers = members.filter((node) => (
        node.id !== anchor.id &&
        node.type === 'prop' &&
        !directedTargets.has(node.id) &&
        (degree.get(node.id) || 0) <= 1
      ))

      movableMembers.forEach((member, index) => {
        const memberRect = nodeRect(member)
        const rightX = hubCenter.x + hubRect.width / 2 + GAP.inline + memberRect.width / 2
        const upperY = hubCenter.y - Math.max(GAP.row, memberRect.height + 44)
        const lowerY = hubCenter.y + Math.max(GAP.row, memberRect.height + 44)
        const slots = [
          { x: rightX, y: hubCenter.y },
          { x: hubCenter.x, y: upperY },
          { x: hubCenter.x, y: lowerY },
          { x: rightX + (memberRect.width + GAP.member) * Math.ceil(index / 3), y: upperY },
          { x: rightX + (memberRect.width + GAP.member) * Math.ceil(index / 3), y: lowerY }
        ]
        moveNodeCenter(member, slots[index] || slots[slots.length - 1])
      })

      const changedIds = new Set([hub.id, ...movableMembers.map((node) => node.id)])
      nextEdges.forEach((edge) => {
        if (!changedIds.has(edge.source) && !changedIds.has(edge.target)) return
        rerouteEdgeBetweenNodes(edge, nodeById)
      })
    })

  return { nodes: nextNodes, edges: nextEdges }
}

function optimizeDirectHubTargets(nodes, edges) {
  const nextNodes = nodes.map((node) => ({
    ...node,
    position: { ...node.position },
    data: { ...node.data }
  }))
  const nextEdges = edges.map((edge) => ({
    ...edge,
    data: {
      ...edge.data,
      points: [...(edge.data?.points || [])]
    }
  }))
  const nodeById = new Map(nextNodes.map((node) => [node.id, node]))
  const degree = new Map(nextNodes.map((node) => [node.id, 0]))
  const adjacency = new Map(nextNodes.map((node) => [node.id, []]))
  nextEdges.forEach((edge) => {
    degree.set(edge.source, (degree.get(edge.source) || 0) + 1)
    degree.set(edge.target, (degree.get(edge.target) || 0) + 1)
    adjacency.get(edge.source)?.push(edge.target)
    adjacency.get(edge.target)?.push(edge.source)
  })

  nextEdges.forEach((edge) => {
    if (!edge.data?.directed) return
    const source = nodeById.get(edge.source)
    const target = nodeById.get(edge.target)
    if (!source || !target) return
    if (!source.type?.startsWith('hub') || target.type !== 'prop') return

    const sourceRect = nodeRect(source)
    const targetRect = nodeRect(target)
    const targetCenter = {
      x: sourceRect.cx,
      y: sourceRect.y + sourceRect.height + GAP.verticalRelation * 2 + targetRect.height / 2
    }

    if ((degree.get(target.id) || 0) > 1) {
      const currentCenter = nodeCenter(target)
      const dx = targetCenter.x - currentCenter.x
      const dy = targetCenter.y - currentCenter.y
      const shifted = collectReachableNodeIds(target.id, source.id, adjacency)
      shifted.forEach((id) => {
        const node = nodeById.get(id)
        if (!node) return
        node.position.x += dx
        node.position.y += dy
      })
      nextEdges.forEach((candidate) => {
        if (shifted.has(candidate.source) || shifted.has(candidate.target) || candidate.id === edge.id) {
          rerouteEdgeBetweenNodes(candidate, nodeById)
        }
      })
      return
    }

    moveNodeCenter(target, targetCenter)
    rerouteEdgeBetweenNodes(edge, nodeById)
  })

  return { nodes: nextNodes, edges: nextEdges }
}

function collectReachableNodeIds(startId, blockedId, adjacency) {
  const queue = [startId]
  const visited = new Set()
  while (queue.length) {
    const id = queue.shift()
    if (visited.has(id) || id === blockedId) continue
    visited.add(id)
    ;(adjacency.get(id) || []).forEach((next) => {
      if (next !== blockedId && !visited.has(next)) queue.push(next)
    })
  }
  return visited
}

function optimizeSharedSourceBranches(nodes, edges) {
  const nextNodes = nodes.map((node) => ({
    ...node,
    position: { ...node.position },
    data: { ...node.data }
  }))
  const nextEdges = edges.map((edge) => ({
    ...edge,
    data: {
      ...edge.data,
      points: [...(edge.data?.points || [])]
    }
  }))
  const nodeById = new Map(nextNodes.map((node) => [node.id, node]))
  const outgoingBySource = new Map()

  nextEdges.forEach((edge) => {
    if (edge.data?.directed) return
    const source = nodeById.get(edge.source)
    const hub = nodeById.get(edge.target)
    if (!source || !hub || source.type !== 'prop' || !['hub-s', 'hub-a', 'hub-m'].includes(hub.type)) return
    const outEdge = nextEdges.find((candidate) => candidate.source === hub.id && candidate.data?.directed)
    const target = outEdge ? nodeById.get(outEdge.target) : null
    if (!outEdge || !target || target.type !== 'prop') return
    if (!outgoingBySource.has(source.id)) outgoingBySource.set(source.id, [])
    outgoingBySource.get(source.id).push({ inEdge: edge, hub, outEdge, target })
  })

  outgoingBySource.forEach((branches, sourceId) => {
    if (branches.length < 2) return
    const source = nodeById.get(sourceId)
    if (!source) return
    const sourceRect = nodeRect(source)
    const sourceCenter = nodeCenter(source)
    const branchGap = 150
    const targetTop = sourceRect.y + sourceRect.height + 118

    branches
      .sort((a, b) => String(a.target.data?.label || '').localeCompare(String(b.target.data?.label || '')))
      .forEach((branch, index) => {
        const layout = branchLayout(index, sourceRect, branchGap, targetTop)
        const targetRect = nodeRect(branch.target)
        const targetCenter = {
          x: layout.targetX,
          y: layout.targetY + targetRect.height / 2
        }
        const targetLeft = targetCenter.x - targetRect.width / 2
        const targetRight = targetCenter.x + targetRect.width / 2
        const sourceLeft = sourceRect.x
        const sourceRight = sourceRect.x + sourceRect.width
        const hubCenter = {
          x: layout.side === 'left'
            ? (sourceLeft + targetRight) / 2
            : layout.side === 'right'
              ? (sourceRight + targetLeft) / 2
              : layout.targetX,
          y: layout.hubY
        }
        moveNodeCenter(branch.target, targetCenter)
        moveNodeCenter(branch.hub, hubCenter)

        const movedTargetRect = nodeRect(branch.target)
        const movedHubRect = nodeRect(branch.hub)
        const sourceOut = pointOnRectToward(sourceRect, hubCenter)
        const hubIn = pointOnRectToward(movedHubRect, sourceOut)
        const targetIn = pointOnRectToward(movedTargetRect, hubCenter)
        const hubOut = pointOnRectToward(movedHubRect, targetIn)

        branch.inEdge.data.points = orthogonal(sourceOut, hubIn)
        branch.inEdge.data.path = pointsToSvgPath(branch.inEdge.data.points)
        branch.outEdge.data.points = orthogonal(hubOut, targetIn)
        branch.outEdge.data.path = pointsToSvgPath(branch.outEdge.data.points)
      })
  })

  return { nodes: nextNodes, edges: nextEdges }
}

function optimizeSharedTargetBranches(nodes, edges) {
  const nextNodes = nodes.map((node) => ({
    ...node,
    position: { ...node.position },
    data: { ...node.data }
  }))
  const nextEdges = edges.map((edge) => ({
    ...edge,
    data: {
      ...edge.data,
      points: [...(edge.data?.points || [])]
    }
  }))
  const nodeById = new Map(nextNodes.map((node) => [node.id, node]))
  const incomingByTarget = new Map()

  nextEdges.forEach((outEdge) => {
    if (!outEdge.data?.directed) return
    const hub = nodeById.get(outEdge.source)
    const target = nodeById.get(outEdge.target)
    if (!hub || !target || target.type !== 'prop' || !['hub-s', 'hub-a', 'hub-m'].includes(hub.type)) return
    const inEdge = nextEdges.find((candidate) => candidate.target === hub.id && !candidate.data?.directed)
    const source = inEdge ? nodeById.get(inEdge.source) : null
    if (!inEdge || !source || source.type !== 'prop') return
    if (!incomingByTarget.has(target.id)) incomingByTarget.set(target.id, [])
    incomingByTarget.get(target.id).push({ source, hub, target, inEdge, outEdge })
  })

  incomingByTarget.forEach((branches) => {
    if (branches.length < 2) return
    const target = branches[0].target
    const targetRect = nodeRect(target)
    const targetCenter = nodeCenter(target)
    const sourceGap = 150
    const sideGap = 150

    branches
      .sort((a, b) => {
        const relA = String(a.hub.data?.relKey || '')
        const relB = String(b.hub.data?.relKey || '')
        return relA.localeCompare(relB, undefined, { numeric: true })
      })
      .forEach((branch, index) => {
        const sourceRect = nodeRect(branch.source)
        const hubRect = nodeRect(branch.hub)
        let sourceCenter
        let hubCenter

        if (index === 0) {
          sourceCenter = {
            x: targetCenter.x,
            y: targetRect.y - Math.max(118, sourceRect.height + hubRect.height + 66)
          }
          hubCenter = {
            x: targetCenter.x,
            y: (sourceCenter.y + targetCenter.y) / 2
          }
        } else {
          const side = index % 2 === 1 ? 1 : -1
          const level = Math.ceil(index / 2)
          sourceCenter = {
            x: targetCenter.x + side * sideGap * level,
            y: targetCenter.y
          }
          hubCenter = {
            x: (sourceCenter.x + targetCenter.x) / 2,
            y: targetCenter.y
          }
        }

        moveNodeCenter(branch.source, sourceCenter)
        moveNodeCenter(branch.hub, hubCenter)

        const movedSourceRect = nodeRect(branch.source)
        const movedHubRect = nodeRect(branch.hub)
        const sourceOut = pointOnRectToward(movedSourceRect, hubCenter)
        const hubIn = pointOnRectToward(movedHubRect, sourceOut)
        const targetIn = pointOnRectToward(targetRect, hubCenter)
        const hubOut = pointOnRectToward(movedHubRect, targetIn)

        branch.inEdge.data.points = orthogonal(sourceOut, hubIn)
        branch.inEdge.data.path = pointsToSvgPath(branch.inEdge.data.points)
        branch.outEdge.data.points = orthogonal(hubOut, targetIn)
        branch.outEdge.data.path = pointsToSvgPath(branch.outEdge.data.points)
      })
  })

  return { nodes: nextNodes, edges: nextEdges }
}

function branchLayout(index, sourceRect, gap, targetTop) {
  if (index === 0) {
    return {
      targetX: sourceRect.cx,
      hubY: sourceRect.y + sourceRect.height + 56,
      targetY: targetTop,
      side: 'bottom'
    }
  }

  const side = index % 2 === 1 ? -1 : 1
  const level = Math.ceil(index / 2)
  return {
    targetX: sourceRect.cx + side * gap * level,
    hubY: sourceRect.cy,
    targetY: sourceRect.cy - PROP_H / 2,
    side: side < 0 ? 'left' : 'right'
  }
}

function normalizeGraph(nodes, edges) {
  if (!nodes.length) return { nodes, edges }
  let minX = Infinity
  let minY = Infinity
  nodes.forEach((node) => {
    minX = Math.min(minX, node.position.x)
    minY = Math.min(minY, node.position.y)
  })
  const dx = GAP.padding - minX
  const dy = GAP.padding - minY
  if (!dx && !dy) return { nodes, edges }

  return {
    nodes: nodes.map((node) => ({
      ...node,
      position: {
        x: node.position.x + dx,
        y: node.position.y + dy
      }
    })),
    edges: edges.map((edge) => {
      const points = (edge.data?.points || []).map((p) => ({ x: p.x + dx, y: p.y + dy }))
      return {
        ...edge,
        data: {
          ...edge.data,
          points,
          path: pointsToSvgPath(points)
        }
      }
    })
  }
}

function nodeCenter(node) {
  const rect = nodeRect(node)
  return { x: rect.cx, y: rect.cy }
}

function connectionPoint(node, toward) {
  return pointOnRectToward(nodeRect(node), toward)
}

function moveNodeCenter(node, center) {
  const rect = nodeRect(node)
  node.position.x = center.x - rect.width / 2
  node.position.y = center.y - rect.height / 2
}

function optimizeDirectedHubs(nodes, edges) {
  const nextNodes = nodes.map((node) => ({
    ...node,
    position: { ...node.position },
    data: { ...node.data }
  }))
  const nextEdges = edges.map((edge) => ({
    ...edge,
    data: {
      ...edge.data,
      points: [...(edge.data?.points || [])]
    }
  }))
  const nodeById = new Map(nextNodes.map((node) => [node.id, node]))
  const nodeDegree = new Map(nextNodes.map((node) => [node.id, 0]))
  nextEdges.forEach((edge) => {
    nodeDegree.set(edge.source, (nodeDegree.get(edge.source) || 0) + 1)
    nodeDegree.set(edge.target, (nodeDegree.get(edge.target) || 0) + 1)
  })

  nextNodes
    .filter((node) => ['hub-s', 'hub-a', 'hub-m'].includes(node.type))
    .forEach((hub) => {
      const inEdge = nextEdges.find((edge) => edge.target === hub.id && !edge.data?.directed)
      const outEdge = nextEdges.find((edge) => edge.source === hub.id && edge.data?.directed)
      if (!inEdge || !outEdge) return

      const source = nodeById.get(inEdge.source)
      const target = nodeById.get(outEdge.target)
      if (!source || !target) return

      let sourceRect = nodeRect(source)
      let targetRect = nodeRect(target)
      const hubRect = nodeRect(hub)
      let hubCenter

      if (target.type?.startsWith('hub')) {
        if (source.type === 'prop') {
          const targetHasDownwardConclusion = nextEdges.some((edge) => {
            if (edge.source !== target.id || !edge.data?.directed) return false
            const conclusion = nodeById.get(edge.target)
            return conclusion && nodeRect(conclusion).cy > targetRect.cy
          })
          const nearSameColumn = nextNodes
            .filter((node) => ![source.id, hub.id, target.id].includes(node.id))
            .map(nodeRect)
            .filter((rect) => Math.abs(rect.cx - targetRect.cx) < Math.max(48, targetRect.width))
          const nearestBelow = nearSameColumn
            .filter((rect) => rect.y >= targetRect.y + targetRect.height)
            .sort((a, b) => a.y - b.y)[0]
          const nearestAbove = nearSameColumn
            .filter((rect) => rect.y + rect.height <= targetRect.y)
            .sort((a, b) => b.y + b.height - (a.y + a.height))[0]
          const belowGap = nearestBelow ? nearestBelow.y - (targetRect.y + targetRect.height) : Infinity
          const aboveGap = nearestAbove ? targetRect.y - (nearestAbove.y + nearestAbove.height) : Infinity
          const needVerticalGap = sourceRect.height + hubRect.height + 46
          let sourceCenter
          if (!targetHasDownwardConclusion && belowGap >= needVerticalGap) {
            sourceCenter = {
              x: targetRect.cx,
              y: targetRect.cy + Math.max(96, sourceRect.height + hubRect.height + 56)
            }
          } else if (!targetHasDownwardConclusion && aboveGap >= needVerticalGap) {
            sourceCenter = {
              x: targetRect.cx,
              y: targetRect.cy - Math.max(96, sourceRect.height + hubRect.height + 56)
            }
          } else {
            const side = sourceRect.cx <= targetRect.cx ? -1 : 1
            sourceCenter = {
              x: targetRect.cx + side * Math.max(108, sourceRect.width + hubRect.width + 52),
              y: targetRect.cy
            }
          }
          moveNodeCenter(source, sourceCenter)
          sourceRect = nodeRect(source)
          hubCenter = Math.abs(sourceRect.cx - targetRect.cx) < 0.5
            ? {
                x: targetRect.cx,
                y: (sourceRect.cy + targetRect.cy) / 2
              }
            : {
                x: (sourceRect.cx + targetRect.cx) / 2,
                y: targetRect.cy
              }
        } else {
          const side = sourceRect.cx <= targetRect.cx ? -1 : 1
          hubCenter = {
            x: targetRect.cx + side * Math.max(58, hubRect.width + 44),
            y: targetRect.cy
          }
        }
      } else if (sourceRect.cy <= targetRect.cy) {
        if (source.type?.startsWith('hub') && target.type === 'prop' && (nodeDegree.get(target.id) || 0) <= 1) {
          moveNodeCenter(target, {
            x: sourceRect.cx,
            y: sourceRect.y + sourceRect.height + GAP.verticalRelation * 2 + hubRect.height + targetRect.height / 2
          })
          targetRect = nodeRect(target)
        }
        hubCenter = {
          x: sourceRect.cx,
          y: Math.max(sourceRect.y + sourceRect.height + 34, (sourceRect.y + sourceRect.height + targetRect.y) / 2)
        }
      } else {
        hubCenter = {
          x: sourceRect.cx,
          y: Math.min(sourceRect.y - 34, (targetRect.y + targetRect.height + sourceRect.y) / 2)
        }
      }

      moveNodeCenter(hub, hubCenter)

      const movedHubRect = nodeRect(hub)
      const movedHubCenter = nodeCenter(hub)
      const sourceOut = connectionPoint(source, movedHubCenter)
      const hubIn = pointOnRectToward(movedHubRect, sourceOut)
      const targetIn = connectionPoint(target, movedHubCenter)
      const hubOut = pointOnRectToward(movedHubRect, targetIn)

      inEdge.data.points = orthogonal(sourceOut, hubIn)
      inEdge.data.path = pointsToSvgPath(inEdge.data.points)
      outEdge.data.points = orthogonal(hubOut, targetIn)
      outEdge.data.path = pointsToSvgPath(outEdge.data.points)
    })

  return { nodes: nextNodes, edges: nextEdges }
}

function sectionToPoints(section) {
  const pts = []
  if (section.startPoint) pts.push({ x: section.startPoint.x, y: section.startPoint.y })
  ;(section.bendPoints || []).forEach((p) => pts.push({ x: p.x, y: p.y }))
  if (section.endPoint) pts.push({ x: section.endPoint.x, y: section.endPoint.y })
  return pts
}

/**
 * @returns {Promise<{ nodes: Array, edges: Array, bounds: object }>}
 */
export async function layoutArgumentGraphWithElk(propositions, relations) {
  return buildManualGraph(propositions, relations)
}

export { buildManualGraph as buildElkGraph, pointsToSvgPath, sectionToPoints }
