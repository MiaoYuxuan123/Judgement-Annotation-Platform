<template>
  <g>
    <path
      :d="pathD"
      fill="none"
      :stroke="edgeColor"
      :stroke-width="edgeWidth"
      stroke-linecap="square"
      stroke-linejoin="miter"
      :class="{ 'ag-edge--selected': data.selected }"
    />
    <polygon v-if="arrowPoints" :points="arrowPoints" :fill="edgeColor" />
    <template v-if="data.editable && data.selected && waypointHandles.length">
      <circle
        v-for="(point, index) in waypointHandles"
        :key="`${id}-wp-${index}`"
        :cx="point.x"
        :cy="point.y"
        r="5"
        class="ag-edge-waypoint"
        @mousedown.stop="startDrag(index, $event)"
      />
    </template>
  </g>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { pointsToSvgPath } from '../../utils/graphLayoutOverride'

const props = defineProps({
  id: String,
  source: String,
  target: String,
  sourceX: Number,
  sourceY: Number,
  targetX: Number,
  targetY: Number,
  sourcePosition: String,
  targetPosition: String,
  data: { type: Object, default: () => ({}) }
})

const draggingIndex = ref(-1)
const dragOffset = ref({ x: 0, y: 0 })

const pathD = computed(() => {
  const pts = props.data.points
  if (pts?.length >= 2) return pointsToSvgPath(pts)
  const endpoints = edgeEndpoints.value
  return `M ${endpoints.a.x} ${endpoints.a.y} L ${endpoints.b.x} ${endpoints.b.y}`
})

const edgeColor = computed(() => {
  if (props.data?.selected) return '#2563eb'
  if (props.data?.highlighted) return '#2563eb'
  return '#111'
})
const edgeWidth = computed(() => (props.data?.highlighted || props.data?.selected ? 2.4 : 1.2))

const edgeEndpoints = computed(() => {
  const pts = props.data.points
  if (pts?.length >= 2) {
    return { a: pts[0], b: pts[pts.length - 1] }
  }
  return {
    a: { x: props.sourceX, y: props.sourceY },
    b: { x: props.targetX, y: props.targetY }
  }
})

const waypointHandles = computed(() => {
  const pts = props.data.points || []
  if (pts.length <= 2) return []
  return pts.slice(1, -1)
})

const arrowPoints = computed(() => {
  if (!props.data?.directed) return null
  const pts = props.data.points
  const a = pts?.length >= 2 ? pts[pts.length - 2] : edgeEndpoints.value.a
  const b = pts?.length >= 2 ? pts[pts.length - 1] : edgeEndpoints.value.b
  const ang = Math.atan2(b.y - a.y, b.x - a.x)
  const size = 7
  const x1 = b.x - size * Math.cos(ang - 0.4)
  const y1 = b.y - size * Math.sin(ang - 0.4)
  const x2 = b.x - size * Math.cos(ang + 0.4)
  const y2 = b.y - size * Math.sin(ang + 0.4)
  return `${b.x},${b.y} ${x1},${y1} ${x2},${y2}`
})

function startDrag(index, event) {
  draggingIndex.value = index
  dragOffset.value = { x: event.clientX, y: event.clientY }
  window.addEventListener('mousemove', onDrag)
  window.addEventListener('mouseup', stopDrag)
  event.preventDefault()
}

function onDrag(event) {
  if (draggingIndex.value < 0 || !props.data?.onWaypointMove) return
  props.data.onWaypointMove({
    edgeId: props.id,
    index: draggingIndex.value,
    clientX: event.clientX,
    clientY: event.clientY
  })
}

function stopDrag() {
  draggingIndex.value = -1
  window.removeEventListener('mousemove', onDrag)
  window.removeEventListener('mouseup', stopDrag)
}

onBeforeUnmount(stopDrag)
</script>

<style scoped>
.ag-edge-waypoint {
  fill: #fff;
  stroke: #2563eb;
  stroke-width: 1.5;
  cursor: move;
}
</style>

<style>
.ag-edge--selected {
  filter: drop-shadow(0 0 2px rgba(37, 99, 235, 0.35));
}
</style>
