<template>
  <div class="graph-canvas" ref="rootRef">
    <div class="graph-canvas-toolbar">
      <el-button-group size="small">
        <el-button @click="zoomBy(0.12)">放大</el-button>
        <el-button @click="zoomBy(-0.12)">缩小</el-button>
        <el-button @click="fitViewNow">适应</el-button>
      </el-button-group>
      <el-button size="small" type="primary" plain @click="toggleFullscreen">
        <span class="fullscreen-icon">{{ isFullscreen ? '⤢' : '⛶' }}</span>
        {{ isFullscreen ? '退出全屏' : '全屏' }}
      </el-button>
    </div>
    <div class="graph-canvas-viewport-wrap" ref="viewportRef">
      <VueFlow
        v-if="propositions.length && !layouting"
        :id="flowId"
        v-model:nodes="nodes"
        v-model:edges="edges"
        :node-types="nodeTypes"
        :edge-types="edgeTypes"
        :min-zoom="0.25"
        :max-zoom="2"
        :nodes-draggable="false"
        :nodes-connectable="false"
        :elements-selectable="false"
        :pan-on-drag="true"
        :zoom-on-scroll="true"
        fit-view-on-init
        class="graph-canvas-flow"
        @nodes-initialized="onNodesInitialized"
      >
        <Background :gap="16" pattern-color="#e8edf3" />
        <Controls :show-interactive="false" />
      </VueFlow>
      <div v-else-if="layouting" class="graph-canvas-empty">布局计算中…</div>
      <div v-else class="graph-canvas-empty">暂无图示数据</div>
    </div>
  </div>
</template>

<script setup>
import { markRaw, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  VueFlow,
  useVueFlow
} from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'

import PropNode from './graph/PropNode.vue'
import HubNode from './graph/HubNode.vue'
import ElkOrthogonalEdge from './graph/ElkOrthogonalEdge.vue'
import { layoutArgumentGraphWithElk } from '../utils/argumentGraphElkLayout'

const props = defineProps({
  propositions: { type: Array, default: () => [] },
  relations: { type: Array, default: () => [] },
  activeRelationId: { type: String, default: '' },
  activeRelationKey: { type: String, default: '' }
})

const rootRef = ref(null)
const viewportRef = ref(null)
const isFullscreen = ref(false)
const layouting = ref(false)
const nodes = ref([])
const edges = ref([])
const flowId = `argument-graph-${Math.random().toString(36).slice(2)}`

const nodeTypes = {
  prop: markRaw(PropNode),
  'hub-s': markRaw(HubNode),
  'hub-a': markRaw(HubNode),
  'hub-m': markRaw(HubNode),
  'hub-j': markRaw(HubNode)
}

const edgeTypes = {
  'elk-orthogonal': markRaw(ElkOrthogonalEdge)
}

const { fitView, zoomIn, zoomOut } = useVueFlow({ id: flowId })

let layoutToken = 0
let resizeObserver = null

async function runLayout() {
  const token = ++layoutToken
  if (!props.propositions?.length) {
    nodes.value = []
    edges.value = []
    layouting.value = false
    return
  }

  layouting.value = true
  try {
    const result = await layoutArgumentGraphWithElk(props.propositions, props.relations)
    if (token !== layoutToken) return
    nodes.value = result.nodes
    edges.value = result.edges
    applyActiveRelationHighlight()
  } catch (err) {
    console.error('[GraphCanvas] ELK layout failed', err)
    nodes.value = []
    edges.value = []
  } finally {
    if (token === layoutToken) layouting.value = false
  }
}

function relationHighlightInfo(activeId, activeKey) {
  const byId = new Map((props.relations || []).map((rel) => [rel.relId, rel]))
  const matchedId = activeKey
    ? (props.relations || []).find((rel) => relationSemanticKey(rel, props.relations) === activeKey)?.relId
    : ''
  const startId = matchedId || activeId
  if (!startId) return { relIds: new Set(), propIds: new Set() }
  const relIds = new Set()
  const propIds = new Set()
  const visit = (relId) => {
    if (!relId || relIds.has(relId)) return
    const rel = byId.get(relId)
    if (!rel) return
    relIds.add(relId)
    const members = rel.members?.length ? rel.members : [rel.source, rel.target].filter(Boolean)
    members.forEach((member) => {
      const value = String(member)
      if (value.startsWith('P')) propIds.add(value)
      if (value.startsWith('R')) visit(value)
    })
  }
  visit(startId)
  return { relIds, propIds }
}

function relationMembers(relation) {
  return relation?.members?.length ? relation.members : [relation?.source, relation?.target].filter(Boolean)
}

function relationSemanticKey(relation, relationList = props.relations, visited = new Set()) {
  if (!relation || visited.has(relation.relId)) return ''
  visited.add(relation.relId)
  const members = relationMembers(relation).map((id) => {
    const value = String(id || '')
    if (value.startsWith('P')) return value
    if (value.startsWith('R')) {
      const child = relationList.find((item) => item.relId === value)
      return child ? relationSemanticKey(child, relationList, new Set(visited)) : value
    }
    return value
  })
  return `${relation.type}(${members.join(',')})`
}

function propNodeMatches(node, propIds) {
  if (!propIds.size || node.type !== 'prop') return false
  if (propIds.has(node.data?.stableId)) return true
  return String(node.data?.label || '')
    .split('/')
    .map((item) => item.trim())
    .some((id) => propIds.has(id))
}

function applyActiveRelationHighlight() {
  const { relIds, propIds } = relationHighlightInfo(props.activeRelationId, props.activeRelationKey)
  nodes.value = nodes.value.map((node) => {
    const highlighted = relIds.has(node.data?.relKey) || propNodeMatches(node, propIds)
    return {
      ...node,
      class: highlighted ? 'is-relation-highlighted' : undefined,
      data: { ...node.data, highlighted }
    }
  })
  edges.value = edges.value.map((edge) => {
    const highlighted = relIds.has(edge.data?.relKey)
    return {
      ...edge,
      class: highlighted ? 'is-relation-highlighted' : undefined,
      data: { ...edge.data, highlighted }
    }
  })
}

function fitViewNow() {
  requestAnimationFrame(() => {
    fitView({ padding: 0.18, duration: 280 })
  })
}

function onNodesInitialized() {
  fitViewNow()
}

function zoomBy(delta) {
  if (delta > 0) zoomIn({ duration: 150 })
  else zoomOut({ duration: 150 })
}

async function toggleFullscreen() {
  const el = rootRef.value
  if (!el) return
  if (!document.fullscreenElement) await el.requestFullscreen?.()
  else await document.exitFullscreen?.()
}

function onFullscreenChange() {
  isFullscreen.value = document.fullscreenElement === rootRef.value
  if (isFullscreen.value) setTimeout(fitViewNow, 80)
}

watch(
  () => [props.propositions, props.relations],
  () => {
    runLayout()
  },
  { deep: true, immediate: true }
)

watch(
  () => [props.activeRelationId, props.activeRelationKey],
  () => applyActiveRelationHighlight()
)

onMounted(() => {
  document.addEventListener('fullscreenchange', onFullscreenChange)
  if (viewportRef.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => fitViewNow())
    resizeObserver.observe(viewportRef.value)
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  resizeObserver?.disconnect()
  layoutToken += 1
})
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

.graph-canvas-flow {
  width: 100%;
  height: 100%;
  min-height: 260px;
  background: #fff;
}

.graph-canvas-empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: #94a3b8;
  font-size: 13px;
  background: #fff;
  z-index: 2;
}

.fullscreen-icon {
  margin-right: 4px;
}

:deep(.vue-flow__node) {
  padding: 0;
  border: none;
  background: transparent;
  box-shadow: none;
}

:deep(.vue-flow__handle) {
  width: 1px;
  height: 1px;
  min-width: 0;
  min-height: 0;
  opacity: 0;
  border: none;
  background: transparent;
}

:deep(.vue-flow__edge-path) {
  stroke: #111;
}

:deep(.vue-flow__controls) {
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.08);
}
</style>
