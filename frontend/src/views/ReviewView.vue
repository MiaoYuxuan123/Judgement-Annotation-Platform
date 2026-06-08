<template>
  <div v-if="review" class="review-page">
    <header class="review-header">
      <div class="review-header-left">
        <el-button class="review-back-btn" @click="$router.push(`/tasks/${taskId}/data`)">← 返回数据列表</el-button>
        <span class="review-logo">⚖</span>
        <h1>裁定界面</h1>
        <el-button v-if="guideAttachmentName" size="small" type="warning" style="margin-left:12px" @click="openAttachment">查看当前指南</el-button>
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
      <aside class="review-lists">
        <div class="review-block">
          <h3>命题列表</h3>
          <el-table :data="activeData.propositions" size="small" stripe empty-text="暂无命题">
            <el-table-column label="序号" width="72" align="center">
              <template #default="{ row }">{{ circledNo(row.sequenceNo) }}</template>
            </el-table-column>
            <el-table-column prop="text" label="命题内容" min-width="120" show-overflow-tooltip />
            <el-table-column prop="tag" label="类型" width="72" align="center" />
          </el-table>
        </div>

        <div class="review-block">
          <h3>关系列表</h3>
          <el-table :data="relationRows" size="small" stripe empty-text="暂无关系">
            <el-table-column label="序号" width="72" align="center">
              <template #default="{ $index }">R{{ $index + 1 }}</template>
            </el-table-column>
            <el-table-column label="关系内容" min-width="120">
              <template #default="{ row }">
                <code class="review-formula">{{ row.formula }}</code>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </aside>

      <main class="review-main" :class="{ 'graph-collapsed': !graphPanelVisible }">
        <div class="review-block review-text-block">
          <div class="review-text-head">
            <h3>原文展示区</h3>
            <el-button link type="primary" @click="toggleGraphPanel">
              {{ graphPanelVisible ? '收起图示' : '展开图示' }}
            </el-button>
          </div>
          <div class="review-original source-text">
            <template v-for="(part, idx) in annotatedParts" :key="idx">
              <template v-if="part.type === 'text'">{{ part.text }}</template>
              <template v-else>
                <mark class="annotation-mark">{{ part.label }}</mark><span class="annotation-text">{{ part.text }}</span>
              </template>
            </template>
          </div>
        </div>

        <div v-show="graphPanelVisible" class="review-block review-graph-block">
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
      </main>

      <aside class="review-sidebar">
        <h3>标注员列表</h3>
        <div class="review-sidebar-list">
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
        </div>
        <footer v-if="showPendingActions" class="review-actions review-sidebar-actions">
          <p v-if="!allAnnotatorsSubmitted" class="review-action-hint">
            尚有 {{ pendingAnnotatorHint }} 未提交，可暂存裁定草稿；全部提交后可确认最终裁定。
          </p>
          <el-button @click="cancelPending">取消</el-button>
          <el-button type="primary" :disabled="!canConfirmFinal" @click="confirmFinal">确认</el-button>
        </footer>
        <footer v-else class="review-actions review-sidebar-actions">
          <p v-if="hasPartialSubmission" class="review-action-hint">
            已提交 {{ submittedAnnotatorCount }}/{{ annotatorCount }}，全部提交后可「全部采纳」或确认最终裁定。
          </p>
          <el-button :disabled="!canAdopt" @click="adoptAll">全部采纳</el-button>
          <el-button type="danger" :disabled="!canReject" @click="openRejectDialog">不予采纳</el-button>
          <el-button type="primary" :disabled="!canEdit" @click="partialModify">部分修改</el-button>
        </footer>
      </aside>
    </div>

    <el-dialog
      v-model="rejectDialogVisible"
      title="不予采纳"
      width="520px"
      class="reject-reason-dialog"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="resetRejectDialog"
    >
      <p class="reject-dialog-hint">请填写不予采纳理由，标注员将看到该理由并重新标注。</p>
      <el-input
        v-model="rejectReasonInput"
        type="textarea"
        class="reject-dialog-textarea"
        :rows="4"
        placeholder="请输入理由..."
        resize="none"
      />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectSubmitting" @click="confirmReject">确认退回</el-button>
      </template>
    </el-dialog>
  </div>
  <el-dialog v-model="previewVisible" title="指南附件预览" width="80%" :close-on-click-modal="false">
    <div style="height:70vh">
      <template v-if="isPreviewable">
        <iframe v-if="previewUrl" :key="previewUrl" :src="previewUrl" style="width:100%;height:100%;border:none" />
      </template>
      <div v-else style="display:flex;align-items:center;justify-content:center;height:100%;color:#999;font-size:16px">
        该文件类型不支持在线预览，请下载后查看
      </div>
    </div>
    <template #footer>
      <el-button @click="previewVisible=false">关闭</el-button>
      <el-button type="primary" @click="downloadAttachment">下载</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/client'
import { buildAnnotatedParts, circledNo, formatRelationFormula } from '../utils/reviewHelpers'
import { allAnnotatorsSubmitted as checkAllAnnotatorsSubmitted, countSubmittedAnnotators } from '../utils/taskRows'

const route = useRoute()
const router = useRouter()
const taskId = computed(() => Number(route.params.taskId))

const review = ref(null)
const taskDetail = ref(null)
const currentDocId = ref(null)
const guideAttachmentName = ref(''), guideVersionId = ref(null)
const previewVisible = ref(false), previewUrl = ref('')
const isPreviewable = computed(() => guideAttachmentName.value.toLowerCase().endsWith('.pdf') || guideAttachmentName.value.toLowerCase().endsWith('.txt'))
const selectedKey = ref('')
const graphPanelVisible = ref(true)
const showGraph = ref(false)
const GraphCanvas = shallowRef(null)
const rejectDialogVisible = ref(false)
const rejectReasonInput = ref('')
const rejectSubmitting = ref(false)

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
  if (!currentDoc.value) return { propositions: [], relations: [], userId: null, type: null, draft: true }
  if (selectedKey.value === 'final') {
    const f = currentDoc.value.finalResult
    return {
      propositions: f.propositions || [],
      relations: f.relations || [],
      userId: null,
      type: 'final',
      draft: false
    }
  }
  const userId = Number(selectedKey.value.replace('annotator-', ''))
  const result = currentDoc.value.annotatorResults.find((r) => r.userId === userId)
  return {
    propositions: result?.propositions || [],
    relations: result?.relations || [],
    userId: result?.userId,
    type: 'annotator',
    draft: Boolean(result?.draft)
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

const annotatorCount = computed(() => {
  if (currentDoc.value?.annotatorCount != null) return currentDoc.value.annotatorCount
  return taskDetail.value?.annotators?.length || 0
})

const submittedAnnotatorCount = computed(() => {
  if (currentDoc.value?.submittedAnnotatorCount != null) {
    return currentDoc.value.submittedAnnotatorCount
  }
  return countSubmittedAnnotators(currentDoc.value)
})

const allAnnotatorsSubmitted = computed(() => {
  if (currentDoc.value?.allAnnotatorsSubmitted != null) {
    return currentDoc.value.allAnnotatorsSubmitted
  }
  return checkAllAnnotatorsSubmitted(currentDoc.value, annotatorCount.value)
})

const hasPartialSubmission = computed(() =>
  submittedAnnotatorCount.value > 0 && !allAnnotatorsSubmitted.value
)

const pendingAnnotatorHint = computed(() => {
  const pending = annotatorCount.value - submittedAnnotatorCount.value
  return pending > 0 ? `${pending} 名标注员` : '标注员'
})

const showPendingActions = computed(() => selectedKey.value === 'final' && pendingFinal.value)

const canAdopt = computed(() =>
  allAnnotatorsSubmitted.value &&
  activeData.value.type === 'annotator' &&
  activeData.value.userId &&
  !activeData.value.draft
)
const canReject = computed(() =>
  activeData.value.type === 'annotator' && activeData.value.userId && !activeData.value.draft
)
const canEdit = computed(() => {
  if (selectedKey.value === 'final' && pendingFinal.value) return true
  return canReject.value
})
const canConfirmFinal = computed(() => showPendingActions.value && allAnnotatorsSubmitted.value)

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
  if (!graphPanelVisible.value || !selectedKey.value || !activeData.value.propositions?.length) return
  await waitForLayout()
  if (!GraphCanvas.value) {
    GraphCanvas.value = (await import('../components/GraphCanvas.vue')).default
  }
  showGraph.value = true
}

function toggleGraphPanel() {
  graphPanelVisible.value = !graphPanelVisible.value
  if (graphPanelVisible.value) {
    refreshGraph()
  } else {
    showGraph.value = false
  }
}

watch([currentDocId, selectedKey, () => activeData.value.propositions.length, graphPanelVisible], refreshGraph, { flush: 'post' })

async function load() {
  showGraph.value = false
  const [reviewData, detail] = await Promise.all([
    client.get(`/reviews/${taskId.value}`),
    client.get(`/tasks/${taskId.value}`)
  ])
  review.value = reviewData
  taskDetail.value = detail
  const snap = detail.configSnapshot
  guideVersionId.value = snap?.id || null
  guideAttachmentName.value = snap?.attachmentName || ''
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

function openRejectDialog() {
  if (!canReject.value) return
  rejectReasonInput.value = ''
  rejectDialogVisible.value = true
}

function resetRejectDialog() {
  rejectReasonInput.value = ''
  rejectSubmitting.value = false
}

async function confirmReject() {
  const reason = rejectReasonInput.value.trim()
  if (!reason) {
    ElMessage.warning('请填写理由')
    return
  }
  rejectSubmitting.value = true
  try {
    await client.post('/reviews/reject', {
      taskId: taskId.value,
      dataId: currentDocId.value,
      annotatorId: activeData.value.userId,
      reason
    })
    rejectDialogVisible.value = false
    ElMessage.success('已退回，等待标注员重新标注')
    router.push(`/tasks/${taskId.value}/data`)
  } finally {
    rejectSubmitting.value = false
  }
}

async function confirmFinal() {
  if (!canConfirmFinal.value) return
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

function openAttachment() {
  if (!guideVersionId.value || !guideAttachmentName.value) return
  const token = localStorage.getItem('jap_token')
  previewUrl.value = `/api/configs/versions/${guideVersionId.value}/attachment?token=${token}&t=${Date.now()}`
  previewVisible.value = true
}

function downloadAttachment() {
  if (!previewUrl.value || !guideAttachmentName.value) return
  const a = document.createElement('a')
  a.href = previewUrl.value
  a.download = guideAttachmentName.value
  a.click()
}

onMounted(load)
</script>

<style scoped>
.review-action-hint {
  margin: 0 0 10px;
  font-size: 12px;
  line-height: 1.5;
  color: #92400e;
}
</style>
