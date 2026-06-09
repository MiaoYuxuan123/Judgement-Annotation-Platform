<template>
  <div v-if="loaded" class="graph-editor-page" :class="{ embedded: props.embedded }">
    <header class="graph-editor-topbar">
      <div class="graph-editor-topbar-left">
        <el-button @click="goBack">返回</el-button>
        <strong class="graph-editor-title">论证图编辑器</strong>
        <span class="graph-editor-doc">{{ documentTitle }}</span>
      </div>
      <div class="graph-editor-topbar-actions">
        <el-button :disabled="!canUndo" @click="undo">撤销</el-button>
        <el-button :disabled="!canRedo" @click="redo">重做</el-button>
        <el-button @click="importFromAnnotation">从标注导入</el-button>
        <el-button @click="fitView">适应画布</el-button>
        <el-button @click="exportPng">导出 PNG</el-button>
        <el-button type="primary" :loading="saving" @click="saveLayout">保存</el-button>
      </div>
    </header>

    <div class="graph-editor-body">
      <aside class="graph-editor-left">
        <div class="graph-editor-panel-title">工具</div>
        <div class="graph-editor-tools">
          <button
            v-for="item in tools"
            :key="item.id"
            class="graph-editor-tool"
            :class="{ active: tool === item.id }"
            @click="selectTool(item.id)"
          >
            <span class="graph-editor-tool-icon">{{ item.icon }}</span>
            <span>{{ item.label }}</span>
          </button>
        </div>

        <div class="graph-editor-panel-title">添加关系节点</div>
        <div class="graph-editor-rel-types">
          <button
            v-for="rel in relTypes"
            :key="rel.id"
            class="graph-editor-rel-btn"
            :class="{ active: tool === 'add-hub' && addRelType === rel.id }"
            @click="selectAddHub(rel.id)"
          >
            <strong>{{ rel.id }}</strong>
            <span>{{ rel.label }}</span>
          </button>
        </div>

        <div class="graph-editor-panel-title">操作说明</div>
        <ul class="graph-editor-guide">
          <li><strong>框选</strong>：框选工具下空白处拖拽框选；Shift+点击增减选中；拖动选中节点可一起移动</li>
          <li><strong>对齐</strong>：拖动节点时靠近其他节点会自动水平/竖直对齐</li>
          <li><strong>拖拽画布</strong>：切换到拖拽画布工具后，按住空白处平移画布；滚轮缩放</li>
          <li><strong>命题节点</strong>：从标注列表选择后添加</li>
          <li><strong>同一关系 I</strong>：框选多个命题节点后，在右侧点「设为同一关系」</li>
          <li><strong>关系节点</strong>：选 S/A/M/J 类型后点击画布</li>
          <li><strong>连线</strong>：从连接点拖到另一节点</li>
          <li><strong>折线</strong>：选中连线后拖拽折点；靠近水平/竖直时会自动吸附</li>
          <li><strong>改连线</strong>：选中连线后拖动两端圆点，或点击端点再点节点接头</li>
          <li><strong>删除</strong>：选中后点删除工具，或按 Delete</li>
        </ul>
      </aside>

      <main class="graph-editor-main">
        <GraphEditorCanvas
          ref="canvasRef"
          v-model:document="graphDocument"
          :tool="tool"
          :add-rel-type="addRelType"
          editable
          v-model:selected-node-id="selectedNodeId"
          v-model:selected-edge-id="selectedEdgeId"
          v-model:selected-node-count="selectedNodeCount"
          v-model:selected-prop-count="selectedPropCount"
          @edit-start="onEditStart"
          @change="onDocumentChange"
        />
      </main>

      <aside class="graph-editor-right">
        <div class="graph-editor-panel-title">属性面板</div>

        <template v-if="selectedNodeCount > 1">
          <div class="graph-editor-prop-block">
            <div class="graph-editor-prop-label">多选</div>
            <div class="graph-editor-prop-value">已选中 {{ selectedNodeCount }} 个节点</div>
            <div class="graph-editor-prop-meta">拖动任一选中节点可一起移动；Shift+点击可增减选中</div>
            <el-button
              class="graph-editor-identity-btn"
              type="primary"
              plain
              size="small"
              :disabled="!canMergeIdentity"
              @click="openIdentityPicker"
            >
              设为同一关系 (I)
            </el-button>
          </div>
        </template>

        <template v-else-if="selectedNode">
          <div class="graph-editor-prop-block">
            <div class="graph-editor-prop-label">节点 ID</div>
            <div class="graph-editor-prop-value">{{ selectedNode.id }}</div>
            <div class="graph-editor-prop-meta">类型：{{ nodeTypeLabel(selectedNode) }}</div>
          </div>
          <el-form v-if="selectedNode.type === 'prop'" label-position="top" size="small" class="graph-editor-form">
            <el-form-item v-if="isIdentityNode(selectedNode)" label="同一关系">
              <div class="graph-editor-prop-value">{{ selectedNode.data?.label || selectedNode.id }}</div>
              <div class="graph-editor-prop-meta">成员：{{ identityMembersText(selectedNode) }}</div>
            </el-form-item>
            <el-form-item v-else label="命题标签">
              <el-input v-model="nodeForm.label" @change="applyNodeForm" />
            </el-form-item>
          </el-form>
          <el-form v-else-if="isHubNode(selectedNode)" label-position="top" size="small" class="graph-editor-form">
            <el-form-item label="关系类型">
              <el-select v-model="nodeForm.relType" @change="applyNodeForm">
                <el-option v-for="rel in relTypes" :key="rel.id" :label="`${rel.id} ${rel.label}`" :value="rel.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="关系编号">
              <el-input v-model="nodeForm.relKey" @change="applyNodeForm" />
            </el-form-item>
          </el-form>
        </template>

        <template v-else-if="selectedEdge">
          <div class="graph-editor-prop-block">
            <div class="graph-editor-prop-label">连线</div>
            <div class="graph-editor-prop-value">{{ selectedEdge.source }} → {{ selectedEdge.target }}</div>
            <div class="graph-editor-prop-meta">
              拖动连线两端的圆点可改连接位置；也可先点选端点，再点击节点上的蓝色接头
            </div>
          </div>
          <el-form label-position="top" size="small" class="graph-editor-form">
            <el-form-item label="箭头">
              <el-switch v-model="edgeForm.directed" @change="applyEdgeForm" />
            </el-form-item>
            <el-form-item>
              <el-button size="small" type="danger" plain @click="clearEdgeBends">清除所有折点</el-button>
            </el-form-item>
          </el-form>
        </template>

        <el-empty v-else description="选中节点或连线以编辑" :image-size="72" />
      </aside>
    </div>

    <footer class="graph-editor-statusbar">
      <span>命题 {{ stats.props }}</span>
      <span>关系节点 {{ stats.hubs }}</span>
      <span>连线 {{ stats.edges }}</span>
      <span>工具：{{ toolLabel }}</span>
      <span v-if="dirty" class="graph-editor-dirty">未保存</span>
    </footer>
  </div>
  <div v-else class="graph-editor-loading">加载中…</div>

  <el-dialog
    v-model="propPickerVisible"
    title="添加命题节点"
    width="520px"
    :close-on-click-modal="false"
    destroy-on-close
    @closed="resetPropPicker"
  >
    <p class="prop-picker-hint">从标注命题列表中选择要加入图示的节点。已在画布上的命题不可重复添加。</p>
    <div v-if="!propositions.length" class="prop-picker-empty">暂无标注命题，请先在标注工作台添加命题。</div>
    <template v-else>
      <div class="prop-picker-toolbar">
        <el-checkbox
          :model-value="allAvailableSelected"
          :indeterminate="someAvailableSelected && !allAvailableSelected"
          :disabled="!availablePropOptions.length"
          @change="toggleSelectAllAvailable"
        >
          全选可添加项（{{ availablePropOptions.length }}）
        </el-checkbox>
        <span class="prop-picker-count">已选 {{ propPickerSelection.length }} 项</span>
      </div>
      <el-checkbox-group v-model="propPickerSelection" class="prop-picker-list">
        <label
          v-for="item in propPickerOptions"
          :key="item.propId"
          class="prop-picker-item"
          :class="{ 'is-on-canvas': item.onCanvas, 'is-checked': propPickerSelection.includes(item.propId) }"
        >
          <el-checkbox :label="item.propId" :disabled="item.onCanvas">
            <span class="prop-picker-id">{{ item.propId }}</span>
            <el-tag size="small" effect="plain">{{ item.tag }}</el-tag>
            <span class="prop-picker-text">{{ item.textPreview }}</span>
            <el-tag v-if="item.onCanvas" size="small" type="info">已在图示中</el-tag>
          </el-checkbox>
        </label>
      </el-checkbox-group>
    </template>
    <template #footer>
      <el-button @click="propPickerVisible = false">取消</el-button>
      <el-button type="primary" :disabled="!propPickerSelection.length" @click="confirmAddPropositions">
        添加至画布（{{ propPickerSelection.length }}）
      </el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="identityPickerVisible"
    title="设为同一关系 (I)"
    width="520px"
    :close-on-click-modal="false"
    destroy-on-close
    @closed="resetIdentityPicker"
  >
    <p class="prop-picker-hint">
      将所选命题合并为一个矩形节点，标签形如 <strong>P1 / P2</strong>。请勾选要合并的命题（至少 2 个）。
    </p>
    <div v-if="!identityPickerOptions.length" class="prop-picker-empty">当前选中的节点中没有可合并的命题节点。</div>
    <template v-else>
      <div class="prop-picker-toolbar">
        <el-checkbox
          :model-value="allIdentitySelected"
          :indeterminate="someIdentitySelected && !allIdentitySelected"
          @change="toggleSelectAllIdentity"
        >
          全选（{{ identityPickerOptions.length }}）
        </el-checkbox>
        <span class="prop-picker-count">已选 {{ identityPickerSelection.length }} 项</span>
      </div>
      <el-checkbox-group v-model="identityPickerSelection" class="prop-picker-list">
        <label
          v-for="item in identityPickerOptions"
          :key="item.id"
          class="prop-picker-item"
          :class="{ 'is-checked': identityPickerSelection.includes(item.id) }"
        >
          <el-checkbox :label="item.id">
            <span class="prop-picker-id">{{ item.displayLabel }}</span>
            <span v-if="item.members.length > 1" class="prop-picker-text">（已含 {{ item.members.join(' / ') }}）</span>
          </el-checkbox>
        </label>
      </el-checkbox-group>
    </template>
    <template #footer>
      <el-button @click="identityPickerVisible = false">取消</el-button>
      <el-button
        type="primary"
        :disabled="identityPickerSelection.length < 2"
        @click="confirmIdentityMerge"
      >
        合并为同一关系（{{ identityPickerSelection.length }}）
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/client'
import GraphEditorCanvas from '../components/GraphEditorCanvas.vue'
import { exportGraphDocumentPng } from '../utils/graphImageExport'
import { annotatePageRoute, arbitrationDraftQuery, fetchAnnotationItem, isArbitrationMode } from '../utils/annotationRoute'
import {
  cloneDocument,
  documentSummary,
  EMPTY_DOCUMENT,
  getCanvasPropIds,
  getPropMemberIds,
  hubTypeForRelation,
  isIdentityPropNode,
  importFromAnnotation as buildFromAnnotation,
  migrateLayoutToDocument
} from '../utils/graphDocument'

const route = useRoute()
const router = useRouter()
const isArbitration = computed(() => isArbitrationMode(route.query))
const props = defineProps({
  embedded: { type: Boolean, default: false }
})
const emit = defineEmits(['close', 'saved'])

const loaded = ref(false)
const saving = ref(false)
const dirty = ref(false)
const documentTitle = ref('')
const propositions = ref([])
const relations = ref([])
const graphDocument = ref(EMPTY_DOCUMENT())
const savedSnapshot = ref('')
const canvasRef = ref(null)
const tool = ref('select')
const addRelType = ref('S')
const selectedNodeId = ref('')
const selectedEdgeId = ref('')
const selectedNodeCount = ref(0)
const selectedPropCount = ref(0)
const history = ref([])
const redoStack = ref([])
const propPickerVisible = ref(false)
const propPickerSelection = ref([])
const identityPickerVisible = ref(false)
const identityPickerSelection = ref([])

const canvasPropIds = computed(() => getCanvasPropIds(graphDocument.value.nodes))

const propPickerOptions = computed(() =>
  propositions.value.map((p) => ({
    propId: p.propId,
    tag: p.tag,
    textPreview: (p.text || '').slice(0, 36) + ((p.text || '').length > 36 ? '…' : ''),
    onCanvas: canvasPropIds.value.has(p.propId)
  }))
)

const availablePropOptions = computed(() => propPickerOptions.value.filter((p) => !p.onCanvas))

const allAvailableSelected = computed(() => {
  const available = availablePropOptions.value.map((p) => p.propId)
  return available.length > 0 && available.every((id) => propPickerSelection.value.includes(id))
})

const someAvailableSelected = computed(() => {
  const available = new Set(availablePropOptions.value.map((p) => p.propId))
  return propPickerSelection.value.some((id) => available.has(id))
})

const tools = [
  { id: 'select', label: '框选', icon: '▭' },
  { id: 'pan', label: '拖拽画布', icon: '✋' },
  { id: 'connect', label: '连线', icon: '🔗' },
  { id: 'add-prop', label: '添加命题', icon: '⬜' },
  { id: 'add-hub', label: '添加关系', icon: '⊕' },
  { id: 'delete', label: '删除', icon: '🗑' }
]

const relTypes = [
  { id: 'S', label: '支持' },
  { id: 'A', label: '反对' },
  { id: 'M', label: '修饰' },
  { id: 'J', label: '联合' }
]

const nodeForm = reactive({ label: '', relType: 'S', relKey: '' })
const edgeForm = reactive({ directed: false })

const stats = computed(() => documentSummary(graphDocument.value))
const canUndo = computed(() => history.value.length > 0)
const canRedo = computed(() => redoStack.value.length > 0)
const toolLabel = computed(() => tools.find((t) => t.id === tool.value)?.label || tool.value)

const selectedNode = computed(() => graphDocument.value.nodes?.find((n) => n.id === selectedNodeId.value) || null)
const selectedEdge = computed(() => graphDocument.value.edges?.find((e) => e.id === selectedEdgeId.value) || null)

function isHubNode(node) {
  return String(node?.type || '').startsWith('hub-')
}

function isIdentityNode(node) {
  return isIdentityPropNode(node)
}

function identityMembersText(node) {
  return getPropMemberIds(node).join('、')
}

const canMergeIdentity = computed(() => selectedPropCount.value >= 2)

const identityPickerOptions = computed(() => {
  const selected = canvasRef.value?.getSelectedNodes?.() || []
  return selected
    .filter((node) => node.type === 'prop')
    .map((node) => ({
      id: node.id,
      displayLabel: node.data?.label || node.id,
      members: getPropMemberIds(node)
    }))
})

const allIdentitySelected = computed(() => (
  identityPickerOptions.value.length > 0
  && identityPickerSelection.value.length === identityPickerOptions.value.length
))

const someIdentitySelected = computed(() => identityPickerSelection.value.length > 0)

function nodeTypeLabel(node) {
  if (node.type === 'prop') {
    return isIdentityNode(node) ? '同一关系 (I)' : '命题'
  }
  return `关系 (${node.data?.relType || '?'})`
}

watch(selectedNode, (node) => {
  if (!node) return
  nodeForm.label = node.data?.label || node.id
  nodeForm.relType = node.data?.relType || 'S'
  nodeForm.relKey = node.data?.relKey || ''
})

watch(selectedEdge, (edge) => {
  if (!edge) return
  edgeForm.directed = edge.data?.directed ?? false
})

function selectTool(id) {
  if (id === 'add-prop') {
    openPropPicker()
    return
  }
  tool.value = id
}

function syncDocumentFromCanvas() {
  const snap = canvasRef.value?.getDocumentSnapshot()
  if (snap) graphDocument.value = snap
}

function openPropPicker() {
  if (!propositions.value.length) {
    ElMessage.warning('暂无标注命题，请先在标注工作台添加')
    return
  }
  syncDocumentFromCanvas()
  propPickerSelection.value = availablePropOptions.value.map((p) => p.propId)
  propPickerVisible.value = true
}

function resetPropPicker() {
  propPickerSelection.value = []
}

function toggleSelectAllAvailable(checked) {
  if (checked) {
    propPickerSelection.value = availablePropOptions.value.map((p) => p.propId)
  } else {
    propPickerSelection.value = []
  }
}

function confirmAddPropositions() {
  const selected = propositions.value.filter((p) => propPickerSelection.value.includes(p.propId))
  if (!selected.length) {
    ElMessage.warning('请选择要添加的命题')
    return
  }
  snapshot('添加命题节点')
  const added = canvasRef.value?.addPropositionNodes(selected) || 0
  if (!added) {
    ElMessage.info('所选命题均已在图示中')
    return
  }
  propPickerVisible.value = false
  dirty.value = true
  ElMessage.success(`已添加 ${added} 个命题节点`)
  fitView()
}

function openIdentityPicker() {
  syncDocumentFromCanvas()
  const options = identityPickerOptions.value
  if (options.length < 2) {
    ElMessage.warning('请至少框选两个命题节点')
    return
  }
  identityPickerSelection.value = options.map((item) => item.id)
  identityPickerVisible.value = true
}

function resetIdentityPicker() {
  identityPickerSelection.value = []
}

function toggleSelectAllIdentity(checked) {
  identityPickerSelection.value = checked
    ? identityPickerOptions.value.map((item) => item.id)
    : []
}

function confirmIdentityMerge() {
  if (identityPickerSelection.value.length < 2) {
    ElMessage.warning('同一关系至少需要两个命题')
    return
  }
  snapshot('同一关系')
  const mergedId = canvasRef.value?.mergeIdentityNodes(identityPickerSelection.value)
  if (!mergedId) {
    ElMessage.error('合并失败，请确认所选均为命题节点')
    return
  }
  identityPickerVisible.value = false
  dirty.value = true
  ElMessage.success('已设为同一关系')
}

function selectAddHub(relType) {
  addRelType.value = relType
  tool.value = 'add-hub'
}

function snapshot(reason = '编辑') {
  history.value.push(JSON.stringify({ doc: graphDocument.value, reason }))
  redoStack.value = []
}

function onEditStart(reason) {
  snapshot(reason)
}

function onDocumentChange() {
  dirty.value = JSON.stringify(graphDocument.value) !== savedSnapshot.value
}

function undo() {
  if (!history.value.length) return
  redoStack.value.push(JSON.stringify(graphDocument.value))
  graphDocument.value = JSON.parse(history.value.pop()).doc
  dirty.value = JSON.stringify(graphDocument.value) !== savedSnapshot.value
  canvasRef.value?.importDocument(graphDocument.value)
}

function redo() {
  if (!redoStack.value.length) return
  history.value.push(JSON.stringify(graphDocument.value))
  graphDocument.value = JSON.parse(redoStack.value.pop())
  dirty.value = JSON.stringify(graphDocument.value) !== savedSnapshot.value
  canvasRef.value?.importDocument(graphDocument.value)
}

async function importFromAnnotation() {
  if (!propositions.value.length) {
    ElMessage.warning('暂无标注数据可导入')
    return
  }
  try {
    await ElMessageBox.confirm('从标注导入将覆盖当前画布，是否继续？', '导入确认', { type: 'warning' })
  } catch {
    return
  }
  snapshot('从标注导入')
  graphDocument.value = await buildFromAnnotation(propositions.value, relations.value)
  canvasRef.value?.importDocument(graphDocument.value)
  dirty.value = true
  ElMessage.success('已从标注数据生成图示')
}

function fitView() {
  canvasRef.value?.fitViewNow()
}

async function exportPng() {
  await exportGraphDocumentPng(graphDocument.value, `${documentTitle.value || '论证图'}.png`)
  ElMessage.success('PNG 已导出')
}

async function saveLayout() {
  saving.value = true
  try {
    syncDocumentFromCanvas()
    const taskId = Number(route.params.taskId)
    const dataId = Number(route.params.dataId)
    if (isArbitration.value) {
      await client.post('/reviews/manual', {
        taskId,
        dataId,
        propositions: propositions.value,
        relations: relations.value,
        graphLayout: graphDocument.value
      })
      await router.replace({
        path: route.path,
        query: arbitrationDraftQuery(route.query)
      })
    } else {
      await client.post('/annotations/layout', {
        taskId,
        dataId,
        graphLayout: graphDocument.value
      })
    }
    savedSnapshot.value = JSON.stringify(graphDocument.value)
    dirty.value = false
    ElMessage.success('图示已保存')
    emit('saved', cloneDocument(graphDocument.value))
  } catch {
    ElMessage.error(isArbitration.value
      ? '保存失败，请确认仍有权访问该裁定任务'
      : '保存失败，请先在标注工作台点击「暂存」')
  } finally {
    saving.value = false
  }
}

function applyNodeForm() {
  if (!selectedNodeId.value) return
  snapshot('修改节点')
  graphDocument.value = cloneDocument(graphDocument.value)
  const node = graphDocument.value.nodes.find((n) => n.id === selectedNodeId.value)
  if (!node) return
  if (node.type === 'prop') {
    node.data.label = nodeForm.label
  } else if (isHubNode(node)) {
    node.data.relType = nodeForm.relType
    node.data.relKey = nodeForm.relKey
    node.type = hubTypeForRelation(nodeForm.relType)
    node.data.hubKind = node.type
    node.data.label = ['J', 'M'].includes(nodeForm.relType) ? '+' : ''
  }
  canvasRef.value?.importDocument(graphDocument.value)
  dirty.value = true
}

function applyEdgeForm() {
  if (!selectedEdgeId.value) return
  snapshot('修改连线')
  canvasRef.value?.patchSelectedEdge({
    directed: edgeForm.directed
  })
}

function clearEdgeBends() {
  snapshot('清除折点')
  canvasRef.value?.patchSelectedEdge({
    directed: edgeForm.directed,
    waypoints: []
  })
  dirty.value = true
}

async function goBack() {
  if (dirty.value) {
    try {
      await ElMessageBox.confirm('图示尚未保存，确定离开？', '未保存的修改', {
        type: 'warning', confirmButtonText: '离开', cancelButtonText: '继续编辑'
      })
    } catch { return }
  }
  if (props.embedded) {
    emit('close')
    return
  }
  router.push(annotatePageRoute(route.params.taskId, route.params.dataId, route.query))
}

onBeforeRouteLeave(async (_to, _from, next) => {
  if (props.embedded) { next(); return }
  if (!dirty.value) { next(); return }
  try {
    await ElMessageBox.confirm('图示尚未保存，确定离开？', '未保存的修改', {
      type: 'warning', confirmButtonText: '离开', cancelButtonText: '继续编辑'
    })
    next()
  } catch { next(false) }
})

async function load() {
  const { taskId, dataId } = route.params
  const data = await fetchAnnotationItem(client, taskId, dataId, route.query)
  documentTitle.value = data.document?.title || ''
  propositions.value = [...(data.annotation?.propositions || [])]
  relations.value = [...(data.annotation?.relations || [])]

  const saved = data.annotation?.graphLayout
  if (saved?.version === 2 && saved.nodes?.length) {
    graphDocument.value = cloneDocument(saved)
  } else if (propositions.value.length) {
    graphDocument.value = await migrateLayoutToDocument(saved, propositions.value, relations.value)
  } else {
    graphDocument.value = EMPTY_DOCUMENT()
  }

  savedSnapshot.value = JSON.stringify(graphDocument.value)
  dirty.value = false
  loaded.value = true
}

onMounted(load)
</script>

<style scoped>
.graph-editor-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f4f7fb;
  color: #0f172a;
}

.graph-editor-page.embedded {
  height: 100%;
  min-height: 0;
  background: #f8fafc;
}

.graph-editor-page.embedded .graph-editor-topbar {
  flex-wrap: wrap;
  padding: 10px 12px;
}

.graph-editor-page.embedded .graph-editor-topbar-actions {
  justify-content: flex-end;
}

.graph-editor-page.embedded .graph-editor-body {
  grid-template-columns: 180px minmax(360px, 1fr) 220px;
}

.graph-editor-page.embedded .graph-editor-left,
.graph-editor-page.embedded .graph-editor-right {
  padding: 12px 10px;
}

.graph-editor-page.embedded .graph-editor-guide {
  font-size: 11px;
}

.graph-editor-topbar,
.graph-editor-statusbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #dbe3ee;
}

.graph-editor-statusbar {
  border-bottom: none;
  border-top: 1px solid #dbe3ee;
  font-size: 12px;
  color: #64748b;
}

.graph-editor-statusbar span + span { margin-left: 16px; }

.graph-editor-topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.graph-editor-title { font-size: 16px; }

.graph-editor-doc {
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.graph-editor-topbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.graph-editor-body {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 220px 1fr 260px;
}

.graph-editor-left,
.graph-editor-right {
  background: #fafbfc;
  border-right: 1px solid #dbe3ee;
  padding: 14px 12px;
  overflow: auto;
}

.graph-editor-right {
  border-right: none;
  border-left: 1px solid #dbe3ee;
}

.graph-editor-main {
  min-width: 0;
  min-height: 0;
  display: flex;
}

.graph-editor-panel-title {
  font-size: 12px;
  font-weight: 700;
  color: #475569;
  margin: 12px 0 8px;
}

.graph-editor-panel-title:first-child { margin-top: 0; }

.graph-editor-tools,
.graph-editor-rel-types {
  display: grid;
  gap: 6px;
}

.graph-editor-tool,
.graph-editor-rel-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #dbe3ee;
  background: #fff;
  border-radius: 8px;
  padding: 8px 10px;
  cursor: pointer;
  text-align: left;
}

.graph-editor-rel-btn {
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.graph-editor-tool.active,
.graph-editor-rel-btn.active {
  border-color: #2563eb;
  background: #eff6ff;
  color: #1d4ed8;
}

.graph-editor-tool-icon { width: 18px; }

.graph-editor-guide {
  margin: 0;
  padding-left: 18px;
  font-size: 12px;
  line-height: 1.7;
  color: #64748b;
}

.graph-editor-guide li + li { margin-top: 4px; }

.graph-editor-prop-block { margin-bottom: 12px; }

.graph-editor-prop-label { font-size: 12px; color: #64748b; }

.graph-editor-prop-value {
  font-size: 15px;
  font-weight: 700;
  margin-top: 4px;
}

.graph-editor-prop-meta {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
}

.graph-editor-identity-btn {
  margin-top: 12px;
  width: 100%;
}

.graph-editor-dirty { color: #d97706; font-weight: 700; }

.graph-editor-loading {
  height: 100vh;
  display: grid;
  place-items: center;
  color: #64748b;
}

.prop-picker-hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
}

.prop-picker-empty {
  padding: 24px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}

.prop-picker-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e2e8f0;
}

.prop-picker-count {
  font-size: 12px;
  color: #64748b;
}

.prop-picker-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 360px;
  overflow: auto;
}

.prop-picker-item {
  display: block;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px 10px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}

.prop-picker-item:hover:not(.is-on-canvas) {
  border-color: #93c5fd;
  background: #f8fbff;
}

.prop-picker-item.is-checked:not(.is-on-canvas) {
  border-color: #2563eb;
  background: #eff6ff;
}

.prop-picker-item.is-on-canvas {
  opacity: 0.55;
  cursor: not-allowed;
  background: #f8fafc;
}

.prop-picker-item :deep(.el-checkbox) {
  width: 100%;
  height: auto;
  align-items: flex-start;
}

.prop-picker-item :deep(.el-checkbox__label) {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  white-space: normal;
  line-height: 1.5;
}

.prop-picker-id {
  font-weight: 700;
  min-width: 28px;
}

.prop-picker-text {
  flex: 1;
  color: #475569;
  font-size: 13px;
}
</style>
