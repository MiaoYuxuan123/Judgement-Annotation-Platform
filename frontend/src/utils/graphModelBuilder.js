import { DagreLayout } from '@antv/layout'

// utils/graphModelBuilder.js
// 图示布局逻辑（与页面共用）

const PROP_H = 30
const GAP = {
  rank: 52,
  node: 40,
  jointRow: 40,
  jointCol: 72
}

const dagreLayout = new DagreLayout({
  type: 'dagre',
  rankdir: 'TB',
  align: 'UL',
  ranksep: GAP.rank,
  nodesep: GAP.node,
  controlPoints: false
})

class UnionFind {
  constructor(ids) {
    this.parent = new Map(ids.map((id) => [id, id]))
  }

  find(id) {
    let root = id
    while (this.parent.get(root) !== root) root = this.parent.get(root)
    let cur = id
    while (this.parent.get(cur) !== cur) {
      const next = this.parent.get(cur)
      this.parent.set(cur, root)
      cur = next
    }
    return root
  }

  union(a, b) {
    const ra = this.find(a)
    const rb = this.find(b)
    if (ra !== rb) this.parent.set(rb, ra)
  }
}

function propLabel(p) {
  return `P${p.sequenceNo}`
}

function getPropMembers(rel) {
  const ids = rel.members?.length ? rel.members : [rel.source, rel.target]
  return ids.filter((id) => typeof id === 'string' && id.startsWith('P'))
}

function propBoxSize(label) {
  return { width: Math.max(42, label.length * 9 + 16), height: PROP_H }
}

function toBounds(x, y, width, height) {
  return { x, y, width, height, cx: x + width / 2, cy: y + height / 2 }
}

function portPoint(bounds, port) {
  const { x, y, width, height, cx, cy } = bounds
  switch (port) {
    case 'top': return { x: cx, y }
    case 'bottom': return { x: cx, y: y + height }
    case 'left': return { x, y: cy }
    case 'right': return { x: x + width, y: cy }
    default: return { x: cx, y: cy }
  }
}

function choosePorts(from, to) {
  const dy = to.cy - from.cy
  const dx = to.cx - from.cx
  if (Math.abs(dy) >= Math.abs(dx)) {
    return dy >= 0
      ? { sourcePort: 'bottom', targetPort: 'top' }
      : { sourcePort: 'top', targetPort: 'bottom' }
  }
  return dx >= 0
    ? { sourcePort: 'right', targetPort: 'left' }
    : { sourcePort: 'left', targetPort: 'right' }
}

function oneBendVertices(from, to, sourcePort, targetPort) {
  const a = portPoint(from, sourcePort)
  const b = portPoint(to, targetPort)
  if (Math.abs(a.x - b.x) < 2 && Math.abs(a.y - b.y) < 2) return []

  if ((sourcePort === 'bottom' && targetPort === 'top') || (sourcePort === 'top' && targetPort === 'bottom')) {
    if (Math.abs(a.x - b.x) < 2) return []
    return [{ x: a.x, y: b.y }]
  }
  if ((sourcePort === 'right' && targetPort === 'left') || (sourcePort === 'left' && targetPort === 'right')) {
    if (Math.abs(a.y - b.y) < 2) return []
    return [{ x: b.x, y: a.y }]
  }
  return [{ x: a.x, y: b.y }]
}

function makeSmartEdge(id, fromBounds, toBounds, fromId, toId, directed, ports) {
  const picked = ports || choosePorts(fromBounds, toBounds)
  const { sourcePort, targetPort } = picked
  return {
    id,
    source: { cell: fromId, port: sourcePort },
    target: { cell: toId, port: targetPort },
    vertices: oneBendVertices(fromBounds, toBounds, sourcePort, targetPort),
    connector: { name: 'normal' },
    attrs: {
      line: {
        stroke: '#111',
        strokeWidth: 1.2,
        targetMarker: directed
          ? { name: 'block', width: 7, height: 5, fill: '#111' }
          : null
      }
    }
  }
}

function hubBetween(from, to, size) {
  const { sourcePort, targetPort } = choosePorts(from, to)
  const a = portPoint(from, sourcePort)
  const b = portPoint(to, targetPort)
  let cx = (a.x + b.x) / 2
  let cy = (a.y + b.y) / 2
  if (sourcePort === 'bottom' && targetPort === 'top') {
    cx = to.cx
    cy = (a.y + b.y) / 2
  } else if (sourcePort === 'top' && targetPort === 'bottom') {
    cx = to.cx
    cy = (a.y + b.y) / 2
  }
  return toBounds(cx - size / 2, cy - size / 2, size, size)
}

function overlaps(a, b, gap = 10) {
  return !(
    a.x + a.width + gap <= b.x
    || b.x + b.width + gap <= a.x
    || a.y + a.height + gap <= b.y
    || b.y + b.height + gap <= a.y
  )
}

function separatePropNodes(posMap, propIds) {
  const ids = [...propIds]
  for (let round = 0; round < 8; round += 1) {
    let moved = false
    for (let i = 0; i < ids.length; i += 1) {
      for (let j = i + 1; j < ids.length; j += 1) {
        const a = posMap.get(ids[i])
        const b = posMap.get(ids[j])
        if (!a || !b || !overlaps(a, b, 8)) continue
        if (Math.abs(a.cy - b.cy) < 20) {
          const shift = (a.width + b.width) / 2 + 16
          if (a.cx <= b.cx) {
            a.x -= shift / 2
            b.x += shift / 2
          } else {
            a.x += shift / 2
            b.x -= shift / 2
          }
        } else if (a.cy <= b.cy) {
          b.y = a.y + a.height + 14
        } else {
          a.y = b.y + b.height + 14
        }
        a.cx = a.x + a.width / 2
        a.cy = a.y + a.height / 2
        b.cx = b.x + b.width / 2
        b.cy = b.y + b.height / 2
        moved = true
      }
    }
    if (!moved) break
  }
}

export function buildGraphModel(propositions, relations) {
  const propMap = new Map(propositions.map((p) => [p.propId, p]))
  const uf = new UnionFind(propositions.map((p) => p.propId))

  relations
    .filter((rel) => String(rel.type).toUpperCase() === 'I')
    .forEach((rel) => {
      const members = getPropMembers(rel).filter((id) => propMap.has(id))
      for (let i = 1; i < members.length; i += 1) uf.union(members[0], members[i])
    })

  const grouped = new Map()
  propositions.forEach((p) => {
    const root = uf.find(p.propId)
    if (!grouped.has(root)) grouped.set(root, [])
    grouped.get(root).push(p)
  })

  const propToNodeId = new Map()
  const propMeta = new Map()
  const layoutNodes = []
  const layoutEdges = []
  const deferred = []

  grouped.forEach((items, root) => {
    const sorted = [...items].sort((a, b) => a.sequenceNo - b.sequenceNo)
    const nodeId = `prop-${root}`
    const label = sorted.map(propLabel).join(' / ')
    const size = propBoxSize(label)
    layoutNodes.push({ id: nodeId, width: size.width, height: size.height })
    propMeta.set(nodeId, { label, ...size })
    sorted.forEach((p) => propToNodeId.set(p.propId, nodeId))
  })

  relations.forEach((rel, index) => {
    const type = String(rel.type || 'S').toUpperCase()
    const relKey = rel.relId || `R${index + 1}`

    if (type === 'I') return

    if (type === 'J') {
      const members = getPropMembers(rel).filter((id) => propToNodeId.has(id))
      if (members.length < 2) return
      const outbound = rel.target && !members.includes(rel.target) ? propToNodeId.get(rel.target) : null
      deferred.push({ kind: 'J', relKey, members, outbound })
      return
    }

    if (!['S', 'A', 'M'].includes(type)) return

    const sourceId = propToNodeId.get(rel.source)
    const targetId = propToNodeId.get(rel.target)
    if (!sourceId || !targetId || sourceId === targetId) return

    layoutEdges.push({ source: sourceId, target: targetId })
    deferred.push({ kind: type, relKey, sourceId, targetId })
  })

  if (!layoutNodes.length) return { nodes: [], edges: [] }

  let positioned = layoutNodes
  if (layoutEdges.length) {
    positioned = dagreLayout.layout({ nodes: layoutNodes, edges: layoutEdges }).nodes
  } else {
    const startX = 40
    positioned = layoutNodes.map((node, idx) => ({
      ...node,
      x: startX + idx * (node.width + GAP.node),
      y: 40 + node.height / 2
    }))
  }

  const posMap = new Map()
  positioned.forEach((node) => {
    if (!node.id.startsWith('prop-')) return
    const meta = propMeta.get(node.id)
    posMap.set(node.id, toBounds(
      node.x - node.width / 2,
      node.y - node.height / 2,
      meta.width,
      meta.height
    ))
  })

  separatePropNodes(posMap, [...propMeta.keys()])

  const x6Nodes = []
  const x6Edges = []
  const hubOffset = new Map()

  function nextHubOffset(anchorId) {
    const n = hubOffset.get(anchorId) || 0
    hubOffset.set(anchorId, n + 1)
    return (n - 0.5) * 16
  }

  propMeta.forEach((meta, nodeId) => {
    const pos = posMap.get(nodeId)
    x6Nodes.push({
      id: nodeId,
      shape: 'prop-box',
      x: pos.x,
      y: pos.y,
      width: meta.width,
      height: meta.height,
      label: meta.label
    })
  })

  deferred
    .filter((item) => item.kind === 'J')
    .forEach((item) => {
      const memberNodes = item.members
        .map((id) => ({ id: propToNodeId.get(id), propId: id }))
        .filter((m) => m.id && posMap.has(m.id))
        .sort((a, b) => (propMap.get(a.propId)?.sequenceNo || 0) - (propMap.get(b.propId)?.sequenceNo || 0))

      if (memberNodes.length < 2) return

      const hubId = `hub-j-${item.relKey}`
      const span = (memberNodes.length - 1) * GAP.jointCol
      const anchorY = Math.min(...memberNodes.map((m) => posMap.get(m.id).y)) - GAP.jointRow
      const centerX = memberNodes.reduce((s, m) => s + posMap.get(m.id).cx, 0) / memberNodes.length

      memberNodes.forEach((member, idx) => {
        const meta = propMeta.get(member.id)
        const cx = centerX - span / 2 + idx * GAP.jointCol
        const bounds = toBounds(cx - meta.width / 2, anchorY, meta.width, meta.height)
        posMap.set(member.id, bounds)
        const existing = x6Nodes.find((n) => n.id === member.id)
        if (existing) {
          existing.x = bounds.x
          existing.y = bounds.y
        }
      })

      separatePropNodes(posMap, [...propMeta.keys()])

      const memberBottom = Math.max(...memberNodes.map((m) => posMap.get(m.id).y + posMap.get(m.id).height))
      const hubBounds = toBounds(centerX - 11, memberBottom + 18, 22, 22)
      posMap.set(hubId, hubBounds)

      x6Nodes.push({
        id: hubId,
        shape: 'hub-plus',
        x: hubBounds.x,
        y: hubBounds.y,
        width: 22,
        height: 22
      })

      memberNodes.forEach((member) => {
        const m = posMap.get(member.id)
        const ports = m.cx < hubBounds.cx - 4
          ? { sourcePort: 'right', targetPort: 'left' }
          : m.cx > hubBounds.cx + 4
            ? { sourcePort: 'left', targetPort: 'right' }
            : { sourcePort: 'bottom', targetPort: 'top' }
        x6Edges.push(makeSmartEdge(
          `e-j-in-${item.relKey}-${member.id}`,
          m,
          hubBounds,
          member.id,
          hubId,
          false,
          ports
        ))
      })

      const outbound = item.outbound
      if (outbound && posMap.has(outbound)) {
        x6Edges.push(makeSmartEdge(
          `e-j-out-${item.relKey}`,
          hubBounds,
          posMap.get(outbound),
          hubId,
          outbound,
          true
        ))
      }
    })

  deferred
    .filter((item) => ['S', 'A', 'M'].includes(item.kind))
    .forEach((item) => {
      const src = posMap.get(item.sourceId)
      const tgt = posMap.get(item.targetId)
      if (!src || !tgt) return

      const hubId = `hub-${item.kind}-${item.relKey}`
      const hubSize = item.kind === 'S' ? 8 : item.kind === 'A' ? 14 : 22
      const hubShape = item.kind === 'S' ? 'hub-support' : item.kind === 'A' ? 'hub-attack' : 'hub-plus'

      let hubBounds = hubBetween(src, tgt, hubSize)
      hubBounds.x += nextHubOffset(item.targetId)
      hubBounds.cx = hubBounds.x + hubSize / 2
      posMap.set(hubId, hubBounds)

      x6Nodes.push({
        id: hubId,
        shape: hubShape,
        x: hubBounds.x,
        y: hubBounds.y,
        width: hubSize,
        height: hubSize
      })

      x6Edges.push(makeSmartEdge(
        `e-${item.kind}-in-${item.relKey}`,
        src,
        hubBounds,
        item.sourceId,
        hubId,
        false
      ))
      x6Edges.push(makeSmartEdge(
        `e-${item.kind}-out-${item.relKey}`,
        hubBounds,
        tgt,
        hubId,
        item.targetId,
        true
      ))
    })

  return { nodes: x6Nodes, edges: x6Edges }
}
