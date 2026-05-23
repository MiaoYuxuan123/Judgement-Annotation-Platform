<template>
  <div class="task-catalog-page">
    <TaskDirectorySidebar
      :tasks="sidebarTasks"
      :keyword="sidebarKeyword"
      :active-task-id="activeTaskId"
      @update:keyword="sidebarKeyword = $event"
      @select="selectSidebarTask"
    />

    <main class="task-catalog-main">
      <div class="task-filter-bar">
        <div class="task-filter-row">
          <span class="task-filter-label">筛选条件：</span>
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="全部状态" value="" />
            <el-option label="标注中" value="标注中" />
            <el-option label="待裁定" value="待裁定" />
            <el-option label="可导出" value="可导出" />
          </el-select>
          <el-select v-model="filters.participation" placeholder="我参与的" clearable style="width: 130px">
            <el-option label="我参与的" value="" />
            <el-option label="标注任务" value="annotate" />
            <el-option label="裁定任务" value="arbitrate" />
          </el-select>
          <el-button type="primary" @click="applyFilters">筛选</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
        <div class="task-filter-row">
          <span class="task-filter-label">排序：</span>
          <el-select v-model="sortBy" style="width: 180px">
            <el-option label="创建时间（降序）" value="createdDesc" />
            <el-option label="创建时间（升序）" value="createdAsc" />
          </el-select>
        </div>
      </div>

      <div class="task-table-wrap">
        <table class="task-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>任务名称</th>
              <th>角色</th>
              <th>任务状态</th>
              <th>任务信息</th>
              <th>操作</th>
              <th>详情</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="row in displayRows" :key="row.key">
              <tr
                class="task-row"
                :class="{ highlight: activeTaskId === row.taskId }"
                :data-task-id="row.taskId"
                :data-row-key="row.key"
              >
                <td>ID-{{ row.taskId }}</td>
                <td>{{ row.taskName }}</td>
                <td>
                  <span class="task-role-tag" :class="`role-${row.role}`">{{ row.roleLabel }}</span>
                </td>
                <td>
                  <span class="task-status-tag" :class="`status-${row.status.type}`">{{ row.status.label }}</span>
                </td>
                <td>
                  <span class="task-info-text" :class="row.infoType">{{ row.info }}</span>
                </td>
                <td>
                  <button
                    v-if="actionFor(row)"
                    class="task-action-btn"
                    :class="actionFor(row).color"
                    @click="goAction(actionFor(row))"
                  >
                    {{ actionFor(row).label }}
                  </button>
                  <span v-else class="task-info-text muted">—</span>
                </td>
                <td>
                  <button
                    class="task-detail-toggle"
                    :class="{ open: expandedKey === row.key }"
                    @click="toggleDetail(row.key, row.taskId)"
                  >
                    详情 {{ expandedKey === row.key ? '▲' : '▼' }}
                  </button>
                </td>
              </tr>
              <tr v-if="expandedKey === row.key" class="task-detail-row">
                <td colspan="7">
                  <TaskInlineDetail
                    :detail="details[row.taskId]"
                    :show-reviewer="false"
                    mode="participant"
                  />
                </td>
              </tr>
            </template>
            <tr v-if="!displayRows.length">
              <td colspan="7" class="task-empty">暂无参与的任务</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="task-note-box">
        <strong>【标注者/裁决者合并视图】</strong>
        同一任务 ID 可因同时拥有标注与裁定身份而出现两行；操作按钮随角色与阶段变化；详情在列表内展开，无需跳转。
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import client from '../../api/client'
import { useAuthStore } from '../../stores/auth'
import TaskDirectorySidebar from '../../components/task/TaskDirectorySidebar.vue'
import TaskInlineDetail from '../../components/task/TaskInlineDetail.vue'
import { buildParticipantRows, participantAction } from '../../utils/taskRows'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const tasks = ref([])
const details = ref({})
const expandedKey = ref(null)
const activeTaskId = ref(null)
const sidebarKeyword = ref('')
const sortBy = ref('createdDesc')
const filters = reactive({ status: '', participation: '' })

const sidebarTasks = computed(() => {
  const seen = new Set()
  return tasks.value.filter((t) => {
    if (seen.has(t.taskId)) return false
    seen.add(t.taskId)
    return true
  })
})

const tableRows = computed(() =>
  buildParticipantRows(tasks.value, details.value, auth.user?.id)
)

const displayRows = computed(() => {
  let rows = tableRows.value
  if (filters.status) {
    rows = rows.filter((r) => {
      const map = { 标注中: '标注中', 待裁定: '待裁定', 可导出: '可导出' }
      return r.detail?.summary?.status === map[filters.status] || filters.status === ''
    })
  }
  if (filters.participation === 'annotate') rows = rows.filter((r) => r.role === 'annotate')
  if (filters.participation === 'arbitrate') rows = rows.filter((r) => r.role === 'arbitrate')
  rows = [...rows].sort((a, b) => {
    const ta = new Date(a.detail?.summary?.createdAt || 0).getTime()
    const tb = new Date(b.detail?.summary?.createdAt || 0).getTime()
    return sortBy.value === 'createdAsc' ? ta - tb : tb - ta
  })
  return rows
})

function actionFor(row) {
  return participantAction(row)
}

function goAction(action) {
  if (action?.route) router.push(action.route)
}

function toggleDetail(key, taskId) {
  expandedKey.value = expandedKey.value === key ? null : key
  if (expandedKey.value && !details.value[taskId]) loadDetail(taskId)
}

function selectSidebarTask(taskId) {
  activeTaskId.value = taskId
  nextTick(() => {
    document.querySelector(`[data-task-id="${taskId}"]`)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  })
}

async function loadDetail(taskId, taskStatus) {
  const detail = await client.get(`/tasks/${taskId}`)
  const total = detail.documents?.length || detail.summary.documentCount || 0
  if (taskStatus === '可导出' || taskStatus === '待裁定') {
    detail._annotateDone = total
  } else {
    detail._annotateDone = Math.max(0, total - 1)
  }
  detail._disputeCount = taskStatus === '待裁定' ? total : 0
  details.value[taskId] = detail
}

async function load() {
  const data = await client.get('/tasks/my', { params: { status: filters.status || undefined } })
  tasks.value = data.list || []
  details.value = {}
  await Promise.all(tasks.value.map((t) => loadDetail(t.taskId, t.status)))
}

function applyFilters() {
  load()
}

function resetFilters() {
  filters.status = ''
  filters.participation = ''
  sidebarKeyword.value = ''
  sortBy.value = 'createdDesc'
  load()
}

onMounted(async () => {
  await load()
  const taskId = Number(route.query.taskId)
  if (taskId) {
    activeTaskId.value = taskId
    nextTick(() => selectSidebarTask(taskId))
  }
})

watch(
  () => route.query.taskId,
  (id) => {
    if (!id) return
    activeTaskId.value = Number(id)
    nextTick(() => selectSidebarTask(Number(id)))
  }
)
</script>
