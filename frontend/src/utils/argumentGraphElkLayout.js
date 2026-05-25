/**
 * 论证图 ELK 自动布局：命题节点 + 关系节点 -> 分层正交布局。
 *
 * 指南图示规则：
 * - 命题：矩形节点。
 * - S：实心圆关系节点，输入 -> ● -> 输出。
 * - A：空心圆关系节点，输入 -> ○ -> 输出。
 * - J：带 + 圆关系节点，多成员无向汇聚。
 * - M：带 + 圆关系节点，输入 -> + -> 输出。
 * - I：命题节点合并为 P1 / P2 形式。
 */
import ELK from 'elkjs/lib/elk.bundled.js'

const elk = new ELK()

const PROP_H = 32
const HUB_SIZE = { S: 12, A: 18, M: 24, J: 24 }

const ROOT_LAYOUT = {
  'elk.algorithm': 'layered',
  'elk.direction': 'DOWN',
  'elk.edgeRouting': 'POLYLINE',
  'elk.spacing.nodeNode': '44',
  'elk.layered.spacing.nodeNodeBetweenLayers': '68',
  'elk.layered.spacing.edgeNodeBetweenLayers': '34',
  'elk.layered.spacing.edgeEdgeBetweenLayers': '18',
  'elk.layered.crossingMinimization.strategy': 'LAYER_SWEEP',
  'elk.layered.nodePlacement.strategy': 'NETWORK_SIMPLEX',
  'elk.layered.layering.strategy': 'NETWORK_SIMPLEX',
  'elk.layered.considerModelOrder.strategy': 'NODES_AND_EDGES',
  'elk.padding': '[top=48,left=48,bottom=48,right=48]'
}

function relationType(rel) {
  return String(rel?.type || 'S').toUpperCase()
}

function relationId(rel, index) {
  return rel?.relId || `R${index + 1}`
}

function relationMembers(rel) {
  return (rel?.members?.length ? rel.members : [rel?.source, rel?.target]).filter(Boolean)
}

function relationKey(rel, index) {
  return relationId(rel, index)
}

function propLabel(p) {
  return p.propId || `P${p.sequenceNo}`
}

function propBoxSize(label) {
  return { width: Math.max(52, label.length * 9 + 22), height: PROP_H }
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

function buildElkGraph(propositions = [], relations = []) {
  const rels = filterGraphRelations(relations || [])
  const visiblePropIds = collectVisiblePropIds(rels)
  propositions = (propositions || []).filter((p) => visiblePropIds.has(p.propId))
  const propMap = new Map(propositions.map((p) => [p.propId, p]))
  const uf = createIdentityGroups(propositions, rels)

  const propToNode = new Map()
  const relToNode = new Map()
  const nodeMeta = new Map()
  const children = []
  const edges = []

  const grouped = new Map()
  propositions.forEach((p) => {
    const root = uf.find(p.propId)
    if (!grouped.has(root)) grouped.set(root, [])
    grouped.get(root).push(p)
  })

  grouped.forEach((items, root) => {
    const sorted = [...items].sort((a, b) => (a.sequenceNo || 0) - (b.sequenceNo || 0))
    const id = `prop-${root}`
    const label = sorted.map(propLabel).join(' / ')
    const size = propBoxSize(label)
    sorted.forEach((p) => propToNode.set(p.propId, id))
    nodeMeta.set(id, { kind: 'prop', label, ...size })
    children.push({ id, width: size.width, height: size.height })
  })

  rels.forEach((rel, index) => {
    const type = relationType(rel)
    if (type === 'I' || !['S', 'A', 'J', 'M'].includes(type)) return

    const key = relationId(rel, index)
    const id = `hub-${type.toLowerCase()}-${key}`
    const hubKind = type === 'S' ? 'hub-s' : type === 'A' ? 'hub-a' : type === 'J' ? 'hub-j' : 'hub-m'
    const size = HUB_SIZE[type]
    relToNode.set(key, id)
    nodeMeta.set(id, {
      kind: hubKind,
      label: type === 'S' || type === 'A' ? '' : '+',
      width: size,
      height: size
    })
    children.push({ id, width: size, height: size })
  })

  function resolveMember(id) {
    if (propToNode.has(id)) return propToNode.get(id)
    if (relToNode.has(id)) return relToNode.get(id)
    return null
  }

  rels.forEach((rel, index) => {
    const type = relationType(rel)
    if (type === 'I' || !['S', 'A', 'J', 'M'].includes(type)) return

    const key = relationId(rel, index)
    const hubId = relToNode.get(key)
    if (!hubId) return

    const members = relationMembers(rel)
    if (type === 'J') {
      const memberNodes = members.map(resolveMember).filter((id) => id && id !== hubId)
      if (memberNodes.length < 2) return
      ;[...new Set(memberNodes)].forEach((memberNode, memberIndex) => {
        edges.push({
          id: `e-j-${key}-${memberIndex}`,
          sources: [memberNode],
          targets: [hubId],
          directed: false
        })
      })
      return
    }

    const sourceNode = resolveMember(members[0])
    const targetNode = resolveMember(members[1])
    if (!sourceNode || !targetNode || sourceNode === targetNode) return

    edges.push({
      id: `e-${type}-in-${key}`,
      sources: [sourceNode],
      targets: [hubId],
      directed: false
    })
    edges.push({
      id: `e-${type}-out-${key}`,
      sources: [hubId],
      targets: [targetNode],
      directed: true
    })
  })

  return {
    graph: {
      id: 'root',
      layoutOptions: ROOT_LAYOUT,
      children,
      edges: edges.map(({ directed, ...edge }) => edge)
    },
    nodeMeta,
    edgeDefs: edges
  }
}

function filterGraphRelations(relations) {
  const relIdSet = new Set(relations.map((rel, index) => relationKey(rel, index)))
  return relations.filter((rel) => {
    const type = relationType(rel)
    const members = relationMembers(rel)
    if (type === 'I') return members.some((id) => String(id).startsWith('P'))
    if (type === 'J') return members.length >= 2
    if (['S', 'A', 'M'].includes(type)) return members.length >= 2
    return members.some((id) => relIdSet.has(id) || String(id).startsWith('P'))
  })
}

function collectVisiblePropIds(relations) {
  const visible = new Set()
  const relMap = new Map(relations.map((rel, index) => [relationKey(rel, index), rel]))
  const visit = (id, seen = new Set()) => {
    if (String(id).startsWith('P')) {
      visible.add(id)
      return
    }
    if (seen.has(id)) return
    const rel = relMap.get(id)
    if (!rel) return
    seen.add(id)
    relationMembers(rel).forEach((member) => visit(member, seen))
  }
  relations.forEach((rel, index) => visit(relationKey(rel, index)))
  return visible
}

function tryBuildGuidelineLayout(propositions = [], relations = []) {
  const rels = filterGraphRelations(relations || [])
  const visiblePropIds = collectVisiblePropIds(rels)
  const visibleProps = (propositions || []).filter((p) => visiblePropIds.has(p.propId))
  if (!rels.length) return null
  const propMap = new Map(visibleProps.map((p) => [p.propId, p]))
  const identityUf = createIdentityGroups(visibleProps, rels)
  const groupedProps = new Map()
  visibleProps.forEach((p) => {
    const root = identityUf.find(p.propId)
    if (!groupedProps.has(root)) groupedProps.set(root, [])
    groupedProps.get(root).push(p)
  })

  const nodes = []
  const edges = []
  const placed = new Map()
  const nodeRects = new Map()
  const relationEndpoints = new Map()
  let componentIndex = 0

  const nextBase = () => {
    const base = {
      x: 48,
      y: 48 + componentIndex * 190
    }
    componentIndex += 1
    return base
  }

  const propNodeInfo = (propId) => {
    const root = identityUf.find(propId)
    const items = [...(groupedProps.get(root) || [propMap.get(propId)])].filter(Boolean).sort((a, b) => (a.sequenceNo || 0) - (b.sequenceNo || 0))
    const label = items.map(propLabel).join(' / ')
    return { id: `prop-${root}`, label }
  }

  const addProp = (propId, x, y) => {
    if (!propMap.has(propId)) return null
    const info = propNodeInfo(propId)
    const id = info.id
    if (!placed.has(id)) {
      const size = propBoxSize(info.label)
      const rect = { id, x, y, width: size.width, height: size.height, cx: x + size.width / 2, cy: y + size.height / 2 }
      placed.set(id, true)
      nodeRects.set(id, rect)
      nodes.push(flowNode(id, 'prop', info.label, x, y, size.width, size.height))
    }
    return nodeRects.get(id)
  }

  const addHub = (id, kind, label, x, y, size) => {
    if (!placed.has(id)) {
      placed.set(id, true)
      nodeRects.set(id, { id, x, y, width: size, height: size, cx: x + size / 2, cy: y + size / 2 })
      nodes.push(flowNode(id, kind, label, x, y, size, size))
    }
    return nodeRects.get(id)
  }

  const ensureMember = (memberId, preferredX, preferredY) => {
    if (String(memberId).startsWith('P')) {
      const rect = addProp(memberId, preferredX, preferredY)
      return rect ? { ...rect, nodeId: rect.id, x: rect.cx, y: rect.cy, rect } : null
    }
    const endpoint = relationEndpoints.get(memberId)
    return endpoint ? { ...endpoint } : null
  }

  const centerPoint = (rect) => ({ x: rect.cx, y: rect.cy })

  const jItems = rels
    .map((rel, index) => ({ rel, key: relationKey(rel, index) }))
    .filter(({ rel }) => relationType(rel) === 'J')

  jItems.forEach(({ rel, key }) => {
    const members = relationMembers(rel).filter((id) => String(id).startsWith('P') && propMap.has(id))
    if (members.length < 2) return
    const base = nextBase()
    const hubId = `hub-j-${key}`
    const memberGap = 92
    const rowY = base.y
    const propRects = members
      .map((propId, memberIndex) => addProp(propId, base.x + memberIndex * memberGap, rowY))
      .filter(Boolean)
    if (propRects.length < 2) return

    const hubCenter =
      propRects.length === 2
        ? {
            x: (propRects[0].cx + propRects[1].cx) / 2,
            y: propRects[0].cy
          }
        : {
            x: (propRects[0].cx + propRects[propRects.length - 1].cx) / 2,
            y: rowY + PROP_H + 58
          }

    addHub(hubId, 'hub-j', '+', hubCenter.x - HUB_SIZE.J / 2, hubCenter.y - HUB_SIZE.J / 2, HUB_SIZE.J)
    propRects.forEach((propRect, memberIndex) => {
      const from =
        propRects.length === 2
          ? pointOnRectToward(propRect, hubCenter)
          : { x: propRect.cx, y: propRect.y + propRect.height, side: 'bottom' }
      edges.push(flowEdge(`e-j-${key}-${memberIndex}`, propRect.id, hubId, false, orthogonalPointsFromRect(from, hubCenter)))
    })
    relationEndpoints.set(key, { x: hubCenter.x, y: hubCenter.y, nodeId: hubId, rect: nodeRects.get(hubId) })
  })

  rels.forEach((rel, index) => {
    const type = relationType(rel)
    if (!['S', 'A', 'M'].includes(type)) return
    const members = relationMembers(rel)
    if (members.length < 2) return
    const key = relationKey(rel, index)
    const base = nextBase()
    const source = ensureMember(members[0], base.x, base.y)
    if (!source) return
    const hubKind = type === 'S' ? 'hub-s' : type === 'A' ? 'hub-a' : 'hub-m'
    const hubId = `hub-${type.toLowerCase()}-${key}`
    const sourceOutBase = source.rect ? pointOnRectToward(source.rect, { x: source.x + 120, y: source.y }) : { x: source.x, y: source.y }
    const hubCenter = { x: sourceOutBase.x + 112, y: sourceOutBase.y }
    addHub(hubId, hubKind, type === 'M' ? '+' : '', hubCenter.x - HUB_SIZE[type] / 2, hubCenter.y - HUB_SIZE[type] / 2, HUB_SIZE[type])

    const targetPreferredX = hubCenter.x + 96
    const targetPreferredY = hubCenter.y - PROP_H / 2
    const target = ensureMember(members[1], targetPreferredX, targetPreferredY)
    if (!target) return

    const sourceOut = source.rect ? exitPointFromRect(source.rect, hubCenter) : { x: source.x, y: source.y }
    const targetIn = target.rect ? entryPointToRect(target.rect, hubCenter) : { x: target.x, y: target.y }
    edges.push(flowEdge(`e-${type}-in-${key}`, source.nodeId, hubId, false, orthogonalPointsFromRect(sourceOut, hubCenter)))
    edges.push(flowEdge(`e-${type}-out-${key}`, hubId, target.nodeId, true, orthogonalPointsToRect(hubCenter, targetIn)))
    relationEndpoints.set(key, { x: hubCenter.x, y: hubCenter.y, nodeId: hubId, rect: nodeRects.get(hubId) })
  })

  rels.forEach((rel, index) => {
    if (relationType(rel) !== 'I') return
    const members = relationMembers(rel).filter((id) => String(id).startsWith('P') && propMap.has(id))
    if (!members.length) return
    const base = nextBase()
    members.forEach((propId) => addProp(propId, base.x, base.y))
  })

  return { nodes, edges, bounds: boundsFromNodes(nodes) }
}

function pointOnRectToward(rect, toward) {
  if (!rect) return toward
  const dx = toward.x - rect.cx
  const dy = toward.y - rect.cy
  if (Math.abs(dx) >= Math.abs(dy)) {
    const right = dx >= 0
    return {
      x: right ? rect.x + rect.width : rect.x,
      y: rect.cy,
      side: right ? 'right' : 'left'
    }
  }
  const bottom = dy >= 0
  return {
    x: rect.cx,
    y: bottom ? rect.y + rect.height : rect.y,
    side: bottom ? 'bottom' : 'top'
  }
}

function exitPointFromRect(rect, toward) {
  return pointOnRectToward(rect, toward)
}

function entryPointToRect(rect, from) {
  if (!rect) return from
  const dx = from.x - rect.cx
  const dy = from.y - rect.cy
  if (Math.abs(dx) >= Math.abs(dy)) {
    return {
      x: dx < 0 ? rect.x : rect.x + rect.width,
      y: rect.cy,
      side: dx < 0 ? 'left' : 'right'
    }
  }
  return {
    x: rect.cx,
    y: dy < 0 ? rect.y : rect.y + rect.height,
    side: dy < 0 ? 'top' : 'bottom'
  }
}

function orthogonalPoints(from, to) {
  if (!from || !to) return []
  if (Math.abs(from.x - to.x) < 0.5 || Math.abs(from.y - to.y) < 0.5) {
    return [from, to]
  }
  const midX = (from.x + to.x) / 2
  return [
    from,
    { x: midX, y: from.y },
    { x: midX, y: to.y },
    to
  ]
}

function orthogonalPointsFromRect(from, to) {
  if (!from?.side || !to) return orthogonalPoints(from, to)
  const clearance = 18
  let first
  if (from.side === 'left') first = { x: from.x - clearance, y: from.y }
  else if (from.side === 'right') first = { x: from.x + clearance, y: from.y }
  else if (from.side === 'top') first = { x: from.x, y: from.y - clearance }
  else first = { x: from.x, y: from.y + clearance }

  if (Math.abs(first.x - to.x) < 0.5 || Math.abs(first.y - to.y) < 0.5) {
    return [from, first, to]
  }
  const midX = (first.x + to.x) / 2
  return [
    from,
    first,
    { x: midX, y: first.y },
    { x: midX, y: to.y },
    to
  ]
}

function orthogonalPointsToRect(from, entry) {
  if (!from || !entry) return []
  if (Math.abs(from.x - entry.x) < 0.5 || Math.abs(from.y - entry.y) < 0.5) {
    return [from, entry]
  }
  if (entry.side === 'left' || entry.side === 'right') {
    return [
      from,
      { x: from.x, y: entry.y },
      entry
    ]
  }
  return [
    from,
    { x: entry.x, y: from.y },
    entry
  ]
}

function flowNode(id, type, label, x, y, width, height) {
  return {
    id,
    type,
    position: { x, y },
    data: { label, hubKind: type },
    style: { width: `${width}px`, height: `${height}px` },
    draggable: false,
    selectable: false,
    connectable: false
  }
}

function flowEdge(id, source, target, directed, points = null) {
  return {
    id,
    source,
    target,
    type: 'elk-orthogonal',
    animated: false,
    selectable: false,
    data: { points, path: points ? pointsToSvgPath(points) : null, directed }
  }
}

function boundsFromNodes(nodes) {
  let maxX = 0
  let maxY = 0
  nodes.forEach((n) => {
    const w = parseFloat(n.style.width) || 52
    const h = parseFloat(n.style.height) || PROP_H
    maxX = Math.max(maxX, n.position.x + w)
    maxY = Math.max(maxY, n.position.y + h)
  })
  return {
    width: Math.max(maxX + 48, 320),
    height: Math.max(maxY + 48, 200)
  }
}

function sectionToPoints(section) {
  const pts = []
  if (section.startPoint) pts.push({ x: section.startPoint.x, y: section.startPoint.y })
  ;(section.bendPoints || []).forEach((p) => pts.push({ x: p.x, y: p.y }))
  if (section.endPoint) pts.push({ x: section.endPoint.x, y: section.endPoint.y })
  return pts
}

function pointsToSvgPath(points) {
  if (!points.length) return ''
  return points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
}

/**
 * @returns {Promise<{ nodes: Array, edges: Array, bounds: object }>}
 */
export async function layoutArgumentGraphWithElk(propositions, relations) {
  const guidelineLayout = tryBuildGuidelineLayout(propositions, relations)
  if (guidelineLayout) return guidelineLayout

  const { graph, nodeMeta, edgeDefs } = buildElkGraph(propositions, relations)
  if (!graph.children.length) {
    return {
      nodes: [],
      edges: [],
      bounds: { width: 400, height: 200 }
    }
  }

  const layouted = await elk.layout(graph)
  const edgeSections = new Map()
  ;(layouted.edges || []).forEach((e) => {
    if (e.sections?.length) edgeSections.set(e.id, e.sections)
  })

  const nodes = (layouted.children || []).map((n) => {
    const meta = nodeMeta.get(n.id) || { kind: 'prop', label: n.id, width: 52, height: PROP_H }
    return {
      id: n.id,
      type: meta.kind,
      position: { x: n.x ?? 0, y: n.y ?? 0 },
      data: {
        label: meta.label,
        hubKind: meta.kind
      },
      style: {
        width: `${meta.width}px`,
        height: `${meta.height}px`
      },
      draggable: false,
      selectable: false,
      connectable: false
    }
  })

  const flowEdges = edgeDefs.map((e) => {
    const sections = edgeSections.get(e.id)
    const points = sections?.[0] ? sectionToPoints(sections[0]) : null
    return {
      id: e.id,
      source: e.sources[0],
      target: e.targets[0],
      type: 'elk-orthogonal',
      animated: false,
      selectable: false,
      data: {
        points,
        path: points ? pointsToSvgPath(points) : null,
        directed: e.directed
      }
    }
  })

  let maxX = 0
  let maxY = 0
  nodes.forEach((n) => {
    const w = parseFloat(n.style.width) || 52
    const h = parseFloat(n.style.height) || PROP_H
    maxX = Math.max(maxX, n.position.x + w)
    maxY = Math.max(maxY, n.position.y + h)
  })

  return {
    nodes,
    edges: flowEdges,
    bounds: {
      width: Math.max(maxX + 48, 320),
      height: Math.max(maxY + 48, 200)
    }
  }
}

export { buildElkGraph, pointsToSvgPath, sectionToPoints }
