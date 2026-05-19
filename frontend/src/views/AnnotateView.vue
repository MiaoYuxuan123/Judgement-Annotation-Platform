<template>
  <div class="annotate-layout" v-if="data">
    <section class="panel work-panel">
      <div class="toolbar">
        <div>
          <h3>{{ data.document.title }}</h3>
          <p class="muted">
            {{ isArbitration ? '裁定修改模式：在标注员结果基础上编辑后提交为最终裁定。' : '选中文本后点击“生成命题”，原文不会被修改。' }}
          </p>
        </div>
        <el-button type="primary" @click="createProposition">生成命题</el-button>
        <el-button v-if="isArbitration" @click="goBackReview">返回裁定</el-button>
      </div>
      <div class="document-text" ref="textRef">{{ data.document.content }}</div>
      <h4>命题列表</h4>
      <div v-for="p in propositions" :key="p.propId" class="prop-item">
        <strong>{{ circledNo(p.sequenceNo) }} · {{ p.tag }}</strong>
        <div>{{ p.text }}</div>
      </div>
      <el-empty v-if="!propositions.length" description="暂无命题" />
    </section>

    <section class="panel work-panel">
      <h3>{{ isArbitration ? '裁定编辑' : '标注操作' }}</h3>
      <el-form label-position="top">
        <el-form-item label="当前命题">
          <el-select v-model="selectedPropId" style="width: 100%" placeholder="选择命题">
            <el-option v-for="p in propositions" :key="p.propId" :label="`${circledNo(p.sequenceNo)} ${p.text}`" :value="p.propId" />
          </el-select>
        </el-form-item>
        <el-form-item label="命题标签">
          <el-select v-model="selectedTag" style="width: 100%" @change="updateTag">
            <el-option v-for="tag in allTags" :key="tag.shortName" :label="`${tag.shortName} · ${tag.name}`" :value="tag.shortName" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-divider />
      <h4>关系标注</h4>
      <el-form label-position="top">
        <el-form-item label="关系类型">
          <el-select v-model="relationForm.type" style="width: 100%">
            <el-option v-for="tag in data.config.relationTypes" :key="tag.shortName" :label="`${tag.shortName} · ${tag.name}`" :value="tag.shortName" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源命题">
          <el-select v-model="relationForm.source" style="width: 100%">
            <el-option v-for="p in propositions" :key="p.propId" :label="circledNo(p.sequenceNo)" :value="p.propId" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标命题">
          <el-select v-model="relationForm.target" style="width: 100%">
            <el-option v-for="p in propositions" :key="p.propId" :label="circledNo(p.sequenceNo)" :value="p.propId" />
          </el-select>
        </el-form-item>
        <el-button @click="addRelation">添加关系</el-button>
      </el-form>
      <el-table :data="relations" size="small" style="margin-top: 12px">
        <el-table-column prop="type" label="类型" width="70" />
        <el-table-column label="来源">
          <template #default="{ row }">{{ propLabel(row.source) }}</template>
        </el-table-column>
        <el-table-column label="目标">
          <template #default="{ row }">{{ propLabel(row.target) }}</template>
        </el-table-column>
      </el-table>
      <el-divider />
      <el-button type="primary" style="width: 100%" @click="submit(false)">
        {{ isArbitration ? '提交裁定结果' : '提交标注' }}
      </el-button>
      <el-button v-if="!isArbitration" style="width: 100%; margin: 8px 0 0" @click="submit(true)">暂存草稿</el-button>
    </section>

    <section class="panel work-panel">
      <div class="toolbar">
        <h3>论证图示</h3>
        <el-tag>{{ propositions.length }} 命题 / {{ relations.length }} 关系</el-tag>
      </div>
      <GraphView :propositions="propositions" :relations="relations" variant="cards" />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import client from '../api/client'
import GraphView from '../components/GraphView.vue'
import { circledNo, propByIdMap } from '../utils/reviewHelpers'

const route = useRoute()
const router = useRouter()
const isArbitration = computed(() => route.query.mode === 'arbitration')

const data = ref(null)
const propositions = ref([])
const relations = ref([])
const selectedPropId = ref('')
const selectedTag = ref('SF')
const relationForm = reactive({ type: 'S', source: '', target: '' })

const allTags = computed(() => [...(data.value?.config.primaryTags || []), ...(data.value?.config.secondaryTags || [])])

watch(selectedPropId, () => {
  const prop = propositions.value.find((p) => p.propId === selectedPropId.value)
  if (prop) selectedTag.value = prop.tag
})

function propLabel(propId) {
  const p = propByIdMap(propositions.value).get(propId)
  return p ? circledNo(p.sequenceNo) : propId
}

function goBackReview() {
  const returnTo = route.query.returnTo || `/review/${route.params.taskId}`
  router.push(returnTo)
}

async function loadArbitrationSource() {
  const review = await client.get(`/reviews/${route.params.taskId}`)
  const doc = review.documents.find((d) => d.document.id === Number(route.params.dataId))
  if (!doc) return

  if (route.query.fromFinal === '1' && doc.finalResult?.propositions) {
    propositions.value = [...doc.finalResult.propositions]
    relations.value = [...(doc.finalResult.relations || [])]
    return
  }

  const fromUserId = Number(route.query.fromUserId)
  const result = doc.annotatorResults.find((r) => r.userId === fromUserId)
  if (result) {
    propositions.value = [...(result.propositions || [])]
    relations.value = [...(result.relations || [])]
  }
}

async function load() {
  data.value = await client.get(`/tasks/${route.params.taskId}/items/${route.params.dataId}`)
  if (isArbitration.value) {
    await loadArbitrationSource()
  } else {
    propositions.value = [...(data.value.annotation.propositions || [])]
    relations.value = [...(data.value.annotation.relations || [])]
  }
  if (propositions.value.length) selectedPropId.value = propositions.value[0].propId
}

function createProposition() {
  const selection = window.getSelection()?.toString().trim()
  if (!selection) {
    ElMessage.warning('请先在左侧原文中选中文本')
    return
  }
  const start = data.value.document.content.indexOf(selection)
  const prop = {
    propId: `P${Date.now()}`,
    sequenceNo: propositions.value.length + 1,
    startPos: start < 0 ? propositions.value.length * 10 : start,
    endPos: start < 0 ? propositions.value.length * 10 + selection.length : start + selection.length,
    text: selection,
    tag: selectedTag.value || 'SF'
  }
  propositions.value.push(prop)
  reorder()
  selectedPropId.value = prop.propId
}

function reorder() {
  propositions.value.sort((a, b) => a.startPos - b.startPos)
  propositions.value = propositions.value.map((p, i) => ({ ...p, sequenceNo: i + 1 }))
}

function updateTag() {
  const index = propositions.value.findIndex((p) => p.propId === selectedPropId.value)
  if (index >= 0) propositions.value[index] = { ...propositions.value[index], tag: selectedTag.value }
}

function addRelation() {
  if (!relationForm.source || !relationForm.target || relationForm.source === relationForm.target) {
    ElMessage.warning('请选择不同的来源和目标命题')
    return
  }
  relations.value.push({
    relId: `R${Date.now()}`,
    type: relationForm.type,
    source: relationForm.source,
    target: relationForm.target
  })
}

async function submit(isDraft) {
  const taskId = Number(route.params.taskId)
  const dataId = Number(route.params.dataId)

  if (isArbitration.value) {
    await client.post('/reviews/manual', {
      taskId,
      dataId,
      propositions: propositions.value,
      relations: relations.value
    })
    ElMessage.success('裁定结果已保存')
    const returnTo = route.query.returnTo || `/review/${taskId}?docId=${dataId}&select=final`
    router.push(returnTo)
    return
  }

  await client.post('/annotations/submit', {
    taskId,
    dataId,
    propositions: propositions.value,
    relations: relations.value,
    isDraft
  })
  ElMessage.success(isDraft ? '草稿已暂存' : '标注已提交')
}

onMounted(load)
</script>
