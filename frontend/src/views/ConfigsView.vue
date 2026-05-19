<template>
  <div class="split">
    <section class="panel">
      <div class="toolbar">
        <h3>指南版本</h3>
        <el-button type="primary" @click="visible=true">新建版本</el-button>
      </div>
      <el-table :data="configs" highlight-current-row @current-change="selected = $event">
        <el-table-column prop="versionName" label="版本" />
        <el-table-column prop="description" label="说明" />
        <el-table-column prop="active" label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.active ? 'success' : 'info'">{{ row.active ? '启用' : '备用' }}</el-tag></template>
        </el-table-column>
      </el-table>
    </section>
    <section class="panel">
      <h3>{{ selected?.versionName || '标签体系' }}</h3>
      <p class="muted">任务启动后会复制该指南快照，后续不可修改。</p>
      <h4>一级标签</h4>
      <div class="tag-row"><el-tag v-for="tag in selected?.primaryTags" :key="tag.shortName">{{ tag.shortName }} · {{ tag.name }}</el-tag></div>
      <h4>GM 二级标签</h4>
      <div class="tag-row"><el-tag v-for="tag in selected?.secondaryTags" :key="tag.shortName" type="warning">{{ tag.shortName }} · {{ tag.name }}</el-tag></div>
      <h4>关系类型</h4>
      <div class="tag-row"><el-tag v-for="tag in selected?.relationTypes" :key="tag.shortName" type="success">{{ tag.shortName }} · {{ tag.name }}</el-tag></div>
    </section>
    <el-dialog v-model="visible" title="新建指南版本" width="460px">
      <el-form :model="form" label-position="top">
        <el-form-item label="版本名称"><el-input v-model="form.versionName" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import client from '../api/client'

const configs = ref([])
const selected = ref(null)
const visible = ref(false)
const form = reactive({ versionName: 'V1.1 课堂扩展指南', description: '基于默认标签体系复制的演示版本' })

async function load() {
  configs.value = await client.get('/configs/versions')
  selected.value = configs.value[0]
}

async function save() {
  await client.post('/configs/versions', form)
  visible.value = false
  load()
}

onMounted(load)
</script>
