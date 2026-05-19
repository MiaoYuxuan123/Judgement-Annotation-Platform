<template>
  <div class="annotate-layout" v-if="data">
    <section class="panel work-panel">
      <div class="toolbar">
        <div>
          <h3>{{ data.document.title }}</h3>
          <p class="muted">选中文本后点击“生成命题”，原文不会被修改。</p>
        </div>
        <el-button type="primary" @click="createProposition">生成命题</el-button>
      </div>
      <div class="document-text" ref="textRef">{{ data.document.content }}</div>
      <h4>命题列表</h4>
      <div v-for="p in propositions" :key="p.propId" class="prop-item">
        <strong>P{{ p.sequenceNo }} · {{ p.tag }}</strong>
        <div>{{ p.text }}</div>
      </div>
      <el-empty v-if="!propositions.length" description="暂无命题，先在原文中选取一段文字" />
    </section>

    <section class="panel work-panel">
      <h3>标注操作</h3>
      <el-form label-position="top">
        <el-form-item label="当前命题">
          <el-select v-model="selectedPropId" style="width: 100%" placeholder="选择命题">
            <el-option v-for="p in propositions" :key="p.propId" :label="`P${p.sequenceNo} ${p.text}`" :value="p.propId" />
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
        <el-form-item label="来源命题"><el-select v-model="relationForm.source" style="width: 100%"><el-option v-for="p in propositions" :key="p.propId" :label="`P${p.sequenceNo}`" :value="p.propId" /></el-select></el-form-item>
        <el-form-item label="目标命题"><el-select v-model="relationForm.target" style="width: 100%"><el-option v-for="p in propositions" :key="p.propId" :label="`P${p.sequenceNo}`" :value="p.propId" /></el-select></el-form-item>
        <el-button @click="addRelation">添加关系</el-button>
      </el-form>
      <el-table :data="relations" size="small" style="margin-top: 12px">
        <el-table-column prop="type" label="类型" width="70" />
        <el-table-column prop="source" label="来源" />
        <el-table-column prop="target" label="目标" />
      </el-table>
      <el-divider />
      <el-button type="primary" style="width: 100%" @click="submit(false)">提交标注</el-button>
      <el-button style="width: 100%; margin: 8px 0 0" @click="submit(true)">暂存草稿</el-button>
    </section>

    <section class="panel work-panel">
      <div class="toolbar">
        <h3>论证图示</h3>
        <el-tag>{{ propositions.length }} 命题 / {{ relations.length }} 关系</el-tag>
      </div>
      <GraphView :propositions="propositions" :relations="relations" />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import client from '../api/client'
import GraphView from '../components/GraphView.vue'

const route = useRoute()
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

async function load() {
  data.value = await client.get(`/tasks/${route.params.taskId}/items/${route.params.dataId}`)
  propositions.value = [...(data.value.annotation.propositions || [])]
  relations.value = [...(data.value.annotation.relations || [])]
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
  relations.value.push({ relId: `R${Date.now()}`, type: relationForm.type, source: relationForm.source, target: relationForm.target })
}

async function submit(isDraft) {
  await client.post('/annotations/submit', {
    taskId: Number(route.params.taskId),
    dataId: Number(route.params.dataId),
    propositions: propositions.value,
    relations: relations.value,
    isDraft
  })
  ElMessage.success(isDraft ? '草稿已暂存' : '标注已提交')
}

onMounted(load)
</script>
