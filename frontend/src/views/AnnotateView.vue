<template>
  <div v-if="data" class="annotate-page">
    <el-alert
      v-if="rejectReason && rejectAlertVisible"
      class="annotate-reject-alert"
      type="warning"
      title="裁定未采纳，请修改后重新提交"
      :description="rejectReason"
      show-icon
      closable
      @close="rejectAlertVisible = false"
    />

    <section class="annotate-topbar modern">
      <div class="annotate-topbar-main">
        <span class="annotate-logo">JAP</span>
        <div class="annotate-title-group">
          <strong>标注工作台</strong>
          <span class="annotate-doc-title">{{ data.document.title }}</span>
        </div>
        <el-button v-if="data.config" size="small" type="warning" @click="openGuide">查看当前指南</el-button>
      </div>
      <div class="annotate-topbar-actions">
        <el-button text class="annotate-user-action" @click="openProfile">个人中心</el-button>
        <el-tag effect="dark" size="small" type="info">{{ roleLabel }}</el-tag>
        <span class="annotate-user-name">{{ auth.user?.realName }}</span>
        <el-button text class="annotate-user-action" @click="logout">退出</el-button>
        <span class="topbar-divider"></span>
        <el-button class="topbar-btn ghost" @click="goBack">返回</el-button>
        <el-button class="topbar-btn ghost" @click="submit(true)">暂存</el-button>
        <el-button type="primary" class="submit-btn" @click="submit(false)">提交</el-button>
      </div>
    </section>

    <div class="annotation-workbench">
      <aside class="annotate-card list-card work-panel">
        <div class="section-title">
          <h3>要素列表</h3>
          <el-tag size="small">{{ propositions.length }}</el-tag>
        </div>
        <div class="list-box proposition-box proposition-table-list">
          <div v-if="propositions.length" class="proposition-table-head">
            <span>序号</span>
            <span>内容</span>
            <span>类型</span>
          </div>
          <div
              v-for="(p, index) in propositions"
              :key="p.propId"
              class="plain-list-item annot-list-item proposition-table-row draggable-element-row"
              :class="{ 'is-unlabeled': !p.tag }"
              draggable="true"
              @dragstart="startElementDrag(index)"
              @dragover.prevent
              @drop.prevent="dropElement(index)"
              @dragend="draggedElementIndex = -1"
              @click="focusProposition(p)"
          >
            <span class="relation-drag-handle" title="拖动调整顺序">⋮⋮</span>
            <span class="prop-col prop-seq">{{ p.propId }}</span>
            <el-tooltip
                :content="p.text"
                placement="top"
                effect="dark"
                :show-after="260"
                popper-class="proposition-full-tooltip"
            >
              <span class="prop-col prop-text">{{ p.text }}</span>
            </el-tooltip>
            <span class="prop-col prop-tag">
              <el-tag v-if="p.tag" size="small" effect="plain">{{ p.tag }}</el-tag>
              <el-tag v-else size="small" type="info" effect="plain">未标注</el-tag>
            </span>
            <span class="item-actions proposition-row-actions">
              <el-button link type="primary" @click.stop="editProposition(p)">标注</el-button>
              <el-button link type="danger" @click.stop="deleteProposition(p)">删除</el-button>
            </span>
          </div>
          <el-empty v-if="!propositions.length" description="暂无要素" :image-size="72" />
        </div>

        <div class="section-title">
          <h3>关系列表</h3>
          <el-tag size="small" type="success">{{ relations.length }}</el-tag>
        </div>
        <div class="list-box relation-box">
          <div
              v-for="(r, index) in relations"
              :key="r.relId"
              class="plain-list-item relation-row draggable-relation-row"
              :class="{ active: activeRelationId === r.relId }"
              draggable="true"
              @dragstart="startRelationDrag(index)"
              @dragover.prevent
              @drop.prevent="dropRelation(index)"
              @dragend="draggedRelationIndex = -1"
              @click="activeRelationId = r.relId"
          >
            <span class="relation-drag-handle" title="拖动调整顺序">⋮⋮</span>
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
          <div ref="sourceTextEl" class="source-text" @mouseup="handleSelection" v-html="markedHtml"></div>
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
                @click="setRelationType(rel.shortName)"
            >
              {{ rel.name }} ({{ rel.shortName }})
            </el-button>
          </div>
          <div class="relation-input-card modern-builder">
            <div class="relation-builder-head">
              <div class="relation-type-preview">{{ relationForm.type }}</div>
              <div>
                <strong>{{ relationTypeName }}</strong>
                <span>{{ supportsMultiMember ? '支持多个命题/关系成员' : '请选择两个命题/关系成员' }}</span>
              </div>
            </div>
            <div ref="memberBuilderEl" class="member-builder">
              <div v-for="(member, index) in relationMembers" :key="index" class="member-slot">
                <span class="member-index">{{ index + 1 }}</span>
                <el-select v-model="relationMembers[index]" placeholder="选择命题/关系" class="relation-select">
                  <el-option v-for="item in relationOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
                <el-button v-if="supportsMultiMember && relationMembers.length > 2" text type="danger" @click="removeMember(index)">删除</el-button>
              </div>
              <el-button class="add-member-btn" :disabled="!supportsMultiMember" @click="addMember">＋</el-button>
            </div>
            <div class="relation-actions">
              <el-button @click="clearRelation">清空</el-button>
              <el-button @click="clearRelation">撤销</el-button>
              <el-button type="primary" @click="addRelation">确认</el-button>
            </div>
          </div>
        </section>
      </main>

      <section class="annotate-card graph-card work-panel">
        <div class="toolbar">
          <h3>逻辑图示</h3>
          <div class="graph-toolbar-actions">
            <el-tag v-if="!graphGenerated" type="info">尚未生成</el-tag>
            <el-button
              size="small"
              type="primary"
              plain
              @click="openGraphEditor"
            >
              {{ graphGenerated ? '编辑图示' : '绘制图示' }}
            </el-button>
          </div>
        </div>
        <div v-if="!graphGenerated" class="graph-placeholder">
          <strong>完成命题与关系标注后生成图示</strong>
          <span>右下角“生成图示”按钮会刷新这里的节点和关系。</span>
        </div>
        <GraphCanvas
          v-else
          :propositions="graphPropositions"
          :relations="graphRelations"
          :active-relation-id="activeRelationId"
          :active-relation-key="activeRelationKey"
          :layout-override="graphLayout"
        />
      </section>
    </div>

    <div v-if="labelDialog" class="floating-label-root" :style="labelPopupStyle">
      <div class="tag-popover draggable-tag-popover">
        <div class="tag-popover-head draggable-tag-head" @mousedown="startLabelDrag">
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
        <template v-if="secondaryTagsForCurrent.length > 0">
          <div class="tag-group-title">{{ primaryTag }} 二级标签</div>
          <div class="tag-choice-grid secondary-grid">
            <button
                v-for="tag in secondaryTagsForCurrent"
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

    <el-dialog v-model="previewVisible" title="指南附件预览" width="82%" :close-on-click-modal="true">
      <div class="guide-attachment-preview">
        <template v-if="isPreviewable">
          <iframe v-if="previewUrl" :key="previewUrl" :src="previewUrl"></iframe>
        </template>
        <div v-else class="guide-attachment-fallback">
          该文件类型不支持在线预览，请下载后查看。
        </div>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button @click="downloadAttachment">下载附件</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="profileVisible" title="个人中心" width="460px">
      <el-form label-position="top">
        <el-form-item label="账号"><el-input :model-value="auth.user?.username" disabled /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="profileForm.realName" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="profileForm.password" placeholder="留空则不修改" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileVisible = false">取消</el-button>
        <el-button type="primary" :loading="profileSaving" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
    <el-drawer
      v-model="graphEditorDrawer"
      direction="rtl"
      size="calc(100vw - 340px)"
      :with-header="false"
      :modal="false"
      modal-class="graph-editor-drawer-overlay"
      :destroy-on-close="true"
      class="graph-editor-drawer"
    >
      <GraphEditorView
        embedded
        @close="graphEditorDrawer = false"
        @saved="handleGraphEditorSaved"
      />
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/client'
import { useAuthStore } from '../stores/auth'
import GraphCanvas from '../components/GraphCanvas.vue'
import GraphEditorView from './GraphEditorView.vue'
import { selectionSpanFromSourceElement } from '../utils/reviewHelpers'
import { cloneLayout, EMPTY_LAYOUT, graphLayoutForSave } from '../utils/graphLayoutOverride'
import { mergeDocumentWithAnnotation, removeRelationsFromDocument } from '../utils/graphDocument'
import { fetchAnnotationItem, isArbitrationMode } from '../utils/annotationRoute'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isArbitration = computed(() => isArbitrationMode(route.query))
const rejectReason = computed(() => data.value?.annotation?.rejectReason || '')
const rejectAlertVisible = ref(true)

const data = ref(null)
const propositions = ref([])
const relations = ref([])
const graphPropositions = ref([])
const graphRelations = ref([])
const graphGenerated = ref(false)
const graphDirty = ref(false)
const graphLayout = ref(EMPTY_LAYOUT())
const graphEditorDrawer = ref(false)
const history = ref([])
const redoStack = ref([])
const labelDialog = ref(false)
const previewVisible = ref(false)
const previewUrl = ref('')
const profileVisible = ref(false)
const profileSaving = ref(false)
const profileForm = reactive({ realName: '', password: '' })
const isPreviewable = computed(() => {
  const name = data.value?.config?.attachmentName || ''
  return name.toLowerCase().endsWith('.pdf') || name.toLowerCase().endsWith('.txt')
})
const selectedText = ref('')
const selectedStart = ref(0)
const selectedEnd = ref(0)
const editingPropositionId = ref('')
const labelPosition = ref({ left: 720, top: 160 })
const sourceTextEl = ref(null)
const memberBuilderEl = ref(null)
const labelDragging = ref(false)
const labelDragOffset = ref({ x: 0, y: 0 })
const primaryTag = ref('')
const secondaryTag = ref('')
const relationForm = reactive({ type: '' })
const relationMembers = ref(['', ''])
const activeRelationId = ref('')
const editingRelationId = ref('')
const draggedRelationIndex = ref(-1)
const draggedElementIndex = ref(-1)
const manualRelationOrder = ref(false)
const manualElementOrder = ref(false)
const savedSnapshot = ref('')
const skipLeaveGuard = ref(false)

function currentSnapshot() {
  return JSON.stringify({ propositions: propositions.value, relations: relations.value })
}

function markSaved() {
  savedSnapshot.value = currentSnapshot()
}

function hasUnsavedChanges() {
  if (labelDialog.value && selectedText.value) return true
  return currentSnapshot() !== savedSnapshot.value
}

async function confirmLeave() {
  if (!hasUnsavedChanges()) return true
  try {
    await ElMessageBox.confirm(
        '当前内容尚未保存，离开后将丢失未保存的修改，是否继续离开？',
        '未保存的内容',
        { type: 'warning', confirmButtonText: '离开', cancelButtonText: '继续编辑' }
    )
    return true
  } catch {
    return false
  }
}

async function goBack() {
  const taskId = route.params.taskId
  const dataId = route.params.dataId
  const target = isArbitration.value
      ? (route.query.returnTo || `/review/${taskId}?docId=${dataId}&select=final`)
      : `/tasks/${taskId}/data`
  if (!(await confirmLeave())) return
  skipLeaveGuard.value = true
  router.push(target)
}

onBeforeRouteLeave(async (_to, _from, next) => {
  if (skipLeaveGuard.value) {
    skipLeaveGuard.value = false
    next()
    return
  }
  if (await confirmLeave()) next()
  else next(false)
})

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

const supportsMultiMember = computed(() => ['J', 'I'].includes(relationForm.type))
const relationTypeName = computed(() => orderedRelationTypes.value.find((item) => item.shortName === relationForm.type)?.name || '关系')
const secondaryTagsForCurrent = computed(() =>
  (data.value?.config.secondaryTags || []).filter(t => t.parentTag === primaryTag.value)
)
const selectedTag = computed(() => {
  const validSecondary = secondaryTagsForCurrent.value.some((tag) => tag.shortName === secondaryTag.value)
  return validSecondary ? secondaryTag.value : primaryTag.value
})
const activeRelationKey = computed(() => {
  const rel = relations.value.find((item) => item.relId === activeRelationId.value)
  return relationSemanticKey(rel, relations.value)
})
const roleLabel = computed(() => {
  if (auth.user?.role === 'admin') return '超级管理员'
  if (auth.user?.canCreateTask) return '任务创建者'
  return '任务参与者'
})

watch(primaryTagOrder, (tags) => {
  if (tags.length > 0 && !primaryTag.value) primaryTag.value = tags[0].shortName
}, { immediate: true })

watch(secondaryTagsForCurrent, (tags) => {
  if (!tags.length) {
    secondaryTag.value = ''
    return
  }
  if (!tags.some((tag) => tag.shortName === secondaryTag.value)) {
    secondaryTag.value = tags[0].shortName
  }
}, { immediate: true })

watch(orderedRelationTypes, (types) => {
  if (types.length > 0 && !types.find(t => t.shortName === relationForm.type)) relationForm.type = types[0].shortName
}, { immediate: true })

const labelPopupStyle = computed(() => ({
  left: `${labelPosition.value.left}px`,
  top: `${labelPosition.value.top}px`,
  maxHeight: `calc(100vh - ${Math.max(32, Math.round(labelPosition.value.top + 16))}px)`
}))

const markedHtml = computed(() => {
  const text = data.value?.document.content || ''
  const ranges = [...propositions.value
      .filter((p) => p.propId !== editingPropositionId.value)
      .map((p) => ({ ...p, kind: 'confirmed' }))]
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
        const propId = escapeHtml(p.propId)
        html += `<mark class="${markClass}" data-prop-id="${propId}">${propId}</mark><span class="${textClass}" data-prop-id="${propId}">${escapeHtml(text.slice(p.startPos, p.endPos))}</span>`
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

function handleSelection(event) {
  const sourceEl = event?.currentTarget
  const content = data.value?.document.content || ''
  const span = selectionSpanFromSourceElement(sourceEl, content)
  if (!span) return

  const selection = window.getSelection()
  const range = selection?.rangeCount ? selection.getRangeAt(0) : null
  const rect = range?.getBoundingClientRect?.() || { right: 0, top: 0 }

  selectedText.value = span.text
  selectedStart.value = span.start
  selectedEnd.value = span.end
  if (overlapsExisting(selectedStart.value, selectedEnd.value, editingPropositionId.value)) {
    ElMessage.warning('该文本已被框定或与已有命题重叠，请选择其他文本')
    window.getSelection()?.removeAllRanges()
    return
  }
  void rect
  addElementFromSelection()
}

function startLabelDrag(event) {
  if (event.button !== 0) return
  labelDragging.value = true
  labelDragOffset.value = {
    x: event.clientX - labelPosition.value.left,
    y: event.clientY - labelPosition.value.top
  }
  event.preventDefault()
}

function moveLabelDrag(event) {
  if (!labelDragging.value) return
  const width = 360
  const minTop = 16
  const minLeft = 16
  labelPosition.value = {
    left: Math.min(window.innerWidth - width - 16, Math.max(minLeft, event.clientX - labelDragOffset.value.x)),
    top: Math.min(window.innerHeight - 80, Math.max(minTop, event.clientY - labelDragOffset.value.y))
  }
}

function stopLabelDrag() {
  labelDragging.value = false
}

function choosePrimary(tag) {
  primaryTag.value = tag
  const nextSecondaryTags = (data.value?.config.secondaryTags || []).filter((item) => item.parentTag === tag)
  secondaryTag.value = nextSecondaryTags[0]?.shortName || ''
}

function cancelLabel() {
  selectedText.value = ''
  selectedStart.value = 0
  selectedEnd.value = 0
  editingPropositionId.value = ''
  labelDialog.value = false
  window.getSelection()?.removeAllRanges()
}

function confirmLabel() {
  if (!selectedText.value) return
  if (selectedStart.value < 0 || selectedEnd.value <= selectedStart.value) {
    ElMessage.warning('当前选区无效，请重新选择原文')
    cancelLabel()
    return
  }
  if (overlapsExisting(selectedStart.value, selectedEnd.value, editingPropositionId.value)) {
    ElMessage.warning('该文本与已有命题重叠，请重新选择')
    return
  }
  snapshot()
  const existingElement = propositions.value.find((item) => item.propId === editingPropositionId.value)
  const prop = {
    elementId: existingElement?.elementId || nextElementId(),
    propId: editingPropositionId.value || nextPropositionId(),
    sequenceNo: propositions.value.length + 1,
    startPos: selectedStart.value,
    endPos: selectedEnd.value,
    text: selectedText.value,
    tag: selectedTag.value || ''
  }
  const existingIndex = propositions.value.findIndex((item) => item.propId === editingPropositionId.value)
  if (existingIndex >= 0) propositions.value[existingIndex] = prop
  else propositions.value.push(prop)
  renumberElements({ sortByText: !manualElementOrder.value })
  graphDirty.value = true
  cancelLabel()
}

function reorder() {
  renumberElements({ sortByText: !manualElementOrder.value })
}

function addElementFromSelection() {
  if (!selectedText.value) return
  snapshot()
  propositions.value.push({
    elementId: nextElementId(),
    propId: nextPropositionId(),
    sequenceNo: propositions.value.length + 1,
    startPos: selectedStart.value,
    endPos: selectedEnd.value,
    text: selectedText.value,
    tag: ''
  })
  renumberElements({ sortByText: !manualElementOrder.value })
  graphDirty.value = true
  selectedText.value = ''
  selectedStart.value = 0
  selectedEnd.value = 0
  window.getSelection()?.removeAllRanges()
}

function renumberElements({ sortByText = false } = {}) {
  const oldToNew = new Map()
  if (sortByText) propositions.value.sort((a, b) => a.startPos - b.startPos)
  propositions.value = propositions.value.map((p, i) => {
    const nextId = `P${i + 1}`
    oldToNew.set(p.propId, nextId)
    return {
      ...p,
      elementId: p.elementId || `E${i + 1}`,
      propId: nextId,
      sequenceNo: i + 1,
      sortOrder: i + 1
    }
  })
  remapRelationMembers(oldToNew)
  if (!manualRelationOrder.value) sortRelationsByMaxProp()
  renumberRelations()
}

function nextElementId() {
  const nums = propositions.value
    .map((p) => Number(String(p.elementId || '').replace(/^E/, '')) || 0)
  return `E${(nums.length ? Math.max(...nums) : 0) + 1}`
}

function nextPropositionId() {
  return `P${propositions.value.length + 1}`
}

function resetRelationMembers(type = relationForm.type) {
  relationMembers.value = ['J', 'I'].includes(type) ? ['', ''] : ['', '']
}

function scrollMemberBuilder(position = 'top') {
  nextTick(() => {
    const el = memberBuilderEl.value
    if (!el) return
    el.scrollTop = position === 'bottom' ? el.scrollHeight : 0
  })
}

function clearRelation(keepType = true) {
  editingRelationId.value = ''
  resetRelationMembers(keepType ? relationForm.type : 'S')
  if (!keepType) relationForm.type = 'S'
  scrollMemberBuilder('top')
}

function addRelation() {
  const members = relationMembers.value.filter(Boolean)
  if (members.length < 2 || new Set(members).size !== members.length) {
    ElMessage.warning('请选择至少两个不同的命题或关系')
    return
  }
  if (!supportsMultiMember.value && members.length !== 2) {
    ElMessage.warning('当前关系类型只支持两个成员')
    return
  }
  snapshot()
  const existingIndex = relations.value.findIndex((item) => item.relId === editingRelationId.value)
  const rel = {
    relId: editingRelationId.value || `R${relations.value.length + 1}`,
    type: relationForm.type,
    source: members[0],
    target: members[1],
    members
  }
  if (existingIndex >= 0) relations.value[existingIndex] = rel
  else relations.value.push(rel)
  activeRelationId.value = rel.relId
  if (!manualRelationOrder.value) sortRelationsByMaxProp()
  renumberRelations()
  graphDirty.value = true
  clearRelation()
}

function editProposition(prop) {
  const tag = prop.tag || primaryTagOrder.value[0]?.shortName || ''
  const secondary = (data.value?.config?.secondaryTags || []).find((item) => item.shortName === tag)
  primaryTag.value = secondary?.parentTag || tag
  secondaryTag.value = secondary ? tag : ''
  selectedText.value = prop.text
  selectedStart.value = prop.startPos
  selectedEnd.value = prop.endPos
  editingPropositionId.value = prop.propId
  labelPosition.value = { left: Math.round(window.innerWidth / 2 - 180), top: 140 }
  labelDialog.value = true
}

function deleteProposition(prop) {
  snapshot()
  propositions.value = propositions.value.filter((item) => item.propId !== prop.propId)
  relations.value = relations.value.filter((rel) => !relationMemberIds(rel).includes(prop.propId))
  removeInvalidRelations()
  if (editingPropositionId.value === prop.propId) cancelLabel()
  renumberElements()
  graphDirty.value = true
}

function focusProposition(prop) {
  const container = sourceTextEl.value
  if (!container || !prop?.propId) return
  const selector = `[data-prop-id="${CSS.escape(prop.propId)}"]`
  const target = container.querySelector(`${selector}.annotation-text`) || container.querySelector(selector)
  if (!target) return
  container.querySelectorAll('.annotation-text.locating, .annotation-mark.locating').forEach((el) => {
    el.classList.remove('locating')
  })
  container.querySelectorAll(selector).forEach((el) => el.classList.add('locating'))
  target.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' })
  window.setTimeout(() => {
    container.querySelectorAll(selector).forEach((el) => el.classList.remove('locating'))
  }, 1600)
}

function updatePropositionTag(prop, tag) {
  snapshot()
  const item = propositions.value.find((p) => p.elementId === prop.elementId || p.propId === prop.propId)
  if (!item) return
  item.tag = tag
  renumberElements()
  graphDirty.value = true
}

function editRelation(rel) {
  relationForm.type = rel.type
  relationMembers.value = [...(rel.members || [rel.source, rel.target]).filter(Boolean)]
  normalizeRelationMembers()
  editingRelationId.value = rel.relId
  activeRelationId.value = rel.relId
  scrollMemberBuilder('top')
}

function deleteRelation(rel) {
  snapshot()
  const beforeRelIds = new Set(relations.value.map((item) => item.relId))
  relations.value = relations.value.filter((item) => item.relId !== rel.relId)
  removeInvalidRelations()
  const afterRelIds = new Set(relations.value.map((item) => item.relId))
  const removedRelIds = [...beforeRelIds].filter((id) => !afterRelIds.has(id))
  if (graphLayout.value?.version === 2) {
    graphLayout.value = removeRelationsFromDocument(graphLayout.value, removedRelIds)
  }
  if (!manualRelationOrder.value) sortRelationsByMaxProp()
  renumberRelations()
  activeRelationId.value = ''
  graphDirty.value = true
}

function renumberRelations() {
  const oldToNew = new Map()
  relations.value = relations.value.map((rel, index) => {
    const nextId = `R${index + 1}`
    oldToNew.set(rel.relId, nextId)
    return { ...rel, relId: nextId }
  })
  remapRelationMembers(oldToNew)
  activeRelationId.value = oldToNew.get(activeRelationId.value) || activeRelationId.value
  editingRelationId.value = oldToNew.get(editingRelationId.value) || editingRelationId.value
}

function sortRelationsByMaxProp() {
  const originalIndex = new Map(relations.value.map((rel, index) => [rel.relId, index]))
  relations.value = [...relations.value].sort((a, b) => {
    const maxDiff = maxPropNoInRelation(a) - maxPropNoInRelation(b)
    if (maxDiff !== 0) return maxDiff
    return (originalIndex.get(a.relId) || 0) - (originalIndex.get(b.relId) || 0)
  })
}

function maxPropNoInRelation(relation, visited = new Set()) {
  if (!relation || visited.has(relation.relId)) return 0
  visited.add(relation.relId)
  return relationMemberIds(relation).reduce((maxNo, id) => {
    const value = String(id || '')
    const prop = propositions.value.find((item) => item.propId === value)
    if (prop) return Math.max(maxNo, prop.sequenceNo || 0)
    if (value.startsWith('R')) {
      const nested = relations.value.find((item) => item.relId === value)
      return Math.max(maxNo, maxPropNoInRelation(nested, visited))
    }
    return maxNo
  }, 0)
}

function startRelationDrag(index) {
  draggedRelationIndex.value = index
}

function dropRelation(targetIndex) {
  const sourceIndex = draggedRelationIndex.value
  draggedRelationIndex.value = -1
  if (sourceIndex < 0 || sourceIndex === targetIndex) return
  snapshot()
  const next = [...relations.value]
  const [moving] = next.splice(sourceIndex, 1)
  next.splice(targetIndex, 0, moving)
  relations.value = next
  manualRelationOrder.value = true
  renumberRelations()
  graphDirty.value = true
}

function startElementDrag(index) {
  draggedElementIndex.value = index
}

function dropElement(targetIndex) {
  const sourceIndex = draggedElementIndex.value
  draggedElementIndex.value = -1
  if (sourceIndex < 0 || sourceIndex === targetIndex) return
  snapshot()
  const next = [...propositions.value]
  const [moving] = next.splice(sourceIndex, 1)
  next.splice(targetIndex, 0, moving)
  propositions.value = next
  manualElementOrder.value = true
  renumberElements()
  graphDirty.value = true
}

function addMember() {
  if (!supportsMultiMember.value) return
  relationMembers.value.push('')
  scrollMemberBuilder('bottom')
}

function removeMember(index) {
  if (relationMembers.value.length <= 2) return
  relationMembers.value.splice(index, 1)
}

function setRelationType(type) {
  relationForm.type = type
  editingRelationId.value = ''
  resetRelationMembers(type)
  scrollMemberBuilder('top')
}

function normalizeRelationMembers() {
  const kept = relationMembers.value.filter(Boolean)
  if (supportsMultiMember.value) {
    relationMembers.value = kept.length >= 2 ? kept : [...kept, ...Array(2 - kept.length).fill('')]
    return
  }
  relationMembers.value = [kept[0] || '', kept[1] || '']
}

async function generateGraph() {
  graphPropositions.value = propositions.value.map((item) => ({ ...item }))
  graphRelations.value = relations.value.map((item) => ({ ...item }))
  if (graphLayout.value?.version === 2 && graphLayout.value.nodes?.length) {
    graphLayout.value = await mergeDocumentWithAnnotation(
      graphLayout.value,
      graphPropositions.value,
      graphRelations.value
    )
  }
  graphGenerated.value = true
  graphDirty.value = false
  ElMessage.success('图示已生成/刷新')
}

function openGraphEditor() {
  if (hasUnsavedChanges()) {
    ElMessage.warning('当前标注内容尚未暂存，请先点击“暂存”后再编辑图示')
    return
  }
  graphEditorDrawer.value = true
}

function handleGraphEditorSaved(nextLayout) {
  graphLayout.value = nextLayout ? cloneLayout(nextLayout) : EMPTY_LAYOUT()
  graphPropositions.value = propositions.value.map((item) => ({ ...item }))
  graphRelations.value = relations.value.map((item) => ({ ...item }))
  graphGenerated.value = true
  graphDirty.value = false
}

function hydrateGraphPreview() {
  const hasV2 = graphLayout.value?.version === 2 && graphLayout.value.nodes?.length
  if (!propositions.value.length) return
  if (isArbitration.value || hasV2) {
    graphPropositions.value = propositions.value.map((item) => ({ ...item }))
    graphRelations.value = relations.value.map((item) => ({ ...item }))
    graphGenerated.value = true
    graphDirty.value = false
  }
}

async function submit(isDraft) {
  const taskId = Number(route.params.taskId)
  const dataId = Number(route.params.dataId)

  if (isArbitration.value) {
    await client.post('/reviews/manual', {
      taskId,
      dataId,
      propositions: propositions.value,
      relations: relations.value,
      graphLayout: graphLayoutForSave(graphLayout.value)
    })
    markSaved()
    ElMessage.success(isDraft ? '裁定草稿已暂存' : '已保存裁定草稿，请在裁定界面确认')
    if (!isDraft) {
      const returnTo = route.query.returnTo || `/review/${taskId}?docId=${dataId}&select=final`
      router.push(returnTo)
    }
    return
  }

  await client.post('/annotations/submit', {
    taskId,
    dataId,
    propositions: propositions.value,
    relations: relations.value,
    graphLayout: graphLayoutForSave(graphLayout.value),
    isDraft
  })
  markSaved()
  ElMessage.success(isDraft ? '草稿已暂存' : '标注已提交')
  if (!isDraft) router.push(`/tasks/${route.params.taskId}/data`)
}

async function load() {
  const taskId = route.params.taskId
  const dataId = route.params.dataId
  data.value = await fetchAnnotationItem(client, taskId, dataId, route.query)
  rejectAlertVisible.value = true
  propositions.value = normalizeElements(data.value.annotation.propositions || [])
  relations.value = [...(data.value.annotation.relations || [])]
  graphLayout.value = data.value.annotation.graphLayout
    ? cloneLayout(data.value.annotation.graphLayout)
    : EMPTY_LAYOUT()
  manualRelationOrder.value = false
  manualElementOrder.value = false
  sortRelationsByMaxProp()
  renumberElements({ sortByText: true })
  graphPropositions.value = []
  graphRelations.value = []
  graphGenerated.value = false
  graphDirty.value = propositions.value.length > 0 || relations.value.length > 0
  hydrateGraphPreview()
  markSaved()
}

function normalizeElements(items) {
  return [...items].map((item, index) => ({
    ...item,
    elementId: item.elementId || `E${index + 1}`,
    tag: item.tag || '',
    sequenceNo: item.sequenceNo || index + 1,
    sortOrder: item.sortOrder || item.sequenceNo || index + 1
  }))
}

function escapeHtml(value) {
  return value.replace(/[&<>"']/g, (ch) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]))
}

function formula(relation) {
  const members = relation.members || [relation.source, relation.target]
  return `${relation.type}(${members.map(displayRelationMember).join(', ')})`
}

function displayRelationMember(id) {
  if (propositions.value.some((item) => item.propId === id)) return id
  const rel = relations.value.find((item) => item.relId === id)
  return rel ? formula(rel) : id
}

function relationMemberIds(relation) {
  return relation.members || [relation.source, relation.target].filter(Boolean)
}

function relationSemanticKey(relation, relationList = relations.value, visited = new Set()) {
  if (!relation || visited.has(relation.relId)) return ''
  visited.add(relation.relId)
  const members = relationMemberIds(relation).map((id) => {
    const value = String(id || '')
    if (propositions.value.some((item) => item.propId === value)) return value
    if (value.startsWith('R')) {
      const child = relationList.find((item) => item.relId === value)
      return child ? relationSemanticKey(child, relationList, new Set(visited)) : value
    }
    return value
  })
  return `${relation.type}(${members.join(',')})`
}

function remapRelationMembers(idMap) {
  if (!idMap.size) return
  relations.value = relations.value.map((rel) => {
    const members = relationMemberIds(rel).map((id) => idMap.get(id) || id)
    return { ...rel, source: members[0], target: members[1], members }
  })
  relationMembers.value = relationMembers.value.map((id) => idMap.get(id) || id)
}

function removeInvalidRelations() {
  let changed = true
  while (changed) {
    changed = false
    const validIds = new Set([
      ...propositions.value.map((item) => item.propId),
      ...relations.value.map((item) => item.relId)
    ])
    const next = relations.value.filter((rel) => relationMemberIds(rel).every((id) => validIds.has(id)))
    changed = next.length !== relations.value.length
    relations.value = next
  }
}

function overlapsExisting(start, end, ignoredPropId = '') {
  return propositions.value.some((p) => p.propId !== ignoredPropId && Math.max(start, p.startPos) < Math.min(end, p.endPos))
}

function openProfile() {
  profileForm.realName = auth.user?.realName || ''
  profileForm.password = ''
  profileVisible.value = true
}

async function saveProfile() {
  profileSaving.value = true
  try {
    await client.put(`/users/${auth.user.id}`, {
      realName: profileForm.realName,
      password: profileForm.password || undefined
    })
    auth.user.realName = profileForm.realName
    ElMessage.success('个人信息已更新')
    profileVisible.value = false
  } catch (e) {
    ElMessage.error('修改失败')
  } finally {
    profileSaving.value = false
  }
}

async function logout() {
  await auth.logout()
  skipLeaveGuard.value = true
  router.push('/login')
}

function attachmentUrl() {
  const config = data.value?.config
  if (!config?.id || !config?.attachmentName) return ''
  const token = localStorage.getItem('jap_token')
  return `/api/configs/versions/${config.id}/attachment?token=${token}&t=${Date.now()}`
}

function openGuide() {
  const url = attachmentUrl()
  if (!url) {
    ElMessage.warning('该版本未上传附件')
    return
  }
  previewUrl.value = url
  previewVisible.value = true
}

function downloadAttachment() {
  const config = data.value?.config
  const url = attachmentUrl()
  if (!url || !config?.attachmentName) {
    ElMessage.warning('该版本未上传附件')
    return
  }
  const a = document.createElement('a')
  a.href = url
  a.download = config.attachmentName
  a.click()
}

onMounted(() => {
  load()
  window.addEventListener('mousemove', moveLabelDrag)
  window.addEventListener('mouseup', stopLabelDrag)
})

onUnmounted(() => {
  window.removeEventListener('mousemove', moveLabelDrag)
  window.removeEventListener('mouseup', stopLabelDrag)
})
</script>
