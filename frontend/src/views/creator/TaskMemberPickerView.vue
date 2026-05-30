<template>
  <section class="panel task-member-picker-page">
    <div class="task-create-header">
      <el-button text @click="goBack">← {{ backLabel }}</el-button>
      <h2>选取参与者</h2>
    </div>

    <p class="hint">标注员与裁定者可重复选取同一账号；创建者也可同时担任标注员或裁定者。</p>

    <div v-if="lockedAnnotators.length || lockedReviewer" class="existing-panel">
      <h3>已在任务中</h3>
      <ul class="member-list">
        <li v-for="user in lockedAnnotators" :key="`a-${user.id}`">
          <span class="role-tag annotate">标注员</span>
          {{ user.realName }}（{{ user.username }}）
          <span class="locked-label">不可取消</span>
        </li>
        <li v-if="lockedReviewer">
          <span class="role-tag arbitrate">裁定者</span>
          {{ lockedReviewer.realName }}（{{ lockedReviewer.username }}）
          <span class="locked-label">不可取消</span>
        </li>
      </ul>
    </div>

    <div class="picker-panel">
      <el-input v-model="keyword" placeholder="搜索姓名 / 账号" clearable style="margin-bottom: 12px" />
      <el-table :data="filteredUsers" size="small">
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column label="标注员" width="100" align="center">
          <template #default="{ row }">
            <el-checkbox
              :model-value="isAnnotator(row.id)"
              :disabled="isLockedAnnotator(row.id)"
              @change="(v) => toggleAnnotator(row.id, v)"
            />
          </template>
        </el-table-column>
        <el-table-column label="裁定者" width="100" align="center">
          <template #default="{ row }">
            <el-checkbox
              :model-value="reviewerId === row.id"
              :disabled="isEdit && !!lockedReviewer"
              @change="(v) => toggleReviewer(row.id, v)"
            />
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="selected-panel">
      <h3>本次选取预览</h3>
      <p class="muted">标注员 {{ displayAnnotatorIds.length }} 人 · 裁定者 {{ reviewerId ? 1 : 0 }} 人</p>
      <div class="selected-actions">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" @click="confirm">确认选取</el-button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import client from '../../api/client'
import { loadTaskCreateDraft, saveTaskCreateDraft } from '../../utils/taskCreateDraft'
import { loadTaskUpdateDraft, saveTaskUpdateDraft } from '../../utils/taskUpdateDraft'
import { tasksReturnRoute } from '../../utils/navigationReturn'

const router = useRouter()
const route = useRoute()
const editTaskId = computed(() => {
  const id = route.params.taskId
  return id ? Number(id) : null
})
const isEdit = computed(() => !!editTaskId.value)

const users = ref([])
const keyword = ref('')
const annotatorIds = ref([])
const reviewerId = ref(null)
const lockedAnnotatorIds = ref([])
const lockedReviewerId = ref(null)

const backLabel = computed(() => (isEdit.value ? '返回任务详情' : '返回创建任务'))

const selectableUsers = computed(() => users.value.filter((u) => u.role !== 'admin'))

const filteredUsers = computed(() =>
  selectableUsers.value.filter((u) => {
    if (!keyword.value) return true
    return `${u.realName} ${u.username}`.includes(keyword.value)
  })
)

const lockedAnnotators = computed(() =>
  selectableUsers.value.filter((u) => lockedAnnotatorIds.value.includes(u.id))
)

const lockedReviewer = computed(() =>
  selectableUsers.value.find((u) => u.id === lockedReviewerId.value) || null
)

const displayAnnotatorIds = computed(() => {
  if (isEdit.value) {
    return annotatorIds.value.filter((id) => !lockedAnnotatorIds.value.includes(id))
  }
  return annotatorIds.value
})

function isAnnotator(id) {
  return annotatorIds.value.includes(id)
}

function isLockedAnnotator(id) {
  return isEdit.value && lockedAnnotatorIds.value.includes(id)
}

function toggleAnnotator(id, checked) {
  if (isLockedAnnotator(id)) return
  if (checked) {
    if (!annotatorIds.value.includes(id)) annotatorIds.value.push(id)
  } else {
    annotatorIds.value = annotatorIds.value.filter((v) => v !== id)
  }
}

function toggleReviewer(id, checked) {
  if (isEdit.value && lockedReviewerId.value) return
  reviewerId.value = checked ? id : (reviewerId.value === id ? null : reviewerId.value)
}

function goBack() {
  if (isEdit.value) {
    router.push(tasksReturnRoute(editTaskId.value))
    return
  }
  router.push('/tasks/create')
}

function confirm() {
  if (isEdit.value) {
    const addAnnotatorIds = annotatorIds.value.filter((id) => !lockedAnnotatorIds.value.includes(id))
    const draft = loadTaskUpdateDraft(editTaskId.value)
    draft.addAnnotatorIds = addAnnotatorIds
    saveTaskUpdateDraft(editTaskId.value, draft)
    ElMessage.success('参与者选取已保存')
    router.push(tasksReturnRoute(editTaskId.value))
    return
  }

  if (!annotatorIds.value.length) {
    ElMessage.warning('请至少选择一名标注员')
    return
  }
  if (!reviewerId.value) {
    ElMessage.warning('请选择裁定者')
    return
  }
  const draft = loadTaskCreateDraft()
  draft.annotatorIds = [...annotatorIds.value]
  draft.reviewerId = reviewerId.value
  saveTaskCreateDraft(draft)
  ElMessage.success('参与者选取已保存')
  router.push('/tasks/create')
}

async function loadExistingForEdit() {
  if (!editTaskId.value) return
  const detail = await client.get(`/tasks/${editTaskId.value}`)
  lockedAnnotatorIds.value = (detail.annotators || []).map((u) => u.id)
  lockedReviewerId.value = detail.reviewer?.id ?? null
  annotatorIds.value = [...lockedAnnotatorIds.value]
  reviewerId.value = lockedReviewerId.value

  const pending = loadTaskUpdateDraft(editTaskId.value).addAnnotatorIds || []
  for (const id of pending) {
    if (!annotatorIds.value.includes(id)) annotatorIds.value.push(id)
  }
}

onMounted(async () => {
  users.value = await client.get('/users')
  if (isEdit.value) {
    await loadExistingForEdit()
    return
  }
  const draft = loadTaskCreateDraft()
  annotatorIds.value = [...(draft.annotatorIds || [])]
  reviewerId.value = draft.reviewerId ?? null
})
</script>

<style scoped>
.task-member-picker-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px;
}

.task-create-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.task-create-header h2 {
  margin: 0;
  font-size: 20px;
}

.hint {
  color: #6b7280;
  margin: 0 0 16px;
}

.existing-panel,
.picker-panel,
.selected-panel {
  margin-bottom: 24px;
}

.existing-panel h3,
.selected-panel h3 {
  margin: 0 0 10px;
  font-size: 15px;
}

.member-list {
  margin: 0;
  padding: 0;
  list-style: none;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.member-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-bottom: 1px solid #f3f4f6;
}

.member-list li:last-child {
  border-bottom: none;
}

.role-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  flex-shrink: 0;
}

.role-tag.annotate {
  background: #ecfdf5;
  color: #047857;
}

.role-tag.arbitrate {
  background: #fff7ed;
  color: #c2410c;
}

.locked-label {
  margin-left: auto;
  font-size: 12px;
  color: #9ca3af;
}

.selected-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.muted {
  color: #6b7280;
  margin: 0 0 8px;
}
</style>
