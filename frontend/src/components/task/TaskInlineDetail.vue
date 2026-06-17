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

      <label>截止日期</label>
      <div>
        <template v-if="editable">
          <div class="deadline-picker-row">
            <el-date-picker
              v-model="deadlineDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
              style="width: 55%"
            />
            <el-time-picker
              v-model="deadlineTime"
              placeholder="选择时间"
              format="HH:mm:ss"
              value-format="HH:mm:ss"
              style="width: 43%"
            />
          </div>
        </template>
        <span v-else class="field-value" :class="{ 'deadline-expired': isExpired }">
          {{ deadlineText }}
        </span>
      </div>

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
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import client from '../../api/client'
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
const form = reactive({ taskName: '', deadline: '' })
const deadlineDate = ref(null)
const deadlineTime = ref(null)

watch(
  () => props.detail?.summary?.taskName,
  (name) => {
    form.taskName = name || ''
  },
  { immediate: true }
)

watch(
  () => props.detail?.summary?.deadline,
  (val) => {
    form.deadline = val || ''
    if (val) {
      const parts = val.split('T')
      deadlineDate.value = parts[0]
      deadlineTime.value = parts[1] || null
    } else {
      deadlineDate.value = null
      deadlineTime.value = null
    }
  },
  { immediate: true }
)

watch([deadlineDate, deadlineTime], ([d, t]) => {
  if (d) {
    form.deadline = d + 'T' + (t || '23:59:59')
  } else {
    form.deadline = null
  }
})
const annotatorNames = computed(() =>
  (props.detail?.annotators || []).map((u) => u.realName).join('、') || '—'
)

const stageText = computed(() => stageLabel(props.detail?.summary?.status))

const deadlineText = computed(() => {
  const d = props.detail?.summary?.deadline
  if (!d) return '不限'
  return new Date(d).toLocaleString('zh-CN')
})

const isExpired = computed(() => {
  const d = props.detail?.summary?.deadline
  return d && new Date(d) < new Date()
})

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
.deadline-expired { color: #f56c6c; font-weight: 600; }
.deadline-picker-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.config-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
