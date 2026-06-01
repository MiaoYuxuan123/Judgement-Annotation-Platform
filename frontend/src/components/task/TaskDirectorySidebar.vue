<template>
  <aside class="task-catalog-sidebar">
    <div class="task-catalog-sidebar-title">任务目录</div>
    <el-input
      v-model="keywordModel"
      class="task-catalog-sidebar-search"
      placeholder="搜索任务..."
      clearable
    />
    <div class="task-catalog-sidebar-list">
      <div
        class="task-catalog-sidebar-item task-catalog-sidebar-all"
        :class="{ active: activeTaskId == null }"
        @click="$emit('select', null)"
      >
        <span class="task-catalog-sidebar-item-name">全部任务</span>
      </div>
      <div
        v-for="item in items"
        :key="item.taskId"
        class="task-catalog-sidebar-item"
        :class="{ active: activeTaskId === item.taskId }"
        @click="$emit('select', item.taskId)"
      >
        <span class="task-catalog-sidebar-item-id">ID-{{ item.taskId }}</span>
        <span class="task-catalog-sidebar-item-name">{{ item.taskName }}</span>
      </div>
      <div v-if="!items.length" class="task-empty" style="padding: 24px 8px">暂无任务</div>
    </div>
    <p v-if="items.length" class="task-catalog-sidebar-hint">（鼠标滚轮上下滚动查看更多）</p>
  </aside>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  tasks: { type: Array, default: () => [] },
  keyword: { type: String, default: '' },
  activeTaskId: { type: [Number, String, null], default: null }
})

const emit = defineEmits(['select', 'update:keyword'])

const keywordModel = computed({
  get: () => props.keyword,
  set: (v) => emit('update:keyword', v)
})

const items = computed(() => {
  const seen = new Set()
  return props.tasks.filter((t) => {
    if (seen.has(t.taskId)) return false
    seen.add(t.taskId)
    const text = `${t.taskId} ${t.taskName}`
    return !props.keyword || text.includes(props.keyword)
  })
})
</script>

<style scoped>
.task-catalog-sidebar-all {
  font-weight: 600;
  color: #374151;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 4px;
}
</style>
