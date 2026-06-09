<template>
  <div class="ag-hub-node" :class="[hubClass, { 'ag-hub-node--editable': data.editable, 'ag-hub--highlighted': selected }]">
    <span v-if="showPlus" class="ag-hub-plus">+</span>
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
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'

const props = defineProps({
  data: { type: Object, default: () => ({}) },
  selected: { type: Boolean, default: false }
})

const hubClass = computed(() => {
  const k = props.data.hubKind || 'hub-m'
  return `ag-hub--${k.replace('hub-', '')}`
})

const showPlus = computed(() => {
  const k = props.data.hubKind || ''
  return ['hub-j', 'hub-m'].includes(k)
})

const handles = [
  { id: 'top', type: 'target', position: Position.Top, style: { left: '50%' } },
  { id: 'right', type: 'source', position: Position.Right, style: { top: '50%' } },
  { id: 'bottom', type: 'source', position: Position.Bottom, style: { left: '50%' } },
  { id: 'left', type: 'target', position: Position.Left, style: { top: '50%' } }
]

function onHandleClick(handleId) {
  if (!props.data.showHandles || !props.data.onHandlePick) return
  props.data.onHandlePick(handleId)
}
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

.ag-hub-node--editable {
  pointer-events: all;
  cursor: grab;
}

.ag-hub-node--editable:active {
  cursor: grabbing;
}

.ag-hub--s {
  background: #1a1817;
  border: none;
}

.ag-hub--a,
.ag-hub--m,
.ag-hub--j {
  background: #fff;
  border: 1.8px solid #1a1817;
}

.ag-hub-plus {
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
  color: #1a1817;
  user-select: none;
  pointer-events: none;
}

.ag-hub--highlighted {
  border-color: #c41e3a;
  box-shadow: 0 0 0 5px rgba(37, 99, 235, .16);
}

.ag-hub--s.ag-hub--highlighted {
  background: #c41e3a;
}

.ag-hub--highlighted .ag-hub-plus {
  color: #c41e3a;
}
</style>
