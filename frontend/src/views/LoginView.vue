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
      <p class="muted">请输入账号和密码。系统会在登录后根据账号权限展示对应功能。</p>
      <el-form :model="form" label-position="top" @keyup.enter="submit">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">进入系统</el-button>
      </el-form>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getDefaultRoute } from '../utils/defaultRoute'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function submit() {
  loading.value = true
  try {
    await auth.login(form)
    router.push(getDefaultRoute(auth))
  } finally {
    loading.value = false
  }
}
</script>
