import { describe, expect, it } from 'vitest'
import { annotationItemUrl, arbitrationDraftQuery, graphEditorRoute, isArbitrationMode } from '../annotationRoute.js'

describe('annotationRoute', () => {
  it('detects arbitration mode from query', () => {
    expect(isArbitrationMode({ mode: 'arbitration' })).toBe(true)
    expect(isArbitrationMode({})).toBe(false)
  })

  it('builds item url with source user for partial modify', () => {
    expect(annotationItemUrl(1, 2, {
      mode: 'arbitration',
      fromUserId: '42'
    })).toBe('/tasks/1/items/2?sourceUserId=42')
  })

  it('builds item url with source arbitration flag', () => {
    expect(annotationItemUrl(1, 2, {
      mode: 'arbitration',
      fromFinal: '1'
    })).toBe('/tasks/1/items/2?sourceArbitration=1')
  })

  it('preserves query when opening graph editor', () => {
    const query = { mode: 'arbitration', fromUserId: '7', returnTo: '/review/1' }
    expect(graphEditorRoute('1', '2', query)).toEqual({
      path: '/annotate/1/2/graph',
      query
    })
  })

  it('normalizes query after arbitration graph save', () => {
    expect(arbitrationDraftQuery({
      mode: 'arbitration',
      fromUserId: '7',
      returnTo: '/review/1'
    })).toEqual({
      mode: 'arbitration',
      fromFinal: '1',
      returnTo: '/review/1'
    })
  })
})
