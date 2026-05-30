<template>
  <el-form :model="form" label-position="top" class="task-form">
    <el-form-item label="任务名称">
      <el-input v-model="form.taskName" placeholder="请输入任务名称" />
    </el-form-item>
    <el-form-item label="任务描述">
      <el-input v-model="form.description" type="textarea" placeholder="请输入任务描述" />
    </el-form-item>
    <el-form-item label="指南版本">
      <el-select v-model="form.configId" placeholder="请选择指南版本" style="width: 100%">
        <el-option
          v-for="cfg in configs"
          :key="cfg.id"
          :label="cfg.versionName"
          :value="cfg.id"
        />
      </el-select>
    </el-form-item>
    <el-form-item v-if="documents.length" label="文书">
      <ul class="task-form-doc-list">
        <li v-for="doc in documents" :key="doc.id || doc.documentId">
          <span v-if="doc.sourceType" class="task-doc-source-tag">{{ sourceLabel(doc.sourceType) }}</span>
          {{ doc.title || doc.fileName }}
        </li>
      </ul>
    </el-form-item>
    <el-form-item label="标注者">
      <el-select v-model="form.annotatorIds" multiple placeholder="请选择标注者" style="width: 100%">
        <el-option v-for="user in annotators" :key="user.id" :label="user.realName" :value="user.id" />
      </el-select>
    </el-form-item>
    <el-form-item label="裁决者">
      <el-select v-model="form.reviewerId" placeholder="请选择裁决者" style="width: 100%">
        <el-option v-for="user in reviewers" :key="user.id" :label="user.realName" :value="user.id" />
      </el-select>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  form: { type: Object, required: true },
  documents: { type: Array, default: () => [] },
  users: { type: Array, default: () => [] },
  configs: { type: Array, default: () => [] }
})

const normalUsers = computed(() => props.users.filter((u) => u.role !== 'admin' && !u.canCreateTask))
const annotators = computed(() => normalUsers.value)
const reviewers = computed(() => normalUsers.value.filter((u) => !props.form.annotatorIds.includes(u.id)))

function sourceLabel(sourceType) {
  if (sourceType === 'UPLOAD') return '自主上传'
  if (sourceType === 'RECREATE') return '范围修改'
  return '文书总库'
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
  border-bottom: 1px solid #f3f4f6;
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-doc-source-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
}
</style>
