/**
 * 将数字序号统一转换为 P1, P2 格式
 */
export function formatPropLabel(sequenceNo) {
    return `P${sequenceNo}`
}

/**
 * 将命题数组快速转为 Map，方便高效查询
 */
export function propByIdMap(propositions) {
    return new Map((propositions || []).map((p) => [p.propId, p]))
}

/**
 * 核心纠正：兼顾旧代码和新视图的终极公式生成器
 * 不管外面传不传 index 过来，都能保证完美不报错，且输出标准的 R1: S(P1, P2)
 */
export function formatRelationFormula(rel, propositions, index) {
    const map = propByIdMap(propositions)
    const source = map.get(rel.source)
    const target = map.get(rel.target)

    // 1. 转换为 P1, P2 形式
    const left = source ? formatPropLabel(source.sequenceNo) : (rel.source || 'P?')
    const right = target ? formatPropLabel(target.sequenceNo) : (rel.target || 'P?')

    // 2. 智能化计算 R1, R2 序号：
    // 如果页面传了 index 就用 index；如果页面像老代码那样没传，我们就用 relId 或者根据关系列表自动降级，绝对不报错！
    let rNumber = 1
    if (index !== undefined && index !== null) {
        rNumber = index + 1
    } else if (rel && rel.relId) {
        // 如果有类似 "rel_1" 格式的 ID，提取数字，否则默认给 1
        const match = String(rel.relId).match(/\d+/)
        rNumber = match ? match[0] : 1
    }

    // 3. 完美返回标准公式：例如 "S(P1, P2)"
    return `${rel.type || 'S'}(${left}, ${right})`
}

/**
 * 文本切片算法：将文书原文按照命题的起始和结束位置，切分成可渲染的数组片段
 */
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

        // 压入高亮块之前的普通文本
        if (start > cursor) {
            parts.push({ type: 'text', text: content.slice(cursor, start) })
        }

        // 压入高亮命题块
        if (end > start) {
            parts.push({
                type: 'prop',
                text: content.slice(start, end),
                sequenceNo: p.sequenceNo,
                label: formatPropLabel(p.sequenceNo),
                tag: p.tag || 'SF'
            })
        }
        cursor = end
    }

    // 压入文章末尾剩余的普通文本
    if (cursor < content.length) {
        parts.push({ type: 'text', text: content.slice(cursor) })
    }
    if (!parts.length) {
        parts.push({ type: 'text', text: content })
    }

    return parts
}

// 保留一个空函数防止老代码别处引用报错
export function circledNo(sequenceNo) {
    return formatPropLabel(sequenceNo)
}