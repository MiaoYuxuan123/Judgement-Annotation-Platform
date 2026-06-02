<template>
  <div v-if="review" class="review-page results-page">
    <header class="review-header">
      <div class="review-header-left">
        <el-button class="review-back-btn" @click="$router.push(`/tasks/${taskId}/data`)">← 返回数据列表</el-button>
        <span class="review-logo">📊</span>
        <h1>结果查看 / 导出</h1>
      </div>
      <div class="review-header-right">
        <el-select v-model="currentDocId" class="review-doc-select" placeholder="选择文书">
          <el-option
            v-for="item in review.documents"
            :key="item.document.id"
            :label="item.document.title"
            :value="item.document.id"
          />
        </el-select>
        <el-button type="primary" :disabled="!canExport || exporting" :loading="exporting" @click="exportData">
          导出 ZIP
        </el-button>
      </div>
    </header>

    <el-alert
      v-if="!canExport"
      class="results-alert"
      title="只有任务创建者和裁决者可以导出结果，标注者可查看所有版本。"
      type="info"
      show-icon
      :closable="false"
    />
    <el-alert
      v-if="exportMessage"
      class="results-alert"
      :title="exportMessage"
      type="success"
      show-icon
      @close="exportMessage = ''"
    />

    <div class="review-body">
      <section class="review-left">
        <div class="review-block">
          <h3>原文展示区</h3>
          <div class="review-original source-text">
            <template v-for="(part, idx) in annotatedParts" :key="idx">
              <template v-if="part.type === 'text'">{{ part.text }}</template>
              <template v-else>
                <mark class="annotation-mark">{{ part.label }}</mark><span class="annotation-text">{{ part.text }}</span>
              </template>
            </template>
          </div>
        </div>

        <div class="review-block review-graph-block">
          <div class="review-block-title">
            <h3>图示区</h3>
          </div>
          <component
            :is="GraphCanvas"
            v-if="showGraph && GraphCanvas"
            :key="`${currentDocId}-${selectedKey}`"
            :propositions="activeData.propositions"
            :relations="activeData.relations"
          />
        </div>
      </section>

      <section class="review-center results-center">
        <div class="review-block">
          <h3>命题列表</h3>
          <el-table :data="activeData.propositions" size="small" stripe empty-text="暂无命题">
            <el-table-column label="命题序号" width="100" align="center">
              <template #default="{ row }">{{ circledNo(row.sequenceNo) }}</template>
            </el-table-column>
            <el-table-column prop="text" label="命题内容" min-width="220" show-overflow-tooltip />
            <el-table-column prop="tag" label="命题类型" width="100" align="center" />
          </el-table>
        </div>

        <div class="review-block">
          <h3>关系列表</h3>
          <el-table :data="relationRows" size="small" stripe empty-text="暂无关系">
            <el-table-column label="关系序号" width="100" align="center">
              <template #default="{ $index }">R{{ $index + 1 }}</template>
            </el-table-column>
            <el-table-column label="关系内容" min-width="260">
              <template #default="{ row }">
                <code class="review-formula">{{ row.formula }}</code>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>

      <aside class="review-sidebar">
        <h3>结果来源</h3>
        <button
          v-for="item in sidebarItems"
          :key="item.key"
          type="button"
          class="review-sidebar-item"
          :class="{ active: selectedKey === item.key }"
          @click="selectedKey = item.key"
        >
          <span class="review-sidebar-icon">{{ item.icon }}</span>
          <span class="review-sidebar-meta">
            <span class="review-sidebar-label">{{ item.label }}</span>
            <span class="review-sidebar-count">{{ item.countText }}</span>
          </span>
        </button>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, shallowRef, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElLoading, ElMessage } from 'element-plus'
import client from '../api/client'
import { useAuthStore } from '../stores/auth'
import { buildAnnotatedParts, circledNo, formatRelationFormula } from '../utils/reviewHelpers'
import { exportTaskZip } from '../utils/taskZipExport'

const route = useRoute()
const auth = useAuthStore()
const taskId = computed(() => Number(route.params.taskId))

const review = ref(null)
const taskDetail = ref(null)
const currentDocId = ref(null)
const selectedKey = ref('')
const exportMessage = ref('')
const exporting = ref(false)
const showGraph = ref(false)
const GraphCanvas = shallowRef(null)

const currentDoc = computed(() => review.value?.documents.find((d) => d.document.id === currentDocId.value))

const annotatorNameMap = computed(() => {
  const map = new Map()
  for (const a of taskDetail.value?.annotators || []) {
    map.set(a.id, a.realName || a.username)
  }
  return map
})

const sidebarItems = computed(() => {
  const items = (currentDoc.value?.annotatorResults || []).map((r, index) => {
    const props = r.propositions || []
    const rels = r.relations || []
    return {
      key: `annotator-${r.userId}`,
      label: annotatorNameMap.value.get(r.userId) || `标注员 ${String.fromCharCode(65 + index)}`,
      icon: '👤',
      countText: `${props.length} 命题 / ${rels.length} 关系`
    }
  })
  const final = currentDoc.value?.finalResult
  if (final && typeof final === 'object' && final.propositions && final.finalResult !== false) {
    const props = final.propositions || []
    const rels = final.relations || []
    items.push({
      key: 'final',
      label: '最终裁定结果',
      icon: '✓',
      countText: `${props.length} 命题 / ${rels.length} 关系`
    })
  }
  return items
})

const activeData = computed(() => {
  if (!currentDoc.value) return { propositions: [], relations: [] }
  if (selectedKey.value === 'final') {
    const f = currentDoc.value.finalResult
    return {
      propositions: f.propositions || [],
      relations: f.relations || []
    }
  }
  const userId = Number(selectedKey.value.replace('annotator-', ''))
  const result = currentDoc.value.annotatorResults.find((r) => r.userId === userId)
  return {
    propositions: result?.propositions || [],
    relations: result?.relations || []
  }
})

const annotatedParts = computed(() => {
  const content = currentDoc.value?.document?.content || ''
  return buildAnnotatedParts(content, activeData.value.propositions)
})

const relationRows = computed(() =>
  (activeData.value.relations || []).map((rel, index) => ({
    relId: rel.relId || `R${index + 1}`,
    formula: formatRelationFormula(rel, activeData.value.propositions, index)
  }))
)

const canExport = computed(() => auth.user?.canCreateTask || taskDetail.value?.reviewer?.id === auth.user?.id)

watch(sidebarItems, (items) => {
  if (!items.length) return
  if (!items.some((i) => i.key === selectedKey.value)) {
    selectedKey.value = items[0].key
  }
})

async function waitForLayout() {
  await nextTick()
  await new Promise((resolve) => {
    requestAnimationFrame(() => requestAnimationFrame(resolve))
  })
}

async function refreshGraph() {
  showGraph.value = false
  if (!selectedKey.value || !activeData.value.propositions?.length) return
  await waitForLayout()
  if (!GraphCanvas.value) {
    GraphCanvas.value = (await import('../components/GraphCanvas.vue')).default
  }
  showGraph.value = true
}

watch([currentDocId, selectedKey, () => activeData.value.propositions.length], refreshGraph, { flush: 'post' })

watch(
  () => route.query.dataId,
  (dataId) => {
    if (!dataId || !review.value?.documents?.length) return
    const docId = Number(dataId)
    const exists = review.value.documents.some((d) => d.document.id === docId)
    if (exists) currentDocId.value = docId
  }
)

function resolveInitialDocId(reviewData) {
  const queryDocId = route.query.dataId ? Number(route.query.dataId) : null
  if (queryDocId != null) {
    const matched = reviewData.documents.find((d) => d.document.id === queryDocId)
    if (matched) return matched.document.id
  }
  return reviewData.documents[0]?.document.id ?? null
}

async function load() {
  showGraph.value = false
  const [reviewData, detail] = await Promise.all([
    client.get(`/reviews/${taskId.value}`),
    client.get(`/tasks/${taskId.value}`)
  ])
  review.value = reviewData
  taskDetail.value = detail
  currentDocId.value = resolveInitialDocId(reviewData)
  if (sidebarItems.value.length) {
    selectedKey.value = sidebarItems.value.find((i) => i.key === 'final')?.key || sidebarItems.value[0].key
  }
}

async function exportData() {
  if (!canExport.value || exporting.value) return
  exporting.value = true
  exportMessage.value = ''
  const loading = ElLoading.service({
    lock: true,
    text: '正在准备导出…',
    background: 'rgba(255,255,255,0.7)'
  })
  try {
    const savedName = await exportTaskZip({
      review: review.value,
      taskDetail: taskDetail.value,
      currentDocId: currentDocId.value,
      onProgress: (text) => {
        if (text) loading.setText(text)
      }
    })
    exportMessage.value = `已保存：${savedName}`
    ElMessage.success('导出完成')
  } catch (err) {
    if (err?.name === 'AbortError') return
    ElMessage.error(err?.message || '导出失败')
  } finally {
    loading.close()
    exporting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.results-page .review-header-right {
  flex-wrap: wrap;
  row-gap: 8px;
}

.results-alert {
  flex-shrink: 0;
  margin: 0 14px 10px;
}

.results-center {
  border-right: none;
}

.review-sidebar-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  min-width: 0;
}

.review-sidebar-label {
  line-height: 1.3;
}

.review-sidebar-count {
  font-size: 11px;
  font-weight: 400;
  color: #94a3b8;
}

.review-sidebar-item.active .review-sidebar-count {
  color: #60a5fa;
}
</style>
