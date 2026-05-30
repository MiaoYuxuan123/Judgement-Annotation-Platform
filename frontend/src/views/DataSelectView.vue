<template>
  <div v-if="detail" class="task-catalog-page data-select-page">
    <DataDirectorySidebar
      :documents="allDocs"
      :keyword="sidebarKeyword"
      :active-doc-id="activeDocId"
      @update:keyword="sidebarKeyword = $event"
      @select="selectSidebarDoc"
    />

    <main class="task-catalog-main">
      <div class="task-filter-bar">
        <div class="task-filter-row">
          <el-button text @click="$router.push('/tasks')">← 返回任务目录</el-button>
          <span class="task-filter-label">ID-{{ id }} {{ detail.summary.taskName }}</span>
          <span class="task-filter-spacer" />
          <span class="task-filter-label">筛选：</span>
          <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 150px">
            <el-option label="全部状态" value="" />
            <el-option label="待标注" value="待标注" />
            <el-option label="已标注" value="已标注" />
            <el-option label="待裁定" value="待裁定" />
            <el-option label="已裁定" value="已裁定" />
          </el-select>
          <el-button type="primary" @click="applyFilters">筛选</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
        <div class="task-filter-row">
          <span class="task-filter-label">排序：</span>
          <el-select v-model="sortBy" style="width: 180px">
            <el-option label="数据ID（降序）" value="idDesc" />
            <el-option label="数据ID（升序）" value="idAsc" />
            <el-option label="标题（A→Z）" value="titleAsc" />
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
            <tr
              v-for="row in displayRows"
              :key="row.id"
              class="task-row"
              :class="{ highlight: activeDocId === row.id }"
            >
              <td>D{{ row.id }}</td>
              <td>{{ row.title }}</td>
              <td>
                <span class="task-role-tag" :class="canReview ? 'role-arbitrate' : 'role-annotate'">
                  {{ roleText }}
                </span>
              </td>
              <td>
                <span class="task-status-tag" :class="statusClass(row)">{{ docStatus(row) }}</span>
              </td>
              <td>
                <button
                  v-if="canAnnotate && detail.summary.status === '标注中'"
                  class="task-action-btn orange"
                  @click="$router.push(`/annotate/${id}/${row.id}`)"
                >
                  {{ docStatus(row) === '已标注' ? '继续标注' : '开始标注' }}
                </button>
                <button
                  v-if="canReview && detail.summary.status === '待裁定'"
                  class="task-action-btn orange"
                  @click="$router.push(`/review/${id}?docId=${row.id}`)"
                >
                  开始裁定
                </button>
                <button
                  v-if="showResultAction"
                  class="task-action-btn green"
                  @click="$router.push(`/results/${id}?dataId=${row.id}`)"
                >
                  查看结果/导出
                </button>
              </td>
            </tr>
            <tr v-if="!displayRows.length">
              <td colspan="5" class="task-empty">暂无数据条目</td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import client from '../api/client'
import { useAuthStore } from '../stores/auth'
import DataDirectorySidebar from '../components/task/DataDirectorySidebar.vue'

const route = useRoute()
const auth = useAuthStore()
const id = route.params.id
const detail = ref(null)
const sidebarKeyword = ref('')
const activeDocId = ref(null)
const sortBy = ref('idDesc')
const filters = reactive({ status: '' })

const canAnnotate = computed(() => detail.value?.annotators.some((u) => u.id === auth.user?.id))
const canReview = computed(() => detail.value?.reviewer.id === auth.user?.id)
const showResultAction = computed(() => {
  if (detail.value?.summary.status !== '可导出') return false
  return canAnnotate.value || canReview.value || auth.user?.canCreateTask
})
const roleText = computed(() => (canReview.value ? '裁定' : '标注'))

const allDocs = computed(() => detail.value?.documents || [])

function docStatus(row) {
  if (row.status === '已裁定') return '已裁定'
  if (detail.value?.summary.status === '可导出') return '已裁定'
  if (detail.value?.summary.status === '待裁定') return canReview.value ? '待裁定' : '已标注'
  return row.status || '待标注'
}

function statusClass(row) {
  const status = docStatus(row)
  if (status === '已裁定') return 'status-done'
  return 'status-progress'
}

const displayRows = computed(() => {
  let rows = [...allDocs.value]
  if (activeDocId.value != null) {
    rows = rows.filter((r) => r.id === activeDocId.value)
  }
  if (filters.status) {
    rows = rows.filter((r) => docStatus(r) === filters.status)
  }
  rows.sort((a, b) => {
    if (sortBy.value === 'titleAsc') return (a.title || '').localeCompare(b.title || '')
    if (sortBy.value === 'idAsc') return a.id - b.id
    return b.id - a.id
  })
  return rows
})

function selectSidebarDoc(docId) {
  activeDocId.value = docId
}

function applyFilters() {
  // client-side only
}

function resetFilters() {
  filters.status = ''
  sidebarKeyword.value = ''
  sortBy.value = 'idDesc'
  activeDocId.value = null
}

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
