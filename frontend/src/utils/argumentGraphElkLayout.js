/**
 * 论证图 ELK 自动布局：命题 + 关系 → 分层正交布局（Top→Down）
 */
import ELK from 'elkjs/lib/elk.bundled.js'

const elk = new ELK()

const PROP_H = 32
const HUB_SIZE = { S: 10, A: 16, M: 22, J: 22 }

const ROOT_LAYOUT = {
  'elk.algorithm': 'layered',
  'elk.direction': 'DOWN',
  'elk.edgeRouting': 'ORTHOGONAL',
  'elk.spacing.nodeNode': '52',
  'elk.layered.spacing.nodeNodeBetweenLayers': '72',
  'elk.layered.spacing.edgeNodeBetweenLayers': '36',
  'elk.layered.spacing.edgeEdgeBetweenLayers': '24',
  'elk.layered.crossingMinimization.strategy': 'LAYER_SWEEP',
  'elk.layered.nodePlacement.strategy': 'NETWORK_SIMPLEX',
  'elk.layered.layering.strategy': 'NETWORK_SIMPLEX',
  'elk.layered.considerModelOrder.strategy': 'NODES_AND_EDGES',
  'elk.padding': '[top=48,left=48,bottom=48,right=48]',
  'elk.hierarchyHandling': 'INCLUDE_CHILDREN'
}

function propLabel(p) {
  return `P${p.sequenceNo}`
}

function propBoxSize(label) {
  return { width: Math.max(48, label.length * 9 + 20), height: PROP_H }
}

function getPropMembers(rel) {
  const ids = rel.members?.length ? rel.members : [rel.source, rel.target]
  return ids.filter((id) => typeof id === 'string' && id.startsWith('P'))
}

class UnionFind {
  constructor(ids) {
    this.parent = new Map(ids.map((id) => [id, id]))
  }

  find(id) {
    let r = id
    while (this.parent.get(r) !== r) r = this.parent.get(r)
    let c = id
    while (this.parent.get(c) !== c) {
      const n = this.parent.get(c)
      this.parent.set(c, r)
      c = n
    }
    return r
  }

  union(a, b) {
    const ra = this.find(a)
    const rb = this.find(b)
    if (ra !== rb) this.parent.set(rb, ra)
  }
}

function buildElkGraph(propositions, relations) {
  const rels = relations || []
  const propMap = new Map((propositions || []).map((p) => [p.propId, p]))
  const uf = new UnionFind((propositions || []).map((p) => p.propId))

  rels
    .filter((r) => String(r.type).toUpperCase() === 'I')
    .forEach((r) => {
      const members = getPropMembers(r).filter((id) => propMap.has(id))
      for (let i = 1; i < members.length; i += 1) uf.union(members[0], members[i])
    })

  const grouped = new Map()
  ;(propositions || []).forEach((p) => {
    const root = uf.find(p.propId)
    if (!grouped.has(root)) grouped.set(root, [])
    grouped.get(root).push(p)
  })

  const propToNode = new Map()
  const nodeMeta = new Map()
  const children = []
  const edges = []

  grouped.forEach((items, root) => {
    const sorted = [...items].sort((a, b) => a.sequenceNo - b.sequenceNo)
    const id = `prop-${root}`
    const label = sorted.map(propLabel).join(' / ')
    const size = propBoxSize(label)
    sorted.forEach((p) => propToNode.set(p.propId, id))
    nodeMeta.set(id, { kind: 'prop', label, ...size })
    children.push({ id, width: size.width, height: size.height })
  })

  rels.forEach((rel, index) => {
    const type = String(rel.type || 'S').toUpperCase()
    const key = rel.relId || `R${index + 1}`
    if (type === 'I') return

    if (type === 'J') {
      const members = getPropMembers(rel).filter((id) => propToNode.has(id))
      if (members.length < 2) return
      const memberIds = members.map((m) => propToNode.get(m))
      const hubId = `hub-j-${key}`
      const hubSize = HUB_SIZE.J
      nodeMeta.set(hubId, { kind: 'hub-j', label: '+', width: hubSize, height: hubSize })
      children.push({ id: hubId, width: hubSize, height: hubSize })

      memberIds.forEach((mid) => {
        edges.push({ id: `e-j-in-${key}-${mid}`, sources: [mid], targets: [hubId] })
      })

      const outboundProp = rel.target && !members.includes(rel.target) ? rel.target : null
      if (outboundProp && propToNode.has(outboundProp)) {
        edges.push({
          id: `e-j-out-${key}`,
          sources: [hubId],
          targets: [propToNode.get(outboundProp)]
        })
      }
      return
    }

    if (!['S', 'A', 'M'].includes(type)) return
    const sourceId = propToNode.get(rel.source)
    const targetId = propToNode.get(rel.target)
    if (!sourceId || !targetId || sourceId === targetId) return

    const hubId = `hub-${type.toLowerCase()}-${key}`
    const hubSize = HUB_SIZE[type]
    const hubKind = type === 'S' ? 'hub-s' : type === 'A' ? 'hub-a' : 'hub-m'
    nodeMeta.set(hubId, {
      kind: hubKind,
      label: type === 'S' ? '' : '+',
      width: hubSize,
      height: hubSize
    })
    children.push({ id: hubId, width: hubSize, height: hubSize })
    edges.push({ id: `e-${type}-in-${key}`, sources: [sourceId], targets: [hubId] })
    edges.push({ id: `e-${type}-out-${key}`, sources: [hubId], targets: [targetId] })
  })

  return {
    graph: {
      id: 'root',
      layoutOptions: ROOT_LAYOUT,
      children,
      edges
    },
    nodeMeta,
    edges
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
  const { graph, nodeMeta, edges: edgeDefs } = buildElkGraph(propositions, relations)
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
    const meta = nodeMeta.get(n.id) || { kind: 'prop', label: n.id, width: 48, height: PROP_H }
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
        directed: /-(out|out-)/.test(e.id)
      }
    }
  })

  let maxX = 0
  let maxY = 0
  nodes.forEach((n) => {
    const w = parseFloat(n.style.width) || 48
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
