<template>
  <el-form :model="form" label-position="top" class="task-form">
    <el-form-item label="任务名称">
      <el-input v-model="form.taskName" placeholder="请输入任务名称" :disabled="isEdit" />
    </el-form-item>
    <el-form-item label="任务描述">
      <el-input v-model="form.description" type="textarea" placeholder="请输入任务描述" :disabled="isEdit" />
    </el-form-item>
    <el-form-item label="截止日期">
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
    </el-form-item>
    <el-form-item label="指南版本">
      <div v-if="isEdit" class="readonly-config-row">
        <span>{{ configLabel }}</span>
        <el-button v-if="form.configId" link type="primary" @click="openConfigView">展开</el-button>
      </div>
      <el-select
        v-else
        v-model="form.configId"
        placeholder="请选择指南版本"
        style="width: 100%"
      >
        <el-option
          v-for="cfg in configs"
          :key="cfg.id"
          :label="cfg.versionName"
          :value="cfg.id"
        />
      </el-select>
    </el-form-item>
    <el-form-item v-if="isEdit" label="文书">
      <ul v-if="allDocuments.length" class="task-form-doc-list">
        <li v-for="doc in allDocuments" :key="docKey(doc)">
          <span v-if="doc.sourceType" class="task-doc-source-tag">{{ sourceLabel(doc.sourceType) }}</span>
          <span v-if="doc.pending" class="task-doc-pending-tag">待保存</span>
          {{ doc.title || doc.fileName }}
        </li>
      </ul>
      <p v-else class="muted">暂无文书</p>
      <el-button v-if="isEdit" type="primary" plain style="margin-top: 8px" @click="$emit('add-documents')">
        增加文书
      </el-button>
    </el-form-item>
    <el-form-item label="参与者">
      <ul v-if="memberEntries.length" class="task-form-doc-list">
        <li v-for="entry in memberEntries" :key="entry.key">
          <span class="task-doc-source-tag" :class="entry.roleClass">{{ entry.roleLabel }}</span>
          <span v-if="entry.pending" class="task-doc-pending-tag">待保存</span>
          {{ entry.name }}
        </li>
      </ul>
      <p v-else class="muted">尚未选取参与者</p>
      <el-button
        v-if="isEdit"
        type="primary"
        plain
        style="margin-top: 8px"
        @click="$emit('add-members')"
      >
        增加参与者
      </el-button>
    </el-form-item>
    <el-form-item v-if="!isEdit" label=" ">
      <el-button type="primary" plain @click="$emit('pick-members')">选取参与者</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { sourceTypeLabel } from '../../utils/taskCreateDraft'
import { buildGuideViewRoute } from '../../utils/navigationReturn'

const props = defineProps({
  form: { type: Object, required: true },
  documents: { type: Array, default: () => [] },
  users: { type: Array, default: () => [] },
  configs: { type: Array, default: () => [] },
  mode: { type: String, default: 'create' },
  contextTaskId: { type: [Number, String, null], default: null },
  returnRowKey: { type: [String, null], default: null }
})

defineEmits(['add-documents', 'add-members', 'pick-members'])

const router = useRouter()
const isEdit = computed(() => props.mode === 'edit')

const deadlineDate = ref(null)
const deadlineTime = ref(null)

// sync from form.deadline to date/time pickers
watch(() => props.form.deadline, (val) => {
  if (val) {
    const parts = val.split('T')
    deadlineDate.value = parts[0]
    deadlineTime.value = parts[1] || null
  } else {
    deadlineDate.value = null
    deadlineTime.value = null
  }
}, { immediate: true })

// sync from date/time pickers to form.deadline
watch([deadlineDate, deadlineTime], ([d, t]) => {
  if (d) {
    props.form.deadline = d + 'T' + (t || '23:59:59')
  } else {
    props.form.deadline = null
  }
})

const userMap = computed(() => {
  const map = new Map()
  for (const u of props.users) map.set(u.id, u)
  return map
})

const configLabel = computed(() => {
  const cfg = props.configs.find((c) => c.id === props.form.configId)
  return cfg?.versionName || `版本 #${props.form.configId || '—'}`
})

const allDocuments = computed(() => {
  const existing = (props.documents || []).map((d) => ({ ...d, pending: false }))
  const pending = (props.form.pendingDocuments || []).map((d) => ({ ...d, pending: true }))
  return [...existing, ...pending]
})

const memberEntries = computed(() => {
  const entries = []
  if (isEdit.value) {
    for (const id of props.form.annotatorIds || []) {
      const u = userMap.value.get(id)
      entries.push({
        key: `a-${id}`,
        name: u?.realName || `用户#${id}`,
        roleLabel: '标注员',
        roleClass: 'role-annotate',
        pending: false
      })
    }
    if (props.form.reviewerId) {
      const u = userMap.value.get(props.form.reviewerId)
      entries.push({
        key: `r-${props.form.reviewerId}`,
        name: u?.realName || `用户#${props.form.reviewerId}`,
        roleLabel: '裁定者',
        roleClass: 'role-arbitrate',
        pending: false
      })
    }
    for (const id of props.form.pendingAddAnnotatorIds || []) {
      const u = userMap.value.get(id)
      entries.push({
        key: `pa-${id}`,
        name: u?.realName || `用户#${id}`,
        roleLabel: '标注员',
        roleClass: 'role-annotate',
        pending: true
      })
    }
    return entries
  }

  for (const id of props.form.annotatorIds || []) {
    const u = userMap.value.get(id)
    entries.push({
      key: `a-${id}`,
      name: u?.realName || `用户#${id}`,
      roleLabel: '标注员',
      roleClass: 'role-annotate',
      pending: false
    })
  }
  if (props.form.reviewerId) {
    const u = userMap.value.get(props.form.reviewerId)
    entries.push({
      key: `r-${props.form.reviewerId}`,
      name: u?.realName || `用户#${props.form.reviewerId}`,
      roleLabel: '裁定者',
      roleClass: 'role-arbitrate',
      pending: false
    })
  }
  return entries
})

function sourceLabel(sourceType) {
  return sourceTypeLabel(sourceType)
}

function docKey(doc) {
  if (doc.id) return `existing-${doc.id}`
  if (doc.key) return doc.key
  return `${doc.sourceType}-${doc.title}-${doc.pending}`
}

function openConfigView() {
  if (!props.form.configId) return
  router.push(buildGuideViewRoute(props.form.configId, {
    returnPath: '/tasks',
    taskId: props.contextTaskId,
    rowKey: props.returnRowKey
  }))
}
</script>

<style scoped>
.task-form {
  max-width: 720px;
}

.task-form-doc-list {
  margin: 0;
  padding: 0;
  list-style: none;
  width: 100%;
}

.task-form-doc-list li {
  padding: 8px 0;
  border-bottom: 1px solid #f6f2eb;
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-doc-source-tag,
.task-doc-pending-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  flex-shrink: 0;
}

.deadline-picker-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.task-doc-source-tag {
  background: #fef2f2;
  color: #991b1b;
}

.task-doc-source-tag.role-annotate {
  background: #edf7f0;
  color: #4a7c59;
}

.task-doc-source-tag.role-arbitrate {
  background: #fef2f2;
  color: #c41e3a;
}

.task-doc-pending-tag {
  background: #fdf8ef;
  color: #b88a3e;
}

.readonly-config-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.muted {
  color: #78716c;
  margin: 0;
}
</style>
