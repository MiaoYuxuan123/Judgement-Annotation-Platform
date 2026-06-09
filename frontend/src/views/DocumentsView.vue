<template>
  <section class="panel">
    <div class="toolbar">
      <h3>文书管理</h3>
      <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
        <el-select v-model="filterDocId" placeholder="文书ID" clearable style="width: 140px">
          <el-option v-for="id in availableDocIds" :key="id" :label="id" :value="id" />
        </el-select>
        <el-input v-model="filterTitle" placeholder="标题" clearable style="width: 200px" />
        <el-select v-model="filterDate" placeholder="上传时间" clearable style="width: 150px">
          <el-option v-for="date in availableDates" :key="date" :label="date" :value="date" />
        </el-select>
        <el-button type="primary" @click="openManualCreate">手动新增</el-button>
      </div>
    </div>

    <el-upload ref="uploadRef" drag multiple accept=".pdf,.docx,.txt,.zip"
      :http-request="uploadFile" :file-list="uploadFileList"
      @preview="openPreviewFromUpload" @remove="handleUploadRemove"
      style="margin-bottom: 16px">
      <template #default>
        <div class="el-upload__text">拖拽或点击上传 PDF / Word / TXT / ZIP</div>
      </template>
    </el-upload>

    <el-table :data="filteredDocuments">
      <el-table-column prop="documentId" label="文书ID" width="110" />
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column prop="type" label="类型" width="130" />
      <el-table-column prop="uploadDate" label="上传时间" width="130" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" @click="show(row.id)">查看</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <p v-if="filteredDocuments.length === 0 && allDocuments.length > 0" style="text-align: center; color: #a8a29e; margin-top: 16px">无匹配结果，请调整筛选条件</p>

    <!-- 查看抽屉 -->
    <el-drawer v-model="drawer" title="文书详情" size="48%">
      <h3>{{ current?.title }}</h3>
      <p class="muted">{{ current?.documentId }} · {{ current?.type }}</p>
      <div class="document-text">{{ current?.content }}</div>
    </el-drawer>

    <!-- 手动新增弹窗 -->
    <el-dialog v-model="visible" title="新增文书" width="620px">
      <el-form :model="form" label-position="top">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="form.type" /></el-form-item>
        <el-form-item label="裁判理由文本"><el-input v-model="form.content" type="textarea" :rows="8" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible=false">取消</el-button>
        <el-button type="primary" @click="saveManual">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量上传预览弹窗 -->
    <el-dialog v-model="previewVisible" title="文件解析预览" width="900px" :close-on-click-modal="false">
      <template v-if="previewItems.length === 0">
        <el-empty description="无解析结果" />
      </template>
      <template v-else>
        <el-table :data="previewItems" @selection-change="onPreviewSelectionChange" ref="previewTableRef">
          <el-table-column type="selection" width="50" />
          <el-table-column prop="filename" label="文件名" min-width="200" />
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="type" label="文书类型" width="140" />
          <el-table-column prop="contentLength" label="字符数" width="100">
            <template #default="{ row }">{{ row.contentLength?.toLocaleString() }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row, $index }">
              <el-button link type="primary" @click="openEditPreviewItem($index)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <template #footer>
        <el-button type="danger" @click="removeSelectedPreview">移除选中</el-button>
        <el-button type="primary" :loading="saving" @click="saveCheckedPreview">确认入库</el-button>
      </template>
    </el-dialog>

    <!-- 单文件编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑文书" width="680px" :close-on-click-modal="false">
      <el-form :model="editForm" label-position="top" v-if="editForm">
        <el-form-item label="文件名"><el-input :model-value="editForm.filename" disabled /></el-form-item>
        <el-form-item label="标题"><el-input v-model="editForm.title" /></el-form-item>
        <el-form-item label="文书类型"><el-input v-model="editForm.type" placeholder="如：民事判决书、刑事裁定书..." /></el-form-item>
        <el-form-item>
          <template #label><span>正文内容（已提取 {{ editForm.contentLength?.toLocaleString() }} 字符，可修改）</span></template>
          <el-input v-model="editForm.content" type="textarea" :rows="16" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmEdit">确认</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/client'

const allDocuments = ref([]), filterDocId = ref(''), filterTitle = ref(''), filterDate = ref('')
const availableDocIds = computed(() => { const ids = allDocuments.value.map(d => d.documentId).filter(Boolean); return [...new Set(ids)].sort() })
const availableDates = computed(() => { const dates = allDocuments.value.map(d => d.uploadDate).filter(Boolean); return [...new Set(dates)].sort() })
const filteredDocuments = computed(() => allDocuments.value.filter(doc => {
  if (filterDocId.value && doc.documentId !== filterDocId.value) return false
  if (filterTitle.value && !doc.title.includes(filterTitle.value)) return false
  if (filterDate.value && doc.uploadDate !== filterDate.value) return false
  return true
}))

const drawer = ref(false), current = ref(null)
const visible = ref(false)
const form = reactive({ title: '', type: '民事判决书', content: '本院认为，依法成立的合同受法律保护。' })

function openManualCreate() { Object.assign(form, { title: '', type: '民事判决书', content: '本院认为，依法成立的合同受法律保护。' }); visible.value = true }
async function load() { const data = await client.get('/documents'); allDocuments.value = data.list || [] }
async function show(id) { current.value = await client.get(`/documents/${id}`); drawer.value = true }
async function saveManual() { await client.post('/documents', form); visible.value = false; load() }
async function remove(row) {
  await ElMessageBox.confirm(`确认删除 ${row.title}？`, '删除文书', { confirmButtonText: '确认', cancelButtonText: '取消' })
  await client.delete(`/documents/${row.id}`); load()
}

// ─── 批量上传 ───
const uploadRef = ref(null), previewVisible = ref(false), previewItems = ref([]), saving = ref(false)
const previewTableRef = ref(null), previewSelection = ref([])
const uploadFileList = ref([])

function onPreviewSelectionChange(val) { previewSelection.value = val }

async function uploadFile({ file, onSuccess, onError }) {
  const fd = new FormData(); fd.append('files', file)
  try {
    const data = await client.post('/documents/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    if (data.errors?.length) ElMessage.warning(data.errors.join('；'))
    if (data.list?.length) {
      const items = data.list.map(item => ({ ...item, _uploadUid: file.uid }))
      previewItems.value = [...previewItems.value, ...items]
      uploadFileList.value.push(file)
      previewVisible.value = true
      const count = data.list.length > 1 ? `ZIP 内 ${data.list.length} 个文件` : file.name
      ElMessage.success(`${count} 解析完成，请在对话框中确认`)
    } else {
      ElMessage.error(`${file.name} 解析失败`)
    }
    onSuccess?.(data)
  } catch (error) { onError?.(error) }
}

function removeSelectedPreview() {
  const selected = previewSelection.value
  if (selected.length === 0) { ElMessage.warning('请先勾选要移除的文件'); return }
  previewItems.value = previewItems.value.filter(item => !selected.includes(item))
  syncUploadFiles()
  if (previewItems.value.length === 0) { previewVisible.value = false; uploadFileList.value = [] }
}

function syncUploadFiles() {
  const activeUids = new Set(previewItems.value.map(p => p._uploadUid))
  uploadFileList.value = uploadFileList.value.filter(f => activeUids.has(f.uid))
}

function openPreviewFromUpload() { if (previewItems.value.length > 0) previewVisible.value = true }

function handleUploadRemove(file) {
  uploadFileList.value = uploadFileList.value.filter(f => f.uid !== file.uid)
  previewItems.value = previewItems.value.filter(item => item._uploadUid !== file.uid)
  if (previewItems.value.length === 0) previewVisible.value = false
}

// ─── 单文件编辑 ───
const editVisible = ref(false), editForm = ref(null), editIndex = ref(-1)

function openEditPreviewItem(idx) {
  editIndex.value = idx
  editForm.value = { ...previewItems.value[idx] }
  editVisible.value = true
}

function confirmEdit() {
  if (editIndex.value >= 0 && editForm.value) {
    previewItems.value[editIndex.value] = { ...editForm.value }
    ElMessage.success('修改已暂存')
  }
  editVisible.value = false
}

// ─── 确认入库（仅勾选的） ───
async function saveCheckedPreview() {
  const selected = previewSelection.value
  if (selected.length === 0) { ElMessage.warning('请先勾选要入库的文件'); return }
  saving.value = true
  try {
    let saved = 0
    for (const item of selected) {
      try {
        await client.post('/documents', { title: item.title, type: item.type, content: item.content })
        saved++
      } catch (e) { ElMessage.error(`保存 "${item.title}" 失败: ${e.message}`) }
    }
    ElMessage.success(`成功入库 ${saved} 篇文书`)
    previewItems.value = previewItems.value.filter(item => !selected.includes(item))
    syncUploadFiles()
    if (previewItems.value.length === 0) { previewVisible.value = false; uploadFileList.value = []; uploadRef.value?.clearFiles() }
    await load()
  } finally { saving.value = false }
}

onMounted(load)
</script>