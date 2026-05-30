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

export function taskFormFromDetail(detail) {
  if (!detail) return emptyTaskForm()
  return {
    taskName: detail.summary?.taskName || '',
    description: detail.summary?.description || '',
    documentIds: (detail.documents || []).map((d) => d.id),
    documents: detail.documents || [],
    annotatorIds: (detail.annotators || []).map((u) => u.id),
    reviewerId: detail.reviewer?.id ?? null,
    configId: detail.configSnapshot?.id ?? null
  }
}
