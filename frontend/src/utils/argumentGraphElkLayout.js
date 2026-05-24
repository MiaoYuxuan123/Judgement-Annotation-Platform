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
  'elk.edgeRouting': 'ORTHOGONAL',
  'elk.spacing.nodeNode': '54',
  'elk.layered.spacing.nodeNodeBetweenLayers': '76',
  'elk.layered.spacing.edgeNodeBetweenLayers': '34',
  'elk.layered.spacing.edgeEdgeBetweenLayers': '22',
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
  const rels = relations || []
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
