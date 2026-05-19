<template>
  <section class="panel">
    <div class="toolbar">
      <div>
        <h3>{{ auth.user?.canCreateTask ? '任务列表 -- 创建者视图' : '任务列表 -- 参与者视图' }}</h3>
        <p class="muted">{{ auth.user?.canCreateTask ? '创建任务并指定标注者、裁决者，任务创建者不参与标注/裁决身份。' : '这里只显示当前账号被任务创建者分配到的任务。' }}</p>
      </div>
      <div>
        <el-select v-model="status" clearable placeholder="阶段筛选" style="width: 140px; margin-right: 8px" @change="load">
          <el-option label="标注中" value="标注中" />
          <el-option label="待裁定" value="待裁定" />
          <el-option label="可导出" value="可导出" />
        </el-select>
        <el-button v-if="auth.user?.canCreateTask" type="primary" @click="openCreate">+ 新增任务</el-button>
      </div>
    </div>
    <div class="task-directory">
      <aside class="directory">
        <strong>任务目录</strong>
        <el-input v-model="keyword" placeholder="搜索 ID / 名称" clearable style="margin: 12px 0" />
        <div v-for="row in filteredTasks" :key="row.taskId" class="dir-item" @click="scrollTo(row.taskId)">
          <span>ID-{{ row.taskId }}</span>
          <small>{{ row.taskName }}</small>
        </div>
      </aside>
      <el-table :data="filteredTasks" row-key="taskId" style="flex: 1">
        <el-table-column prop="taskId" label="ID" width="100" />
        <el-table-column prop="taskName" label="任务名称" min-width="220" />
        <el-table-column prop="status" label="任务状态" width="110"><template #default="{ row }"><el-tag>{{ row.status }}</el-tag></template></el-table-column>
        <el-table-column prop="documentCount" label="数据" width="80" />
        <el-table-column v-if="auth.user?.canCreateTask" prop="annotatorCount" label="标注员" width="90" />
        <el-table-column v-if="auth.user?.canCreateTask" prop="reviewerName" label="裁决者" width="120" />
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/tasks/${row.taskId}`)">详情</el-button>
            <el-button v-if="!auth.user?.canCreateTask" link type="warning" @click="$router.push(`/tasks/${row.taskId}/data`)">数据列表</el-button>
            <el-button v-if="row.status === '可导出'" link type="success" @click="$router.push(`/results/${row.taskId}`)">查看结果/导出</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog v-model="visible" title="创建任务" width="720px">
      <el-form :model="form" label-position="top">
        <el-form-item label="任务名称"><el-input v-model="form.taskName" /></el-form-item>
        <el-form-item label="任务描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="文书">
          <el-select v-model="form.documentIds" multiple style="width: 100%">
            <el-option v-for="doc in documents" :key="doc.id" :label="doc.title" :value="doc.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标注者（从非任务创建者中选择）">
          <el-select v-model="form.annotatorIds" multiple style="width: 100%">
            <el-option v-for="user in annotators" :key="user.id" :label="user.realName" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="裁决者（从非任务创建者中选择）">
          <el-select v-model="form.reviewerId" style="width: 100%">
            <el-option v-for="user in reviewers" :key="user.id" :label="user.realName" :value="user.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="create">创建</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import client from '../api/client'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const tasks = ref([])
const users = ref([])
const documents = ref([])
const visible = ref(false)
const status = ref('')
const keyword = ref('')
const form = reactive({ taskName: '新裁判文书标注任务', description: '演示从创建到导出的完整流程', documentIds: [], annotatorIds: [], reviewerId: 5, configId: 1 })

const normalUsers = computed(() => users.value.filter((u) => u.role !== 'admin' && !u.canCreateTask))
const annotators = computed(() => normalUsers.value)
const reviewers = computed(() => normalUsers.value.filter((u) => !form.annotatorIds.includes(u.id)))
const filteredTasks = computed(() => tasks.value.filter((task) => !keyword.value || `${task.taskId} ${task.taskName}`.includes(keyword.value)))

async function load() {
  const endpoint = auth.user?.canCreateTask ? '/tasks' : '/tasks/my'
  const data = await client.get(endpoint, { params: { status: status.value } })
  tasks.value = data.list || []
}

async function openCreate() {
  const [docData, userData] = await Promise.all([client.get('/documents'), client.get('/users')])
  documents.value = docData.list || []
  users.value = userData
  form.documentIds = documents.value.slice(0, 1).map((d) => d.id)
  form.annotatorIds = annotators.value.slice(0, 2).map((u) => u.id)
  form.reviewerId = reviewers.value[0]?.id || 5
  visible.value = true
}

function scrollTo(taskId) {
  const row = document.querySelector(`[data-row-key="${taskId}"]`)
  row?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

async function create() {
  await client.post('/tasks', form)
  visible.value = false
  await load()
}

onMounted(load)
</script>
