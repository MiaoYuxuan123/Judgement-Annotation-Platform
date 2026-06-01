export function emptyTaskForm() {
  return {
    taskName: '',
    description: '',
    documentIds: [],
    annotatorIds: [],
    reviewerId: null,
    configId: null
  }
}

export function taskFormFromDetail(detail, pendingDocuments = [], pendingAddAnnotatorIds = []) {
  if (!detail) return emptyTaskForm()
  return {
    taskName: detail.summary?.taskName || '',
    description: detail.summary?.description || '',
    documentIds: (detail.documents || []).map((d) => d.id),
    documents: detail.documents || [],
    pendingDocuments: [...pendingDocuments],
    annotatorIds: (detail.annotators || []).map((u) => u.id),
    pendingAddAnnotatorIds: [...pendingAddAnnotatorIds],
    lockedAnnotatorIds: (detail.annotators || []).map((u) => u.id),
    reviewerId: detail.reviewer?.id ?? null,
    configId: detail.configSnapshot?.id ?? null
  }
}
