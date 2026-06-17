import JSZip from 'jszip'
import { circledNo, formatRelationFormula } from './reviewHelpers'
import { renderGraphPngBlob } from './graphImageExport'
import { saveBlobAs } from './saveFile'
import { isDocExportable } from './taskRows'

export { isDocExportable, normalizeDocStatus, resolveDocStage } from './taskRows'

// utils/taskZipExport.js
// 组装 ZIP、生成 CSV/PNG

const UTF8_BOM = '\uFEFF'

function sanitizePath(name) {
  return String(name || '未命名')
    .replace(/[\\/:*?"<>|]/g, '_')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 80) || '未命名'
}

function csvEscape(value) {
  const text = value == null ? '' : String(value)
  if (/[",\n\r]/.test(text)) return `"${text.replace(/"/g, '""')}"`
  return text
}

function buildPropositionsCsv(propositions) {
  const header = ['命题序号', '命题内容', '命题类型']
  const rows = (propositions || []).map((p) => [
    circledNo(p.sequenceNo),
    p.text,
    p.tag
  ])
  return UTF8_BOM + [header, ...rows].map((row) => row.map(csvEscape).join(',')).join('\r\n')
}

function buildRelationsCsv(propositions, relations) {
  const header = ['关系序号', '关系内容']
  const rows = (relations || []).map((rel, index) => [
    `R${index + 1}`,
    formatRelationFormula(rel, propositions, index)
  ])
  return UTF8_BOM + [header, ...rows].map((row) => row.map(csvEscape).join(',')).join('\r\n')
}

function dateStamp() {
  const date = new Date()
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0')
  ].join('')
}

function defaultZipName(taskName, docTitle, taskId, docId) {
  const taskPart = sanitizePath(taskName || `任务${taskId}`)
  const docPart = sanitizePath(docTitle || `文书${docId}`)
  return `${taskPart}_${docPart}_${dateStamp()}.zip`
}

function batchZipName(taskName, taskId, count) {
  const taskPart = sanitizePath(taskName || `任务${taskId}`)
  return `${taskPart}_批量导出${count}篇_${dateStamp()}.zip`
}

function buildAnnotatorNameMap(taskDetail) {
  const nameMap = new Map()
  for (const a of taskDetail?.annotators || []) {
    nameMap.set(a.id, a.realName || a.username)
  }
  return nameMap
}

function resolveTaskMeta(review, taskDetail) {
  return {
    taskName: review?.task?.taskName || taskDetail?.summary?.taskName || `任务${review?.task?.taskId || ''}`,
    taskId: review?.task?.taskId || taskDetail?.summary?.taskId
  }
}

async function addVersionFolder(folder, label, propositions, relations, graphLayout, onProgress) {
  const props = propositions || []
  const rels = relations || []
  const hasV2Graph = graphLayout?.version === 2 && graphLayout.nodes?.length

  folder.file('要素列表.csv', buildPropositionsCsv(props))
  folder.file('关系列表.csv', buildRelationsCsv(props, rels))

  if (props.length || hasV2Graph) {
    onProgress?.(`正在生成图示：${label}`)
    const png = await renderGraphPngBlob(props, rels, graphLayout)
    if (png) folder.file('论证图示.png', png)
    else folder.file('论证图示.txt', '图示数据为空或渲染失败')
  } else {
    folder.file('论证图示.txt', '该版本暂无命题，无法生成图示')
  }
}

async function addDocumentToZip(root, docEntry, { taskName, nameMap, onProgress, annotatorId = null }) {
  const doc = docEntry.document
  const rootLabel = `${doc.id}_${sanitizePath(doc.title)}`
  const folder = root.folder(rootLabel)

  const exportedAt = new Date().toLocaleString('zh-CN', { hour12: false })
  const annotatorOnly = annotatorId != null
  folder.file(
    '导出说明.txt',
    [
      `任务名称：${taskName}`,
      `文书：${doc.title}（ID ${doc.id}）`,
      `导出时间：${exportedAt}`,
      '',
      '目录结构：',
      annotatorOnly
        ? '  {我的标注或最终裁定结果}/'
        : '  {标注员姓名或最终裁定结果}/',
      '    要素列表.csv',
      '    关系列表.csv',
      '    论证图示.png',
      '',
      annotatorOnly
        ? '说明：本 ZIP 仅包含您本人的标注结果与最终裁定结果。'
        : '说明：CSV 列与页面列表一致；UTF-8 编码，可用 Excel 直接打开。'
    ].join('\n')
  )

  const annotators = (docEntry.annotatorResults || []).filter(
    (result) => annotatorId == null || result.userId === annotatorId
  )
  if (!annotators.length && !isDocExportable(docEntry)) {
    throw new Error(`「${doc.title}」暂无可导出的标注或裁定结果`)
  }

  onProgress?.(`正在处理文书：${doc.title}`)

  for (let i = 0; i < annotators.length; i += 1) {
    const result = annotators[i]
    const label = annotatorOnly
      ? '我的标注'
      : (nameMap.get(result.userId) || `标注员_${result.userId}`)
    const versionFolder = folder.folder(sanitizePath(label))
    await addVersionFolder(
      versionFolder,
      `${rootLabel}/${label}`,
      result.propositions,
      result.relations,
      result.graphLayout,
      onProgress
    )
  }

  const final = docEntry.finalResult
  if (isDocExportable(docEntry)) {
    const finalFolder = folder.folder('最终裁定结果')
    await addVersionFolder(
      finalFolder,
      `${rootLabel}/最终裁定结果`,
      final.propositions,
      final.relations,
      final.graphLayout,
      onProgress
    )
  }

  return doc
}

/**
 * 导出当前文书下全部标注员版本与最终裁定结果为 ZIP，并由用户选择保存位置与文件名。
 */
/** 仅本任务创建者、裁定者可导出全部结果；标注员只导出本人 + 最终裁定。 */
export function canAccessAllTaskResults(taskDetail, user) {
  const userId = user?.id
  if (!userId || !taskDetail) return false
  return taskDetail.reviewer?.id === userId || taskDetail.summary?.creatorId === userId
}

export function resolveExportAnnotatorId(taskDetail, user) {
  const userId = user?.id
  if (!userId) return null
  if (canAccessAllTaskResults(taskDetail, user)) return null
  if (taskDetail?.annotators?.some((u) => u.id === userId)) return userId
  return null
}

export async function exportTaskZip({ review, taskDetail, currentDocId, onProgress, annotatorId = null }) {
  if (!review?.documents?.length) {
    throw new Error('暂无可导出的文书数据')
  }

  const docEntry = review.documents.find((d) => d.document.id === currentDocId)
  if (!docEntry) {
    throw new Error('请先选择要导出的文书')
  }

  const { taskName, taskId } = resolveTaskMeta(review, taskDetail)
  const doc = docEntry.document
  const suggestedName = defaultZipName(taskName, doc.title, taskId, doc.id)
  const nameMap = buildAnnotatorNameMap(taskDetail)

  const zip = new JSZip()
  await addDocumentToZip(zip, docEntry, { taskName, nameMap, onProgress, annotatorId })

  onProgress?.('正在压缩打包…')
  const blob = await zip.generateAsync({ type: 'blob', compression: 'DEFLATE', compressionOptions: { level: 6 } })

  onProgress?.('请选择保存位置…')
  const savedName = await saveBlobAs(blob, suggestedName)
  return savedName
}

/**
 * 批量导出多篇「可导出」文书为单个 ZIP（每篇文书一个子目录）。
 */
export async function exportTaskZipBatch({ review, taskDetail, docIds, onProgress, annotatorId = null }) {
  if (!review?.documents?.length) {
    throw new Error('暂无可导出的文书数据')
  }

  const ids = [...new Set((docIds || []).filter((id) => id != null))]
  if (!ids.length) {
    throw new Error('请先选择要导出的文书')
  }

  const docEntries = ids
    .map((id) => review.documents.find((d) => d.document.id === id))
    .filter(Boolean)

  const exportable = docEntries.filter(isDocExportable)
  if (!exportable.length) {
    throw new Error('所选文书均未进入「可导出」阶段，无法导出')
  }

  const skipped = docEntries.length - exportable.length
  if (skipped > 0) {
    onProgress?.(`已跳过 ${skipped} 篇非「可导出」文书，继续导出其余 ${exportable.length} 篇…`)
  }

  const { taskName, taskId } = resolveTaskMeta(review, taskDetail)
  const nameMap = buildAnnotatorNameMap(taskDetail)
  const zip = new JSZip()
  const exportedAt = new Date().toLocaleString('zh-CN', { hour12: false })
  const exportedDocs = []

  for (const docEntry of exportable) {
    const doc = await addDocumentToZip(zip, docEntry, { taskName, nameMap, onProgress, annotatorId })
    exportedDocs.push(doc)
  }

  zip.file(
    '批量导出说明.txt',
    [
      `任务名称：${taskName}`,
      `导出时间：${exportedAt}`,
      `导出文书数：${exportedDocs.length}`,
      '',
      '文书列表：',
      ...exportedDocs.map((doc) => `  - D${doc.id} ${doc.title}`),
      '',
      annotatorId != null
        ? '每个子目录结构与单篇导出一致，仅含您本人的标注结果与最终裁定结果。'
        : '每个子目录结构与单篇导出一致，含各标注员版本及最终裁定结果。'
    ].join('\n')
  )

  onProgress?.('正在压缩打包…')
  const blob = await zip.generateAsync({ type: 'blob', compression: 'DEFLATE', compressionOptions: { level: 6 } })
  const suggestedName = batchZipName(taskName, taskId, exportedDocs.length)

  onProgress?.('请选择保存位置…')
  const savedName = await saveBlobAs(blob, suggestedName)
  return { savedName, exportedCount: exportedDocs.length, skippedCount: skipped }
}
