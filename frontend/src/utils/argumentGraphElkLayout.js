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
  const propMap = new Map(visibleProps.map((p) => [p.propId, p]))
  const relMap = new Map(rels.map((rel, index) => [relationKey(rel, index), rel]))
  const jRelations = rels
    .map((rel, index) => ({ rel, key: relationKey(rel, index) }))
    .filter(({ rel }) => relationType(rel) === 'J')
    .map((item) => ({
      ...item,
      members: relationMembers(item.rel).filter((id) => String(id).startsWith('P') && propMap.has(id))
    }))
    .filter((item) => item.members.length >= 2 && item.members.length <= 4)

  const outerBySource = new Map()
  rels.forEach((rel, index) => {
    const type = relationType(rel)
    const members = relationMembers(rel)
    if (!['S', 'A', 'M'].includes(type) || members.length < 2) return
    if (!relMap.has(members[0]) || !String(members[1]).startsWith('P') || !propMap.has(members[1])) return
    outerBySource.set(members[0], { rel, key: relationKey(rel, index), target: members[1] })
  })

  if (!jRelations.length && !outerBySource.size) return null

  const nodes = []
  const edges = []
  const placed = new Map()
  const nodeRects = new Map()
  let offsetX = 48
  let offsetY = 48

  const addProp = (propId, x, y) => {
    const id = `prop-${propId}`
    if (!placed.has(id)) {
      const p = propMap.get(propId)
      const label = propLabel(p)
      const size = propBoxSize(label)
      const rect = { id, x, y, width: size.width, height: size.height, cx: x + size.width / 2, cy: y + size.height / 2 }
      placed.set(id, true)
      nodeRects.set(id, rect)
      nodes.push(flowNode(id, 'prop', label, x, y, size.width, size.height))
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

  const componentEndpoints = new Map()

  jRelations.forEach((jr, sgIndex) => {
    const baseX = offsetX + (sgIndex % 2) * 420
    const baseY = offsetY + Math.floor(sgIndex / 2) * 240
    const jHub = `hub-j-${jr.key}`
    const propGap = 86
    const firstY = baseY
    const propX = baseX
    const jCenter = {
      x: baseX + 30,
      y: firstY + ((jr.members.length - 1) * propGap) / 2 + PROP_H / 2
    }

    jr.members.forEach((propId, i) => {
      const y = firstY + i * propGap
      const propRect = addProp(propId, propX, y)
      const from = pointOnRectToward(propRect, jCenter)
      edges.push(flowEdge(`e-j-${jr.key}-${i}`, `prop-${propId}`, jHub, false, [from, jCenter]))
    })

    addHub(jHub, 'hub-j', '+', jCenter.x - HUB_SIZE.J / 2, jCenter.y - HUB_SIZE.J / 2, HUB_SIZE.J)
    componentEndpoints.set(jr.key, { x: jCenter.x, y: jCenter.y, nodeId: jHub, index: sgIndex })
  })

  outerBySource.forEach((outer, sourceKey) => {
    const start = componentEndpoints.get(sourceKey)
    if (!start) return
    const outerType = relationType(outer.rel)
    const outerKind = outerType === 'S' ? 'hub-s' : outerType === 'A' ? 'hub-a' : 'hub-m'
    const outerHub = `hub-${outerType.toLowerCase()}-${outer.key}`
    const outerCenter = { x: start.x + 120, y: start.y }
    const targetX = outerCenter.x + 94
    const targetY = start.y - PROP_H / 2
    addHub(outerHub, outerKind, outerType === 'M' ? '+' : '', outerCenter.x - HUB_SIZE[outerType] / 2, outerCenter.y - HUB_SIZE[outerType] / 2, HUB_SIZE[outerType])
    const targetRect = addProp(outer.target, targetX, targetY)
    edges.push(flowEdge(`e-${outerType}-in-${outer.key}`, start.nodeId, outerHub, false, [start, outerCenter]))
    edges.push(flowEdge(`e-${outerType}-out-${outer.key}`, outerHub, `prop-${outer.target}`, true, [outerCenter, pointOnRectToward(targetRect, outerCenter)]))
  })

  return { nodes, edges, bounds: boundsFromNodes(nodes) }
}

function pointOnRectToward(rect, toward) {
  if (!rect) return toward
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
