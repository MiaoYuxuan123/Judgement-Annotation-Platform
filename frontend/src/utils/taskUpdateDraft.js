function draftKey(taskId) {
  return `jap_task_update_${taskId}`
}

export function loadTaskUpdateDraft(taskId) {
  try {
    const raw = sessionStorage.getItem(draftKey(taskId))
    if (!raw) return { addDocuments: [], addAnnotatorIds: [] }
    const parsed = JSON.parse(raw)
    return {
      addDocuments: parsed.addDocuments || [],
      addAnnotatorIds: parsed.addAnnotatorIds || []
    }
  } catch {
    return { addDocuments: [], addAnnotatorIds: [] }
  }
}

export function saveTaskUpdateDraft(taskId, draft) {
  sessionStorage.setItem(draftKey(taskId), JSON.stringify(draft))
}

export function clearTaskUpdateDraft(taskId) {
  sessionStorage.removeItem(draftKey(taskId))
}

export function toUpdatePayload(form) {
  return {
    deadline: form.deadline || '',
    addAnnotatorIds: form.pendingAddAnnotatorIds || [],
    documents: (form.pendingDocuments || []).map((doc) => {
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
