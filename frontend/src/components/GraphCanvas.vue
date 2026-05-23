<template>
  <div class="graph-canvas" ref="rootRef">
    <div class="graph-canvas-toolbar">
      <el-button-group size="small">
        <el-button @click="zoomBy(0.1)">放大</el-button>
        <el-button @click="zoomBy(-0.1)">缩小</el-button>
        <el-button @click="resetView">重置</el-button>
      </el-button-group>
      <el-button size="small" type="primary" plain @click="toggleFullscreen">
        <span class="fullscreen-icon">{{ isFullscreen ? '⤢' : '⛶' }}</span>
        {{ isFullscreen ? '退出全屏' : '全屏' }}
      </el-button>
    </div>
    <div class="graph-canvas-viewport-wrap">
      <div ref="containerRef" class="graph-canvas-viewport" />
      <div v-if="!propositions.length" class="graph-canvas-empty">暂无图示数据</div>
    </div>
  </div>
</template>

<script setup>
import { Graph } from '@antv/x6'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ensureGraphShapes } from '../utils/graphX6Shapes'
import { buildGraphModel } from '../utils/graphModelBuilder'

const props = defineProps({
  propositions: { type: Array, default: () => [] },
  relations: { type: Array, default: () => [] }
})

const rootRef = ref(null)
const containerRef = ref(null)
const isFullscreen = ref(false)
let graph = null
let resizeObserver = null
let renderRetryId = 0

function getContainerSize() {
  const el = containerRef.value
  if (!el) return null
  const width = Math.max(el.clientWidth, el.offsetWidth)
  const height = Math.max(el.clientHeight, el.offsetHeight)
  if (width < 2 || height < 2) return null
  return { width, height }
}

function destroyGraph() {
  graph?.dispose()
  graph = null
}

function fitGraphView() {
  if (!graph || !containerRef.value) return
  const size = getContainerSize()
  if (!size) return
  graph.resize(size.width, size.height)
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      if (!graph) return
      graph.zoomToFit({ padding: 24, maxScale: 1 })
      graph.centerContent()
    })
  })
}

function handleContainerResize() {
  if (!containerRef.value) return
  const size = getContainerSize()
  if (!size) return
  if (!graph) {
    scheduleRender()
    return
  }
  graph.resize(size.width, size.height)
  if (props.propositions.length && graph.getCellCount() > 0) fitGraphView()
}

function renderGraph() {
  if (!containerRef.value) return false
  const size = getContainerSize()
  if (!size) return false

  if (!graph) {
    ensureGraphShapes()
    graph = new Graph({
      container: containerRef.value,
      width: size.width,
      height: size.height,
      autoResize: true,
      background: { color: '#fff' },
      grid: { visible: false },
      panning: { enabled: true },
      mousewheel: {
        enabled: true,
        modifiers: null,
        factor: 1.08,
        minScale: 0.3,
        maxScale: 2
      },
      interacting: {
        nodeMovable: false,
        edgeMovable: false,
        magnetConnectable: false,
        arrowheadMovable: false,
        vertexMovable: false,
        vertexAddable: false,
        vertexDeletable: false
      }
    })
  }

  if (!props.propositions.length) {
    graph.clearCells()
    return true
  }

  ensureGraphShapes()
  const model = buildGraphModel(props.propositions, props.relations)
  graph.clearCells()
  if (!model.nodes.length) return false

  graph.fromJSON({ nodes: model.nodes, edges: model.edges })
  fitGraphView()
  return graph.getCellCount() > 0
}

function scheduleRender(attempt = 0) {
  cancelAnimationFrame(renderRetryId)
  renderRetryId = requestAnimationFrame(async () => {
    await nextTick()
    if (!containerRef.value) {
      if (attempt < 80) scheduleRender(attempt + 1)
      return
    }
    if (!props.propositions.length) {
      graph?.clearCells()
      return
    }
    const ok = renderGraph()
    if (!ok && attempt < 80) {
      destroyGraph()
      scheduleRender(attempt + 1)
    }
  })
}

function zoomBy(delta) {
  if (!graph) return
  const next = graph.zoom() + delta
  graph.zoom(Math.min(2, Math.max(0.3, next)), { absolute: true })
}

function resetView() {
  if (!graph) return
  fitGraphView()
}

async function toggleFullscreen() {
  const el = rootRef.value
  if (!el) return
  if (!document.fullscreenElement) await el.requestFullscreen?.()
  else await document.exitFullscreen?.()
}

function onFullscreenChange() {
  isFullscreen.value = document.fullscreenElement === rootRef.value
  if (isFullscreen.value) fitGraphView()
}

onMounted(async () => {
  document.addEventListener('fullscreenchange', onFullscreenChange)
  await nextTick()
  scheduleRender()
  if (containerRef.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => handleContainerResize())
    resizeObserver.observe(containerRef.value)
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  cancelAnimationFrame(renderRetryId)
  resizeObserver?.disconnect()
  resizeObserver = null
  destroyGraph()
})

watch(
  () => [props.propositions, props.relations],
  () => scheduleRender(),
  { deep: true, flush: 'post' }
)
</script>

<style scoped>
.graph-canvas {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.graph-canvas:fullscreen {
  padding: 16px;
  background: #f8fafc;
}

.graph-canvas-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
  flex-shrink: 0;
}

.graph-canvas-viewport-wrap {
  flex: 1;
  min-height: 260px;
  position: relative;
  overflow: hidden;
}

.graph-canvas-viewport {
  width: 100%;
  height: 100%;
  min-height: 260px;
}

.graph-canvas-empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: #94a3b8;
  font-size: 13px;
  pointer-events: none;
  z-index: 1;
}

.fullscreen-icon {
  margin-right: 4px;
}
</style>
