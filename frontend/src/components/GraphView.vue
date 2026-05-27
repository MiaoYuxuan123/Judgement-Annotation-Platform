<template>
  <div class="graph">
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
      <text v-for="rel in renderedRelations" :key="`${rel.relId}-label`" :x="(rel.x1 + rel.x2) / 2" :y="(rel.y1 + rel.y2) / 2 - 6" fill="#334155" font-size="12">
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
  relations: { type: Array, default: () => [] }
})

function pos(index) {
  return {
    x: 38 + (index % 2) * 230,
    y: 32 + Math.floor(index / 2) * 116
  }
}

function nodeStyle(index) {
  const p = pos(index)
  return { left: `${p.x}px`, top: `${p.y}px` }
}

const renderedRelations = computed(() => {
  const indexById = new Map(props.propositions.map((p, i) => [p.propId, i]))
  return props.relations
      .filter((rel) => indexById.has(rel.source) && indexById.has(rel.target))
      .map((rel) => {
        const a = pos(indexById.get(rel.source))
        const b = pos(indexById.get(rel.target))
        return { ...rel, x1: a.x + 150, y1: a.y + 34, x2: b.x, y2: b.y + 34 }
      })
})
</script>
