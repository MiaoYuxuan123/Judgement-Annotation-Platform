<template>
  <div v-if="detail" class="task-catalog-page data-select-page">
    <aside class="task-catalog-sidebar">
      <div class="task-catalog-sidebar-title">数据条目目录</div>
      <el-input
        v-model="keyword"
        class="task-catalog-sidebar-search"
        placeholder="搜索数据ID / 来源"
        clearable
      />
      <div class="task-catalog-sidebar-list">
        <div
          v-for="doc in filteredDocs"
          :key="doc.id"
          class="task-catalog-sidebar-item"
          :class="{ active: activeDocId === doc.id }"
          @click="activeDocId = doc.id"
        >
          <span class="task-catalog-sidebar-item-id">D{{ doc.id }}</span>
          <span class="task-catalog-sidebar-item-name">{{ doc.title }}</span>
        </div>
      </div>
    </aside>

    <main class="task-catalog-main">
      <div class="task-filter-bar">
        <div class="task-filter-row">
          <el-button text @click="$router.push('/tasks')">← 返回任务目录</el-button>
          <span class="task-filter-label">ID-{{ id }} {{ detail.summary.taskName }}</span>
          <span class="task-filter-spacer" />
          <el-select v-model="status" clearable placeholder="全部状态" style="width: 150px">
            <el-option label="待标注" value="待标注" />
            <el-option label="已标注" value="已标注" />
            <el-option label="待裁定" value="待裁定" />
            <el-option label="已裁定" value="已裁定" />
          </el-select>
        </div>
      </div>

      <div class="task-table-wrap">
        <table class="task-table">
          <thead>
            <tr>
              <th>数据ID</th>
              <th>来源（文件名/文本）</th>
              <th>当前身份</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in filteredDocs" :key="row.id" class="task-row">
              <td>D{{ row.id }}</td>
              <td>{{ row.title }}</td>
              <td><span class="task-role-tag" :class="canReview ? 'role-arbitrate' : 'role-annotate'">{{ roleText }}</span></td>
              <td><span class="task-status-tag" :class="statusTagClass">{{ rowStatus }}</span></td>
              <td>
                <button
                  v-if="canAnnotate && detail.summary.status === '标注中'"
                  class="task-action-btn orange"
                  @click="$router.push(`/annotate/${id}/${row.id}`)"
                >
                  {{ rowStatus === '已标注' ? '继续标注' : '开始标注' }}
                </button>
                <button
                  v-if="canReview && detail.summary.status === '待裁定'"
                  class="task-action-btn orange"
                  @click="$router.push(`/review/${id}?dataId=${row.id}`)"
                >
                  开始裁定
                </button>
                <button
                  v-if="showResultAction"
                  class="task-action-btn green"
                  @click="$router.push(`/results/${id}?dataId=${row.id}`)"
                >
                  查看结果
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import client from '../api/client'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const auth = useAuthStore()
const id = route.params.id
const detail = ref(null)
const keyword = ref('')
const status = ref('')
const activeDocId = ref(null)

const canAnnotate = computed(() => detail.value?.annotators.some((u) => u.id === auth.user?.id))
const canReview = computed(() => detail.value?.reviewer.id === auth.user?.id)
const showResultAction = computed(() => detail.value?.summary.status === '可导出')
const roleText = computed(() => (canReview.value ? '裁定' : '标注'))
const rowStatus = computed(() => {
  if (detail.value?.summary.status === '可导出') return '已裁定'
  if (canReview.value) return '待裁定'
  return '待标注'
})
const statusTagClass = computed(() => {
  if (rowStatus.value === '已裁定') return 'status-done'
  if (rowStatus.value === '待裁定') return 'status-progress'
  return 'status-progress'
})

const filteredDocs = computed(() =>
  (detail.value?.documents || []).filter((doc) => {
    const matchKeyword = !keyword.value || `D${doc.id} ${doc.title}`.includes(keyword.value)
    const matchStatus = !status.value || status.value === rowStatus.value
    return matchKeyword && matchStatus
  })
)

async function load() {
  detail.value = await client.get(`/tasks/${id}`)
}

onMounted(load)
</script>

<style scoped>
.data-select-page {
  min-height: calc(100vh - 52px);
}
</style>
