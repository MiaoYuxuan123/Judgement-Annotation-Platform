<template>
  <section class="panel">
    <div class="toolbar">
      <h3>任务管理</h3>
      <div>
        <el-select v-model="status" clearable placeholder="阶段筛选" style="width: 140px; margin-right: 8px" @change="load">
          <el-option label="标注中" value="标注中" />
          <el-option label="待裁定" value="待裁定" />
          <el-option label="可导出" value="可导出" />
        </el-select>
        <el-button type="primary" @click="openCreate">创建任务</el-button>
      </div>
    </div>
    <el-table :data="tasks">
      <el-table-column prop="taskName" label="任务名称" min-width="220" />
      <el-table-column prop="status" label="阶段" width="110"><template #default="{ row }"><el-tag>{{ row.status }}</el-tag></template></el-table-column>
      <el-table-column prop="documentCount" label="文书数" width="90" />
      <el-table-column prop="annotatorCount" label="标注员" width="90" />
      <el-table-column prop="reviewerName" label="裁定者" width="120" />
      <el-table-column label="操作" width="130"><template #default="{ row }"><el-button link type="primary" @click="$router.push(`/tasks/${row.taskId}`)">详情</el-button></template></el-table-column>
    </el-table>
    <el-dialog v-model="visible" title="创建任务" width="720px">
      <el-form :model="form" label-position="top">
        <el-form-item label="任务名称"><el-input v-model="form.taskName" /></el-form-item>
        <el-form-item label="任务描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="文书">
          <el-select v-model="form.documentIds" multiple style="width: 100%">
            <el-option v-for="doc in documents" :key="doc.id" :label="doc.title" :value="doc.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标注员">
          <el-select v-model="form.annotatorIds" multiple style="width: 100%">
            <el-option v-for="user in annotators" :key="user.id" :label="user.realName" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="裁定者">
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

const tasks = ref([])
const users = ref([])
const documents = ref([])
const visible = ref(false)
const status = ref('')
const form = reactive({ taskName: '新裁判文书标注任务', description: '演示从创建到导出的完整流程', documentIds: [], annotatorIds: [], reviewerId: 5, configId: 1 })

const annotators = computed(() => users.value.filter((u) => u.role === 'annotator'))
const reviewers = computed(() => users.value.filter((u) => u.role === 'reviewer'))

async function load() {
  const data = await client.get('/tasks', { params: { status: status.value } })
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

async function create() {
  await client.post('/tasks', form)
  visible.value = false
  await load()
}

onMounted(load)
</script>
