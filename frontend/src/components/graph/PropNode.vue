<template>
  <div class="ag-prop-node">
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

.ag-prop-label {
  padding: 0 6px;
  white-space: nowrap;
}
</style>
