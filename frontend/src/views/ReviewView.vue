<template>
  <div v-if="review">
    <section class="panel" style="margin-bottom: 14px">
      <div class="toolbar">
        <div>
          <h3>{{ review.task.taskName }}</h3>
          <p class="muted">双标注结果并排展示，裁定者可采纳任一方或手动保存最终版本。</p>
        </div>
        <el-select v-model="currentDocId" style="width: 280px">
          <el-option v-for="item in review.documents" :key="item.document.id" :label="item.document.title" :value="item.document.id" />
        </el-select>
      </div>
    </section>
    <div v-if="current" class="review-grid">
      <section v-for="result in paddedResults" :key="result.userId || Math.random()" class="panel">
        <div class="toolbar">
          <h3>标注员 {{ result.userId || '-' }}</h3>
          <el-button type="primary" :disabled="!result.userId" @click="adopt(result)">全部采纳</el-button>
        </div>
        <el-table :data="result.propositions || []" size="small">
          <el-table-column prop="sequenceNo" label="#" width="50" />
          <el-table-column prop="tag" label="标签" width="90" />
          <el-table-column prop="text" label="命题" />
        </el-table>
        <GraphView :propositions="result.propositions || []" :relations="result.relations || []" style="margin-top: 12px" />
      </section>
    </div>
    <section v-if="current" class="panel" style="margin-top: 14px">
      <div class="toolbar">
        <h3>最终裁定编辑区</h3>
        <el-button type="success" @click="saveManual">保存最终裁定</el-button>
      </div>
      <el-alert title="MVP 中手动裁定默认使用左侧第一份标注作为基础，可在后续迭代中加入逐条编辑。" type="info" show-icon :closable="false" />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import client from '../api/client'
import GraphView from '../components/GraphView.vue'

const route = useRoute()
const review = ref(null)
const currentDocId = ref(null)

const current = computed(() => review.value?.documents.find((d) => d.document.id === currentDocId.value))
const paddedResults = computed(() => {
  const results = [...(current.value?.annotatorResults || [])]
  while (results.length < 2) results.push({ propositions: [], relations: [] })
  return results.slice(0, 2)
})

async function load() {
  review.value = await client.get(`/reviews/${route.params.taskId}`)
  currentDocId.value = review.value.documents[0]?.document.id
}

async function adopt(result) {
  await client.post('/reviews/adopt', { taskId: Number(route.params.taskId), dataId: currentDocId.value, annotatorId: result.userId })
  ElMessage.success('已采纳该标注结果')
  load()
}

async function saveManual() {
  const base = paddedResults.value[0]
  await client.post('/reviews/manual', { taskId: Number(route.params.taskId), dataId: currentDocId.value, propositions: base.propositions || [], relations: base.relations || [] })
  ElMessage.success('最终裁定已保存')
  load()
}

onMounted(load)
</script>
