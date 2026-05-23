import JSZip from 'jszip'
import { circledNo, formatRelationFormula } from './reviewHelpers'
import { renderGraphPngBlob } from './graphImageExport'
import { saveBlobAs } from './saveFile'

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

function defaultZipName(taskName, docTitle, taskId, docId) {
  const taskPart = sanitizePath(taskName || `任务${taskId}`)
  const docPart = sanitizePath(docTitle || `文书${docId}`)
  const date = new Date()
  const stamp = [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0')
  ].join('')
  return `${taskPart}_${docPart}_${stamp}.zip`
}

async function addVersionFolder(folder, label, propositions, relations, onProgress) {
  const props = propositions || []
  const rels = relations || []

  folder.file('命题列表.csv', buildPropositionsCsv(props))
  folder.file('关系列表.csv', buildRelationsCsv(props, rels))

  if (props.length) {
    onProgress?.(`正在生成图示：${label}`)
    const png = await renderGraphPngBlob(props, rels)
    if (png) folder.file('论证图示.png', png)
    else folder.file('论证图示.txt', '图示数据为空或渲染失败')
  } else {
    folder.file('论证图示.txt', '该版本暂无命题，无法生成图示')
  }
}

/**
 * 导出当前文书下全部标注员版本与最终裁定结果为 ZIP，并由用户选择保存位置与文件名。
 */
export async function exportTaskZip({ review, taskDetail, currentDocId, onProgress }) {
  if (!review?.documents?.length) {
    throw new Error('暂无可导出的文书数据')
  }

  const docEntry = review.documents.find((d) => d.document.id === currentDocId)
  if (!docEntry) {
    throw new Error('请先选择要导出的文书')
  }

  const doc = docEntry.document
  const taskName = review.task?.taskName || taskDetail?.summary?.taskName || `任务${review.task?.taskId || ''}`
  const taskId = review.task?.taskId || taskDetail?.summary?.taskId
  const suggestedName = defaultZipName(taskName, doc.title, taskId, doc.id)

  const nameMap = new Map()
  for (const a of taskDetail?.annotators || []) {
    nameMap.set(a.id, a.realName || a.username)
  }

  const zip = new JSZip()
  const rootLabel = `${doc.id}_${sanitizePath(doc.title)}`
  const root = zip.folder(rootLabel)

  const exportedAt = new Date().toLocaleString('zh-CN', { hour12: false })
  root.file(
    '导出说明.txt',
    [
      `任务名称：${taskName}`,
      `文书：${doc.title}（ID ${doc.id}）`,
      `导出时间：${exportedAt}`,
      '',
      '目录结构：',
      '  {标注员姓名或最终裁定结果}/',
      '    命题列表.csv',
      '    关系列表.csv',
      '    论证图示.png',
      '',
      '说明：CSV 列与页面列表一致；UTF-8 编码，可用 Excel 直接打开。'
    ].join('\n')
  )

  const annotators = docEntry.annotatorResults || []
  if (!annotators.length && !(docEntry.finalResult && typeof docEntry.finalResult === 'object' && docEntry.finalResult.propositions)) {
    throw new Error('当前文书暂无可导出的标注或裁定结果')
  }

  onProgress?.(`正在处理文书：${doc.title}`)

  for (let i = 0; i < annotators.length; i += 1) {
    const result = annotators[i]
    const label = nameMap.get(result.userId) || `标注员_${result.userId}`
    const folder = root.folder(sanitizePath(label))
    await addVersionFolder(
      folder,
      `${rootLabel}/${label}`,
      result.propositions,
      result.relations,
      onProgress
    )
  }

  const final = docEntry.finalResult
  if (final && typeof final === 'object' && final.propositions) {
    const folder = root.folder('最终裁定结果')
    await addVersionFolder(
      folder,
      `${rootLabel}/最终裁定结果`,
      final.propositions,
      final.relations,
      onProgress
    )
  }

  onProgress?.('正在压缩打包…')
  const blob = await zip.generateAsync({ type: 'blob', compression: 'DEFLATE', compressionOptions: { level: 6 } })

  onProgress?.('请选择保存位置…')
  const savedName = await saveBlobAs(blob, suggestedName)
  return savedName
}
