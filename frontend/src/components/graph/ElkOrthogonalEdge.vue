<template>
  <g>
    <path
      :d="pathD"
      fill="none"
      stroke="#111"
      stroke-width="1.2"
      stroke-linecap="square"
      stroke-linejoin="miter"
    />
    <polygon v-if="arrowPoints" :points="arrowPoints" fill="#111" />
  </g>
</template>

<script setup>
import { computed } from 'vue'
import { getSmoothStepPath } from '@vue-flow/core'

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
  if (props.data?.path) return props.data.path
  const [d] = getSmoothStepPath({
    sourceX: props.sourceX,
    sourceY: props.sourceY,
    targetX: props.targetX,
    targetY: props.targetY,
    sourcePosition: props.sourcePosition,
    targetPosition: props.targetPosition,
    borderRadius: 0
  })
  return d
})

const arrowPoints = computed(() => {
  if (!props.data?.directed) return null
  const pts = props.data.points
  let a
  let b
  if (pts?.length >= 2) {
    a = pts[pts.length - 2]
    b = pts[pts.length - 1]
  } else {
    b = { x: props.targetX, y: props.targetY }
    a = { x: props.sourceX, y: props.sourceY }
  }
  const ang = Math.atan2(b.y - a.y, b.x - a.x)
  const size = 7
  const x1 = b.x - size * Math.cos(ang - 0.4)
  const y1 = b.y - size * Math.sin(ang - 0.4)
  const x2 = b.x - size * Math.cos(ang + 0.4)
  const y2 = b.y - size * Math.sin(ang + 0.4)
  return `${b.x},${b.y} ${x1},${y1} ${x2},${y2}`
})
</script>
