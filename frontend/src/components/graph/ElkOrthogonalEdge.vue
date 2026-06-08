<template>
  <g>
    <path
      :d="pathD"
      fill="none"
      :stroke="edgeColor"
      :stroke-width="edgeWidth"
      stroke-linecap="square"
      stroke-linejoin="miter"
    />
    <polygon v-if="arrowPoints" :points="arrowPoints" :fill="edgeColor" />
  </g>
</template>

<script setup>
import { computed } from 'vue'
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

const pathD = computed(() => {
  const pts = props.data.points
  if (pts?.length >= 2) {
    return pts.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
  }
  const endpoints = edgeEndpoints.value
  return `M ${endpoints.a.x} ${endpoints.a.y} L ${endpoints.b.x} ${endpoints.b.y}`
})

const edgeColor = computed(() => (props.data?.highlighted ? '#2563eb' : '#111'))
const edgeWidth = computed(() => (props.data?.highlighted ? 3 : 1.2))

const edgeEndpoints = computed(() => {
  const pts = props.data.points
  if (pts?.length >= 2) {
    return {
      a: pts[0],
      b: pts[pts.length - 1]
    }
  }
  return {
    a: { x: props.sourceX, y: props.sourceY },
    b: { x: props.targetX, y: props.targetY }
  }
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
</script>
