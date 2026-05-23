import { layoutArgumentGraphWithElk, pointsToSvgPath } from './argumentGraphElkLayout'

const PAD = 40

function hubRadius(kind) {
  if (kind === 'hub-s') return 5
  if (kind === 'hub-a') return 8
  return 11
}

function renderToSvgString(layout) {
  const { nodes, edges } = layout
  if (!nodes.length) return null

  let minX = Infinity
  let minY = Infinity
  let maxX = -Infinity
  let maxY = -Infinity

  const touch = (x, y) => {
    minX = Math.min(minX, x)
    minY = Math.min(minY, y)
    maxX = Math.max(maxX, x)
    maxY = Math.max(maxY, y)
  }

  const parts = []

  edges.forEach((e) => {
    const d = e.data?.path
    if (!d) return
    parts.push(`<path d="${d}" fill="none" stroke="#111" stroke-width="1.2"/>`)
    if (e.data?.directed && e.data?.points?.length >= 2) {
      const pts = e.data.points
      const a = pts[pts.length - 2]
      const b = pts[pts.length - 1]
      const ang = Math.atan2(b.y - a.y, b.x - a.x)
      const size = 7
      const x1 = b.x - size * Math.cos(ang - 0.4)
      const y1 = b.y - size * Math.sin(ang - 0.4)
      const x2 = b.x - size * Math.cos(ang + 0.4)
      const y2 = b.y - size * Math.sin(ang + 0.4)
      parts.push(`<polygon points="${b.x},${b.y} ${x1},${y1} ${x2},${y2}" fill="#111"/>`)
    }
    e.data?.points?.forEach((p) => touch(p.x, p.y))
  })

  nodes.forEach((n) => {
    const w = parseFloat(n.style?.width) || 48
    const h = parseFloat(n.style?.height) || 32
    const x = n.position.x
    const y = n.position.y
    touch(x, y)
    touch(x + w, y + h)

    if (n.type === 'prop') {
      parts.push(
        `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="#fff" stroke="#111" stroke-width="1.2"/>`,
        `<text x="${x + w / 2}" y="${y + h / 2 + 1}" text-anchor="middle" dominant-baseline="middle" font-size="13" font-weight="600" fill="#111" font-family="Inter,'Microsoft YaHei',sans-serif">${escapeXml(n.data?.label || '')}</text>`
      )
      return
    }

    const r = hubRadius(n.type)
    const cx = x + w / 2
    const cy = y + h / 2
    if (n.type === 'hub-s') {
      parts.push(`<circle cx="${cx}" cy="${cy}" r="${r}" fill="#111"/>`)
    } else {
      parts.push(`<circle cx="${cx}" cy="${cy}" r="${r}" fill="#fff" stroke="#111" stroke-width="1.8"/>`)
      if (n.data?.label) {
        parts.push(
          `<text x="${cx}" y="${cy + 1}" text-anchor="middle" dominant-baseline="middle" font-size="14" font-weight="700" fill="#111">+</text>`
        )
      }
    }
  })

  if (!Number.isFinite(minX)) return null

  const vx = minX - PAD
  const vy = minY - PAD
  const vw = maxX - minX + PAD * 2
  const vh = maxY - minY + PAD * 2

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${Math.ceil(vw)}" height="${Math.ceil(vh)}" viewBox="${vx} ${vy} ${vw} ${vh}">${parts.join('')}</svg>`
}

function escapeXml(s) {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

async function svgStringToPngBlob(svgString) {
  const url = URL.createObjectURL(new Blob([svgString], { type: 'image/svg+xml;charset=utf-8' }))
  try {
    const img = new Image()
    await new Promise((resolve, reject) => {
      img.onload = resolve
      img.onerror = reject
      img.src = url
    })
    const canvas = document.createElement('canvas')
    canvas.width = img.width
    canvas.height = img.height
    const ctx = canvas.getContext('2d')
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.drawImage(img, 0, 0)
    return await new Promise((resolve) => canvas.toBlob(resolve, 'image/png'))
  } finally {
    URL.revokeObjectURL(url)
  }
}

/**
 * 离屏渲染论证图示并导出 PNG（与页面 ELK 布局一致）
 */
export async function renderGraphPngBlob(propositions, relations) {
  if (!propositions?.length) return null
  const layout = await layoutArgumentGraphWithElk(propositions, relations || [])
  const svg = renderToSvgString(layout)
  if (!svg) return null
  return svgStringToPngBlob(svg)
}

export { pointsToSvgPath }
