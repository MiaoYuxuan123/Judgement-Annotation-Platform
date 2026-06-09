<template>
  <div class="task-catalog-page">
    <TaskDirectorySidebar
      :tasks="tasks"
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
          <el-select v-model="filters.assign" placeholder="全部分配" clearable style="width: 130px">
            <el-option label="全部分配" value="" />
            <el-option label="我创建的" value="mine" />
          </el-select>
          <el-button type="primary" @click="applyFilters">筛选</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <span class="task-filter-spacer" />
          <el-button type="success" @click="openCreate">+ 新增任务</el-button>
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
              <th>标注员</th>
              <th>操作</th>
              <th>详情</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="row in displayRows" :key="row.taskId">
              <tr
                class="task-row"
                :class="{ highlight: activeTaskId === row.taskId }"
                :data-task-id="row.taskId"
              >
                <td>ID-{{ row.taskId }}</td>
                <td>{{ row.taskName }}</td>
                <td><span class="task-role-tag role-creator">创建者</span></td>
                <td>
                  <span class="task-status-tag" :class="statusClass(row.status)">{{ displayStatus(row.status) }}</span>
                </td>
                <td>{{ row.annotatorText }}</td>
                <td class="task-action-cell">
                  <button
                    v-for="(action, actionIdx) in actionsFor(row)"
                    :key="actionIdx"
                    class="task-action-btn"
                    :class="action.color"
                    @click="goAction(action)"
                  >
                    {{ action.label }}
                  </button>
                </td>
                <td>
                  <button
                    class="task-detail-toggle"
                    :class="{ open: expandedKey === row.taskId }"
                    @click="toggleDetail(row.taskId)"
                  >
                    详情 {{ expandedKey === row.taskId ? '▲' : '▼' }}
                  </button>
                </td>
              </tr>
              <tr v-if="expandedKey === row.taskId" class="task-detail-row">
                <td colspan="7">
                  <div class="task-detail-panel">
                    <div class="task-detail-panel-title">▼ 任务详情</div>
                    <TaskForm
                      v-if="editForms[row.taskId]"
                      :form="editForms[row.taskId]"
                      :documents="details[row.taskId]?.documents || []"
                      :users="users"
                      :configs="configs"
                      mode="edit"
                      :context-task-id="row.taskId"
                      @add-documents="goAddDocuments(row.taskId)"
                      @add-members="goAddMembers(row.taskId)"
                    />
                    <div class="task-detail-actions">
                      <el-button type="primary" :loading="savingTaskId === row.taskId" @click="saveDetail(row.taskId)">
                        修改配置
                      </el-button>
                      <el-button @click="cancelDetail(row.taskId)">取消</el-button>
                    </div>
                  </div>
                </td>
              </tr>
            </template>
            <tr v-if="!displayRows.length">
              <td colspan="7" class="task-empty">暂无任务</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="task-note-box creator">
        <strong>【创建者视图说明】</strong>
        左侧为任务目录，默认展示全部任务；点击某一任务后右侧仅显示该任务并展开详情。新增任务时可从文书总库选取或自主上传文书。
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../../api/client'
import TaskDirectorySidebar from '../../components/task/TaskDirectorySidebar.vue'
import TaskForm from '../../components/task/TaskForm.vue'
import { taskFormFromDetail } from '../../utils/taskForm'
import { creatorActions } from '../../utils/taskRows'
import {
  clearTaskUpdateDraft,
  loadTaskUpdateDraft,
  toUpdatePayload
} from '../../utils/taskUpdateDraft'
import { syncTasksRoute, tasksReturnRoute } from '../../utils/navigationReturn'

const router = useRouter()
const route = useRoute()
const tasks = ref([])
const details = ref({})
const users = ref([])
const configs = ref([])
const editForms = reactive({})
const expandedKey = ref(null)
const activeTaskId = ref(null)
const sidebarKeyword = ref('')
const sortBy = ref('createdDesc')
const filters = reactive({ status: '', assign: '' })
const savingTaskId = ref(null)
const deletingTaskId = ref(null)

const displayRows = computed(() => {
  let rows = tasks.value.map((t) => ({
    ...t,
    status: details.value[t.taskId]?.summary?.status || t.status,
    detail: details.value[t.taskId],
    annotatorText: details.value[t.taskId]?.annotators?.map((u) => u.realName).join('、') || `${t.annotatorCount} 人`
  }))
  if (filters.status) rows = rows.filter((r) => r.status === filters.status)
  if (activeTaskId.value != null) rows = rows.filter((r) => r.taskId === activeTaskId.value)
  rows.sort((a, b) => {
    const ta = new Date(details.value[a.taskId]?.summary?.createdAt || 0).getTime()
    const tb = new Date(details.value[b.taskId]?.summary?.createdAt || 0).getTime()
    return sortBy.value === 'createdAsc' ? ta - tb : tb - ta
  })
  return rows
})

function displayStatus(status) {
  return status || '标注中'
}

function statusClass(status) {
  if (status === '可导出') return 'status-done'
  return 'status-progress'
}

function actionsFor(row) {
  return creatorActions(row)
}

function goAction(action) {
  if (action?.action === 'delete') {
    removeTask(action.taskId, action.taskName)
    return
  }
  if (action?.route) router.push(action.route)
}

async function ensureFormResources() {
  const jobs = []
  if (!users.value.length) jobs.push(client.get('/users').then((d) => { users.value = d }))
  if (!configs.value.length) jobs.push(client.get('/configs/versions').then((d) => { configs.value = d || [] }))
  await Promise.all(jobs)
}

async function buildEditForm(taskId) {
  const draft = loadTaskUpdateDraft(taskId)
  Object.assign(editForms, {
    [taskId]: taskFormFromDetail(
      details.value[taskId],
      draft.addDocuments || [],
      draft.addAnnotatorIds || []
    )
  })
}

async function restoreFromRoute() {
  const taskId = Number(route.query.taskId)
  if (!taskId) {
    activeTaskId.value = null
    expandedKey.value = null
    return
  }
  activeTaskId.value = taskId
  if (route.query.expand === '1') {
    expandedKey.value = taskId
  }
  if (!details.value[taskId]) await loadDetail(taskId)
  await ensureFormResources()
  await buildEditForm(taskId)
}

async function toggleDetail(taskId) {
  if (expandedKey.value === taskId) {
    expandedKey.value = null
    syncTasksRoute(router, activeTaskId.value ?? taskId, null, false)
    return
  }
  expandedKey.value = taskId
  activeTaskId.value = taskId
  syncTasksRoute(router, taskId)
  if (!details.value[taskId]) await loadDetail(taskId)
  await ensureFormResources()
  await buildEditForm(taskId)
}

async function selectSidebarTask(taskId) {
  activeTaskId.value = taskId
  syncTasksRoute(router, taskId)
  if (taskId == null) {
    expandedKey.value = null
    return
  }
  expandedKey.value = taskId
  if (!details.value[taskId]) await loadDetail(taskId)
  await ensureFormResources()
  await buildEditForm(taskId)
}

async function loadDetail(taskId) {
  details.value[taskId] = await client.get(`/tasks/${taskId}`)
}

async function load() {
  const data = await client.get('/tasks', { params: { status: filters.status || undefined } })
  tasks.value = data.list || []
  await Promise.all(tasks.value.map((t) => loadDetail(t.taskId)))
}

function applyFilters() {
  load()
}

function resetFilters() {
  filters.status = ''
  filters.assign = ''
  sidebarKeyword.value = ''
  sortBy.value = 'createdDesc'
  activeTaskId.value = null
  expandedKey.value = null
  load()
}

function openCreate() {
  router.push('/tasks/create')
}

function goAddDocuments(taskId) {
  syncTasksRoute(router, taskId)
  router.push(`/tasks/${taskId}/documents/add`)
}

function goAddMembers(taskId) {
  syncTasksRoute(router, taskId)
  router.push(`/tasks/${taskId}/members/add`)
}

async function saveDetail(taskId) {
  const form = editForms[taskId]
  if (!form) return
  savingTaskId.value = taskId
  try {
    const payload = toUpdatePayload(form)
    if (!payload.addAnnotatorIds.length && !payload.documents.length) {
      ElMessage.info('没有新增标注员或文书')
      return
    }
    details.value[taskId] = await client.put(`/tasks/${taskId}/config`, payload)
    clearTaskUpdateDraft(taskId)
    await load()
    await buildEditForm(taskId)
    expandedKey.value = taskId
    activeTaskId.value = taskId
    syncTasksRoute(router, taskId)
    ElMessage.success('配置已保存')
  } finally {
    savingTaskId.value = null
  }
}

function cancelDetail(taskId) {
  clearTaskUpdateDraft(taskId)
  Object.assign(editForms, { [taskId]: taskFormFromDetail(details.value[taskId], []) })
}

async function removeTask(taskId, taskName) {
  await ElMessageBox.confirm(
    `确认删除任务「${taskName}」？删除后将一并移除该任务的文书、成员、标注与裁定记录，且不可恢复。`,
    '删除任务',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
  )
  deletingTaskId.value = taskId
  try {
    await client.delete(`/tasks/${taskId}`)
    ElMessage.success('任务已删除')
    delete details.value[taskId]
    delete editForms[taskId]
    clearTaskUpdateDraft(taskId)
    if (activeTaskId.value === taskId) {
      activeTaskId.value = null
      expandedKey.value = null
      syncTasksRoute(router, null, null, false)
    }
    await load()
  } finally {
    deletingTaskId.value = null
  }
}

onMounted(async () => {
  await load()
  await restoreFromRoute()
})

watch(
  () => [route.query.taskId, route.query.expand],
  async () => {
    await restoreFromRoute()
  }
)
</script>
