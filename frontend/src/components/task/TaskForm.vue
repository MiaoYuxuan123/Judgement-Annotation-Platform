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
    <el-form-item label="文书">
      <el-select v-model="form.documentIds" multiple placeholder="请选择文书" style="width: 100%">
        <el-option v-for="doc in documents" :key="doc.id" :label="doc.title" :value="doc.id" />
      </el-select>
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
</script>

<style scoped>
.task-form {
  max-width: 720px;
}
</style>
