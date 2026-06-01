<template>
  <aside class="task-catalog-sidebar">
    <div class="task-catalog-sidebar-title">数据条目目录</div>
    <el-input
      v-model="keywordModel"
      class="task-catalog-sidebar-search"
      placeholder="搜索数据ID / 来源"
      clearable
    />
    <div class="task-catalog-sidebar-list">
      <div
        class="task-catalog-sidebar-item task-catalog-sidebar-all"
        :class="{ active: activeDocId == null }"
        @click="$emit('select', null)"
      >
        <span class="task-catalog-sidebar-item-name">全部文书</span>
      </div>
      <div
        v-for="item in items"
        :key="item.id"
        class="task-catalog-sidebar-item"
        :class="{ active: activeDocId === item.id }"
        @click="$emit('select', item.id)"
      >
        <span class="task-catalog-sidebar-item-id">D{{ item.id }}</span>
        <span class="task-catalog-sidebar-item-name">{{ item.title }}</span>
      </div>
      <div v-if="!items.length" class="task-empty" style="padding: 24px 8px">暂无文书</div>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  documents: { type: Array, default: () => [] },
  keyword: { type: String, default: '' },
  activeDocId: { type: [Number, String, null], default: null }
})

const emit = defineEmits(['select', 'update:keyword'])

const keywordModel = computed({
  get: () => props.keyword,
  set: (v) => emit('update:keyword', v)
})

const items = computed(() =>
  props.documents.filter((doc) => {
    const text = `D${doc.id} ${doc.title}`
    return !props.keyword || text.includes(props.keyword)
  })
)
</script>

<style scoped>
.task-catalog-sidebar-all {
  font-weight: 600;
  color: #374151;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 4px;
}
</style>
