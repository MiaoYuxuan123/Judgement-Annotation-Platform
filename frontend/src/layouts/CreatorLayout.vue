<template>
  <div class="task-shell creator-shell">
    <header class="task-shell-header">
      <div class="task-shell-header-left">
        <span class="task-shell-title">任务目录</span>
      </div>
      <div class="task-shell-header-right">
        <el-button text @click="openProfile">个人中心</el-button>
        <el-tag effect="dark" type="warning" size="small">任务创建者</el-tag>
        <span>{{ auth.user?.realName }}</span>
        <el-button text @click="logout">退出</el-button>
      </div>
    </header>
    <div class="task-shell-body">
      <slot />
    </div>
  </div>
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
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import client from '../api/client'
import { ElMessage } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()
const profileVisible = ref(false)
const profileSaving = ref(false)
const profileForm = reactive({ realName: '', password: '' })

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
  } catch (e) { ElMessage.error('修改失败') }
  finally { profileSaving.value = false }
}

async function logout() {
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.creator-shell .task-shell-header {
  background: linear-gradient(135deg, #1a1817 0%, #2c1f1a 50%, #1a1817 100%);
}
</style>
