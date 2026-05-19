<template>
  <div>
    <section class="panel" style="margin-bottom: 14px">
      <div class="toolbar">
        <div>
          <h3>结果查看与导出</h3>
          <p class="muted">导出文件附带标签指南版本信息；MVP 中除 JSON 外返回模拟下载地址。</p>
        </div>
        <div>
          <el-select v-model="format" style="width: 120px; margin-right: 8px">
            <el-option label="JSON" value="json" />
            <el-option label="XLSX" value="xlsx" />
            <el-option label="PNG" value="png" />
            <el-option label="SVG" value="svg" />
            <el-option label="ZIP" value="zip" />
          </el-select>
          <el-button type="primary" @click="exportData">导出</el-button>
        </div>
      </div>
      <el-progress v-if="exportInfo" :percentage="exportInfo.progress" />
      <el-alert v-if="exportInfo" style="margin-top: 12px" type="success" :title="`导出完成：${exportInfo.downloadUrl}`" show-icon />
    </section>
    <section class="panel">
      <el-empty v-if="!results.length" description="暂无最终裁定结果，可先进入裁定页面保存" />
      <el-tabs v-else>
        <el-tab-pane v-for="result in results" :key="result.dataId" :label="`文书 ${result.dataId}`">
          <el-table :data="result.propositions" style="margin-bottom: 14px">
            <el-table-column prop="sequenceNo" label="#" width="60" />
            <el-table-column prop="tag" label="标签" width="100" />
            <el-table-column prop="text" label="命题文本" />
          </el-table>
          <GraphView :propositions="result.propositions" :relations="result.relations" />
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import client from '../api/client'
import GraphView from '../components/GraphView.vue'

const route = useRoute()
const results = ref([])
const format = ref('json')
const exportInfo = ref(null)

async function load() {
  results.value = await client.get(`/tasks/${route.params.taskId}/results`)
}

async function exportData() {
  exportInfo.value = await client.get(`/tasks/${route.params.taskId}/export`, { params: { format: format.value } })
}

onMounted(load)
</script>
