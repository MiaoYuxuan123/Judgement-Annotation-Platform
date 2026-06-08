<template>
  <router-view v-if="shell === 'public'" />
  <CreatorLayout v-else-if="shell === 'creator'">
    <router-view />
  </CreatorLayout>
  <ParticipantLayout v-else-if="shell === 'participant'">
    <router-view />
  </ParticipantLayout>
  <el-container v-else class="shell" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <el-aside :width="sidebarCollapsed ? '72px' : '248px'" class="sidebar">
      <div class="brand">
        <div class="brand-mark">JAP</div>
        <div v-if="!sidebarCollapsed">
          <strong>裁判文书标注平台</strong>
          <span>Judgment Annotation</span>
        </div>
      </div>
      <el-menu :default-active="route.path" router class="nav">
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <span v-if="sidebarCollapsed">{{ item.label.slice(0, 2) }}</span>
          <span v-else>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <div class="topbar-left">
          <el-button text class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">{{ sidebarCollapsed ? '展开' : '收起' }}</el-button>
          <div>
            <div class="page-title">{{ title }}</div>
          </div>
        </div>
        <div class="userbox">
          <el-tag effect="plain">{{ roleName }}</el-tag>
          <span>{{ auth.user?.realName }}</span>
          <el-button text @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import client from './api/client'
import CreatorLayout from './layouts/CreatorLayout.vue'
import ParticipantLayout from './layouts/ParticipantLayout.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const sidebarCollapsed = ref(false)

const adminPaths = ['/users', '/documents', '/configs']

const shell = computed(() => {
  if (route.meta.public || !auth.user) return 'public'
  if (route.meta.fullscreen) return 'public'
  if (auth.user?.role === 'admin' && adminPaths.includes(route.path)) return 'admin'
  if (auth.user?.canCreateTask) return 'creator'
  if (auth.user?.role !== 'admin') return 'participant'
  return 'admin'
})

const titleMap = {
  '/dashboard': '工作台',
  '/users': '用户管理',
  '/documents': '文书总库',
  '/configs': '配置中心',
  '/tasks': '任务管理'
}

const title = computed(() => titleMap[route.path] || '业务流程')
const roleName = computed(() => {
  if (auth.user?.role === 'admin') return '超级管理员'
  if (auth.user?.canCreateTask) return '任务创建者'
  return '任务参与者'
})

const menuItems = computed(() => {
  if (auth.user?.role === 'admin') {
    return [
      { path: '/documents', label: '文书总库' },
      { path: '/configs', label: '配置中心' },
      { path: '/users', label: '用户管理' }
    ]
  }
  return []
})

async function logout() {
  await auth.logout()
  router.push('/login')
}

let heartbeat = null
onMounted(() => {
  heartbeat = setInterval(() => {
    if (auth.isLoggedIn) client.get('/users/me').catch(() => {})
  }, 60_000)
})
onUnmounted(() => {
  if (heartbeat) clearInterval(heartbeat)
})
</script>
