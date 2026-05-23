<template>
  <div v-if="review">
    <section class="panel annotate-topbar">
      <div>
        <el-button text @click="$router.push(`/tasks/${route.params.taskId}/data`)">← 返回</el-button>
        <strong>{{ review.task.taskName }} — 结果查看</strong>
        <span class="muted">可切换不同标注员版本与最终裁定结果。</span>
      </div>
      <div>
        <el-select v-model="format" style="width: 120px" :disabled="!canExport">
          <el-option label="JSON" value="json" />
          <el-option label="XLSX" value="xlsx" />
          <el-option label="PNG" value="png" />
          <el-option label="SVG" value="svg" />
          <el-option label="ZIP" value="zip" />
        </el-select>
        <el-button type="primary" :disabled="!canExport" @click="exportData">导出</el-button>
      </div>
    </section>

    <el-alert v-if="!canExport" title="只有任务创建者和裁决者可以导出结果，标注者可查看所有结果。" type="info" show-icon :closable="false" style="margin-bottom: 14px" />
    <el-alert v-if="exportInfo" :title="`导出完成：${exportInfo.downloadUrl}`" type="success" show-icon style="margin-bottom: 14px" />

    <div class="result-layout">
      <section class="panel work-panel">
        <div class="toolbar">
          <h3>图示区</h3>
          <el-button @click="fullscreen = true">全屏</el-button>
        </div>
        <GraphView :propositions="selectedVersion.propositions || []" :relations="selectedVersion.relations || []" />
      </section>

      <section class="panel work-panel">
        <h3>原文参考区</h3>
        <el-select v-model="currentDocId" style="width: 100%; margin-bottom: 12px">
          <el-option v-for="item in review.documents" :key="item.document.id" :label="item.document.title" :value="item.document.id" />
        </el-select>
        <div class="document-text">{{ current?.document.content }}</div>
        <el-divider />
        <h3>命题列表</h3>
        <el-table :data="selectedVersion.propositions || []" size="small">
          <el-table-column prop="propId" label="编号" width="80" />
          <el-table-column prop="tag" label="标签" width="90" />
          <el-table-column prop="text" label="文本" />
        </el-table>
        <h3>关系列表</h3>
        <el-table :data="selectedVersion.relations || []" size="small">
          <el-table-column prop="relId" label="编号" width="80" />
          <el-table-column prop="type" label="关系" width="80" />
          <el-table-column label="公式"><template #default="{ row }">{{ row.type }}({{ row.source }}, {{ row.target }})</template></el-table-column>
        </el-table>
      </section>

      <aside class="panel work-panel">
        <h3>结果来源</h3>
        <div v-for="source in versions" :key="source.key" class="version-card" :class="{ active: selectedKey === source.key }" @click="selectedKey = source.key">
          <strong>{{ source.name }}</strong>
          <span>{{ source.propositions.length }} 命题 / {{ source.relations.length }} 关系</span>
        </div>
      </aside>
    </div>

    <el-dialog v-model="fullscreen" title="图示全屏预览" fullscreen>
      <GraphView :propositions="selectedVersion.propositions || []" :relations="selectedVersion.relations || []" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import client from '../api/client'
import GraphView from '../components/GraphView.vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const auth = useAuthStore()
const review = ref(null)
const detail = ref(null)
const currentDocId = ref(null)
const selectedKey = ref('')
const format = ref('zip')
const exportInfo = ref(null)
const fullscreen = ref(false)

const current = computed(() => review.value?.documents.find((d) => d.document.id === currentDocId.value))
const versions = computed(() => {
  const list = (current.value?.annotatorResults || []).map((result) => ({ key: `u-${result.userId}`, name: `标注员 ${result.userId}`, ...result }))
  if (current.value?.finalResult && typeof current.value.finalResult === 'object') list.push({ key: 'final', name: '最终裁定结果', ...current.value.finalResult })
  return list
})
const selectedVersion = computed(() => versions.value.find((v) => v.key === selectedKey.value) || versions.value[0] || { propositions: [], relations: [] })
const canExport = computed(() => auth.user?.canCreateTask || detail.value?.reviewer.id === auth.user?.id)

watch(currentDocId, () => {
  selectedKey.value = versions.value[0]?.key || ''
})

async function load() {
  const taskId = route.params.taskId
  const [reviewData, detailData] = await Promise.all([client.get(`/reviews/${taskId}`), client.get(`/tasks/${taskId}`)])
  review.value = reviewData
  detail.value = detailData
  const queryDocId = Number(route.query.dataId)
  const docIds = review.value.documents.map((d) => d.document.id)
  currentDocId.value = docIds.includes(queryDocId) ? queryDocId : review.value.documents[0]?.document.id
  selectedKey.value = versions.value[0]?.key || ''
}

async function exportData() {
  exportInfo.value = await client.get(`/tasks/${route.params.taskId}/export`, { params: { format: format.value } })
}

onMounted(load)
</script>
