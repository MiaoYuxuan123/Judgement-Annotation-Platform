<template>
  <div class="graph" :class="`graph--${variant}`">
    <svg>
      <defs>
        <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
          <path d="M 0 0 L 10 5 L 0 10 z" fill="#64748b" />
        </marker>
      </defs>
      <line
          v-for="rel in renderedRelations"
          :key="rel.relId"
          :x1="rel.x1"
          :y1="rel.y1"
          :x2="rel.x2"
          :y2="rel.y2"
          stroke="#64748b"
          stroke-width="2"
          marker-end="url(#arrow)"
      />
      <circle
          v-if="variant === 'circles'"
          v-for="(p, index) in propositions"
          :key="`dot-${p.propId}`"
          :cx="nodePos(index).x + 22"
          :cy="nodePos(index).y + 22"
          r="22"
          fill="#fff"
          stroke="#2563eb"
          stroke-width="2"
      />
      <text
          v-if="variant === 'circles'"
          v-for="(p, index) in propositions"
          :key="`label-${p.propId}`"
          :x="nodePos(index).x + 22"
          :y="nodePos(index).y + 28"
          text-anchor="middle"
          font-size="14"
          fill="#1e3a8a"
          font-weight="700"
      >
        {{ p.sequenceNo }}
      </text>
      <text
          v-for="rel in renderedRelations"
          :key="`${rel.relId}-label`"
          :x="(rel.x1 + rel.x2) / 2"
          :y="(rel.y1 + rel.y2) / 2 - 6"
          fill="#334155"
          font-size="12"
      >
        {{ rel.type }}
      </text>
    </svg>
    <div v-for="(p, index) in propositions" :key="p.propId" class="graph-node" :style="nodeStyle(index)">
      <strong>{{ p.propId || `P${p.sequenceNo}` }} · {{ p.tag }}</strong>
      <div>{{ p.text }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  propositions: { type: Array, default: () => [] },
  relations: { type: Array, default: () => [] },
  variant: { type: String, default: 'cards' }
})

function nodePos(index) {
  const col = index % 3
  const row = Math.floor(index / 3)
  return { x: 48 + col * 150, y: 40 + row * 110 }
}

function nodeStyle(index) {
  const p = nodePos(index)
  return { left: `${p.x}px`, top: `${p.y}px` }
}

const renderedRelations = computed(() => {
  const indexById = new Map(props.propositions.map((p, i) => [p.propId, i]))
  return props.relations
      .filter((rel) => indexById.has(rel.source) && indexById.has(rel.target))
      .map((rel) => {
        const a = nodePos(indexById.get(rel.source))
        const b = nodePos(indexById.get(rel.target))
        const offset = props.variant === 'circles' ? 44 : 34
        const width = props.variant === 'circles' ? 44 : 150
        return {
          ...rel,
          x1: a.x + width,
          y1: a.y + offset,
          x2: b.x,
          y2: b.y + offset
        }
      })
})
</script>
