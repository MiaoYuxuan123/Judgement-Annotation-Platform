<template>
  <div v-if="review" class="review-page">
    <header class="review-header">
      <div class="review-header-left">
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
        <el-button text @click="$router.push(`/tasks/${taskId}`)">返回任务</el-button>
        <span class="review-user">{{ auth.user?.realName || '裁定者' }}</span>
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
          <GraphCanvas :propositions="activeData.propositions" :relations="activeData.relations" variant="circles" />
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

        <footer class="review-actions">
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
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/client'
import GraphCanvas from '../components/GraphCanvas.vue'
import { useAuthStore } from '../stores/auth'
import { buildAnnotatedParts, circledNo, formatRelationFormula } from '../utils/reviewHelpers'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const taskId = computed(() => Number(route.params.taskId))

const review = ref(null)
const taskDetail = ref(null)
const currentDocId = ref(null)
const selectedKey = ref('')

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
    items.push({ key: 'final', label: '裁定结果', icon: '✓', type: 'final' })
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
      formula: formatRelationFormula(rel, activeData.value.propositions)
    }))
)

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

async function load() {
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
  await load()
  const nextDoc = findNextPendingDoc()
  if (nextDoc) {
    currentDocId.value = nextDoc
    await load()
    ElMessage.info('已加载下一份待裁定文书')
  } else {
    ElMessage.success('本任务裁定已完成')
    router.push('/tasks')
  }
}

function findNextPendingDoc() {
  const docs = review.value?.documents || []
  const pending = docs.filter((d) => !d.finalResult || typeof d.finalResult !== 'object' || !d.finalResult.propositions)
  return pending.find((d) => d.document.id !== currentDocId.value)?.document.id
}

function partialModify() {
  const query = {
    mode: 'arbitration',
    returnTo: `/review/${taskId.value}?docId=${currentDocId.value}`
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
