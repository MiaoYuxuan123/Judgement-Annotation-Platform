<template>
  <g class="ag-polyline-edge">
    <!-- 未选中：宽透明命中区，便于选中连线 -->
    <path
      v-if="!isSelected"
      :d="pathD"
      fill="none"
      stroke="transparent"
      stroke-width="20"
      stroke-linecap="round"
      stroke-linejoin="round"
      class="ag-edge-hit"
      @pointerdown.stop.prevent="onEdgePointerDown"
    />

    <!-- 可见路径（不参与命中） -->
    <path
      :d="pathD"
      fill="none"
      :stroke="edgeColor"
      :stroke-width="edgeWidth"
      stroke-linecap="round"
      stroke-linejoin="round"
      class="ag-edge-visible"
      :class="{ 'ag-edge--selected': isSelected }"
      pointer-events="none"
    />
    <polygon v-if="arrowPoints" :points="arrowPoints" :fill="edgeColor" pointer-events="none" />

    <!-- 选中后：端点 + 折点操作 -->
    <g v-if="data.editable && isSelected" class="ag-edge-handles" pointer-events="all">
      <g
        class="ag-edge-handle-group ag-edge-endpoint-group"
        :class="{ 'is-active': data.activeEndpoint === 'source' }"
      >
        <circle
          :cx="sourcePoint.x"
          :cy="sourcePoint.y"
          :r="ENDPOINT_HIT_R"
          class="ag-edge-endpoint-hit"
          @pointerdown.stop.prevent="onEndpointDown('source', $event)"
          @click.stop="onEndpointActivate('source')"
        />
        <circle
          :cx="sourcePoint.x"
          :cy="sourcePoint.y"
          :r="ENDPOINT_VISIBLE_R"
          class="ag-edge-endpoint-dot ag-edge-endpoint-dot--source"
          pointer-events="none"
        />
      </g>
      <g
        class="ag-edge-handle-group ag-edge-endpoint-group"
        :class="{ 'is-active': data.activeEndpoint === 'target' }"
      >
        <circle
          :cx="targetPoint.x"
          :cy="targetPoint.y"
          :r="ENDPOINT_HIT_R"
          class="ag-edge-endpoint-hit"
          @pointerdown.stop.prevent="onEndpointDown('target', $event)"
          @click.stop="onEndpointActivate('target')"
        />
        <circle
          :cx="targetPoint.x"
          :cy="targetPoint.y"
          :r="ENDPOINT_VISIBLE_R"
          class="ag-edge-endpoint-dot ag-edge-endpoint-dot--target"
          pointer-events="none"
        />
      </g>
      <g
        v-for="(mid, index) in segmentMidpoints"
        :key="`${id}-seg-${index}`"
        class="ag-edge-handle-group"
      >
        <circle
          :cx="mid.x"
          :cy="mid.y"
          :r="SEGMENT_HIT_R"
          class="ag-edge-segment-hit"
          @pointerdown.stop.prevent="onSegmentMidDown(mid.segmentIndex, $event)"
        />
        <circle
          :cx="mid.x"
          :cy="mid.y"
          :r="SEGMENT_VISIBLE_R"
          class="ag-edge-segment-dot"
          pointer-events="none"
        />
      </g>
      <g
        v-for="(point, index) in bendHandles"
        :key="`${id}-bend-${index}`"
        class="ag-edge-handle-group"
        :class="{ 'is-hover': hoverTarget === `bend-${index}` }"
        @pointerenter="hoverTarget = `bend-${index}`"
        @pointerleave="hoverTarget = ''"
      >
        <circle
          :cx="point.x"
          :cy="point.y"
          :r="BEND_HIT_R"
          class="ag-edge-bend-hit"
          @pointerdown.stop.prevent="onBendDown(point.bendIndex, $event)"
        />
        <circle
          :cx="point.x"
          :cy="point.y"
          :r="BEND_VISIBLE_R"
          class="ag-edge-bend-dot"
          pointer-events="none"
        />
      </g>
    </g>
  </g>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { pointsToSvgPath, segmentMidpoints as calcSegmentMidpoints } from '../../utils/graphDocument'

const DRAG_THRESHOLD = 3
const SEGMENT_VISIBLE_R = 4
const SEGMENT_HIT_R = 12
const BEND_VISIBLE_R = 5.5
const BEND_HIT_R = 14
const ENDPOINT_VISIBLE_R = 6
const ENDPOINT_HIT_R = 14

const props = defineProps({
  id: String,
  source: String,
  target: String,
  sourceX: Number,
  sourceY: Number,
  targetX: Number,
  targetY: Number,
  selected: { type: Boolean, default: false },
  data: { type: Object, default: () => ({}) }
})

const dragMode = ref(null)
const dragIndex = ref(-1)
const dragStart = ref(null)
const segmentActivated = ref(false)
const hoverTarget = ref('')

const pathD = computed(() => {
  const pts = effectivePoints.value
  if (pts?.length >= 2) return pointsToSvgPath(pts)
  return `M ${props.sourceX} ${props.sourceY} L ${props.targetX} ${props.targetY}`
})

const effectivePoints = computed(() => {
  const pts = (props.data.points || []).map((point) => ({ ...point }))
  if (pts.length < 2) return pts
  const preview = props.data.reconnectPreview
  if (preview?.end === 'source') pts[0] = { x: preview.x, y: preview.y }
  if (preview?.end === 'target') pts[pts.length - 1] = { x: preview.x, y: preview.y }
  return pts
})

const sourcePoint = computed(() => effectivePoints.value[0] || { x: props.sourceX, y: props.sourceY })
const targetPoint = computed(() => {
  const pts = effectivePoints.value
  if (pts.length >= 2) return pts[pts.length - 1]
  return { x: props.targetX, y: props.targetY }
})

const isSelected = computed(() => props.selected || props.data?.selected)

const edgeColor = computed(() => (isSelected.value || props.data?.highlighted ? '#c41e3a' : '#1a1817'))
const edgeWidth = computed(() => (isSelected.value || props.data?.highlighted ? 2.6 : 1.6))

const segmentMidpoints = computed(() => calcSegmentMidpoints(props.data.points || []))

const bendHandles = computed(() => {
  const pts = props.data.points || []
  if (pts.length <= 2) return []
  return pts.slice(1, -1).map((p, i) => ({ ...p, bendIndex: i + 1 }))
})

const arrowPoints = computed(() => {
  if (!props.data?.directed) return null
  const pts = effectivePoints.value
  if (!pts || pts.length < 2) return null
  const a = pts[pts.length - 2]
  const b = pts[pts.length - 1]
  const ang = Math.atan2(b.y - a.y, b.x - a.x)
  const size = 7
  return `${b.x},${b.y} ${b.x - size * Math.cos(ang - 0.4)},${b.y - size * Math.sin(ang - 0.4)} ${b.x - size * Math.cos(ang + 0.4)},${b.y - size * Math.sin(ang + 0.4)}`
})

function onEdgePointerDown(event) {
  props.data.onEdgeSelect?.({ edgeId: props.id, event })
}

function bindDragListeners() {
  window.addEventListener('pointermove', onMove, { capture: true })
  window.addEventListener('pointerup', onUp, { capture: true })
  window.addEventListener('pointercancel', onUp, { capture: true })
}

function unbindDragListeners() {
  window.removeEventListener('pointermove', onMove, { capture: true })
  window.removeEventListener('pointerup', onUp, { capture: true })
  window.removeEventListener('pointercancel', onUp, { capture: true })
}

function onSegmentMidDown(segmentIndex, event) {
  if (event.button !== 0) return
  dragMode.value = 'segment'
  dragIndex.value = segmentIndex
  segmentActivated.value = false
  dragStart.value = { x: event.clientX, y: event.clientY }
  bindDragListeners()
  event.target?.setPointerCapture?.(event.pointerId)
}

function onEndpointActivate(end) {
  props.data.onEndpointActivate?.({ edgeId: props.id, end })
}

function onEndpointDown(end, event) {
  if (event.button !== 0) return
  onEndpointActivate(end)
  dragMode.value = 'endpoint'
  dragIndex.value = end === 'source' ? 0 : 1
  endpointEnd.value = end
  dragStart.value = { x: event.clientX, y: event.clientY }
  props.data.onEndpointDrag?.({ edgeId: props.id, end, phase: 'start', event })
  bindDragListeners()
  event.target?.setPointerCapture?.(event.pointerId)
}

const endpointEnd = ref('source')

function onBendDown(bendIndex, event) {
  if (event.button !== 0) return
  dragMode.value = 'bend'
  dragIndex.value = bendIndex
  segmentActivated.value = true
  dragStart.value = { x: event.clientX, y: event.clientY }
  props.data.onBendDrag?.({ edgeId: props.id, bendIndex, phase: 'start', event })
  bindDragListeners()
  event.target?.setPointerCapture?.(event.pointerId)
}

function dragDistance(event) {
  if (!dragStart.value) return 0
  const dx = event.clientX - dragStart.value.x
  const dy = event.clientY - dragStart.value.y
  return Math.sqrt(dx * dx + dy * dy)
}

function onMove(event) {
  if (dragMode.value === 'segment') {
    if (!segmentActivated.value) {
      if (dragDistance(event) < DRAG_THRESHOLD) return
      segmentActivated.value = true
      props.data.onSegmentMidDrag?.({
        edgeId: props.id,
        segmentIndex: dragIndex.value,
        phase: 'start',
        event
      })
    }
    props.data.onSegmentMidDrag?.({
      edgeId: props.id,
      segmentIndex: dragIndex.value,
      phase: 'move',
      event
    })
  } else if (dragMode.value === 'bend') {
    props.data.onBendDrag?.({
      edgeId: props.id,
      bendIndex: dragIndex.value,
      phase: 'move',
      event
    })
  } else if (dragMode.value === 'endpoint') {
    props.data.onEndpointDrag?.({
      edgeId: props.id,
      end: endpointEnd.value,
      phase: 'move',
      event
    })
  }
}

function onUp(event) {
  if (dragMode.value === 'segment' && segmentActivated.value) {
    props.data.onSegmentMidDrag?.({
      edgeId: props.id,
      segmentIndex: dragIndex.value,
      phase: 'end',
      event
    })
  } else if (dragMode.value === 'bend') {
    props.data.onBendDrag?.({
      edgeId: props.id,
      bendIndex: dragIndex.value,
      phase: 'end',
      event
    })
  } else if (dragMode.value === 'endpoint') {
    props.data.onEndpointDrag?.({
      edgeId: props.id,
      end: endpointEnd.value,
      phase: 'end',
      event
    })
  }
  endpointEnd.value = 'source'
  dragMode.value = null
  dragIndex.value = -1
  segmentActivated.value = false
  dragStart.value = null
  unbindDragListeners()
}

onBeforeUnmount(unbindDragListeners)
</script>

<style scoped>
.ag-edge-hit {
  cursor: pointer;
  pointer-events: stroke;
}

.ag-edge-visible {
  pointer-events: none;
}

.ag-edge-handles {
  pointer-events: all;
}

.ag-edge-handle-group {
  pointer-events: all;
}

.ag-edge-segment-hit,
.ag-edge-bend-hit,
.ag-edge-endpoint-hit {
  fill: transparent;
  cursor: crosshair;
}

.ag-edge-bend-hit {
  cursor: move;
}

.ag-edge-endpoint-hit {
  cursor: grab;
}

.ag-edge-endpoint-dot {
  fill: #fff;
  stroke: #c41e3a;
  stroke-width: 2;
}

.ag-edge-endpoint-dot--target {
  fill: #fef2f2;
}

.ag-edge-endpoint-group.is-active .ag-edge-endpoint-dot {
  fill: #c41e3a;
  stroke: #fff;
  stroke-width: 2;
}

.ag-edge-endpoint-group:hover .ag-edge-endpoint-dot {
  fill: #fef2f2;
  stroke: #991b1b;
}

.ag-edge-segment-dot {
  fill: #fff;
  stroke: #e3828a;
  stroke-width: 1.5;
}

.ag-edge-handle-group:hover .ag-edge-segment-dot {
  fill: #fef2f2;
  stroke: #c41e3a;
  stroke-width: 2;
}

.ag-edge-bend-dot {
  fill: #c41e3a;
  stroke: #fff;
  stroke-width: 1.5;
}

.ag-edge-handle-group:hover .ag-edge-bend-dot,
.ag-edge-handle-group.is-hover .ag-edge-bend-dot {
  fill: #991b1b;
  stroke: #c41e3a;
  stroke-width: 2;
}
</style>

<style>
.ag-edge--selected {
  filter: drop-shadow(0 0 3px rgba(37, 99, 235, 0.4));
}

.ag-polyline-edge {
  pointer-events: visiblePainted;
}
</style>
