import { Graph } from '@antv/x6'
import { buildGraphModel } from './graphModelBuilder'
import { ensureGraphShapes } from './graphX6Shapes'

// utils/graphImageExport.js
// 离屏渲染图示为 PNG

const EXPORT_WIDTH = 1200
const EXPORT_HEIGHT = 800

function waitFrames(count = 2) {
  return new Promise((resolve) => {
    let left = count
    const tick = () => {
      left -= 1
      if (left <= 0) resolve()
      else requestAnimationFrame(tick)
    }
    requestAnimationFrame(tick)
  })
}

async function svgElementToPngBlob(svg, padding = 32) {
  if (!svg) return null

  const bbox = svg.getBBox?.()
  if (!bbox || (!bbox.width && !bbox.height)) return null

  const viewX = bbox.x - padding
  const viewY = bbox.y - padding
  const viewW = bbox.width + padding * 2
  const viewH = bbox.height + padding * 2

  const clone = svg.cloneNode(true)
  clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
  clone.setAttribute('width', String(Math.ceil(viewW)))
  clone.setAttribute('height', String(Math.ceil(viewH)))
  clone.setAttribute('viewBox', `${viewX} ${viewY} ${viewW} ${viewH}`)

  const svgString = new XMLSerializer().serializeToString(clone)
  const url = URL.createObjectURL(new Blob([svgString], { type: 'image/svg+xml;charset=utf-8' }))

  try {
    const img = new Image()
    await new Promise((resolve, reject) => {
      img.onload = resolve
      img.onerror = reject
      img.src = url
    })

    const canvas = document.createElement('canvas')
    canvas.width = Math.ceil(viewW)
    canvas.height = Math.ceil(viewH)
    const ctx = canvas.getContext('2d')
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.drawImage(img, 0, 0, canvas.width, canvas.height)

    return await new Promise((resolve) => canvas.toBlob(resolve, 'image/png'))
  } finally {
    URL.revokeObjectURL(url)
  }
}

/**
 * 离屏渲染论证图示并导出 PNG
 */
export async function renderGraphPngBlob(propositions, relations) {
  if (!propositions?.length) return null

  const model = buildGraphModel(propositions, relations || [])
  if (!model.nodes.length) return null

  const container = document.createElement('div')
  container.style.cssText = `position:fixed;left:-12000px;top:0;width:${EXPORT_WIDTH}px;height:${EXPORT_HEIGHT}px;visibility:hidden;pointer-events:none;`
  document.body.appendChild(container)

  let graph = null
  try {
    ensureGraphShapes()
    graph = new Graph({
      container,
      width: EXPORT_WIDTH,
      height: EXPORT_HEIGHT,
      background: { color: '#fff' },
      grid: { visible: false },
      panning: false,
      mousewheel: false,
      interacting: false
    })

    graph.fromJSON({ nodes: model.nodes, edges: model.edges })
    graph.zoomToFit({ padding: 40, maxScale: 1 })
    graph.centerContent()
    await waitFrames(3)

    return await svgElementToPngBlob(container.querySelector('svg'))
  } finally {
    graph?.dispose()
    container.remove()
  }
}
