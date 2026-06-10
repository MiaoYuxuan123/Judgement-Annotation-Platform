/**
 * 将要素显示编号统一取出。旧数据没有 propId 时回退为 P 序号。
 */
export function formatPropLabel(propOrSequenceNo) {
    if (propOrSequenceNo && typeof propOrSequenceNo === 'object') {
        return propOrSequenceNo.propId || `P${propOrSequenceNo.sequenceNo || ''}`
    }
    return `P${propOrSequenceNo}`
}

/**
 * 将命题数组快速转为 Map，方便高效查询
 */
export function propByIdMap(propositions) {
    return new Map((propositions || []).map((p) => [p.propId, p]))
}

/**
 * 核心纠正：兼顾旧代码和新视图的终极公式生成器
 */
export function formatRelationFormula(rel, propositions, index) {
    const map = propByIdMap(propositions)
    const source = map.get(rel.source)
    const target = map.get(rel.target)

    const left = source ? formatPropLabel(source) : (rel.source || 'E?')
    const right = target ? formatPropLabel(target) : (rel.target || 'E?')

    let rNumber = 1
    if (index !== undefined && index !== null) {
        rNumber = index + 1
    } else if (rel && rel.relId) {
        const match = String(rel.relId).match(/\d+/)
        rNumber = match ? match[0] : 1
    }

    return `${rel.type || 'S'}(${left}, ${right})`
}

function spansOverlap(aStart, aEnd, bStart, bEnd) {
    return Math.max(aStart, bStart) < Math.min(aEnd, bEnd)
}

function isInsideAnnotationMark(node) {
    const el = node?.nodeType === Node.TEXT_NODE ? node.parentElement : node
    return Boolean(el?.closest?.('.annotation-mark'))
}

/** 统计 DOM 片段中对应原文的字符数（跳过命题标签 mark） */
function measureContentTextLength(root) {
    if (!root) return 0
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT)
    let length = 0
    while (walker.nextNode()) {
        if (!isInsideAnnotationMark(walker.currentNode)) {
            length += walker.currentNode.textContent.length
        }
    }
    return length
}

/** 将原文展示区内的选区端点映射为 content 中的字符下标 */
export function domPointToContentOffset(sourceRoot, container, offset) {
    if (!sourceRoot || !container) return -1
    const range = document.createRange()
    range.selectNodeContents(sourceRoot)
    try {
        range.setEnd(container, offset)
    } catch {
        return -1
    }
    return measureContentTextLength(range.cloneContents())
}

/**
 * 根据 DOM 选区计算在 document.content 中的起止位置（保留空格，不依赖 indexOf）
 */
export function selectionSpanFromSourceElement(sourceEl, content) {
    const selection = window.getSelection()
    if (!selection?.rangeCount || selection.isCollapsed) return null

    const range = selection.getRangeAt(0)
    if (!sourceEl?.contains(range.startContainer) || !sourceEl.contains(range.endContainer)) {
        return null
    }

    let start = domPointToContentOffset(sourceEl, range.startContainer, range.startOffset)
    let end = domPointToContentOffset(sourceEl, range.endContainer, range.endOffset)
    if (start < 0 || end < 0) return null
    if (start > end) [start, end] = [end, start]
    if (start === end) return null

    const text = String(content || '').slice(start, end)
    if (!text.trim()) return null

    return { start, end, text }
}

/**
 * 标注页选区：在原文中找不与其他命题重叠的匹配片段
 */
export function findAvailableTextSpan(content, text, propositions, preferredStart = 0) {
    const needle = String(text || '').trim()
    if (!needle || !content) return { start: -1, end: -1 }

    const ranges = (propositions || [])
        .filter((p) => p.startPos != null && p.endPos != null)
        .map((p) => [p.startPos, p.endPos])

    let from = Math.max(0, preferredStart)
    while (from <= content.length) {
        const idx = content.indexOf(needle, from)
        if (idx < 0) break
        const end = idx + needle.length
        const blocked = ranges.some(([s, e]) => spansOverlap(idx, end, s, e))
        if (!blocked) return { start: idx, end }
        from = idx + 1
    }
    return { start: -1, end: -1 }
}

function clampPos(value, min, max) {
    return Math.max(min, Math.min(value, max))
}

/**
 * 文本切片：完整保留原文，仅按 startPos/endPos 高亮对应片段
 */
export function buildAnnotatedParts(content, propositions) {
    if (!content) return []

    const sorted = [...(propositions || [])]
        .filter((p) => p.startPos != null && p.endPos != null && p.endPos > p.startPos)
        .sort((a, b) => a.startPos - b.startPos || (a.sequenceNo || 0) - (b.sequenceNo || 0))

    const parts = []
    let cursor = 0

    for (const p of sorted) {
        const start = clampPos(p.startPos, 0, content.length)
        const end = clampPos(p.endPos, start, content.length)

        // 重叠区间跳过，避免打乱原文顺序
        if (start < cursor) continue

        if (start > cursor) {
            parts.push({ type: 'text', text: content.slice(cursor, start) })
        }

        parts.push({
            type: 'prop',
            text: content.slice(start, end),
            sequenceNo: p.sequenceNo,
            label: formatPropLabel(p),
            tag: p.tag || 'SF'
        })

        cursor = end
    }

    if (cursor < content.length) {
        parts.push({ type: 'text', text: content.slice(cursor) })
    }
    if (!parts.length) {
        parts.push({ type: 'text', text: content })
    }

    return parts
}

export function circledNo(propOrSequenceNo) {
    return formatPropLabel(propOrSequenceNo)
}
