<template>
  <div class="ag-prop-node" :class="{ 'ag-prop-node--highlighted': data.highlighted }">
    <span class="ag-prop-label">{{ data.label }}</span>
    <Handle id="top" type="target" :position="Position.Top" />
    <Handle id="bottom" type="source" :position="Position.Bottom" />
    <Handle id="left" type="target" :position="Position.Left" />
    <Handle id="right" type="source" :position="Position.Right" />
  </div>
</template>

<script setup>
import { Handle, Position } from '@vue-flow/core'

defineProps({
  data: { type: Object, default: () => ({}) }
})
</script>

<style scoped>
.ag-prop-node {
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  display: grid;
  place-items: center;
  border: 1.2px solid #111;
  background: #fff;
  font-size: 13px;
  font-weight: 600;
  color: #111;
  font-family: Inter, 'Microsoft YaHei', sans-serif;
  pointer-events: none;
}

.ag-prop-node--highlighted {
  border-color: #2563eb;
  border-width: 2.4px;
  color: #1d4ed8;
  background: #eff6ff;
  box-shadow: 0 0 0 5px rgba(37, 99, 235, .14);
}

.ag-prop-label {
  padding: 0 6px;
  white-space: nowrap;
}
</style>
