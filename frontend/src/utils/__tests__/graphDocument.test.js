import { describe, expect, it } from 'vitest'
import {
  createPropNode,
  createPropNodeFromProposition,
  createHubNode,
  createEdge,
  segmentMidpoints,
  insertBendAtSegment,
  moveBendPoint,
  getSnapAnchorExcludeIds,
  snapBendInPath,
  snapNodePosition,
  snapNodesGroupPositions,
  EMPTY_DOCUMENT,
  nextPropId,
  getCanvasPropIds,
  computeGridPositions,
  findNearestHandle,
  findReconnectHandleSnap,
  getNodeHandles,
  CARDINAL_HANDLE_IDS,
  mergePropNodesAsIdentity,
  rebuildEdgePaths,
  inferHandleId,
  inferHandleIdFromDirection,
  ensureEdgeHandles,
  updateEdgesAfterNodeMove
} from '../graphDocument.js'

describe('graphDocument', () => {
  it('creates proposition and hub nodes', () => {
    const prop = createPropNode('P1', 10, 20)
    expect(prop.type).toBe('prop')
    expect(prop.position).toEqual({ x: 10, y: 20 })

    const hub = createHubNode('S', 'R1', 100, 100)
    expect(hub.type).toBe('hub-s')
    expect(hub.data.relKey).toBe('R1')
  })

  it('generates next proposition id', () => {
    const nodes = [createPropNode('P1', 0, 0), createPropNode('P2', 0, 0)]
    expect(nextPropId(nodes)).toBe('P3')
  })

  it('computes segment midpoints for draw.io style editing', () => {
    const points = [{ x: 0, y: 0 }, { x: 100, y: 0 }, { x: 100, y: 50 }]
    const mids = segmentMidpoints(points)
    expect(mids).toHaveLength(2)
    expect(mids[0]).toMatchObject({ segmentIndex: 0, x: 50, y: 0 })
    expect(mids[1]).toMatchObject({ segmentIndex: 1, x: 100, y: 25 })
  })

  it('inserts bend at segment midpoint', () => {
    const points = [{ x: 0, y: 0 }, { x: 100, y: 0 }]
    const { points: next, bendIndex } = insertBendAtSegment(points, 0)
    expect(next).toHaveLength(3)
    expect(next[1]).toEqual({ x: 50, y: 0 })
    expect(bendIndex).toBe(1)
  })

  it('moves interior bend point', () => {
    const points = [{ x: 0, y: 0 }, { x: 50, y: 0 }, { x: 100, y: 0 }]
    const moved = moveBendPoint(points, 1, { x: 50, y: 40 })
    expect(moved[1]).toEqual({ x: 50, y: 40 })
  })

  it('rebuilds edge paths from node handles', () => {
    const nodes = [createPropNode('P1', 0, 0), createPropNode('P2', 200, 0)]
    const edges = [createEdge('e1', 'P1', 'P2', 'right', 'left')]
    const rebuilt = rebuildEdgePaths(nodes, edges)
    expect(rebuilt[0].data.points.length).toBeGreaterThanOrEqual(2)
    expect(rebuilt[0].data.path).toContain('M')
  })

  it('infers top/bottom handles when importing auto-layout edges', () => {
    const nodes = [createPropNode('P1', 100, 100), createPropNode('P2', 100, 200)]
    const sourceBottom = { x: 130, y: 134 }
    const targetTop = { x: 130, y: 200 }
    const edges = [{
      id: 'e1',
      source: 'P1',
      target: 'P2',
      type: 'polyline',
      data: {
        points: [sourceBottom, targetTop],
        directed: true
      }
    }]
    const rebuilt = rebuildEdgePaths(nodes, edges, { inferHandles: true })
    expect(rebuilt[0].sourceHandle).toBe('bottom')
    expect(rebuilt[0].targetHandle).toBe('top')
    expect(inferHandleId(nodes[0], sourceBottom)).toBe('bottom')
    expect(inferHandleId(nodes[1], targetTop)).toBe('top')
  })

  it('preserves target handle when only source handle changes', () => {
    const nodes = [createPropNode('P1', 0, 0), createPropNode('P2', 200, 0)]
    const edges = [{
      id: 'e1',
      source: 'P1',
      target: 'P2',
      sourceHandle: 'right',
      targetHandle: 'left',
      type: 'polyline',
      data: {
        points: [{ x: 60, y: 17 }, { x: 200, y: 17 }],
        waypoints: [],
        directed: false
      }
    }]
    const updated = [{
      ...edges[0],
      sourceHandle: 'bottom'
    }]
    const rebuilt = rebuildEdgePaths(nodes, updated, { inferHandles: false })
    expect(rebuilt[0].sourceHandle).toBe('bottom')
    expect(rebuilt[0].targetHandle).toBe('left')
  })

  it('findReconnectHandleSnap prefers handles on the anchor node', () => {
    const nodes = [createPropNode('P1', 0, 0), createPropNode('P2', 200, 0)]
    const edge = { source: 'P1', target: 'P2' }
    const topHandle = { x: 30, y: 0 }
    const snap = findReconnectHandleSnap(nodes, topHandle, edge, 'source')
    expect(snap?.nodeId).toBe('P1')
    expect(snap?.handleId).toBe('top')
  })

  it('corrects mismatched default left/right handles from saved paths', () => {
    const nodes = [createPropNode('P1', 100, 100), createPropNode('P2', 100, 220)]
    const edges = [{
      id: 'e1',
      source: 'P1',
      target: 'P2',
      sourceHandle: 'right',
      targetHandle: 'left',
      type: 'polyline',
      data: {
        points: [{ x: 130, y: 134 }, { x: 130, y: 220 }],
        directed: true
      }
    }]
    const rebuilt = rebuildEdgePaths(nodes, edges, { inferHandles: true })
    expect(rebuilt[0].sourceHandle).toBe('bottom')
    expect(rebuilt[0].targetHandle).toBe('top')
  })

  it('normalizes J relation hub-center endpoints to border handles', () => {
    const hub = createHubNode('J', 'R1', 200, 100)
    const left = createPropNode('P1', 100, 100)
    const right = createPropNode('P2', 280, 100)
    const nodes = [left, hub, right]
    const hubCenter = { x: 212, y: 112 }

    const edges = [{
      id: 'e1',
      source: 'P1',
      target: hub.id,
      type: 'polyline',
      data: { points: [{ x: 160, y: 117 }, hubCenter], directed: false }
    }, {
      id: 'e2',
      source: hub.id,
      target: 'P2',
      type: 'polyline',
      data: { points: [hubCenter, { x: 280, y: 117 }], directed: false }
    }]

    const rebuilt = rebuildEdgePaths(nodes, edges, { inferHandles: true })
    expect(rebuilt[0].targetHandle).toBe('left')
    expect(rebuilt[1].sourceHandle).toBe('right')

    const inEnd = rebuilt[0].data.points[rebuilt[0].data.points.length - 1]
    const outStart = rebuilt[1].data.points[0]
    expect(Math.hypot(inEnd.x - hubCenter.x, inEnd.y - hubCenter.y)).toBeGreaterThan(8)
    expect(Math.hypot(outStart.x - hubCenter.x, outStart.y - hubCenter.y)).toBeGreaterThan(8)
    expect(inferHandleIdFromDirection(hub, left.position)).toBe('left')
  })

  it('empty document template', () => {
    expect(EMPTY_DOCUMENT()).toEqual({ version: 2, nodes: [], edges: [] })
  })

  it('tracks canvas proposition ids', () => {
    const nodes = [createPropNode('P1', 0, 0), createHubNode('S', 'R1', 0, 0)]
    expect(getCanvasPropIds(nodes)).toEqual(new Set(['P1']))
  })

  it('merges proposition nodes into identity group', () => {
    const nodes = [createPropNode('P1', 0, 0), createPropNode('P2', 120, 0)]
    const edges = [createEdge('e1', 'P1', 'P2', 'right', 'left')]
    const result = mergePropNodesAsIdentity(nodes, edges, ['P1', 'P2'])
    expect(result.nodes).toHaveLength(1)
    expect(result.nodes[0].data.label).toBe('P1 / P2')
    expect(result.nodes[0].data.identityMembers).toEqual(['P1', 'P2'])
    expect(getCanvasPropIds(result.nodes)).toEqual(new Set(['P1', 'P2']))
    expect(result.edges).toHaveLength(0)
  })

  it('merges edges when combining identity nodes', () => {
    const hub = createHubNode('S', 'R1', 60, 0)
    const nodes = [createPropNode('P1', 0, 0), createPropNode('P2', 120, 0), hub]
    const edges = [
      createEdge('e1', 'P1', hub.id, 'right', 'left'),
      createEdge('e2', 'P2', hub.id, 'right', 'left')
    ]
    const result = mergePropNodesAsIdentity(nodes, edges, ['P1', 'P2'])
    expect(result.nodes).toHaveLength(2)
    expect(result.edges).toHaveLength(1)
    expect(result.edges[0].source).toBe('P1')
    expect(result.edges[0].target).toBe(hub.id)
  })

  it('computes grid positions for batch add', () => {
    const positions = computeGridPositions([], 3)
    expect(positions).toHaveLength(3)
    expect(positions[0]).toEqual({ x: 80, y: 80 })
  })

  it('creates prop node from proposition data', () => {
    const node = createPropNodeFromProposition({ propId: 'P2', tag: 'SF', text: 'hello' }, 10, 20)
    expect(node.id).toBe('P2')
    expect(node.data.tag).toBe('SF')
  })

  it('translates edge waypoints when nodes move', () => {
    const nodes = [createPropNode('P1', 0, 0), createPropNode('P2', 200, 0)]
    const edges = [{
      ...createEdge('e1', 'P1', 'P2', 'right', 'left'),
      data: { waypoints: [{ x: 100, y: 0 }, { x: 100, y: 20 }] }
    }]
    const deltas = new Map([['P1', { dx: 10, dy: 5 }]])
    const updated = updateEdgesAfterNodeMove(nodes, edges, deltas)
    expect(updated[0].data.waypoints[0]).toEqual({ x: 110, y: 5 })
  })

  it('snaps bend points to horizontal and vertical guides', () => {
    const points = [{ x: 0, y: 0 }, { x: 52, y: 18 }, { x: 100, y: 0 }]
    const horizontal = snapBendInPath(points, 1, { x: 50, y: 3 })
    expect(horizontal.y).toBe(0)
    expect(horizontal.hint).toBe('horizontal')

    const vertical = snapBendInPath(points, 1, { x: 98, y: 40 })
    expect(vertical.x).toBe(100)
    expect(vertical.hint).toBe('vertical')
  })

  it('clears waypoints when explicitly empty', () => {
    const nodes = [createPropNode('P1', 0, 0), createPropNode('P2', 200, 0)]
    const edges = [{
      ...createEdge('e1', 'P1', 'P2', 'right', 'left'),
      data: {
        waypoints: [],
        points: [{ x: 0, y: 17 }, { x: 100, y: 17 }, { x: 100, y: 0 }, { x: 200, y: 17 }]
      }
    }]
    const rebuilt = rebuildEdgePaths(nodes, edges)
    expect(rebuilt[0].data.waypoints).toEqual([])
    expect(rebuilt[0].data.points).toHaveLength(2)
  })

  it('snaps dragged node to nearby node alignment', () => {
    const nodes = [createPropNode('P1', 0, 0), createPropNode('P2', 200, 0)]
    const snapped = snapNodePosition(nodes[1], { x: 198, y: 6 }, nodes, new Set(['P2']))
    expect(snapped.y).toBe(0)
    expect(snapped.hint).toBe('horizontal')
  })

  it('does not snap hub to connected proposition nodes', () => {
    const nodes = [createPropNode('P1', 0, 0), createHubNode('S', 'R1', 100, 10)]
    const hub = { ...nodes[1], position: { x: 100, y: 6 } }
    const edges = [createEdge('e1', 'P1', hub.id, 'right', 'left')]
    const excludeIds = getSnapAnchorExcludeIds([hub], edges)
    const snapped = snapNodePosition(hub, hub.position, nodes, excludeIds)
    expect(snapped.y).toBe(6)
    expect(snapped.hint).toBe('')
  })

  it('finds nearest cardinal handle for hub nodes', () => {
    const nodes = [createHubNode('S', 'R1', 100, 100)]
    const near = findNearestHandle(nodes, { x: nodes[0].position.x + 7, y: nodes[0].position.y + 7 })
    expect(near?.handleId).toBe('top')
    expect(getNodeHandles(nodes[0])).toEqual(CARDINAL_HANDLE_IDS)
  })
})
