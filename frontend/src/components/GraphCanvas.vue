<template>
  <div class="graph-canvas" ref="rootRef">
    <div class="graph-canvas-viewport-wrap" ref="viewportRef">
      <VueFlow
        v-if="hasGraphInput && !layouting"
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
        <Background :gap="16" pattern-color="#d0c9bc" />
      </VueFlow>
      <div v-else-if="layouting" class="graph-canvas-empty">布局计算中…</div>
      <div v-else class="graph-canvas-empty">暂无图示数据</div>
      <div class="graph-side-toolbar" aria-label="图示工具栏">
        <button type="button" class="graph-tool-btn" title="放大" @click="zoomBy(0.12)">＋</button>
        <button type="button" class="graph-tool-btn" title="缩小" @click="zoomBy(-0.12)">－</button>
        <button type="button" class="graph-tool-btn graph-tool-text" title="适应视图" @click="fitViewNow">适应</button>
        <button type="button" class="graph-tool-btn graph-tool-text" :title="isFullscreen ? '退出全屏' : '全屏'" @click="toggleFullscreen">
          {{ isFullscreen ? '退出' : '全屏' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, markRaw, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'

import PropNode from './graph/PropNode.vue'
import HubNode from './graph/HubNode.vue'
import ElkOrthogonalEdge from './graph/ElkOrthogonalEdge.vue'
import { layoutArgumentGraphWithElk } from '../utils/argumentGraphElkLayout'
import { applyLayoutOverride, EMPTY_LAYOUT } from '../utils/graphLayoutOverride'
import { rebuildEdgePaths } from '../utils/graphDocument'

const props = defineProps({
  propositions: { type: Array, default: () => [] },
  relations: { type: Array, default: () => [] },
  activeRelationId: { type: String, default: '' },
  activeRelationKey: { type: String, default: '' },
  layoutOverride: { type: Object, default: null }
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
  'elk-orthogonal': markRaw(ElkOrthogonalEdge),
  polyline: markRaw(ElkOrthogonalEdge)
}

const { fitView, zoomIn, zoomOut } = useVueFlow({ id: flowId })

let layoutToken = 0
let resizeObserver = null

const hasGraphInput = computed(
  () =>
    props.propositions?.length > 0 ||
    (props.layoutOverride?.version === 2 && props.layoutOverride?.nodes?.length > 0)
)

function applyHighlight(nextNodes, nextEdges) {
  const { relIds, propIds } = relationHighlightInfo(props.activeRelationId, props.activeRelationKey)
  const nodeById = new Map(nextNodes.map((node) => [node.id, node]))
  nodes.value = nextNodes.map((node) => {
    const highlighted = relIds.has(node.data?.relKey) || propNodeMatches(node, propIds)
    return {
      ...node,
      selected: highlighted,
      class: highlighted ? 'is-relation-highlighted' : undefined,
      data: { ...node.data, highlighted }
    }
  })
  edges.value = nextEdges.map((edge) => {
    const highlighted = edgeMatchesHighlight(edge, nodeById, relIds, propIds)
    return {
      ...edge,
      selected: highlighted,
      class: highlighted ? 'is-relation-highlighted' : undefined,
      data: { ...edge.data, highlighted }
    }
  })
}

async function runLayout() {
  const token = ++layoutToken
  if (!props.propositions?.length && !(props.layoutOverride?.version === 2 && props.layoutOverride?.nodes?.length)) {
    nodes.value = []
    edges.value = []
    layouting.value = false
    return
  }

  layouting.value = true
  try {
    if (props.layoutOverride?.version === 2 && props.layoutOverride.nodes?.length) {
      if (token !== layoutToken) return
      const docEdges = rebuildEdgePaths(props.layoutOverride.nodes, props.layoutOverride.edges || [])
      applyHighlight(props.layoutOverride.nodes, docEdges)
      return
    }
    const auto = await layoutArgumentGraphWithElk(props.propositions, props.relations)
    if (token !== layoutToken) return
    const merged = applyLayoutOverride(auto, props.layoutOverride || EMPTY_LAYOUT())
    applyHighlight(merged.nodes, merged.edges)
  } catch (err) {
    console.error('[GraphCanvas] layout failed', err)
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
  const memberIds = propMemberIds(node)
  return memberIds.some((id) => propIds.has(id))
}

function propMemberIds(node) {
  const ids = []
  if (node.data?.stableId) ids.push(node.data.stableId)
  if (Array.isArray(node.data?.identityMembers)) ids.push(...node.data.identityMembers)
  ids.push(
    ...String(node.data?.label || '')
    .split('/')
    .map((item) => item.trim())
      .filter(Boolean)
  )
  return [...new Set(ids)]
}

function edgeMatchesHighlight(edge, nodeById, relIds, propIds) {
  if (relIds.has(edge.data?.relKey)) return true
  const source = nodeById.get(edge.source)
  const target = nodeById.get(edge.target)
  if (!source || !target) return false

  const sourceRelMatch = relIds.has(source.data?.relKey)
  const targetRelMatch = relIds.has(target.data?.relKey)
  const sourcePropMatch = propNodeMatches(source, propIds)
  const targetPropMatch = propNodeMatches(target, propIds)

  if (sourceRelMatch || targetRelMatch) return true
  if (sourcePropMatch && targetPropMatch) return true

  const activeDirectRels = [...relIds]
    .map((id) => (props.relations || []).find((rel) => rel.relId === id))
    .filter(Boolean)
  return activeDirectRels.some((rel) => edgeMatchesRelationMembers(edge, source, target, rel))
}

function edgeMatchesRelationMembers(edge, source, target, relation) {
  const members = relationMembers(relation).map(String)
  if (!members.length) return false

  const sourceIds = graphElementIds(source)
  const targetIds = graphElementIds(target)
  if (members.some((id) => sourceIds.has(id)) && members.some((id) => targetIds.has(id))) return true

  const type = String(relation.type || '').toUpperCase()
  if (['S', 'A', 'M'].includes(type) && members.length >= 2) {
    return connectsMemberPair(sourceIds, targetIds, members[0], members[1])
  }

  return false
}

function graphElementIds(node) {
  const ids = new Set()
  if (!node) return ids
  if (node.data?.relKey) ids.add(node.data.relKey)
  if (node.type === 'prop') propMemberIds(node).forEach((id) => ids.add(id))
  return ids
}

function connectsMemberPair(sourceIds, targetIds, a, b) {
  return (sourceIds.has(a) && targetIds.has(b)) || (sourceIds.has(b) && targetIds.has(a))
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
  () => [props.propositions, props.relations, props.layoutOverride],
  () => runLayout(),
  { deep: true, immediate: true }
)

watch(
  () => [props.activeRelationId, props.activeRelationKey],
  () => {
    if (!nodes.value.length) return
    applyHighlight(nodes.value, edges.value)
  }
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
  border: 1px solid #ede8df;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.graph-canvas:fullscreen {
  padding: 16px;
  background: #faf7f2;
}

.graph-canvas-viewport-wrap {
  flex: 1;
  min-height: 260px;
  position: relative;
  overflow: hidden;
}

.graph-canvas:fullscreen .graph-canvas-viewport-wrap {
  min-height: calc(100vh - 32px);
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
  color: #a8a29e;
  font-size: 13px;
  background: #fff;
  z-index: 2;
}

.graph-side-toolbar {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 5;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 6px;
  border: 1px solid #d0c9bc;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(8px);
}

.graph-tool-btn {
  width: 42px;
  height: 34px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: #44403c;
  font: inherit;
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;
  transition: background .15s ease, border-color .15s ease, color .15s ease;
}

.graph-tool-btn:hover {
  border-color: #f0b0b5;
  background: #fef2f2;
  color: #991b1b;
}

.graph-tool-text {
  font-size: 12px;
  font-weight: 700;
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
  stroke: #1a1817;
}

</style>
