<template>
  <div class="ag-hub-node" :class="hubClass">
    <span v-if="showPlus" class="ag-hub-plus">+</span>
    <Handle id="top" type="target" :position="Position.Top" />
    <Handle id="bottom" type="source" :position="Position.Bottom" />
    <Handle id="left" type="target" :position="Position.Left" />
    <Handle id="right" type="source" :position="Position.Right" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'

const props = defineProps({
  data: { type: Object, default: () => ({}) }
})

const hubClass = computed(() => {
  const k = props.data.hubKind || 'hub-m'
  return [`ag-hub--${k.replace('hub-', '')}`, props.data.highlighted ? 'ag-hub--highlighted' : '']
})

const showPlus = computed(() => {
  const k = props.data.hubKind || ''
  return ['hub-j', 'hub-m'].includes(k)
})
</script>

<style scoped>
.ag-hub-node {
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  border-radius: 50%;
  display: grid;
  place-items: center;
  pointer-events: none;
}

.ag-hub--s {
  background: #111;
  border: none;
}

.ag-hub--a,
.ag-hub--m,
.ag-hub--j {
  background: #fff;
  border: 1.8px solid #111;
}

.ag-hub-plus {
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
  color: #111;
  user-select: none;
}

.ag-hub--highlighted {
  border-color: #2563eb;
  box-shadow: 0 0 0 5px rgba(37, 99, 235, .16);
}

.ag-hub--s.ag-hub--highlighted {
  background: #2563eb;
}

.ag-hub--highlighted .ag-hub-plus {
  color: #2563eb;
}
</style>
