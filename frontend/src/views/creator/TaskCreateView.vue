<template>
  <section class="panel task-create-page">
    <div class="task-create-header">
      <el-button text @click="goBack">← 返回任务目录</el-button>
      <h2>新增任务</h2>
    </div>

    <el-form :model="form" label-position="top" class="task-form">
      <el-form-item label="任务名称" required>
        <el-input v-model="form.taskName" placeholder="请输入任务名称" />
      </el-form-item>
      <el-form-item label="任务描述">
        <el-input v-model="form.description" type="textarea" placeholder="请输入任务描述" />
      </el-form-item>
      <el-form-item label="指南版本" required>
        <el-select v-model="form.configId" placeholder="请选择指南版本" style="width: 100%">
          <el-option v-for="cfg in configs" :key="cfg.id" :label="cfg.versionName" :value="cfg.id" />
        </el-select>
      </el-form-item>

      <el-form-item label="任务文书" required>
        <div class="task-doc-picker-summary">
          <p v-if="!form.documents.length" class="muted">尚未选取文书，请前往选取或上传。</p>
          <ul v-else class="task-doc-selected-list">
            <li v-for="doc in form.documents" :key="documentKey(doc)">
              <span class="task-doc-source-tag">{{ sourceTypeLabel(doc.sourceType) }}</span>
              <span>{{ doc.title || doc.fileName }}</span>
              <el-button link type="danger" @click="removeDocument(doc)">移除</el-button>
            </li>
          </ul>
          <el-button type="primary" plain @click="goPickDocuments">选取 / 上传文书</el-button>
        </div>
      </el-form-item>

      <el-form-item label="标注者" required>
        <el-select v-model="form.annotatorIds" multiple placeholder="请选择标注者" style="width: 100%">
          <el-option v-for="user in annotators" :key="user.id" :label="user.realName" :value="user.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="裁决者" required>
        <el-select v-model="form.reviewerId" placeholder="请选择裁决者" style="width: 100%">
          <el-option v-for="user in reviewers" :key="user.id" :label="user.realName" :value="user.id" />
        </el-select>
      </el-form-item>
    </el-form>

    <div class="task-create-actions">
      <el-button @click="goBack">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">创建任务</el-button>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import client from '../../api/client'
import {
  clearTaskCreateDraft,
  documentKey,
  loadTaskCreateDraft,
  saveTaskCreateDraft,
  sourceTypeLabel,
  toCreatePayload
} from '../../utils/taskCreateDraft'

const router = useRouter()
const submitting = ref(false)
const users = ref([])
const configs = ref([])
const form = reactive(loadTaskCreateDraft())

const normalUsers = computed(() => users.value.filter((u) => u.role !== 'admin' && !u.canCreateTask))
const annotators = computed(() => normalUsers.value)
const reviewers = computed(() => normalUsers.value.filter((u) => !form.annotatorIds.includes(u.id)))

function persistDraft() {
  saveTaskCreateDraft(form)
}

function goBack() {
  persistDraft()
  router.push('/tasks')
}

function goPickDocuments() {
  persistDraft()
  router.push('/tasks/create/documents')
}

function removeDocument(doc) {
  const key = documentKey(doc)
  form.documents = form.documents.filter((item) => documentKey(item) !== key)
  persistDraft()
}

async function submit() {
  if (!form.taskName?.trim()) {
    ElMessage.warning('请填写任务名称')
    return
  }
  if (!form.configId) {
    ElMessage.warning('请选择指南版本')
    return
  }
  if (!form.documents.length) {
    ElMessage.warning('请至少选取一篇文书')
    return
  }
  if (!form.annotatorIds.length) {
    ElMessage.warning('请选择标注者')
    return
  }
  if (!form.reviewerId) {
    ElMessage.warning('请选择裁决者')
    return
  }
  submitting.value = true
  try {
    await client.post('/tasks', toCreatePayload(form))
    clearTaskCreateDraft()
    ElMessage.success('任务创建成功')
    router.push('/tasks')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  Object.assign(form, loadTaskCreateDraft())
  const [userList, configList] = await Promise.all([
    client.get('/users'),
    client.get('/configs/versions')
  ])
  users.value = userList || []
  configs.value = configList || []
  if (!form.configId && configs.value.length) {
    form.configId = configs.value[0].id
  }
})
</script>

<style scoped>
.task-create-page {
  max-width: 820px;
  margin: 0 auto;
  padding: 24px;
}

.task-create-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.task-create-header h2 {
  margin: 0;
  font-size: 20px;
}

.task-doc-picker-summary {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-doc-selected-list {
  margin: 0;
  padding: 0;
  list-style: none;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.task-doc-selected-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-bottom: 1px solid #f3f4f6;
}

.task-doc-selected-list li:last-child {
  border-bottom: none;
}

.task-doc-source-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  flex-shrink: 0;
}

.task-create-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 24px;
}

.muted {
  color: #6b7280;
  margin: 0;
}
</style>
