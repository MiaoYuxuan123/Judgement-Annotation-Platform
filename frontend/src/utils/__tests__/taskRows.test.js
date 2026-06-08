import { describe, expect, it } from 'vitest'
import {
  allAnnotatorsSubmitted,
  canParticipantAnnotateDoc,
  hasAnyAnnotatorSubmitted,
  participantActions,
  resolveDocStage,
  resolveDocStageForParticipant,
  resolveTaskViewerRoles,
  reviewerCanAccessReview
} from '../taskRows.js'

describe('taskRows', () => {
  it('moves a submitted annotator document to arbitration stage', () => {
    const stage = resolveDocStage({
      annotatorResults: [
        { userId: 3, draft: false, propositions: [{ propId: 'P1' }], relations: [] }
      ]
    }, { viewerRole: 'annotator', userId: 3, documentStatus: '标注中' })

    expect(stage).toBe('待裁定')
  })

  it('marks documents with final result as exportable', () => {
    const stage = resolveDocStage({
      finalResult: { finalResult: true, propositions: [{ propId: 'P1' }], relations: [] }
    }, { viewerRole: 'reviewer', documentStatus: '待裁定' })

    expect(stage).toBe('可导出')
  })

  it('returns both annotate and arbitrate entries for dual-role participants', () => {
    const roles = resolveTaskViewerRoles({
      annotators: [{ id: 3, username: 'annotator1' }],
      reviewer: { id: 3, username: 'annotator1' }
    }, { id: 3, username: 'annotator1' })

    expect(roles.map((role) => role.role)).toEqual(['annotate', 'arbitrate'])
  })

  it('offers export action when participant row is exportable', () => {
    const actions = participantActions({
      taskId: 1001,
      personalStage: '可导出',
      roles: [{ role: 'annotate' }],
      detail: { documents: [] }
    }, 3)

    expect(actions).toHaveLength(1)
    expect(actions[0].label).toBe('查看结果/导出')
  })

  it('allows reviewer early access when any annotator submitted', () => {
    const entry = {
      annotatorResults: [
        { userId: 3, draft: false, propositions: [{ propId: 'P1' }], relations: [] }
      ]
    }
    expect(reviewerCanAccessReview(entry, { annotatorCount: 2, documentStatus: '标注中' })).toBe(true)
    expect(allAnnotatorsSubmitted(entry, 2)).toBe(false)
    expect(hasAnyAnnotatorSubmitted(entry)).toBe(true)
  })

  it('blocks final adjudication until all annotators submit', () => {
    const entry = {
      annotatorResults: [
        { userId: 3, draft: false, propositions: [{ propId: 'P1' }], relations: [] },
        { userId: 4, draft: false, propositions: [{ propId: 'P1' }], relations: [] }
      ]
    }
    expect(allAnnotatorsSubmitted(entry, 2)).toBe(true)
    expect(resolveDocStage(entry, { viewerRole: 'reviewer', annotatorCount: 2, documentStatus: '标注中' })).toBe('待裁定')
  })

  it('prioritizes personal annotator stage for dual-role participants after submit', () => {
    const entry = {
      annotatorResults: [
        { userId: 3, draft: false, propositions: [{ propId: 'P1' }], relations: [] }
      ]
    }
    const options = {
      annotatorCount: 2,
      documentStatus: '标注中',
      userId: 3,
      isAnnotator: true,
      isReviewer: true
    }

    expect(resolveDocStageForParticipant(entry, options)).toBe('待裁定')
    expect(canParticipantAnnotateDoc(entry, options)).toBe(false)
    expect(reviewerCanAccessReview(entry, options)).toBe(true)
  })
})
