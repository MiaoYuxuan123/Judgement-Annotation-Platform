<template>
  <div class="task-shell creator-shell">
    <header class="task-shell-header">
      <div class="task-shell-header-left">
        <span class="task-shell-title">任务目录</span>
        <span class="task-shell-perspective">（创建者视角）</span>
      </div>
      <div class="task-shell-header-right">
        <el-tag effect="dark" type="warning" size="small">任务创建者</el-tag>
        <span>{{ auth.user?.realName }}</span>
        <el-button text @click="logout">退出</el-button>
      </div>
    </header>
    <div class="task-shell-body">
      <slot />
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

async function logout() {
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.creator-shell .task-shell-header {
  background: #1e40af;
}
</style>
