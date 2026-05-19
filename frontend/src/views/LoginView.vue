<template>
  <div class="login-page">
    <section class="login-hero">
      <h1>裁判文书论证标注系统</h1>
      <p>面向法学研究与教学的协同标注平台，覆盖文书导入、命题抽取、标签标注、论证关系、双标注裁定和结构化导出。</p>
      <div class="tag-row">
        <el-tag>Vue 3</el-tag>
        <el-tag type="success">Spring Boot</el-tag>
        <el-tag type="warning">四阶段流程</el-tag>
        <el-tag type="info">论证图示</el-tag>
      </div>
    </section>
    <section class="login-card">
      <h2>登录系统</h2>
      <p class="muted">演示账号：admin / creator / annotator1 / annotator2 / reviewer，密码均为 123456。</p>
      <el-form :model="form" label-position="top" @keyup.enter="submit">
        <el-form-item label="账号">
          <el-select v-model="form.username" filterable style="width: 100%">
            <el-option label="admin - 超级管理员" value="admin" />
            <el-option label="creator - 任务创建者" value="creator" />
            <el-option label="annotator1 - 标注员一" value="annotator1" />
            <el-option label="annotator2 - 标注员二" value="annotator2" />
            <el-option label="reviewer - 裁定者" value="reviewer" />
          </el-select>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">进入工作台</el-button>
      </el-form>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ username: 'creator', password: '123456' })

async function submit() {
  loading.value = true
  try {
    await auth.login(form)
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>
