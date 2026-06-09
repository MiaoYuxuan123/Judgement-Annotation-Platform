<template>
  <div class="graph-editor-canvas" ref="rootRef">
    <div class="graph-editor-canvas-viewport" ref="viewportRef">
      <VueFlow
        v-if="!layouting"
        :id="flowId"
        v-model:nodes="nodes"
        v-model:edges="edges"
        :node-types="nodeTypes"
        :edge-types="edgeTypes"
        :min-zoom="0.15"
        :max-zoom="3"
        :nodes-draggable="editable && tool === 'select'"
        :nodes-connectable="editable && tool === 'connect'"
        :elements-selectable="editable && tool === 'select'"
        :pan-on-drag="tool !== 'connect' && !isAddMode"
        :selection-key-code="tool === 'select'"
        :multi-selection-key-code="'Shift'"
        :select-nodes-on-drag="false"
        :zoom-on-scroll="true"
        :pan-on-scroll="false"
        :prevent-scrolling="true"
        :delete-key-code="editable ? 'Delete' : null"
        fit-view-on-init
        class="graph-editor-flow"
        :class="{
          'is-add-mode': isAddMode,
          'is-connect-mode': tool === 'connect',
          'is-select-mode': tool === 'select',
          'is-reconnect-mode': isReconnectMode
        }"
        @nodes-initialized="onNodesInitialized"
        @node-drag-start="onNodeDragStart"
        @node-drag="onNodeDrag"
        @node-drag-stop="onNodeDragStop"
        @selection-drag-start="onSelectionDragStart"
        @selection-drag="onSelectionDrag"
        @selection-drag-stop="onNodeDragStop"
        @node-click="onNodeClick"
        @edge-click="onEdgeClick"
        @pane-click="onPaneClick"
        @connect="onConnect"
        @selection-change="onSelectionChange"
        @nodes-change="onNodesChange"
        @edges-change="onEdgesChange"
      >
        <Background :gap="16" pattern-color="#e8edf3" />
        <Controls v-if="editable" :show-interactive="false" />
      </VueFlow>
      <div v-else class="graph-editor-empty">加载中…</div>
      <div v-if="isAddMode" class="graph-editor-cursor-hint">点击画布放置关系节点</div>
      <div v-if="tool === 'connect'" class="graph-editor-cursor-hint connect">从节点连接点拖拽到目标节点</div>
      <div v-if="snapHint" class="graph-editor-snap-hint">{{ snapHint }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, markRaw, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'

import PropNode from './graph/PropNode.vue'
import HubNode from './graph/HubNode.vue'
import PolylineEdge from './graph/PolylineEdge.vue'
import {
  cloneDocument,
  computeGridPositions,
  createEdge,
  createHubNode,
  createPropNodeFromProposition,
  decorateForEditor,
  findReconnectHandleSnap,
  getCanvasPropIds,
  insertBendAtSegment,
  mergePropNodesAsIdentity,
  moveBendPoint,
  nextEdgeId,
  nextRelationId,
  rebuildEdgePaths,
  resolveEdgePoints,
  inferHandleId,
  snapBendInPath,
  snapNodesGroupPositions,
  updateEdgesAfterNodeMove
} from '../utils/graphDocument'
import { pointsToSvgPath } from '../utils/graphLayoutOverride'

const props = defineProps({
  document: { type: Object, required: true },
  editable: { type: Boolean, default: true },
  tool: { type: String, default: 'select' },
  addKind: { type: String, default: '' },
  addRelType: { type: String, default: 'S' },
  selectedNodeId: { type: String, default: '' },
  selectedEdgeId: { type: String, default: '' },
  selectedPropCount: { type: Number, default: 0 }
})

const emit = defineEmits([
  'update:document',
  'update:selectedNodeId',
  'update:selectedEdgeId',
  'update:selectedNodeCount',
  'update:selectedPropCount',
  'edit-start',
  'change'
])

const rootRef = ref(null)
const viewportRef = ref(null)
const layouting = ref(false)
const nodes = ref([])
const edges = ref([])
const snapHint = ref('')
const flowId = `graph-editor-${Math.random().toString(36).slice(2)}`

const nodeTypes = {
  prop: markRaw(PropNode),
  'hub-s': markRaw(HubNode),
  'hub-a': markRaw(HubNode),
  'hub-m': markRaw(HubNode),
  'hub-j': markRaw(HubNode)
}

const edgeTypes = {
  polyline: markRaw(PolylineEdge),
  'elk-orthogonal': markRaw(PolylineEdge)
}

const isAddMode = computed(() => props.tool === 'add-hub')
const isReconnectMode = computed(() => (
  props.tool === 'select' && (edges.value.some((edge) => edge.selected) || !!reconnectDrag.value)
))

const { fitView, zoomIn, zoomOut, screenToFlowCoordinate, removeNodes, removeEdges, removeSelectedElements } = useVueFlow({ id: flowId })

let suppressEmit = false
let resizeObserver = null
const pendingSegmentInsert = new Map()
const dragPrevPositions = new Map()
const activeEndpoint = ref('')
const reconnectSnap = ref(null)
const reconnectDrag = ref(null)

function stripNodeData(data = {}) {
  const {
    editable, selected, highlighted, showHandles, snapHandleId, onHandlePick,
    ...rest
  } = data
  return rest
}

function stripEdgeData(data = {}) {
  const {
    editable, selected, onSegmentMidDrag, onBendDrag, onEdgeSelect, onEndpointDrag,
    onEndpointActivate, points, path, reconnectPreview, activeEndpoint,
    ...rest
  } = data
  return rest
}

function resolveReconnectEnd(edge, nodeId) {
  if (nodeId === edge.source) return 'source'
  if (nodeId === edge.target) return 'target'
  return activeEndpoint.value || 'source'
}

function syncNodeReconnectDecorations() {
  const selectedEdge = edges.value.find((edge) => edge.selected)
  const showHandles = props.editable && (props.tool === 'connect' || isReconnectMode.value)

  nodes.value = nodes.value.map((node) => ({
    ...node,
    data: {
      ...node.data,
      showHandles,
      snapHandleId: reconnectSnap.value?.nodeId === node.id ? reconnectSnap.value.handleId : '',
      onHandlePick: selectedEdge
        ? (handleId) => {
          const end = resolveReconnectEnd(selectedEdge, node.id)
          assignEdgeEndpoint(selectedEdge.id, end, node.id, handleId)
          activeEndpoint.value = end
        }
        : undefined
    }
  }))
}

function applyEdgeEndpoint(edgeId, end, nodeId, handleId) {
  const edge = edges.value.find((item) => item.id === edgeId)
  if (!edge) return false
  if (end === 'source' && nodeId === edge.target) return false
  if (end === 'target' && nodeId === edge.source) return false

  edges.value = edges.value.map((item) => {
    if (item.id !== edgeId) return item
    if (end === 'source') {
      return { ...item, source: nodeId, sourceHandle: handleId }
    }
    return { ...item, target: nodeId, targetHandle: handleId }
  })
  edges.value = attachEdgeHandlers(rebuildEdgePaths(nodes.value, edges.value, { inferHandles: false }))
  emitDocument()
  return true
}

function assignEdgeEndpoint(edgeId, end, nodeId, handleId) {
  emit('edit-start', '调整连线端点')
  applyEdgeEndpoint(edgeId, end, nodeId, handleId)
}

function handleEndpointActivate({ end }) {
  activeEndpoint.value = end
  edges.value = attachEdgeHandlers(edges.value)
}

function handleEndpointDrag({ edgeId, end, phase, event }) {
  const edge = edges.value.find((item) => item.id === edgeId)
  if (!edge) return
  const flowPoint = screenToFlowCoordinate({ x: event.clientX, y: event.clientY })

  if (phase === 'start') {
    emit('edit-start', '调整连线端点')
    activeEndpoint.value = end
    reconnectDrag.value = { edgeId, end }
  }

  const snap = findReconnectHandleSnap(nodes.value, flowPoint, edge, end)
  reconnectSnap.value = snap
  const previewPoint = snap?.point || flowPoint
  reconnectDrag.value = { edgeId, end, preview: { end, x: previewPoint.x, y: previewPoint.y } }
  syncNodeReconnectDecorations()
  edges.value = edges.value.map((item) => {
    if (item.id !== edgeId) return item
    return {
      ...item,
      data: {
        ...item.data,
        activeEndpoint: end,
        reconnectPreview: reconnectDrag.value.preview,
        onSegmentMidDrag: handleSegmentMidDrag,
        onBendDrag: handleBendDrag,
        onEdgeSelect: handleEdgeSelect,
        onEndpointDrag: handleEndpointDrag,
        onEndpointActivate: handleEndpointActivate
      }
    }
  })

  if (phase === 'end') {
    if (snap) {
      applyEdgeEndpoint(edgeId, end, snap.nodeId, snap.handleId)
    } else {
      const anchorNodeId = end === 'source' ? edge.source : edge.target
      const anchorNode = nodes.value.find((item) => item.id === anchorNodeId)
      if (anchorNode) {
        const handleId = inferHandleId(anchorNode, flowPoint)
        applyEdgeEndpoint(edgeId, end, anchorNodeId, handleId)
      }
    }
    reconnectDrag.value = null
    reconnectSnap.value = null
    edges.value = attachEdgeHandlers(edges.value)
    syncNodeReconnectDecorations()
  }
}

function formatSnapHint(hint) {
  if (hint === 'horizontal') return '已对齐水平'
  if (hint === 'vertical') return '已对齐竖直'
  if (hint === 'both') return '已对齐水平/竖直'
  return ''
}

function applySnapToBend(points, bendIndex, flowPoint) {
  const snapped = snapBendInPath(points, bendIndex, flowPoint)
  snapHint.value = formatSnapHint(snapped.hint)
  return { x: snapped.x, y: snapped.y }
}

function attachEdgeHandlers(edgeList) {
  syncNodeReconnectDecorations()
  return edgeList.map((edge) => ({
    ...edge,
    zIndex: edge.selected ? 1000 : 0,
    data: {
      ...edge.data,
      onSegmentMidDrag: handleSegmentMidDrag,
      onBendDrag: handleBendDrag,
      onEdgeSelect: handleEdgeSelect,
      onEndpointDrag: handleEndpointDrag,
      onEndpointActivate: handleEndpointActivate,
      activeEndpoint: edge.selected ? (reconnectDrag.value?.edgeId === edge.id ? reconnectDrag.value.end : activeEndpoint.value) : '',
      reconnectPreview: reconnectDrag.value?.edgeId === edge.id ? reconnectDrag.value.preview : undefined
    }
  }))
}

function handleEdgeSelect({ edgeId }) {
  if (props.tool === 'delete') {
    emit('edit-start', '删除连线')
    deleteEdge(edgeId)
    return
  }
  if (props.tool === 'pan' || props.tool === 'connect') return
  nodes.value = nodes.value.map((node) => ({
    ...node,
    selected: false,
    data: { ...node.data, selected: false, highlighted: false }
  }))
  edges.value = attachEdgeHandlers(
    edges.value.map((edge) => ({
      ...edge,
      selected: edge.id === edgeId,
      zIndex: edge.id === edgeId ? 1000 : 0,
      data: { ...edge.data, selected: edge.id === edgeId }
    }))
  )
  emit('update:selectedEdgeId', edgeId)
  emit('update:selectedNodeId', '')
  activeEndpoint.value = 'source'
  emitSelectionCounts([])
  syncNodeReconnectDecorations()
}

function syncFromDocument(doc, { clearSelection = false } = {}) {
  const selectedNodeIds = clearSelection
    ? new Set()
    : new Set(nodes.value.filter((n) => n.selected).map((n) => n.id))
  const selectedEdgeIds = clearSelection
    ? new Set()
    : new Set(edges.value.filter((e) => e.selected).map((e) => e.id))

  const decorated = decorateForEditor(doc.nodes || [], doc.edges || [], {
    editable: props.editable,
    tool: props.tool
  })
  nodes.value = decorated.nodes.map((node) => {
    const isSelected = selectedNodeIds.has(node.id)
    return {
      ...node,
      selected: isSelected,
      data: { ...node.data, selected: isSelected, highlighted: isSelected }
    }
  })
  const rebuilt = rebuildEdgePaths(decorated.nodes, decorated.edges)
  edges.value = attachEdgeHandlers(
    rebuilt.map((edge) => {
      const isSelected = selectedEdgeIds.has(edge.id)
      return {
        ...edge,
        selected: isSelected,
        zIndex: isSelected ? 1000 : 0,
        data: { ...edge.data, selected: isSelected }
      }
    })
  )
}

function emitDocument() {
  if (suppressEmit) return
  const doc = cloneDocument(props.document)
  doc.nodes = nodes.value.map(({ id, type, position, data, style }) => ({
    id, type, position: { ...position }, data: { ...data }, style: { ...style }
  }))
  doc.edges = edges.value.map(({ id, source, target, sourceHandle, targetHandle, type, data }) => ({
    id, source, target, sourceHandle, targetHandle, type: 'polyline',
    data: {
      directed: data?.directed ?? false,
      relKey: data?.relKey || '',
      waypoints: data?.waypoints || (data?.points?.length > 2 ? data.points.slice(1, -1) : [])
    }
  }))
  emit('update:document', doc)
  emit('change', doc)
}

function handleSegmentMidDrag({ edgeId, segmentIndex, phase, event }) {
  const edge = edges.value.find((e) => e.id === edgeId)
  if (!edge) return
  const flowPoint = screenToFlowCoordinate({ x: event.clientX, y: event.clientY })

  if (phase === 'start') {
    emit('edit-start', '添加折点')
    const pts = edge.data.points || resolveEdgePoints(edge, new Map(nodes.value.map((n) => [n.id, n])))
    const { points, bendIndex } = insertBendAtSegment(pts, segmentIndex)
    pendingSegmentInsert.set(edgeId, bendIndex)
    updateEdgePoints(edgeId, points, false)
    return
  }

  const bendIndex = pendingSegmentInsert.get(edgeId)
  if (bendIndex == null) return
  const current = edges.value.find((e) => e.id === edgeId)
  const snapped = applySnapToBend(current.data.points, bendIndex, flowPoint)
  const moved = moveBendPoint(current.data.points, bendIndex, snapped)
  updateEdgePoints(edgeId, moved, phase === 'end')
  if (phase === 'end') {
    pendingSegmentInsert.delete(edgeId)
    snapHint.value = ''
  }
}

function handleBendDrag({ edgeId, bendIndex, phase, event }) {
  if (phase === 'start') emit('edit-start', '调整折点')
  const flowPoint = screenToFlowCoordinate({ x: event.clientX, y: event.clientY })
  const edge = edges.value.find((e) => e.id === edgeId)
  if (!edge) return
  const snapped = applySnapToBend(edge.data.points, bendIndex, flowPoint)
  const moved = moveBendPoint(edge.data.points, bendIndex, snapped)
  updateEdgePoints(edgeId, moved, phase === 'end')
  if (phase === 'end') snapHint.value = ''
}

function updateEdgePoints(edgeId, points, shouldEmit = true) {
  edges.value = attachEdgeHandlers(edges.value.map((edge) => {
    if (edge.id !== edgeId) return edge
    const waypoints = points.length > 2 ? points.slice(1, -1) : []
    return {
      ...edge,
      data: {
        ...edge.data,
        points,
        path: pointsToSvgPath(points),
        waypoints
      }
    }
  }))
  if (shouldEmit) emitDocument()
}

function captureDragPositions(nodeList = nodes.value) {
  dragPrevPositions.clear()
  nodeList.forEach((node) => {
    dragPrevPositions.set(node.id, { x: node.position.x, y: node.position.y })
  })
}

function applyDragDeltas(deltas) {
  if (!deltas.size) return
  edges.value = attachEdgeHandlers(updateEdgesAfterNodeMove(nodes.value, edges.value, deltas))
}

function collectDragDeltas(movedNodes) {
  const deltas = new Map()
  for (const node of movedNodes) {
    const prev = dragPrevPositions.get(node.id)
    if (!prev) continue
    const dx = node.position.x - prev.x
    const dy = node.position.y - prev.y
    if (Math.abs(dx) < 0.01 && Math.abs(dy) < 0.01) continue
    deltas.set(node.id, { dx, dy })
    dragPrevPositions.set(node.id, { x: node.position.x, y: node.position.y })
  }
  return deltas
}

function onNodeDragStart() {
  captureDragPositions()
  emit('edit-start', '移动节点')
}

function onSelectionDragStart({ nodes: draggedNodes }) {
  captureDragPositions(draggedNodes || nodes.value.filter((n) => n.selected))
  emit('edit-start', '移动节点')
}

function applyNodeDragSnap(dragNodes) {
  if (!dragNodes?.length) return

  const moving = dragNodes.map((drag) => {
    const base = nodes.value.find((node) => node.id === drag.id) || drag
    return { ...base, position: { ...drag.position } }
  })
  const { positions, hint } = snapNodesGroupPositions(moving, nodes.value, undefined, edges.value)
  nodes.value = nodes.value.map((node) => {
    const nextPos = positions.get(node.id)
    if (!nextPos) return node
    if (nextPos.x === node.position.x && nextPos.y === node.position.y) return node
    return { ...node, position: nextPos }
  })
  snapHint.value = hint ? formatSnapHint(hint) : ''
}

function onNodeDrag({ node }) {
  if (!node) return
  applyNodeDragSnap([node])
  const current = nodes.value.find((n) => n.id === node.id)
  if (current) applyDragDeltas(collectDragDeltas([current]))
}

function onSelectionDrag({ nodes: draggedNodes }) {
  if (!draggedNodes?.length) return
  applyNodeDragSnap(draggedNodes)
  const currents = draggedNodes
    .map((node) => nodes.value.find((n) => n.id === node.id))
    .filter(Boolean)
  applyDragDeltas(collectDragDeltas(currents))
}

function onNodeDragStop() {
  dragPrevPositions.clear()
  snapHint.value = ''
  emitDocument()
}

function onNodeClick({ node, event }) {
  if (props.tool === 'delete') {
    deleteNode(node.id)
    event.stopPropagation()
  }
}

function onEdgeClick({ edge, event }) {
  if (props.tool === 'delete') {
    deleteEdge(edge.id)
    event.stopPropagation()
  }
}

function clearSelection() {
  removeSelectedElements()
  nodes.value = nodes.value.map((node) => ({
    ...node,
    selected: false,
    data: { ...node.data, selected: false, highlighted: false }
  }))
  edges.value = attachEdgeHandlers(
    edges.value.map((edge) => ({
      ...edge,
      selected: false,
      zIndex: 0,
      data: { ...edge.data, selected: false }
    }))
  )
  emit('update:selectedNodeId', '')
  emit('update:selectedEdgeId', '')
  activeEndpoint.value = ''
  reconnectDrag.value = null
  reconnectSnap.value = null
  emitSelectionCounts([])
  syncNodeReconnectDecorations()
}

function onPaneClick(event) {
  if (props.tool === 'add-hub') {
    emit('edit-start', '添加关系节点')
    addHubAt(event)
    return
  }
  if (props.tool === 'select') {
    clearSelection()
  }
}

function flowPointFromEvent(event) {
  return screenToFlowCoordinate({ x: event.clientX, y: event.clientY })
}

function addHubAt(event) {
  const pt = flowPointFromEvent(event)
  const relKey = nextRelationId(nodes.value)
  const node = createHubNode(props.addRelType, relKey, pt.x - 12, pt.y - 12)
  nodes.value = [...nodes.value, decorateForEditor([node], [], { editable: props.editable, tool: props.tool }).nodes[0]]
  emitDocument()
}

function getSelectedNodes() {
  return nodes.value
    .filter((node) => node.selected)
    .map(({ id, type, position, data, style }) => ({
      id,
      type,
      position: { ...position },
      data: stripNodeData(data),
      style: style ? { ...style } : undefined
    }))
}

function mergeIdentityNodes(nodeIds) {
  const result = mergePropNodesAsIdentity(
    nodes.value.map(({ id, type, position, data, style }) => ({
      id, type, position: { ...position }, data: stripNodeData(data), style: style ? { ...style } : undefined
    })),
    edges.value.map(({ id, source, target, sourceHandle, targetHandle, type, data }) => ({
      id, source, target, sourceHandle, targetHandle, type,
      data: stripEdgeData(data)
    })),
    nodeIds
  )
  if (!result) return null

  const decorated = decorateForEditor(result.nodes, result.edges, {
    editable: props.editable,
    tool: props.tool
  })
  nodes.value = decorated.nodes.map((node) => ({
    ...node,
    selected: node.id === result.mergedNodeId,
    data: {
      ...node.data,
      selected: node.id === result.mergedNodeId,
      highlighted: node.id === result.mergedNodeId
    }
  }))
  edges.value = attachEdgeHandlers(decorated.edges)
  emit('update:selectedNodeId', result.mergedNodeId)
  emit('update:selectedEdgeId', '')
  emitSelectionCounts(nodes.value.filter((node) => node.selected))
  emitDocument()
  return result.mergedNodeId
}

function addPropositionNodes(propositionList) {
  if (!propositionList?.length) return 0
  const existingIds = getCanvasPropIds(nodes.value)
  const toAdd = propositionList.filter((p) => !existingIds.has(p.propId))
  if (!toAdd.length) return 0

  const positions = computeGridPositions(nodes.value, toAdd.length)
  const newNodes = toAdd.map((p, i) => createPropNodeFromProposition(p, positions[i].x, positions[i].y))
  const decorated = decorateForEditor(newNodes, [], { editable: props.editable, tool: props.tool })
  nodes.value = [...nodes.value, ...decorated.nodes]
  emitDocument()
  return toAdd.length
}

function onConnect(params) {
  emit('edit-start', '连接节点')
  const id = nextEdgeId(edges.value)
  const edge = createEdge(id, params.source, params.target, params.sourceHandle, params.targetHandle, {
    directed: false
  })
  const nodeById = new Map(nodes.value.map((n) => [n.id, n]))
  const points = resolveEdgePoints(edge, nodeById)
  edge.data.points = points
  edge.data.waypoints = []
  edges.value = attachEdgeHandlers([...edges.value, edge])
  emitDocument()
}

function deleteNode(nodeId) {
  const node = nodes.value.find((n) => n.id === nodeId)
  if (!node) return
  removeNodes([node])
}

function deleteEdge(edgeId) {
  const edge = edges.value.find((e) => e.id === edgeId)
  if (!edge) return
  removeEdges([edge])
}

function pruneOrphanEdges() {
  const nodeIds = new Set(nodes.value.map((n) => n.id))
  edges.value = attachEdgeHandlers(edges.value.filter((e) => nodeIds.has(e.source) && nodeIds.has(e.target)))
}

function emitSelectionCounts(selNodes = nodes.value.filter((n) => n.selected)) {
  emit('update:selectedNodeCount', selNodes.length)
  emit('update:selectedPropCount', selNodes.filter((node) => node.type === 'prop').length)
}

function syncSelectionFromCanvas() {
  if (props.tool !== 'select') return
  const selNodes = nodes.value.filter((n) => n.selected)
  const selEdges = edges.value.filter((e) => e.selected)
  emit('update:selectedNodeId', selNodes.length === 1 ? selNodes[0].id : '')
  emit('update:selectedEdgeId', selEdges.length === 1 ? selEdges[0].id : '')
  emitSelectionCounts(selNodes)
}

function refreshDecorations() {
  const selectedNodeIds = new Set(nodes.value.filter((n) => n.selected).map((n) => n.id))
  const selectedEdgeIds = new Set(edges.value.filter((e) => e.selected).map((e) => e.id))
  const edgeReconnect = edges.value.some((edge) => edge.selected) || !!reconnectDrag.value
  const decorated = decorateForEditor(
    nodes.value.map(({ id, type, position, data, style, selected }) => ({
      id, type, position, style, selected: selectedNodeIds.has(id) || !!selected, data: stripNodeData(data)
    })),
    edges.value.map(({ id, source, target, sourceHandle, targetHandle, type, data, selected }) => ({
      id, source, target, sourceHandle, targetHandle, type, selected: selectedEdgeIds.has(id) || !!selected, data: stripEdgeData(data)
    })),
    { editable: props.editable, tool: props.tool, edgeReconnect }
  )
  nodes.value = decorated.nodes.map((node) => {
    const isSelected = selectedNodeIds.has(node.id)
    return {
      ...node,
      selected: isSelected,
      data: { ...node.data, selected: isSelected, highlighted: isSelected }
    }
  })
  const rebuilt = rebuildEdgePaths(decorated.nodes, decorated.edges)
  edges.value = attachEdgeHandlers(
    rebuilt.map((edge) => {
      const isSelected = selectedEdgeIds.has(edge.id)
      return {
        ...edge,
        selected: isSelected,
        zIndex: isSelected ? 1000 : 0,
        data: { ...edge.data, selected: isSelected }
      }
    })
  )
}

function onSelectionChange({ nodes: selNodes, edges: selEdges }) {
  if (props.tool !== 'select') return
  emit('update:selectedNodeId', selNodes?.length === 1 ? selNodes[0].id : '')
  emit('update:selectedEdgeId', selEdges?.length === 1 ? selEdges[0].id : '')
  emitSelectionCounts(selNodes || [])
  if (selEdges?.length === 1) {
    activeEndpoint.value = 'source'
  } else if (!selEdges?.length) {
    activeEndpoint.value = ''
    reconnectDrag.value = null
    reconnectSnap.value = null
  }
  edges.value = attachEdgeHandlers(edges.value)
}

function onNodesChange(changes) {
  const hasRemove = changes.some((c) => c.type === 'remove')
  const hasSelect = changes.some((c) => c.type === 'select')
  const hasPositionEnd = changes.some((c) => c.type === 'position' && c.dragging === false)

  if (hasRemove) {
    emit('edit-start', '删除节点')
    nextTick(() => {
      pruneOrphanEdges()
      syncSelectionFromCanvas()
      emitDocument()
    })
    return
  }
  if (hasPositionEnd) {
    nextTick(() => emitDocument())
    return
  }
  if (hasSelect) {
    nextTick(() => syncSelectionFromCanvas())
  }
}

function onEdgesChange(changes) {
  nextTick(() => {
    edges.value = attachEdgeHandlers(edges.value)
    if (changes.some((c) => c.type === 'remove')) {
      emit('edit-start', '删除连线')
      emitDocument()
      syncSelectionFromCanvas()
    } else if (changes.some((c) => c.type === 'select')) {
      syncSelectionFromCanvas()
    }
  })
}

function fitViewNow() {
  requestAnimationFrame(() => fitView({ padding: 0.2, duration: 280 }))
}

function onNodesInitialized() {
  fitViewNow()
}

function importDocument(doc) {
  suppressEmit = true
  syncFromDocument(doc, { clearSelection: true })
  suppressEmit = false
  emit('update:selectedNodeId', '')
  emit('update:selectedEdgeId', '')
  emitSelectionCounts([])
  fitViewNow()
}

function getDocumentSnapshot() {
  const doc = cloneDocument(props.document)
  doc.nodes = nodes.value.map(({ id, type, position, data, style }) => ({ id, type, position: { ...position }, data: { ...data }, style: { ...style } }))
  doc.edges = edges.value.map(({ id, source, target, sourceHandle, targetHandle, data }) => ({
    id, source, target, sourceHandle, targetHandle, type: 'polyline',
    data: { directed: data?.directed ?? false, relKey: data?.relKey || '', waypoints: data?.waypoints || [] }
  }))
  return doc
}

function patchSelectedEdge(patch, edgeIdOverride) {
  const edgeId = edgeIdOverride || props.selectedEdgeId || edges.value.find((e) => e.selected)?.id
  if (!edgeId) return
  edges.value = edges.value.map((edge) => {
    if (edge.id !== edgeId) return edge
    const nextData = {
      ...edge.data,
      directed: patch.directed ?? edge.data.directed
    }
    if (patch.waypoints !== undefined) {
      nextData.waypoints = patch.waypoints
      delete nextData.points
      delete nextData.path
    }
    return {
      ...edge,
      sourceHandle: patch.sourceHandle || edge.sourceHandle,
      targetHandle: patch.targetHandle || edge.targetHandle,
      data: nextData
    }
  })
  edges.value = attachEdgeHandlers(rebuildEdgePaths(nodes.value, edges.value))
  emitDocument()
}

defineExpose({
  fitViewNow,
  importDocument,
  getDocumentSnapshot,
  patchSelectedEdge,
  deleteNode,
  deleteEdge,
  addPropositionNodes,
  getSelectedNodes,
  mergeIdentityNodes,
  zoomBy: (d) => (d > 0 ? zoomIn({ duration: 150 }) : zoomOut({ duration: 150 }))
})

watch(() => props.document, (doc) => {
  if (suppressEmit) return
  syncFromDocument(doc)
}, { deep: true })

watch(() => props.tool, (tool, prevTool) => {
  if (prevTool === 'select' && tool !== 'select') {
    emit('update:selectedNodeId', '')
    emit('update:selectedEdgeId', '')
    emitSelectionCounts([])
  }
  refreshDecorations()
})

onMounted(() => {
  syncFromDocument(props.document)
  if (viewportRef.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => fitViewNow())
    resizeObserver.observe(viewportRef.value)
  }
})

onBeforeUnmount(() => resizeObserver?.disconnect())
</script>

<style scoped>
.graph-editor-canvas {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  position: relative;
}

.graph-editor-canvas-viewport {
  flex: 1;
  min-height: 240px;
  position: relative;
}

.graph-editor-flow {
  width: 100%;
  height: 100%;
  background: #fff;
}

.graph-editor-flow.is-select-mode {
  cursor: default;
}

.graph-editor-flow.is-add-mode,
.graph-editor-flow.is-connect-mode {
  cursor: crosshair;
}

.graph-editor-empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: #94a3b8;
  font-size: 13px;
}

.graph-editor-cursor-hint {
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(37, 99, 235, 0.92);
  color: #fff;
  font-size: 12px;
  padding: 6px 14px;
  border-radius: 999px;
  pointer-events: none;
  z-index: 10;
}

.graph-editor-cursor-hint.connect {
  background: rgba(15, 23, 42, 0.82);
}

.graph-editor-snap-hint {
  position: absolute;
  top: 48px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(22, 163, 74, 0.92);
  color: #fff;
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 999px;
  pointer-events: none;
  z-index: 10;
}

:deep(.vue-flow__node) {
  padding: 0;
  border: none;
  background: transparent;
  box-shadow: none;
}

:deep(.vue-flow__node.selected) {
  z-index: 2;
}

:deep(.vue-flow__selection) {
  background: rgba(37, 99, 235, 0.08);
  border: 1px solid rgba(37, 99, 235, 0.55);
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

:deep(.graph-editor-flow.is-reconnect-mode .vue-flow__edges) {
  z-index: 10;
}

:deep(.graph-editor-flow.is-reconnect-mode .vue-flow__nodes) {
  z-index: 1;
}

:deep(.graph-editor-flow.is-reconnect-mode .vue-flow__node) {
  pointer-events: none;
}

:deep(.graph-editor-flow.is-reconnect-mode .ag-handle--visible),
:deep(.graph-editor-flow.is-reconnect-mode .vue-flow__handle) {
  pointer-events: all !important;
  cursor: crosshair;
}

:deep(.graph-editor-flow.is-reconnect-mode .ag-handle--visible) {
  width: 10px !important;
  height: 10px !important;
  min-width: 10px !important;
  min-height: 10px !important;
}

:deep(.graph-editor-flow.is-connect-mode .ag-handle--visible),
:deep(.graph-editor-flow.is-reconnect-mode .ag-handle--visible),
:deep(.graph-editor-flow.is-add-mode .ag-handle--visible) {
  opacity: 1 !important;
}
</style>
