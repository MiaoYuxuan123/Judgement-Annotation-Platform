<template>
  <div class="graph-canvas" ref="rootRef">
    <div class="graph-canvas-toolbar">
      <el-button-group size="small">
        <el-button @click="zoomBy(0.1)">放大</el-button>
        <el-button @click="zoomBy(-0.1)">缩小</el-button>
        <el-button @click="resetView">重置</el-button>
      </el-button-group>
      <el-button size="small" type="primary" plain @click="toggleFullscreen">
        <span class="fullscreen-icon">⛶</span> 全屏
      </el-button>
    </div>
    <div
        class="graph-canvas-viewport"
        @wheel.prevent="onWheel"
        @mousedown="onPanStart"
    >
      <div
          class="graph-canvas-stage"
          :style="{ transform: `translate(${translate.x}px, ${translate.y}px) scale(${scale})` }"
      >
        <GraphView :propositions="propositions" :relations="relations" :variant="variant" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import GraphView from './GraphView.vue'

defineProps({
  propositions: { type: Array, default: () => [] },
  relations: { type: Array, default: () => [] },
  variant: { type: String, default: 'circles' }
})

const rootRef = ref(null)
const scale = ref(1)
const translate = reactive({ x: 0, y: 0 })
const panning = reactive({ active: false, startX: 0, startY: 0, originX: 0, originY: 0 })

function zoomBy(delta) {
  scale.value = Math.min(2.5, Math.max(0.4, +(scale.value + delta).toFixed(2)))
}

function resetView() {
  scale.value = 1
  translate.x = 0
  translate.y = 0
}

function onWheel(event) {
  zoomBy(event.deltaY < 0 ? 0.08 : -0.08)
}

function onPanStart(event) {
  if (event.button !== 0) return
  panning.active = true
  panning.startX = event.clientX
  panning.startY = event.clientY
  panning.originX = translate.x
  panning.originY = translate.y
  window.addEventListener('mousemove', onPanMove)
  window.addEventListener('mouseup', onPanEnd)
}

function onPanMove(event) {
  if (!panning.active) return
  translate.x = panning.originX + (event.clientX - panning.startX)
  translate.y = panning.originY + (event.clientY - panning.startY)
}

function onPanEnd() {
  panning.active = false
  window.removeEventListener('mousemove', onPanMove)
  window.removeEventListener('mouseup', onPanEnd)
}

async function toggleFullscreen() {
  const el = rootRef.value
  if (!el) return
  if (!document.fullscreenElement) await el.requestFullscreen?.()
  else await document.exitFullscreen?.()
}
</script>
