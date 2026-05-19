<template>
  <div v-if="data">
    <section class="panel annotate-topbar">
      <div>
        <el-button text @click="$router.push(`/tasks/${route.params.taskId}/data`)">← 返回数据选择</el-button>
        <strong>{{ data.document.title }}</strong>
        <span class="muted">选中文本后自动选择标签，编号使用 P1、P2、P3...</span>
      </div>
      <div>
        <el-button @click="undo">撤回</el-button>
        <el-button @click="redo">重做</el-button>
        <el-button @click="submit(true)">暂存</el-button>
        <el-button type="primary" @click="submit(false)">提交</el-button>
      </div>
    </section>

    <div class="annotation-workbench">
      <aside class="panel work-panel">
        <h3>命题列表</h3>
        <div v-for="p in propositions" :key="p.propId" class="prop-item">
          <strong>{{ p.propId }} · {{ p.tag }}</strong>
          <div>{{ p.text }}</div>
        </div>
        <el-empty v-if="!propositions.length" description="暂无命题" />
        <el-divider />
        <h3>关系列表</h3>
        <div v-for="r in relations" :key="r.relId" class="prop-item">
          <strong>{{ r.relId }}</strong>
          <div>{{ r.type }}({{ r.source }}, {{ r.target }})</div>
        </div>
        <el-empty v-if="!relations.length" description="暂无关系" />
      </aside>

      <main class="center-work">
        <section class="panel work-panel">
          <div class="toolbar">
            <h3>文本标注区</h3>
            <el-tag>原文不可修改</el-tag>
          </div>
          <div class="document-text" @mouseup="handleSelection" v-html="markedHtml"></div>
        </section>

        <section class="panel relation-builder">
          <h3>关系生成区</h3>
          <div class="relation-buttons">
            <el-button v-for="rel in data.config.relationTypes" :key="rel.shortName" :type="relationForm.type === rel.shortName ? 'primary' : 'default'" @click="relationForm.type = rel.shortName">
              {{ rel.name }} ({{ rel.shortName }})
            </el-button>
          </div>
          <el-form :model="relationForm" inline style="margin-top: 14px">
            <el-form-item label="第一个命题/关系">
              <el-select v-model="relationForm.source" style="width: 180px">
                <el-option v-for="item in relationOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="第二个命题/关系">
              <el-select v-model="relationForm.target" style="width: 180px">
                <el-option v-for="item in relationOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="层级">
              <el-select v-model="relationForm.level" style="width: 110px">
                <el-option v-for="level in ['M1','M2','M3','M4']" :key="level" :label="level" :value="level" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button @click="clearRelation">清空</el-button>
              <el-button @click="clearRelation">撤销</el-button>
              <el-button type="primary" @click="addRelation">确认</el-button>
            </el-form-item>
          </el-form>
        </section>
      </main>

      <section class="panel work-panel">
        <div class="toolbar">
          <h3>图示区</h3>
          <el-button @click="fullscreen = true">全屏</el-button>
        </div>
        <GraphView :propositions="propositions" :relations="relations" />
      </section>
    </div>

    <el-dialog v-model="labelDialog" title="选择命题标签" width="360px" @close="selectedText=''">
      <el-radio-group v-model="primaryTag" class="label-grid">
        <el-radio-button v-for="tag in data.config.primaryTags" :key="tag.shortName" :label="tag.shortName">{{ tag.shortName }}</el-radio-button>
      </el-radio-group>
      <div v-if="primaryTag === 'GM'" class="tag-row" style="margin-top: 14px">
        <el-radio-group v-model="secondaryTag">
          <el-radio-button v-for="tag in data.config.secondaryTags" :key="tag.shortName" :label="tag.shortName">{{ tag.shortName }}</el-radio-button>
        </el-radio-group>
      </div>
      <p class="muted">选中文本：{{ selectedText }}</p>
      <template #footer>
        <el-button @click="cancelLabel">取消</el-button>
        <el-button type="primary" @click="confirmLabel">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="fullscreen" title="论证图示全屏预览" fullscreen>
      <GraphView :propositions="propositions" :relations="relations" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import client from '../api/client'
import GraphView from '../components/GraphView.vue'

const route = useRoute()
const router = useRouter()
const data = ref(null)
const propositions = ref([])
const relations = ref([])
const history = ref([])
const redoStack = ref([])
const labelDialog = ref(false)
const fullscreen = ref(false)
const selectedText = ref('')
const selectedStart = ref(0)
const primaryTag = ref('GM')
const secondaryTag = ref('GM-L')
const relationForm = reactive({ type: 'S', source: '', target: '', level: 'M1' })

const relationOptions = computed(() => [
  ...propositions.value.map((p) => ({ label: `${p.propId} ${p.text.slice(0, 12)}`, value: p.propId })),
  ...relations.value.map((r) => ({ label: `${r.relId} ${r.type}(${r.source}, ${r.target})`, value: r.relId }))
])

const markedHtml = computed(() => {
  const text = data.value?.document.content || ''
  let html = ''
  let cursor = 0
  propositions.value
    .slice()
    .sort((a, b) => a.startPos - b.startPos)
    .forEach((p) => {
      html += escapeHtml(text.slice(cursor, p.startPos))
      html += `<mark class="annotation-mark">${p.propId}</mark><span class="annotation-text">${escapeHtml(text.slice(p.startPos, p.endPos))}</span>`
      cursor = p.endPos
    })
  html += escapeHtml(text.slice(cursor))
  return html
})

function snapshot() {
  history.value.push(JSON.stringify({ propositions: propositions.value, relations: relations.value }))
  redoStack.value = []
}

function restore(raw) {
  const state = JSON.parse(raw)
  propositions.value = state.propositions
  relations.value = state.relations
}

function undo() {
  if (!history.value.length) return
  redoStack.value.push(JSON.stringify({ propositions: propositions.value, relations: relations.value }))
  restore(history.value.pop())
}

function redo() {
  if (!redoStack.value.length) return
  history.value.push(JSON.stringify({ propositions: propositions.value, relations: relations.value }))
  restore(redoStack.value.pop())
}

function handleSelection() {
  const text = window.getSelection()?.toString().trim()
  if (!text) return
  selectedText.value = text
  selectedStart.value = data.value.document.content.indexOf(text)
  labelDialog.value = true
}

function cancelLabel() {
  selectedText.value = ''
  labelDialog.value = false
  window.getSelection()?.removeAllRanges()
}

function confirmLabel() {
  if (!selectedText.value) return
  snapshot()
  const start = selectedStart.value < 0 ? propositions.value.length * 10 : selectedStart.value
  const prop = {
    propId: `P${propositions.value.length + 1}`,
    sequenceNo: propositions.value.length + 1,
    startPos: start,
    endPos: start + selectedText.value.length,
    text: selectedText.value,
    tag: primaryTag.value === 'GM' ? secondaryTag.value : primaryTag.value
  }
  propositions.value.push(prop)
  reorder()
  cancelLabel()
}

function reorder() {
  propositions.value.sort((a, b) => a.startPos - b.startPos)
  propositions.value = propositions.value.map((p, i) => ({ ...p, propId: `P${i + 1}`, sequenceNo: i + 1 }))
}

function clearRelation() {
  relationForm.source = ''
  relationForm.target = ''
  relationForm.level = 'M1'
}

function addRelation() {
  if (!relationForm.source || !relationForm.target || relationForm.source === relationForm.target) {
    ElMessage.warning('请选择不同的命题或关系')
    return
  }
  snapshot()
  relations.value.push({ relId: `R${relations.value.length + 1}`, type: relationForm.type, source: relationForm.source, target: relationForm.target, level: relationForm.level })
  clearRelation()
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
  if (!isDraft) router.push(`/tasks/${route.params.taskId}/data`)
}

async function load() {
  data.value = await client.get(`/tasks/${route.params.taskId}/items/${route.params.dataId}`)
  propositions.value = [...(data.value.annotation.propositions || [])]
  relations.value = [...(data.value.annotation.relations || [])]
}

function escapeHtml(value) {
  return value.replace(/[&<>"']/g, (ch) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]))
}

onMounted(load)
</script>
