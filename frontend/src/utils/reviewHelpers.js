const CIRCLED = [
    '①', '②', '③', '④', '⑤', '⑥', '⑦', '⑧', '⑨', '⑩',
    '⑪', '⑫', '⑬', '⑭', '⑮', '⑯', '⑰', '⑱', '⑲', '⑳'
]

export function circledNo(sequenceNo) {
    if (sequenceNo >= 1 && sequenceNo <= CIRCLED.length) return CIRCLED[sequenceNo - 1]
    return `(${sequenceNo})`
}

export function propByIdMap(propositions) {
    return new Map((propositions || []).map((p) => [p.propId, p]))
}

export function formatRelationFormula(rel, propositions) {
    const map = propByIdMap(propositions)
    const source = map.get(rel.source)
    const target = map.get(rel.target)
    const left = source ? circledNo(source.sequenceNo) : rel.source
    const right = target ? circledNo(target.sequenceNo) : rel.target
    return `${rel.type}(${left}, ${right})`
}

/** 将命题按原文位置切分为可渲染片段 */
export function buildAnnotatedParts(content, propositions) {
    if (!content) return []
    const sorted = [...(propositions || [])]
        .filter((p) => p.startPos != null && p.endPos != null && p.endPos > p.startPos)
        .sort((a, b) => a.startPos - b.startPos)

    const parts = []
    let cursor = 0
    for (const p of sorted) {
        const start = Math.max(0, Math.min(p.startPos, content.length))
        const end = Math.max(start, Math.min(p.endPos, content.length))
        if (start > cursor) parts.push({ type: 'text', text: content.slice(cursor, start) })
        if (end > start) parts.push({ type: 'prop', text: content.slice(start, end), sequenceNo: p.sequenceNo })
        cursor = end
    }
    if (cursor < content.length) parts.push({ type: 'text', text: content.slice(cursor) })
    if (!parts.length) parts.push({ type: 'text', text: content })
    return parts
}
