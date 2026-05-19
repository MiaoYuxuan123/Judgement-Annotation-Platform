<template>
  <section class="panel">
    <div class="toolbar">
      <h3>文书总库</h3>
      <div>
        <el-input v-model="keyword" placeholder="搜索文书标题 / ID" style="width: 220px; margin-right: 8px" @keyup.enter="load" />
        <el-button @click="load">搜索</el-button>
        <el-button type="primary" @click="visible=true">手动新增</el-button>
      </div>
    </div>
    <el-upload drag multiple :auto-upload="false" style="margin-bottom: 16px">
      <div>拖拽 PDF / Word / TXT 到这里，MVP 中会生成模拟解析文本</div>
    </el-upload>
    <el-table :data="documents">
      <el-table-column prop="documentId" label="文书ID" width="110" />
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column prop="type" label="类型" width="130" />
      <el-table-column prop="uploadDate" label="上传时间" width="130" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }"><el-button link type="primary" @click="show(row.id)">查看</el-button></template>
      </el-table-column>
    </el-table>
    <el-drawer v-model="drawer" title="文书详情" size="48%">
      <h3>{{ current?.title }}</h3>
      <p class="muted">{{ current?.documentId }} · {{ current?.type }}</p>
      <div class="document-text">{{ current?.content }}</div>
    </el-drawer>
    <el-dialog v-model="visible" title="新增文书" width="620px">
      <el-form :model="form" label-position="top">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="form.type" /></el-form-item>
        <el-form-item label="裁判理由文本"><el-input v-model="form.content" type="textarea" :rows="8" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import client from '../api/client'

const documents = ref([])
const keyword = ref('')
const drawer = ref(false)
const visible = ref(false)
const current = ref(null)
const form = reactive({ title: '', type: '民事判决书', content: '本院认为，依法成立的合同受法律保护。' })

async function load() {
  const data = await client.get('/documents', { params: { keyword: keyword.value } })
  documents.value = data.list || []
}

async function show(id) {
  current.value = await client.get(`/documents/${id}`)
  drawer.value = true
}

async function save() {
  await client.post('/documents', form)
  visible.value = false
  load()
}

onMounted(load)
</script>
