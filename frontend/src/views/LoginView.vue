<template>
  <div class="login-page">
    <section class="login-hero">
      <h1>裁判文书论证标注系统</h1>
      <p>面向法学研究与教学的协同标注平台，覆盖文书导入、命题抽取、标签标注、论证关系、双标注裁定和结构化导出。</p>
    </section>
    <section class="login-card">
      <h2>登录系统</h2>
      <p class="muted">请输入账号和密码。系统会在登录后根据账号权限展示对应功能。</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            autocomplete="current-password"
          />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
          进入系统
        </el-button>
      </el-form>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { getDefaultRoute } from '../utils/defaultRoute'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const formRef = ref()
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

onMounted(() => {
  auth.ensureSession()
  if (auth.isLoggedIn) {
    router.replace(getDefaultRoute(auth))
  }
})

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await auth.login({
      username: form.username.trim(),
      password: form.password
    })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    if (redirect && redirect.startsWith('/') && !redirect.startsWith('/login')) {
      router.push(redirect)
    } else {
      router.push(getDefaultRoute(auth))
    }
  } catch (error) {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '登录失败'
    if (status === 401) {
      ElMessage.error(message)
    } else {
      ElMessage.error(message)
    }
  } finally {
    loading.value = false
  }
}
</script>
