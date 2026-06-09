<template>
  <div class="ag-prop-node" :class="{ 'ag-prop-node--highlighted': selected, 'ag-prop-node--editable': data.editable }">
    <span class="ag-prop-label">{{ data.label }}</span>
    <Handle
      v-for="handle in handles"
      :key="handle.id"
      :id="handle.id"
      :type="handle.type"
      :position="handle.position"
      :style="handle.style"
      :class="{
        'ag-handle--visible': data.showHandles,
        'ag-handle--snap': data.snapHandleId === handle.id
      }"
      @click.stop="onHandleClick(handle.id)"
    />
  </div>
</template>

<script setup>
import { Handle, Position } from '@vue-flow/core'

const props = defineProps({
  data: { type: Object, default: () => ({}) },
  selected: { type: Boolean, default: false }
})

function onHandleClick(handleId) {
  if (!props.data.showHandles || !props.data.onHandlePick) return
  props.data.onHandlePick(handleId)
}

const handles = [
  { id: 'top', type: 'target', position: Position.Top, style: { left: '50%' } },
  { id: 'top-left', type: 'target', position: Position.Top, style: { left: '0%' } },
  { id: 'top-right', type: 'source', position: Position.Top, style: { left: '100%' } },
  { id: 'right', type: 'source', position: Position.Right, style: { top: '50%' } },
  { id: 'bottom-right', type: 'source', position: Position.Bottom, style: { left: '100%' } },
  { id: 'bottom', type: 'source', position: Position.Bottom, style: { left: '50%' } },
  { id: 'bottom-left', type: 'target', position: Position.Bottom, style: { left: '0%' } },
  { id: 'left', type: 'target', position: Position.Left, style: { top: '50%' } }
]
</script>

<style scoped>
.ag-prop-node {
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  display: grid;
  place-items: center;
  border: 1.2px solid #1a1817;
  background: #fff;
  font-size: 13px;
  font-weight: 600;
  color: #1a1817;
  font-family: Inter, 'Microsoft YaHei', sans-serif;
  pointer-events: none;
}

.ag-prop-node--editable {
  pointer-events: all;
  cursor: grab;
}

:deep(.ag-handle--visible) {
  pointer-events: all !important;
  cursor: crosshair;
}

.ag-prop-node--editable:active {
  cursor: grabbing;
}

.ag-prop-node--highlighted {
  border-color: #c41e3a;
  border-width: 2.4px;
  color: #991b1b;
  background: #fef2f2;
  box-shadow: 0 0 0 5px rgba(37, 99, 235, .14);
}

.ag-prop-label {
  padding: 0 6px;
  white-space: nowrap;
  pointer-events: none;
}
</style>

<style>
.ag-handle--visible {
  width: 7px !important;
  height: 7px !important;
  min-width: 7px !important;
  min-height: 7px !important;
  opacity: 1 !important;
  border: 1.5px solid #c41e3a !important;
  background: #fff !important;
  border-radius: 50%;
}

.ag-handle--snap {
  width: 9px !important;
  height: 9px !important;
  min-width: 9px !important;
  min-height: 9px !important;
  border: 2px solid #4a7c59 !important;
  background: #edf7f0 !important;
  box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.2);
}
</style>
