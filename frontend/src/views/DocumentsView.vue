<template>
  <section class="panel">
    <div class="toolbar">
      <h3>文书总库</h3>
      <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
        <el-select v-model="filterDocId" placeholder="文书ID" clearable style="width: 140px">
          <el-option v-for="id in availableDocIds" :key="id" :label="id" :value="id" />
        </el-select>
        <el-input v-model="filterTitle" placeholder="标题" clearable style="width: 200px" />
        <el-select v-model="filterDate" placeholder="上传时间" clearable style="width: 150px">
          <el-option v-for="date in availableDates" :key="date" :label="date" :value="date" />
        </el-select>
        <el-button type="primary" @click="visible=true">手动新增</el-button>
      </div>
    </div>
    <el-upload ref="uploadRef" drag multiple accept=".pdf,.docx,.txt" :http-request="uploadFile" :on-preview="openPreviewFromUpload" :on-remove="handleRemoveUploadFile" :show-file-list="true" style="margin-bottom: 16px">
      <template #default><div class="el-upload__text">拖拽或点击上传 PDF / Word / TXT，解析后可编辑标题、类型与正文，确认后入库</div></template>
    </el-upload>
    <el-table :data="filteredDocuments">
      <el-table-column prop="documentId" label="文书ID" width="110" />
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column prop="type" label="类型" width="130" />
      <el-table-column prop="uploadDate" label="上传时间" width="130" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }"><el-button link type="primary" @click="show(row.id)">查看</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template>
      </el-table-column>
    </el-table>
    <p v-if="filteredDocuments.length === 0 && allDocuments.length > 0" style="text-align: center; color: #999; margin-top: 16px">无匹配结果，请调整筛选条件</p>
    <el-drawer v-model="drawer" title="文书详情" size="48%"><h3>{{ current?.title }}</h3><p class="muted">{{ current?.documentId }} · {{ current?.type }}</p><div class="document-text">{{ current?.content }}</div></el-drawer>
    <el-dialog v-model="visible" title="新增文书" width="620px">
      <el-form :model="form" label-position="top"><el-form-item label="标题"><el-input v-model="form.title" /></el-form-item><el-form-item label="类型"><el-input v-model="form.type" /></el-form-item><el-form-item label="裁判理由文本"><el-input v-model="form.content" type="textarea" :rows="8" /></el-form-item></el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="previewVisible" title="解析预览 - 请确认并编辑后入库" width="780px" :close-on-click-modal="false">
      <template v-if="previewItems.length === 0"><el-empty description="无解析结果" /></template>
      <template v-else>
        <el-tabs v-model="activePreviewTab" type="card">
          <el-tab-pane v-for="(item, idx) in previewItems" :key="idx" :label="item.filename" :name="idx">
            <el-form :model="item" label-position="top" style="margin-top: 12px">
              <el-form-item label="原标题（可修改）"><el-input v-model="item.title" /></el-form-item>
              <el-form-item label="文书类型（可自定义）"><el-input v-model="item.type" placeholder="如：民事判决书、刑事裁定书..." /></el-form-item>
              <el-form-item><template #label><span>正文内容（已提取 {{ item.contentLength?.toLocaleString() }} 字符，可修改）</span></template><el-input v-model="item.content" type="textarea" :rows="14" /></el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </template>
      <template #footer>
        <el-button @click="cancelPreview">全部丢弃</el-button>
        <el-button v-if="previewItems.length > 1" @click="removePreviewItem(activePreviewTab)">移除此项</el-button>
        <el-button type="primary" :loading="saving" @click="saveAllPreview">全部确认入库</el-button>
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
const filteredDocuments = computed(() => allDocuments.value.filter(doc => { if (filterDocId.value && doc.documentId !== filterDocId.value) return false; if (filterTitle.value && !doc.title.includes(filterTitle.value)) return false; if (filterDate.value && doc.uploadDate !== filterDate.value) return false; return true }))
const drawer = ref(false), visible = ref(false), current = ref(null), form = reactive({ title: '', type: '民事判决书', content: '本院认为，依法成立的合同受法律保护。' })
const uploadRef = ref(null), previewVisible = ref(false), previewItems = ref([]), activePreviewTab = ref(0), saving = ref(false)
async function load() { const data = await client.get('/documents'); allDocuments.value = data.list || [] }
async function show(id) { current.value = await client.get(`/documents/${id}`); drawer.value = true }
async function save() { await client.post('/documents', form); visible.value = false; load() }
async function remove(row) { await ElMessageBox.confirm(`确认删除 ${row.title}？`, '删除文书', { confirmButtonText: '确认', cancelButtonText: '取消' }); await client.delete(`/documents/${row.id}`); load() }
async function uploadFile({ file, onSuccess, onError }) { const fd = new FormData(); fd.append('files', file); try { const data = await client.post('/documents/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } }); if (data.errors?.length) ElMessage.warning(data.errors.join('；')); if (data.list?.length) { const items = data.list.map(item => ({ ...item, _uploadUid: file.uid })); previewItems.value = [...previewItems.value, ...items]; activePreviewTab.value = previewItems.value.length - data.list.length; previewVisible.value = true; ElMessage.success(`${file.name} 解析完成，请在对话框中确认`) } else ElMessage.error(`${file.name} 解析失败`); onSuccess?.(data) } catch (error) { onError?.(error) } }
function openPreviewFromUpload() { if (previewItems.value.length > 0) previewVisible.value = true }
function handleRemoveUploadFile(uploadFile) { const idx = previewItems.value.findIndex(item => item._uploadUid === uploadFile.uid); if (idx !== -1) { previewItems.value.splice(idx, 1); if (previewItems.value.length === 0) previewVisible.value = false; else if (activePreviewTab.value >= previewItems.value.length) activePreviewTab.value = previewItems.value.length - 1 } }
function removePreviewItem(idx) { previewItems.value.splice(idx, 1); if (previewItems.value.length === 0) previewVisible.value = false; else if (activePreviewTab.value >= previewItems.value.length) activePreviewTab.value = previewItems.value.length - 1 }
function cancelPreview() { previewItems.value = []; previewVisible.value = false; uploadRef.value?.clearFiles() }
async function saveAllPreview() { saving.value = true; try { let saved = 0; for (const item of previewItems.value) { try { await client.post('/documents', { title: item.title, type: item.type, content: item.content }); saved++ } catch (e) { ElMessage.error(`保存 "${item.title}" 失败: ${e.message}`) } } ElMessage.success(`成功入库 ${saved} 篇文书`); previewItems.value = []; previewVisible.value = false; uploadRef.value?.clearFiles(); await load() } finally { saving.value = false } }
onMounted(load)
</script>