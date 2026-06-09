<template>
  <section class="panel task-doc-picker-page">
    <div class="task-create-header">
      <el-button text @click="goBack">← {{ editTaskId ? '返回任务详情' : '返回创建任务' }}</el-button>
      <h2>选取 / 上传文书</h2>
    </div>

    <div v-if="editTaskId && existingDocuments.length" class="existing-panel">
      <h3>任务已有文书（{{ existingDocuments.length }}）</h3>
      <ul class="task-doc-selected-list existing-list">
        <li v-for="doc in existingDocuments" :key="`ex-${doc.id}`">
          <span class="task-doc-source-tag">{{ sourceTypeLabel(doc.sourceType) }}</span>
          <span>{{ doc.title || doc.fileName }}</span>
          <span class="locked-label">已在任务中</span>
        </li>
      </ul>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="文书总库" name="global">
        <p class="hint">从超级管理员维护的文书总库中选取；可直接使用原文，也可修改标注范围后作为新文书加入任务。</p>
        <el-table :data="globalDocuments" @row-click="previewGlobal">
          <el-table-column prop="documentId" label="ID" width="90" />
          <el-table-column prop="title" label="标题" min-width="220" />
          <el-table-column prop="type" label="类型" width="120" />
          <el-table-column label="操作" width="300">
            <template #default="{ row }">
              <el-tag v-if="isExistingGlobal(row.id)" type="info" size="small">已在任务中</el-tag>
              <template v-else>
                <el-button link type="primary" @click.stop="addGlobal(row)">直接使用</el-button>
                <el-button link type="warning" @click.stop="openRecreate(row)">修改范围</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="自主上传" name="upload">
        <p class="hint">上传的文书仅用于当前任务，不会进入文书总库，其他创建者不可见。</p>
        <el-upload
          ref="uploadRef"
          drag
          multiple
          accept=".pdf,.docx,.txt"
          :http-request="uploadFile"
          :show-file-list="true"
        >
          <div class="el-upload__text">拖拽或点击上传 PDF / Word / TXT</div>
        </el-upload>
      </el-tab-pane>
    </el-tabs>

    <div class="selected-panel">
      <h3>已选文书（{{ selected.length }}）</h3>
      <ul v-if="selected.length" class="task-doc-selected-list">
        <li v-for="doc in selected" :key="documentKey(doc)">
          <span class="task-doc-source-tag">{{ sourceTypeLabel(doc.sourceType) }}</span>
          <span>{{ doc.title || doc.fileName }}</span>
          <el-button link type="danger" @click="removeSelected(doc)">移除</el-button>
        </li>
      </ul>
      <p v-else class="muted">尚未选择文书</p>
      <div class="selected-actions">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" :disabled="!selected.length" @click="confirm">确认选取</el-button>
      </div>
    </div>

    <el-dialog v-model="previewVisible" title="文书预览" width="720px">
      <template v-if="previewDoc">
        <h3>{{ previewDoc.title }}</h3>
        <p class="muted">{{ previewDoc.documentId }} · {{ previewDoc.type }}</p>
        <div class="document-text">{{ previewDoc.content }}</div>
      </template>
    </el-dialog>

    <el-dialog v-model="recreateVisible" title="修改标注范围" width="780px" :close-on-click-modal="false">
      <template v-if="recreateForm">
        <el-form label-position="top">
          <el-form-item label="文书标题">
            <el-input v-model="recreateForm.title" />
          </el-form-item>
          <el-form-item label="正文（可裁剪或编辑标注范围）">
            <el-input v-model="recreateForm.extractedText" type="textarea" :rows="16" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="recreateVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRecreate">加入已选</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="uploadPreviewVisible" title="上传预览 - 确认后加入已选" width="780px" :close-on-click-modal="false">
      <el-tabs v-if="uploadPreviewItems.length" v-model="uploadPreviewTab" type="card">
        <el-tab-pane v-for="(item, idx) in uploadPreviewItems" :key="idx" :label="item.fileName" :name="idx">
          <el-form label-position="top" style="margin-top: 12px">
            <el-form-item label="标题">
              <el-input v-model="item.title" />
            </el-form-item>
            <el-form-item label="正文">
              <el-input v-model="item.extractedText" type="textarea" :rows="14" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="discardUploadPreview">丢弃</el-button>
        <el-button type="primary" @click="confirmUploadPreview">加入已选</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import client from '../../api/client'
import {
  documentKey,
  loadTaskCreateDraft,
  saveTaskCreateDraft,
  sourceTypeLabel
} from '../../utils/taskCreateDraft'
import { loadTaskUpdateDraft, saveTaskUpdateDraft } from '../../utils/taskUpdateDraft'
import { tasksReturnRoute } from '../../utils/navigationReturn'

const router = useRouter()
const route = useRoute()
const editTaskId = computed(() => {
  const id = route.params.taskId
  return id ? Number(id) : null
})
const activeTab = ref('global')
const globalDocuments = ref([])
const selected = ref([])
const previewVisible = ref(false)
const previewDoc = ref(null)
const recreateVisible = ref(false)
const recreateForm = ref(null)
const uploadRef = ref(null)
const uploadPreviewVisible = ref(false)
const uploadPreviewItems = ref([])
const uploadPreviewTab = ref(0)
const existingDocuments = ref([])

function isExistingGlobal(globalDocId) {
  return existingDocuments.value.some(
    (d) => d.sourceType === 'GLOBAL' && Number(d.globalDocId || d.id) === Number(globalDocId)
  )
}

function goBack() {
  if (editTaskId.value) {
    router.push(tasksReturnRoute(editTaskId.value))
    return
  }
  router.push('/tasks/create')
}

function loadSelectedFromDraft() {
  if (editTaskId.value) {
    selected.value = [...(loadTaskUpdateDraft(editTaskId.value).addDocuments || [])]
    return
  }
  selected.value = [...(loadTaskCreateDraft().documents || [])]
}

function persistSelected() {
  if (editTaskId.value) {
    const draft = loadTaskUpdateDraft(editTaskId.value)
    draft.addDocuments = selected.value
    saveTaskUpdateDraft(editTaskId.value, draft)
    return
  }
  const draft = loadTaskCreateDraft()
  draft.documents = selected.value
  saveTaskCreateDraft(draft)
}

function addDocument(doc) {
  const key = documentKey(doc)
  if (selected.value.some((item) => documentKey(item) === key)) {
    ElMessage.warning('该文书已在已选列表中')
    return
  }
  selected.value.push({ ...doc, key })
}

function removeSelected(doc) {
  const key = documentKey(doc)
  selected.value = selected.value.filter((item) => documentKey(item) !== key)
}

function addGlobal(row) {
  if (isExistingGlobal(row.id)) {
    ElMessage.warning('该文书已在任务中')
    return
  }
  addDocument({
    sourceType: 'GLOBAL',
    globalDocId: row.id,
    title: row.title,
    fileName: row.title
  })
  ElMessage.success('已加入已选列表')
}

async function previewGlobal(row) {
  previewDoc.value = await client.get(`/documents/${row.id}`)
  previewVisible.value = true
}

async function openRecreate(row) {
  const detail = await client.get(`/documents/${row.id}`)
  recreateForm.value = reactive({
    sourceType: 'RECREATE',
    globalDocId: row.id,
    title: detail.title || row.title,
    fileName: (detail.title || row.title) + '.txt',
    extractedText: detail.content || '',
    _originalText: detail.content || ''
  })
  recreateVisible.value = true
}

function confirmRecreate() {
  const modified = recreateForm.value.extractedText?.trim()
  if (!modified) {
    ElMessage.warning('正文不能为空')
    return
  }
  if (isExistingGlobal(recreateForm.value.globalDocId)) {
    ElMessage.warning('该文书已在任务中')
    recreateVisible.value = false
    return
  }
  const originalText = (recreateForm.value._originalText || '').trim()
  if (originalText && modified === originalText) {
    addDocument({
      sourceType: 'GLOBAL',
      globalDocId: recreateForm.value.globalDocId,
      title: recreateForm.value.title,
      fileName: recreateForm.value.fileName
    })
    ElMessage.success('未修改正文，已作为文书总库原文加入')
  } else {
    addDocument({
      sourceType: 'RECREATE',
      globalDocId: recreateForm.value.globalDocId,
      title: recreateForm.value.title,
      fileName: recreateForm.value.fileName,
      extractedText: modified
    })
    ElMessage.success('修改后的文书已加入已选列表')
  }
  recreateVisible.value = false
}

async function uploadFile({ file, onSuccess, onError }) {
  const fd = new FormData()
  fd.append('files', file)
  try {
    const data = await client.post('/tasks/documents/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (data.errors?.length) ElMessage.warning(data.errors.join('；'))
    if (data.list?.length) {
      uploadPreviewItems.value = data.list.map((item) => ({ ...item, _uid: file.uid }))
      uploadPreviewTab.value = 0
      uploadPreviewVisible.value = true
    }
    onSuccess?.(data)
  } catch (error) {
    onError?.(error)
  }
}

function discardUploadPreview() {
  uploadPreviewItems.value = []
  uploadPreviewVisible.value = false
  uploadRef.value?.clearFiles()
}

function confirmUploadPreview() {
  for (const item of uploadPreviewItems.value) {
    addDocument({
      sourceType: 'UPLOAD',
      fileName: item.fileName,
      title: item.title || item.fileName,
      extractedText: item.extractedText
    })
  }
  ElMessage.success('上传文书已加入已选列表')
  discardUploadPreview()
}

function confirm() {
  persistSelected()
  ElMessage.success('文书选取已保存')
  if (editTaskId.value) {
    router.push(tasksReturnRoute(editTaskId.value))
    return
  }
  router.push('/tasks/create')
}

onMounted(async () => {
  loadSelectedFromDraft()
  if (editTaskId.value) {
    const detail = await client.get(`/tasks/${editTaskId.value}`)
    existingDocuments.value = detail.documents || []
  }
  const data = await client.get('/documents')
  globalDocuments.value = data.list || []
})
</script>

<style scoped>
.task-doc-picker-page {
  max-width: 960px;
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

.existing-panel {
  margin-bottom: 20px;
}

.existing-panel h3 {
  margin: 0 0 10px;
  font-size: 15px;
}

.existing-list .locked-label {
  margin-left: auto;
  font-size: 12px;
  color: #a8a29e;
}

.hint {
  color: #78716c;
  margin: 0 0 12px;
}

.selected-panel {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid #e0dbd2;
}

.selected-panel h3 {
  margin: 0 0 12px;
  font-size: 16px;
}

.task-doc-selected-list {
  margin: 0 0 16px;
  padding: 0;
  list-style: none;
  border: 1px solid #e0dbd2;
  border-radius: 8px;
  overflow: hidden;
}

.task-doc-selected-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-bottom: 1px solid #f6f2eb;
}

.task-doc-selected-list li:last-child {
  border-bottom: none;
}

.task-doc-source-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #fef2f2;
  color: #991b1b;
  flex-shrink: 0;
}

.selected-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.document-text {
  max-height: 420px;
  overflow: auto;
  white-space: pre-wrap;
  line-height: 1.7;
  background: #faf7f2;
  padding: 12px;
  border-radius: 8px;
}

.muted {
  color: #78716c;
}
</style>
