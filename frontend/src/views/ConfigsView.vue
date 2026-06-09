<template>
  <section class="panel">
    <div class="toolbar">
      <h3>指南版本控制</h3>
      <div style="display: flex; gap: 8px">
        <el-button type="primary" @click="openCreateVersion">新增指南版本</el-button>
        <el-button @click="openVersionList">查看已有版本</el-button>
        <el-button :disabled="currentConfig?.id && !dirty" @click="handleSaveVersion">保存当前版本</el-button>
      </div>
    </div>

    <div v-if="!currentConfig" style="text-align: center; padding: 40px; color: #a8a29e">
      请选择或创建指南版本
    </div>

    <template v-if="currentConfig">
      <!-- 指南附件 -->
      <div class="config-table-section">
        <div class="config-table-header">
          <h4>指南附件</h4>
          <div style="display:flex;gap:8px;align-items:center">
            <span v-if="currentConfig.attachmentName" style="color:#78716c;font-size:13px">{{ currentConfig.attachmentName }}</span>
            <el-button v-if="currentConfig.attachmentName" size="small" @click="viewAttachment">查看</el-button>
            <el-upload :show-file-list="false" :http-request="uploadAttachment" accept=".pdf,.docx,.txt">
              <el-button size="small" type="primary">{{ currentConfig.attachmentName ? '重新上传' : '上传附件' }}</el-button>
            </el-upload>
            <el-button v-if="currentConfig.attachmentName" size="small" type="danger" @click="removeAttachment">删除附件</el-button>
          </div>
        </div>
      </div>
      <!-- 一级标签配置 -->
      <div class="config-table-section">
        <div class="config-table-header">
          <h4>一级标签配置</h4>
          <el-button size="small" type="primary" @click="openTagDialog('primary')">+ 新增标签</el-button>
        </div>
        <el-table :data="currentConfig.primaryTags" size="small">
          <el-table-column prop="shortName" label="简称" width="90" />
          <el-table-column prop="name" label="标签名称" min-width="160" />
          <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="140">
            <template #default="{ row, $index }">
              <el-button link type="primary" @click="openTagDialog('primary', row, $index)">编辑</el-button>
              <el-button link type="danger" @click="removeTag('primary', $index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- GM二级标签配置 -->
      <div class="config-table-section">
        <div class="config-table-header">
          <h4>二级标签配置</h4>
          <el-button size="small" type="primary" @click="openTagDialog('secondary')">+ 新增标签</el-button>
        </div>
        <el-table :data="currentConfig.secondaryTags" size="small">
          <el-table-column prop="shortName" label="简称" width="90" />
          <el-table-column prop="name" label="标签名称" min-width="160" />
          <el-table-column prop="parentTag" label="所属一级标签" width="120">
            <template #default="{ row }">
              <el-tag size="small" type="warning">{{ row.parentTag }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="140">
            <template #default="{ row, $index }">
              <el-button link type="primary" @click="openTagDialog('secondary', row, $index)">编辑</el-button>
              <el-button link type="danger" @click="removeTag('secondary', $index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 关系类型配置 -->
      <div class="config-table-section">
        <div class="config-table-header">
          <h4>关系类型配置</h4>
          <el-button size="small" type="primary" @click="openTagDialog('relation')">+ 新增关系</el-button>
        </div>
        <el-table :data="currentConfig.relationTypes" size="small">
          <el-table-column prop="shortName" label="简称" width="90" />
          <el-table-column prop="name" label="关系名称" min-width="160" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column label="操作" width="140">
            <template #default="{ row, $index }">
              <el-button link type="primary" @click="openTagDialog('relation', row, $index)">编辑</el-button>
              <el-button link type="danger" @click="removeTag('relation', $index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <!-- 查看已有版本对话框 -->
    <el-dialog v-model="versionListVisible" title="已有指南版本" width="680px">
      <el-table :data="configs">
        <el-table-column prop="versionName" label="版本名称" min-width="160" />
        <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="110" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="selectVersion(row)">选择</el-button>
            <el-button link type="danger" @click="deleteVersion(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="versionListVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 保存：版本名称与说明对话框 -->
    <el-dialog v-model="saveNameVisible" :title="currentConfig?.id ? '保存版本' : '保存新版本'" width="460px">
      <el-form :model="saveNameForm" label-position="top">
        <el-form-item label="版本名称"><el-input v-model="saveNameForm.versionName" placeholder="如 V1.1 课堂扩展指南" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="saveNameForm.description" type="textarea" :rows="3" placeholder="可选" /></el-form-item>
      </el-form>
      <template #footer>
        <template v-if="currentConfig?.id">
          <el-button @click="saveNameVisible = false">取消</el-button>
          <el-button type="primary" @click="saveAsOverwrite">覆盖原版本</el-button>
          <el-button @click="confirmSaveAsNew">另存为新版本</el-button>
        </template>
        <template v-else>
          <el-button @click="saveNameVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmSaveAsNew">确认保存</el-button>
        </template>
      </template>
    </el-dialog>

    <!-- 标签编辑对话框 -->
    <el-dialog v-model="tagDialogVisible" :title="tagDialogTitle" width="460px">
      <el-form :model="tagForm" label-position="top">
        <el-form-item label="简称">
          <el-input v-model="tagForm.shortName" placeholder="" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="tagForm.name" placeholder="" />
        </el-form-item>
        <el-form-item v-if="tagDialogType === 'secondary'" label="所属一级标签">
          <el-select v-model="tagForm.parentTag" style="width: 100%" placeholder="请选择一级标签">
            <el-option v-for="t in currentConfig?.primaryTags" :key="t.shortName" :label="`${t.shortName} ${t.name}`" :value="t.shortName" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述（可选）">
          <el-input v-model="tagForm.description" type="textarea" :rows="3" placeholder="标签用途说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tagDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmTag">确认</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="previewVisible" title="附件预览" width="80%" :close-on-click-modal="false">
      <div style="height:70vh">
        <template v-if="isPreviewable">
          <iframe v-if="previewUrl" :key="previewUrl" :src="previewUrl" style="width:100%;height:100%;border:none" />
        </template>
        <div v-else style="display:flex;align-items:center;justify-content:center;height:100%;color:#a8a29e;font-size:16px">
          该文件类型不支持在线预览，请下载后查看
        </div>
      </div>
      <template #footer>
        <el-button @click="previewVisible=false">关闭</el-button>
        <el-button type="primary" @click="downloadAttachment">下载</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/client'

const configs = ref([])
const currentConfig = ref(null)

const dirty = ref(false)
const previewVisible = ref(false), previewUrl = ref('')
const isPreviewable = computed(() => {
  const name = currentConfig.value?.attachmentName || ''
  return name.toLowerCase().endsWith('.pdf') || name.toLowerCase().endsWith('.txt')
})

// ─── Version management ───

function openCreateVersion() {
  const cfg = {
    id: null,
    versionName: '',
    description: '',
    primaryTags: [],
    secondaryTags: [],
    relationTypes: []
  }
  currentConfig.value = cfg
  dirty.value = false
  ElMessage.success('新版本草稿已创建，请配置标签后保存')
}

const versionListVisible = ref(false)

function openVersionList() {
  versionListVisible.value = true
}

function selectVersion(row) {
  loadVersionIntoEditor(row)
  versionListVisible.value = false
  ElMessage.success(`已加载版本「${row.versionName}」`)
}

async function deleteVersion(row) {
  await ElMessageBox.confirm(`确认删除版本「${row.versionName}」？`, '删除版本', { confirmButtonText: '确认', cancelButtonText: '取消' })
  await client.delete(`/configs/versions/${row.id}`)
  ElMessage.success('删除成功')
  if (currentConfig.value?.id === row.id) {
    currentConfig.value = null
    dirty.value = false
  }
  await load()
}

function loadVersionIntoEditor(row) {
  currentConfig.value = JSON.parse(JSON.stringify(row))
  dirty.value = false
}

// ─── Save flow ───

const saveNameVisible = ref(false)
const saveNameForm = reactive({ versionName: '', description: '' })

function handleSaveVersion() {
  if (!currentConfig.value) return
  if (currentConfig.value.id) {
    saveNameForm.versionName = currentConfig.value.versionName || ''
    saveNameForm.description = currentConfig.value.description || ''
  } else {
    saveNameForm.versionName = ''
    saveNameForm.description = ''
  }
  saveNameVisible.value = true
}

async function saveAsOverwrite() {
  if (!saveNameForm.versionName.trim()) { ElMessage.warning('请输入版本名称'); return }
  saveNameVisible.value = false
  const cfg = currentConfig.value
  await client.put(`/configs/versions/${cfg.id}`, {
    versionName: saveNameForm.versionName.trim(),
    description: saveNameForm.description.trim(),
    primaryTags: cfg.primaryTags,
    secondaryTags: cfg.secondaryTags,
    relationTypes: cfg.relationTypes
  })
  ElMessage.success('版本已覆盖保存')
  await load()
}

async function confirmSaveAsNew() {
  if (!saveNameForm.versionName.trim()) { ElMessage.warning('请输入版本名称'); return }
  saveNameVisible.value = false
  const cfg = currentConfig.value
  const result = await client.post('/configs/versions', {
    versionName: saveNameForm.versionName.trim(),
    description: saveNameForm.description.trim(),
    primaryTags: cfg.primaryTags,
    secondaryTags: cfg.secondaryTags,
    relationTypes: cfg.relationTypes
  })
  ElMessage.success('新版本已保存')
  await load()
  const fresh = configs.value.find(c => c.id === result.configId)
  if (fresh) loadVersionIntoEditor(fresh)
}

// ─── Tag editing ───

const tagDialogVisible = ref(false)
const tagDialogType = ref('primary')
const tagEditing = ref(false)
const tagEditIndex = ref(-1)
const tagForm = reactive({ shortName: '', name: '', description: '', parentTag: '' })

const tagDialogTitle = computed(() => {
  const map = { primary: '一级标签', secondary: '二级标签', relation: '关系类型' }
  return tagEditing.value ? `编辑${map[tagDialogType.value]}` : `新增${map[tagDialogType.value]}`
})

function openTagDialog(type, row, index) {
  tagDialogType.value = type
  if (row) {
    tagEditing.value = true
    tagEditIndex.value = index
    tagForm.name = row.name
    tagForm.shortName = row.shortName
    tagForm.description = row.description || ''
    tagForm.parentTag = row.parentTag || ''
  } else {
    tagEditing.value = false
    tagEditIndex.value = -1
    tagForm.name = ''
    tagForm.shortName = ''
    tagForm.description = ''
    tagForm.parentTag = currentConfig.value?.primaryTags?.[0]?.shortName || ''
  }
  tagDialogVisible.value = true
}

function confirmTag() {
  if (!tagForm.name.trim() || !tagForm.shortName.trim()) { ElMessage.warning('名称和简称不能为空'); return }
  if (tagDialogType.value === 'secondary' && !tagForm.parentTag.trim()) { ElMessage.warning('请选择所属一级标签'); return }

  const item = {
    shortName: tagForm.shortName.trim(),
    name: tagForm.name.trim(),
    description: tagForm.description.trim(),
    parentTag: tagDialogType.value === 'secondary' ? tagForm.parentTag.trim() : ''
  }

  const key = tagDialogType.value === 'primary' ? 'primaryTags' : tagDialogType.value === 'secondary' ? 'secondaryTags' : 'relationTypes'
  if (tagEditing.value) {
    currentConfig.value[key][tagEditIndex.value] = item
  } else {
    currentConfig.value[key].push(item)
  }
  tagDialogVisible.value = false
  dirty.value = true
}

function removeTag(type, index) {
  const key = type === 'primary' ? 'primaryTags' : type === 'secondary' ? 'secondaryTags' : 'relationTypes'
  currentConfig.value[key].splice(index, 1)
  dirty.value = true
}

// ─── Load ───

async function load() {
  configs.value = await client.get('/configs/versions')
}

// ─── Attachment ───

async function uploadAttachment({ file, onSuccess, onError }) {
  if (!currentConfig.value?.id) { ElMessage.warning('请先保存版本后再上传附件'); onError?.(); return }
  const fd = new FormData(); fd.append('file', file)
  try {
    await client.post(`/configs/versions/${currentConfig.value.id}/attachment`, fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    currentConfig.value.attachmentName = file.name
    ElMessage.success('附件上传成功')
    onSuccess?.()
  } catch (e) { ElMessage.error('上传失败: ' + (e.message || '')); onError?.(e) }
}

async function removeAttachment() {
  if (!currentConfig.value?.id) return
  try {
    await client.put(`/configs/versions/${currentConfig.value.id}`, {
      versionName: currentConfig.value.versionName,
      description: currentConfig.value.description,
      primaryTags: currentConfig.value.primaryTags,
      secondaryTags: currentConfig.value.secondaryTags,
      relationTypes: currentConfig.value.relationTypes,
      attachmentName: ''
    })
    currentConfig.value.attachmentName = null
    ElMessage.success('附件已删除')
  } catch (e) { ElMessage.error('删除失败') }
}

async function viewAttachment() {
  if (!currentConfig.value?.id || !currentConfig.value?.attachmentName) return
  const token = localStorage.getItem('jap_token')
  previewUrl.value = `/api/configs/versions/${currentConfig.value.id}/attachment?token=${token}&t=${Date.now()}`
  previewVisible.value = true
}

function downloadAttachment() {
  if (!previewUrl.value || !currentConfig.value?.attachmentName) return
  const a = document.createElement('a')
  a.href = previewUrl.value
  a.download = currentConfig.value.attachmentName
  a.click()
}

onMounted(load)
</script>

<style scoped>
.config-table-section {
  margin-bottom: 24px;
}
.config-table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.config-table-header h4 {
  margin: 0;
}
</style>