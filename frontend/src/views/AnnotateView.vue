<template>
  <div v-if="data" class="annotate-page">
    <section class="annotate-topbar modern">
      <div>
        <span class="annotate-logo">JAP</span>
        <strong>标注工作台</strong>
        <span class="annotate-doc-title">{{ data.document.title }}</span>
      </div>
      <div>
        <el-button class="topbar-btn" @click="$router.push(`/tasks/${route.params.taskId}/data`)">返回</el-button>
        <el-button class="topbar-btn" @click="submit(true)">暂存</el-button>
        <el-button type="primary" class="submit-btn" @click="submit(false)">提交</el-button>
      </div>
    </section>

    <div class="annotation-workbench">
      <aside class="annotate-card list-card work-panel">
        <div class="section-title">
          <h3>命题列表</h3>
          <el-tag size="small">{{ propositions.length }}</el-tag>
        </div>
        <div class="list-box proposition-box">
          <div v-for="p in propositions" :key="p.propId" class="plain-list-item annot-list-item">
            <span class="item-main">{{ p.propId }} ({{ p.tag }}) {{ p.text }}</span>
            <span class="item-actions">
              <el-button link type="primary" @click="editProposition(p)">修改</el-button>
              <el-button link type="danger" @click="deleteProposition(p)">删除</el-button>
            </span>
          </div>
          <el-empty v-if="!propositions.length" description="暂无命题" :image-size="72" />
        </div>

        <div class="section-title">
          <h3>关系列表</h3>
          <el-tag size="small" type="success">{{ relations.length }}</el-tag>
        </div>
        <div class="list-box relation-box">
          <div
            v-for="(r, index) in relations"
            :key="r.relId"
            class="plain-list-item relation-row"
            :class="{ active: activeRelationId === r.relId }"
            @click="activeRelationId = r.relId"
          >
            <span class="item-main">{{ r.relId }}, {{ formula(r) }}</span>
            <span class="item-actions">
              <el-button link type="primary" @click.stop="editRelation(r)">修改</el-button>
              <el-button link type="danger" @click.stop="deleteRelation(r)">删除</el-button>
            </span>
          </div>
          <el-empty v-if="!relations.length" description="暂无关系" :image-size="72" />
        </div>
      </aside>

      <main class="center-work">
        <section class="annotate-card text-card work-panel">
          <div class="toolbar">
            <h3>原文展示区</h3>
            <div>
              <el-button @click="undo">撤回</el-button>
              <el-button @click="redo">重做</el-button>
            </div>
          </div>
          <div class="source-text" @mouseup="handleSelection" v-html="markedHtml"></div>
        </section>

        <section class="relation-builder">
        <div class="section-title relation-title">
          <h3>关系生成区</h3>
          <div class="relation-title-actions">
            <span v-if="graphDirty" class="graph-dirty">图示待刷新</span>
            <span v-else class="muted">图示已同步</span>
            <el-button type="primary" class="generate-graph-btn" @click="generateGraph">生成图示</el-button>
          </div>
          </div>
          <div class="relation-buttons">
            <el-button
              v-for="rel in orderedRelationTypes"
              :key="rel.shortName"
              :class="{ active: relationForm.type === rel.shortName }"
              @click="relationForm.type = rel.shortName"
            >
              {{ rel.name }} ({{ rel.shortName }})
            </el-button>
          </div>
          <div class="relation-input-card">
            <div class="relation-type-preview">{{ relationForm.type }}</div>
            <el-select v-model="relationForm.source" placeholder="命题/关系" class="relation-select">
              <el-option v-for="item in relationOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="relationForm.target" placeholder="命题/关系" class="relation-select">
              <el-option v-for="item in relationOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="relationForm.level" class="level-select">
              <el-option v-for="level in ['M1','M2','M3','M4']" :key="level" :label="level" :value="level" />
            </el-select>
            <div class="relation-actions">
              <el-button type="primary" @click="clearRelation">清空</el-button>
              <el-button type="primary" @click="clearRelation">撤销</el-button>
              <el-button type="primary" @click="addRelation">确认</el-button>
            </div>
          </div>
        </section>
      </main>

      <section class="annotate-card graph-card work-panel">
        <div class="toolbar">
          <h3>逻辑图示</h3>
          <el-tag v-if="!graphGenerated" type="info">尚未生成</el-tag>
        </div>
        <div v-if="!graphGenerated" class="graph-placeholder">
          <strong>完成命题与关系标注后生成图示</strong>
          <span>右下角“生成图示”按钮会刷新这里的节点和关系。</span>
        </div>
        <GraphView v-else :propositions="graphPropositions" :relations="graphRelations" />
        <div class="graph-footer">
          <el-button class="fullscreen-btn" :disabled="!graphGenerated" @click="fullscreen = true">↗ 全屏预览</el-button>
        </div>
      </section>
    </div>

    <div v-if="labelDialog" class="floating-label-root" :style="labelPopupStyle">
      <div class="tag-popover">
        <div class="tag-popover-head">
          <strong>选择命题标签</strong>
          <span>{{ selectedText }}</span>
        </div>
        <div class="tag-group-title">一级标签</div>
        <div class="tag-choice-grid">
        <button
          v-for="tag in primaryTagOrder"
          :key="tag.shortName"
          class="modern-tag-option"
          :class="{ selected: primaryTag === tag.shortName }"
          @click="choosePrimary(tag.shortName)"
        >
          <strong>{{ tag.shortName }}</strong>
          <span>{{ tag.name }}</span>
        </button>
        </div>
        <template v-if="primaryTag === 'GM'">
          <div class="tag-group-title">GM 二级标签</div>
          <div class="tag-choice-grid secondary-grid">
            <button
              v-for="tag in data.config.secondaryTags"
              :key="tag.shortName"
              class="modern-tag-option compact"
              :class="{ selected: secondaryTag === tag.shortName }"
              @click="secondaryTag = tag.shortName"
            >
              <strong>{{ tag.shortName }}</strong>
              <span>{{ tag.name }}</span>
            </button>
          </div>
        </template>
        <div class="modern-tag-actions">
          <el-button @click="cancelLabel">取消</el-button>
          <el-button type="primary" @click="confirmLabel">确定</el-button>
        </div>
      </div>
      <!--
          {{ tag.shortName }}
        </button>
        <div class="tag-popup-actions">
          <button @click="cancelLabel">取消</button>
          <button @click="confirmLabel">确定</button>
        </div>
      </div>
      <div v-if="primaryTag === 'GM'" class="tag-popup secondary-popup">
        <div class="tag-popup-title">二级标签（GM）</div>
        <button
          v-for="tag in data.config.secondaryTags"
          :key="tag.shortName"
          class="secondary-option"
          :class="{ selected: secondaryTag === tag.shortName }"
          @click="secondaryTag = tag.shortName"
        >
          {{ tag.shortName }}
        </button>
        <div class="tag-popup-actions">
          <button @click="cancelLabel">取消</button>
          <button @click="confirmLabel">确定</button>
        </div>
      </div>
      -->
    </div>

    <el-dialog v-model="fullscreen" title="论证图示全屏预览" fullscreen>
      <GraphView v-if="graphGenerated" :propositions="graphPropositions" :relations="graphRelations" />
      <el-empty v-else description="请先在关系生成区点击“生成图示”" />
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
const graphPropositions = ref([])
const graphRelations = ref([])
const graphGenerated = ref(false)
const graphDirty = ref(false)
const history = ref([])
const redoStack = ref([])
const labelDialog = ref(false)
const fullscreen = ref(false)
const selectedText = ref('')
const selectedStart = ref(0)
const selectedEnd = ref(0)
const labelPosition = ref({ left: 720, top: 160 })
const primaryTag = ref('GM')
const secondaryTag = ref('GM-L')
const relationForm = reactive({ type: 'S', source: '', target: '', level: 'M1' })
const activeRelationId = ref('')
const editingRelationId = ref('')

const relationOptions = computed(() => [
  ...propositions.value.map((p) => ({ label: `${p.propId} ${p.text.slice(0, 12)}`, value: p.propId })),
  ...relations.value.map((r) => ({ label: `${r.relId} ${formula(r)}`, value: r.relId }))
])

const primaryTagOrder = computed(() => {
  const order = ['GM', 'SM', 'SF', 'GF']
  return [...(data.value?.config.primaryTags || [])].sort((a, b) => order.indexOf(a.shortName) - order.indexOf(b.shortName))
})

const orderedRelationTypes = computed(() => {
  const order = ['S', 'J', 'M', 'A', 'I']
  return [...(data.value?.config.relationTypes || [])].sort((a, b) => order.indexOf(a.shortName) - order.indexOf(b.shortName))
})

const labelPopupStyle = computed(() => ({
  left: `${labelPosition.value.left}px`,
  top: `${labelPosition.value.top}px`
}))

const markedHtml = computed(() => {
  const text = data.value?.document.content || ''
  const ranges = [...propositions.value.map((p) => ({ ...p, kind: 'confirmed' }))]
  if (labelDialog.value && selectedText.value) {
    ranges.push({ startPos: selectedStart.value, endPos: selectedEnd.value, propId: '待确认', kind: 'pending' })
  }
  let html = ''
  let cursor = 0
  ranges
    .sort((a, b) => a.startPos - b.startPos)
    .forEach((p) => {
      if (p.startPos < cursor) return
      html += escapeHtml(text.slice(cursor, p.startPos))
      const markClass = p.kind === 'pending' ? 'annotation-mark pending' : 'annotation-mark'
      const textClass = p.kind === 'pending' ? 'annotation-text pending' : 'annotation-text'
      html += `<mark class="${markClass}">${escapeHtml(p.propId)}</mark><span class="${textClass}">${escapeHtml(text.slice(p.startPos, p.endPos))}</span>`
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
  graphDirty.value = true
}

function redo() {
  if (!redoStack.value.length) return
  history.value.push(JSON.stringify({ propositions: propositions.value, relations: relations.value }))
  restore(redoStack.value.pop())
  graphDirty.value = true
}

function handleSelection() {
  const selection = window.getSelection()
  const text = selection?.toString().trim()
  if (!text) return
  const range = selection.getRangeAt(0)
  const rect = range.getBoundingClientRect()
  selectedText.value = text
  selectedStart.value = data.value.document.content.indexOf(text)
  selectedEnd.value = selectedStart.value + text.length
  if (selectedStart.value >= 0 && overlapsExisting(selectedStart.value, selectedEnd.value)) {
    ElMessage.warning('该文本已被标注或与已有命题重叠，请选择其他文本')
    window.getSelection()?.removeAllRanges()
    return
  }
  labelPosition.value = {
    left: Math.min(window.innerWidth - 260, Math.max(160, rect.right + 8)),
    top: Math.min(window.innerHeight - 260, Math.max(82, rect.top - 8))
  }
  labelDialog.value = true
}

function choosePrimary(tag) {
  primaryTag.value = tag
  if (tag === 'GM' && !secondaryTag.value) secondaryTag.value = 'GM-L'
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
  graphDirty.value = true
  cancelLabel()
}

function reorder() {
  propositions.value.sort((a, b) => a.startPos - b.startPos)
  propositions.value = propositions.value.map((p, i) => ({ ...p, propId: `P${i + 1}`, sequenceNo: i + 1 }))
  renumberRelations()
}

function clearRelation() {
  relationForm.source = ''
  relationForm.target = ''
  relationForm.level = 'M1'
  editingRelationId.value = ''
}

function addRelation() {
  if (!relationForm.source || !relationForm.target || relationForm.source === relationForm.target) {
    ElMessage.warning('请选择不同的命题或关系')
    return
  }
  snapshot()
  const existingIndex = relations.value.findIndex((item) => item.relId === editingRelationId.value)
  const rel = {
    relId: editingRelationId.value || `R${relations.value.length + 1}`,
    type: relationForm.type,
    source: relationForm.source,
    target: relationForm.target,
    level: relationForm.level
  }
  if (existingIndex >= 0) relations.value[existingIndex] = rel
  else relations.value.push(rel)
  renumberRelations()
  activeRelationId.value = rel.relId
  graphDirty.value = true
  clearRelation()
}

function editProposition(prop) {
  snapshot()
  primaryTag.value = prop.tag.startsWith('GM') ? 'GM' : prop.tag
  secondaryTag.value = prop.tag.startsWith('GM') ? prop.tag : 'GM-L'
  selectedText.value = prop.text
  selectedStart.value = prop.startPos
  selectedEnd.value = prop.endPos
  labelPosition.value = { left: Math.round(window.innerWidth / 2 - 180), top: 140 }
  labelDialog.value = true
  propositions.value = propositions.value.filter((item) => item.propId !== prop.propId)
  graphDirty.value = true
}

function deleteProposition(prop) {
  snapshot()
  propositions.value = propositions.value.filter((item) => item.propId !== prop.propId)
  relations.value = relations.value.filter((rel) => rel.source !== prop.propId && rel.target !== prop.propId)
  reorder()
  graphDirty.value = true
}

function editRelation(rel) {
  relationForm.type = rel.type
  relationForm.source = rel.source
  relationForm.target = rel.target
  relationForm.level = rel.level || 'M1'
  editingRelationId.value = rel.relId
  activeRelationId.value = rel.relId
}

function deleteRelation(rel) {
  snapshot()
  relations.value = relations.value.filter((item) => item.relId !== rel.relId)
  renumberRelations()
  activeRelationId.value = ''
  graphDirty.value = true
}

function renumberRelations() {
  relations.value = relations.value.map((rel, index) => ({ ...rel, relId: `R${index + 1}` }))
}

function generateGraph() {
  graphPropositions.value = propositions.value.map((item) => ({ ...item }))
  graphRelations.value = relations.value.map((item) => ({ ...item }))
  graphGenerated.value = true
  graphDirty.value = false
  ElMessage.success('图示已生成/刷新')
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
  graphPropositions.value = []
  graphRelations.value = []
  graphGenerated.value = false
  graphDirty.value = propositions.value.length > 0 || relations.value.length > 0
}

function escapeHtml(value) {
  return value.replace(/[&<>"']/g, (ch) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]))
}

function formula(relation) {
  return `${relation.type}(${displayRelationMember(relation.source)}, ${displayRelationMember(relation.target)})`
}

function displayRelationMember(id) {
  if (String(id).startsWith('P')) return id
  const rel = relations.value.find((item) => item.relId === id)
  return rel ? formula(rel) : id
}

function overlapsExisting(start, end) {
  return propositions.value.some((p) => Math.max(start, p.startPos) < Math.min(end, p.endPos))
}

onMounted(load)
</script>
