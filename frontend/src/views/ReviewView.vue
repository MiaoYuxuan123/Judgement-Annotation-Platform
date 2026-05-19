<template>
  <div v-if="review">
    <section class="panel annotate-topbar">
      <div>
        <el-button text @click="$router.push(`/tasks/${route.params.taskId}/data`)">← 返回数据选择</el-button>
        <strong>{{ review.task.taskName }} — 裁定界面</strong>
        <span class="muted">右侧选择标注版本，左侧图示与中间列表实时切换。</span>
      </div>
      <el-select v-model="currentDocId" style="width: 280px">
        <el-option v-for="item in review.documents" :key="item.document.id" :label="item.document.title" :value="item.document.id" />
      </el-select>
    </section>

    <div v-if="current" class="review-workbench">
      <section class="panel work-panel">
        <div class="toolbar">
          <h3>图示区</h3>
          <el-button @click="fullscreen = true">全屏</el-button>
        </div>
        <GraphView :propositions="selectedVersion.propositions || []" :relations="selectedVersion.relations || []" />
      </section>

      <section class="panel work-panel">
        <h3>原文参考区</h3>
        <div class="document-text">{{ current.document.content }}</div>
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
        <h3>标注员版本</h3>
        <div
          v-for="result in versions"
          :key="result.key"
          class="version-card"
          :class="{ active: selectedKey === result.key }"
          @click="selectedKey = result.key"
        >
          <strong>{{ result.name }}</strong>
          <span>{{ result.propositions.length }} 命题 / {{ result.relations.length }} 关系</span>
        </div>
        <el-divider />
        <el-button type="primary" style="width: 100%" :disabled="!selectedVersion.userId" @click="adopt">全部采纳</el-button>
        <el-button style="width: 100%; margin: 8px 0 0" :disabled="!selectedVersion.userId" @click="partialEdit">部分修改</el-button>
      </aside>
    </div>

    <el-dialog v-model="fullscreen" title="裁定图示全屏预览" fullscreen>
      <GraphView :propositions="selectedVersion.propositions || []" :relations="selectedVersion.relations || []" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import client from '../api/client'
import GraphView from '../components/GraphView.vue'

const route = useRoute()
const router = useRouter()
const review = ref(null)
const currentDocId = ref(Number(route.query.dataId) || null)
const selectedKey = ref('')
const fullscreen = ref(false)

const current = computed(() => review.value?.documents.find((d) => d.document.id === currentDocId.value))
const versions = computed(() => {
  const base = (current.value?.annotatorResults || []).map((result) => ({
    key: `u-${result.userId}`,
    name: `标注员 ${result.userId}`,
    ...result
  }))
  if (current.value?.finalResult && typeof current.value.finalResult === 'object') {
    base.push({ key: 'final', name: '最终裁定结果', ...current.value.finalResult })
  }
  return base
})
const selectedVersion = computed(() => versions.value.find((v) => v.key === selectedKey.value) || versions.value[0] || { propositions: [], relations: [] })

watch(currentDocId, () => {
  selectedKey.value = versions.value[0]?.key || ''
})

async function load() {
  review.value = await client.get(`/reviews/${route.params.taskId}`)
  currentDocId.value = currentDocId.value || review.value.documents[0]?.document.id
  selectedKey.value = versions.value[0]?.key || ''
}

async function adopt() {
  await client.post('/reviews/adopt', { taskId: Number(route.params.taskId), dataId: currentDocId.value, annotatorId: selectedVersion.value.userId })
  ElMessage.success('已将该版本转存为最终裁定结果')
  await load()
}

function partialEdit() {
  router.push(`/annotate/${route.params.taskId}/${currentDocId.value}`)
}

onMounted(load)
</script>
