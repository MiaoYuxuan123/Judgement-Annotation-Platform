<template>
  <div class="task-detail-panel">
    <div class="task-detail-panel-title">{{ open ? '▼' : '▶' }} 任务详情</div>
    <div class="task-detail-form">
      <label>任务名称</label>
      <div>
        <el-input v-if="editable" v-model="form.taskName" />
        <span v-else class="field-value">{{ detail?.summary?.taskName }}</span>
      </div>

      <label>标注员</label>
      <div>
        <el-input
          v-if="editable"
          :model-value="annotatorNames"
          readonly
          placeholder="创建任务时指定"
        />
        <span v-else class="field-value">{{ annotatorNames }}</span>
      </div>

      <label>数据区</label>
      <div class="field-value">已上传 {{ detail?.summary?.documentCount || 0 }} 条文本</div>

      <label>当前阶段</label>
      <div><span class="task-detail-stage">{{ stageText }}</span></div>

      <label>当前标签</label>
      <div class="field-value config-row">
        {{ detail?.configSnapshot?.versionName || '—' }}
        <el-button
          v-if="detail?.configSnapshot?.id"
          link
          type="primary"
          size="small"
          @click="openConfigView"
        >
          展开
        </el-button>
      </div>

      <template v-if="showReviewer">
        <label>裁决者</label>
        <div class="field-value">{{ detail?.reviewer?.realName || '—' }}</div>
      </template>

      <template v-if="mode === 'participant'">
        <label>任务描述</label>
        <div class="field-value">{{ detail?.summary?.description || '—' }}</div>
      </template>
    </div>
    <div v-if="editable" class="task-detail-actions">
      <el-button type="primary" @click="$emit('save')">修改配置</el-button>
      <el-button @click="$emit('cancel')">取消</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { buildGuideViewRoute } from '../../utils/navigationReturn'
import { stageLabel } from '../../utils/taskRows'

const props = defineProps({
  detail: { type: Object, default: null },
  open: { type: Boolean, default: true },
  editable: { type: Boolean, default: false },
  showReviewer: { type: Boolean, default: true },
  mode: { type: String, default: 'creator' },
  returnRowKey: { type: [String, null], default: null }
})

defineEmits(['save', 'cancel'])

const router = useRouter()
const form = reactive({ taskName: '' })

watch(
  () => props.detail?.summary?.taskName,
  (name) => {
    form.taskName = name || ''
  },
  { immediate: true }
)

const annotatorNames = computed(() =>
  (props.detail?.annotators || []).map((u) => u.realName).join('、') || '—'
)

const stageText = computed(() => stageLabel(props.detail?.summary?.status))

function openConfigView() {
  const configId = props.detail?.configSnapshot?.id
  const taskId = props.detail?.summary?.taskId
  if (!configId) return
  router.push(buildGuideViewRoute(configId, {
    returnPath: '/tasks',
    taskId,
    rowKey: props.returnRowKey
  }))
}
</script>

<style scoped>
.config-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
