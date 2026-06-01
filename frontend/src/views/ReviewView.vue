<template>
  <div v-if="review" class="review-page">
    <header class="review-header">
      <div class="review-header-left">
        <el-button class="review-back-btn" @click="$router.push(`/tasks/${taskId}/data`)">← 返回数据列表</el-button>
        <span class="review-logo">⚖</span>
        <h1>裁定界面</h1>
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
      </div>
    </header>

    <div class="review-body">
      <section class="review-left">
        <div class="review-block">
          <h3>原文展示区</h3>
          <div class="review-original">
            <template v-for="(part, idx) in annotatedParts" :key="idx">
              <span v-if="part.type === 'text'">{{ part.text }}</span>
              <span v-else class="review-prop-inline">
                <sup class="review-prop-badge">{{ circledNo(part.sequenceNo) }}</sup>{{ part.text }}
              </span>
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

      <section class="review-center">
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

        <footer v-if="showPendingActions" class="review-actions">
          <el-button size="large" @click="cancelPending">取消</el-button>
          <el-button type="primary" size="large" @click="confirmFinal">确认</el-button>
        </footer>
        <footer v-else class="review-actions">
          <el-button size="large" :disabled="!canAdopt" @click="adoptAll">全部采纳</el-button>
          <el-button type="primary" size="large" :disabled="!canEdit" @click="partialModify">部分修改</el-button>
        </footer>
      </section>

      <aside class="review-sidebar">
        <h3>标注员列表</h3>
        <button
            v-for="item in sidebarItems"
            :key="item.key"
            type="button"
            class="review-sidebar-item"
            :class="{ active: selectedKey === item.key }"
            @click="selectedKey = item.key"
        >
          <span class="review-sidebar-icon">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </button>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/client'
import { buildAnnotatedParts, circledNo, formatRelationFormula } from '../utils/reviewHelpers'

const route = useRoute()
const router = useRouter()
const taskId = computed(() => Number(route.params.taskId))

const review = ref(null)
const taskDetail = ref(null)
const currentDocId = ref(null)
const selectedKey = ref('')
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
  const items = (currentDoc.value?.annotatorResults || []).map((r, index) => ({
    key: `annotator-${r.userId}`,
    label: annotatorNameMap.value.get(r.userId) || `标注员 ${String.fromCharCode(65 + index)}`,
    icon: '👤',
    type: 'annotator',
    userId: r.userId
  }))
  const final = currentDoc.value?.finalResult
  if (final && typeof final === 'object' && final.propositions) {
    const pending = final.finalResult === false
    items.push({
      key: 'final',
      label: pending ? '裁定结果（待确认）' : '裁定结果',
      icon: pending ? '⏳' : '✓',
      type: 'final',
      pending
    })
  }
  return items
})

const activeData = computed(() => {
  if (!currentDoc.value) return { propositions: [], relations: [], userId: null, type: null }
  if (selectedKey.value === 'final') {
    const f = currentDoc.value.finalResult
    return {
      propositions: f.propositions || [],
      relations: f.relations || [],
      userId: null,
      type: 'final'
    }
  }
  const userId = Number(selectedKey.value.replace('annotator-', ''))
  const result = currentDoc.value.annotatorResults.find((r) => r.userId === userId)
  return {
    propositions: result?.propositions || [],
    relations: result?.relations || [],
    userId: result?.userId,
    type: 'annotator'
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

const pendingFinal = computed(() => {
  const f = currentDoc.value?.finalResult
  return f && typeof f === 'object' && f.propositions && f.finalResult === false
})

const showPendingActions = computed(() => selectedKey.value === 'final' && pendingFinal.value)

const canAdopt = computed(() => activeData.value.type === 'annotator' && activeData.value.userId)
const canEdit = computed(() => selectedKey.value && activeData.value.propositions.length >= 0)

watch(sidebarItems, (items) => {
  if (!items.length) return
  if (!items.some((i) => i.key === selectedKey.value)) {
    selectedKey.value = items[0].key
  }
})

watch(
    () => route.query.docId,
    (docId) => {
      if (docId) currentDocId.value = Number(docId)
    }
)

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

async function load() {
  showGraph.value = false
  const [reviewData, detail] = await Promise.all([
    client.get(`/reviews/${taskId.value}`),
    client.get(`/tasks/${taskId.value}`)
  ])
  review.value = reviewData
  taskDetail.value = detail
  const queryDoc = route.query.docId ? Number(route.query.docId) : null
  currentDocId.value = queryDoc || reviewData.documents[0]?.document.id
  if (route.query.select === 'final' && currentDoc.value?.finalResult) {
    selectedKey.value = 'final'
  } else if (sidebarItems.value.length) {
    selectedKey.value = sidebarItems.value[0].key
  }
}

async function adoptAll() {
  if (!canAdopt.value) return
  await ElMessageBox.confirm('确认将该标注员结果作为最终裁定版？', '全部采纳', { type: 'warning' })
  await client.post('/reviews/adopt', {
    taskId: taskId.value,
    dataId: currentDocId.value,
    annotatorId: activeData.value.userId
  })
  ElMessage.success('已采纳为最终裁定版')
  router.push(`/tasks/${taskId.value}/data`)
}

async function confirmFinal() {
  if (!showPendingActions.value) return
  await ElMessageBox.confirm('确认将该裁定结果作为最终版？', '确认裁定', { type: 'warning' })
  await client.post('/reviews/confirm', { taskId: taskId.value, dataId: currentDocId.value })
  ElMessage.success('裁定结果已确认')
  router.push(`/tasks/${taskId.value}/data`)
}

async function cancelPending() {
  if (!showPendingActions.value) return
  await ElMessageBox.confirm('取消后将丢弃本次部分修改的裁定草稿，是否继续？', '取消裁定', { type: 'warning' })
  await client.post('/reviews/cancel-pending', { taskId: taskId.value, dataId: currentDocId.value })
  ElMessage.info('已取消待确认的裁定结果')
  await load()
  if (sidebarItems.value.length) {
    selectedKey.value = sidebarItems.value.find((i) => i.type === 'annotator')?.key || sidebarItems.value[0].key
  }
}

function partialModify() {
  const query = {
    mode: 'arbitration',
    returnTo: `/review/${taskId.value}?docId=${currentDocId.value}&select=final`
  }
  if (activeData.value.type === 'annotator' && activeData.value.userId) {
    query.fromUserId = String(activeData.value.userId)
  } else if (selectedKey.value === 'final') {
    query.fromFinal = '1'
  }
  router.push({
    path: `/annotate/${taskId.value}/${currentDocId.value}`,
    query
  })
}

onMounted(load)
</script>
