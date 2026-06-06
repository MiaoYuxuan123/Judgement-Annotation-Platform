import { describe, expect, it } from 'vitest'
import { buildElkGraph } from '../argumentGraphElkLayout.js'

const propositions = [
  { propId: 'P1', sequenceNo: 1 },
  { propId: 'P2', sequenceNo: 2 },
  { propId: 'P3', sequenceNo: 3 },
  { propId: 'P4', sequenceNo: 4 }
]

function center(node) {
  const width = Number.parseFloat(node.style.width)
  const height = Number.parseFloat(node.style.height)
  return {
    x: node.position.x + width / 2,
    y: node.position.y + height / 2
  }
}

function nodeByLabel(graph, label) {
  return graph.nodes.find((node) => node.data?.label === label)
}

function nodesByType(graph, type) {
  return graph.nodes.filter((node) => node.type === type)
}

describe('argumentGraphElkLayout', () => {
  it('reuses the J plus for M(J(P1,P2,P3), P4)', () => {
    const graph = buildElkGraph(propositions, [
      { relId: 'R1', type: 'J', members: ['P1', 'P2', 'P3'] },
      { relId: 'R2', type: 'M', members: ['R1', 'P4'] }
    ])

    const jHubs = nodesByType(graph, 'hub-j')
    expect(jHubs).toHaveLength(1)
    expect(nodesByType(graph, 'hub-m')).toHaveLength(0)

    const p4 = nodeByLabel(graph, 'P4')
    const directedToP4 = graph.edges.find((edge) => edge.data.directed && edge.target === p4.id)
    expect(directedToP4?.source).toBe(jHubs[0].id)
  })

  it('aligns A(P3, S(P1,P2)) relation hubs on one axis', () => {
    const graph = buildElkGraph(propositions, [
      { relId: 'R1', type: 'S', members: ['P1', 'P2'] },
      { relId: 'R2', type: 'A', members: ['P3', 'R1'] }
    ])

    const aHub = nodesByType(graph, 'hub-a')[0]
    const sHub = nodesByType(graph, 'hub-s')[0]
    const p3 = nodeByLabel(graph, 'P3')
    expect(Math.abs(center(aHub).y - center(sHub).y)).toBeLessThan(1)
    expect(Math.abs(center(aHub).y - center(p3).y)).toBeLessThan(1)

    const edge = graph.edges.find((candidate) => candidate.source === aHub.id && candidate.target === sHub.id)
    expect(edge?.data.directed).toBe(true)
  })

  it('places the support dot between P2 and P3 for S(M(P1,P2), P3)', () => {
    const graph = buildElkGraph(propositions, [
      { relId: 'R1', type: 'M', members: ['P1', 'P2'] },
      { relId: 'R2', type: 'S', members: ['R1', 'P3'] }
    ])

    const p2 = nodeByLabel(graph, 'P2')
    const p3 = nodeByLabel(graph, 'P3')
    const sHub = nodesByType(graph, 'hub-s')[0]
    const sCenter = center(sHub)

    expect(sCenter.y).toBeGreaterThan(center(p2).y)
    expect(sCenter.y).toBeLessThan(center(p3).y)
    expect(graph.edges.some((edge) => edge.source === p2.id && edge.target === sHub.id)).toBe(true)
  })

  it('renders I(P1,P2) as a single merged proposition rectangle', () => {
    const graph = buildElkGraph(propositions, [
      { relId: 'R1', type: 'I', members: ['P1', 'P2'] }
    ])

    const propNodes = nodesByType(graph, 'prop')
    expect(propNodes).toHaveLength(1)
    expect(propNodes[0].data.label).toBe('P1 / P2')
  })
})
