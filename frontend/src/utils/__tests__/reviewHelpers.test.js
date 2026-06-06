import { describe, expect, it } from 'vitest'
import {
  buildAnnotatedParts,
  findAvailableTextSpan,
  formatRelationFormula
} from '../reviewHelpers.js'

describe('reviewHelpers', () => {
  it('finds the next available non-overlapping text span', () => {
    const content = '合同成立。合同生效。'
    const result = findAvailableTextSpan(content, '合同', [
      { startPos: 0, endPos: 2 }
    ])

    expect(result).toEqual({ start: 5, end: 7 })
  })

  it('builds annotated text parts while preserving plain text', () => {
    const parts = buildAnnotatedParts('依法成立的合同', [
      { sequenceNo: 1, startPos: 0, endPos: 4, tag: 'GM-L' }
    ])

    expect(parts).toEqual([
      { type: 'prop', text: '依法成立', sequenceNo: 1, label: 'P1', tag: 'GM-L' },
      { type: 'text', text: '的合同' }
    ])
  })

  it('formats relation formulas with P-style proposition labels', () => {
    const formula = formatRelationFormula(
      { relId: 'R1', type: 'S', source: 'a', target: 'b' },
      [
        { propId: 'a', sequenceNo: 1 },
        { propId: 'b', sequenceNo: 2 }
      ]
    )

    expect(formula).toBe('S(P1, P2)')
  })
})
