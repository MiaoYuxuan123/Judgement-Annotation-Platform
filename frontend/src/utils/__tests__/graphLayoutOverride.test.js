import { describe, expect, it } from 'vitest'
import {
  applyLayoutOverride,
  EMPTY_LAYOUT,
  extractLayoutOverride,
  handleConnectionPoint,
  isLayoutEmpty,
  graphLayoutForSave,
  updateNodePosition
} from '../graphLayoutOverride.js'

describe('graphLayoutOverride', () => {
  const baseLayout = {
    nodes: [
      {
        id: 'P1',
        type: 'prop',
        position: { x: 0, y: 0 },
        style: { width: '60px', height: '34px' },
        data: { label: 'P1' }
      },
      {
        id: 'P2',
        type: 'prop',
        position: { x: 200, y: 0 },
        style: { width: '60px', height: '34px' },
        data: { label: 'P2' }
      }
    ],
    edges: [
      {
        id: 'e1',
        source: 'P1',
        target: 'P2',
        data: { directed: true, points: [{ x: 60, y: 17 }, { x: 120, y: 17 }, { x: 120, y: 17 }, { x: 200, y: 17 }] }
      }
    ]
  }

  it('returns empty layout template', () => {
    expect(EMPTY_LAYOUT()).toEqual({ version: 1, nodePositions: {}, edgeStyles: {} })
    expect(isLayoutEmpty(EMPTY_LAYOUT())).toBe(true)
  })

  it('detects v2 document layout as non-empty', () => {
    const v2 = { version: 2, nodes: [{ id: 'P1' }], edges: [] }
    expect(isLayoutEmpty(v2)).toBe(false)
    expect(graphLayoutForSave(v2)).toEqual(v2)
  })

  it('omits empty v1 layout on save to avoid overwriting v2', () => {
    expect(graphLayoutForSave(EMPTY_LAYOUT())).toBeNull()
  })

  it('applies node position override', () => {
    const merged = applyLayoutOverride(baseLayout, {
      version: 1,
      nodePositions: { P1: { x: 40, y: 80 } },
      edgeStyles: {}
    })
    expect(merged.nodes[0].position).toEqual({ x: 40, y: 80 })
    expect(merged.nodes[1].position).toEqual({ x: 200, y: 0 })
  })

  it('extracts moved node positions', () => {
    const merged = applyLayoutOverride(baseLayout, {
      version: 1,
      nodePositions: { P1: { x: 40, y: 80 } },
      edgeStyles: {}
    })
    const extracted = extractLayoutOverride(merged.nodes, merged.edges, baseLayout)
    expect(extracted.nodePositions.P1).toEqual({ x: 40, y: 80 })
  })

  it('updates node position helper', () => {
    const next = updateNodePosition(EMPTY_LAYOUT(), 'P1', { x: 12.4, y: 20.6 })
    expect(next.nodePositions.P1).toEqual({ x: 12, y: 21 })
  })

  it('resolves eight handle connection points', () => {
    const node = baseLayout.nodes[0]
    expect(handleConnectionPoint(node, 'right')).toEqual({ x: 60, y: 17 })
    expect(handleConnectionPoint(node, 'top-left')).toEqual({ x: 0, y: 0 })
    expect(handleConnectionPoint(node, 'bottom-right')).toEqual({ x: 60, y: 34 })
  })
})
