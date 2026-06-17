const DRAFT_KEY = 'jap_task_create_draft'

export function emptyTaskCreateDraft() {
  return {
    taskName: '',
    description: '',
    deadline: '',
    configId: null,
    annotatorIds: [],
    reviewerId: null,
    documents: []
  }
}

export function loadTaskCreateDraft() {
  try {
    const raw = sessionStorage.getItem(DRAFT_KEY)
    if (!raw) return emptyTaskCreateDraft()
    const parsed = JSON.parse(raw)
    return { ...emptyTaskCreateDraft(), ...parsed, documents: parsed.documents || [] }
  } catch {
    return emptyTaskCreateDraft()
  }
}

export function saveTaskCreateDraft(draft) {
  sessionStorage.setItem(DRAFT_KEY, JSON.stringify(draft))
}

export function clearTaskCreateDraft() {
  sessionStorage.removeItem(DRAFT_KEY)
}

export function documentKey(doc) {
  if (doc.key) return doc.key
  if (doc.sourceType === 'GLOBAL') return `GLOBAL-${doc.globalDocId}`
  if (doc.sourceType === 'RECREATE') return `RECREATE-${doc.globalDocId}-${doc.fileName}`
  return `UPLOAD-${doc.fileName}-${(doc.extractedText || '').length}`
}

export function sourceTypeLabel(sourceType) {
  if (sourceType === 'UPLOAD') return '自主上传'
  if (sourceType === 'RECREATE') return '范围修改'
  return '文书总库'
}

export function toCreatePayload(draft) {
  return {
    taskName: draft.taskName,
    description: draft.description,
    deadline: draft.deadline || '',
    configId: draft.configId,
    annotatorIds: draft.annotatorIds,
    reviewerId: draft.reviewerId,
    documents: (draft.documents || []).map((doc) => {
      if (doc.sourceType === 'GLOBAL') {
        return { sourceType: 'GLOBAL', globalDocId: doc.globalDocId }
      }
      if (doc.sourceType === 'RECREATE') {
        return {
          sourceType: 'RECREATE',
          globalDocId: doc.globalDocId,
          fileName: doc.fileName,
          extractedText: doc.extractedText
        }
      }
      return {
        sourceType: 'UPLOAD',
        fileName: doc.fileName,
        extractedText: doc.extractedText
      }
    })
  }
}
