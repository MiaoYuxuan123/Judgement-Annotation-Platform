<template>
  <router-view v-if="route.meta.public" />
  <el-container v-else class="shell">
    <el-aside width="248px" class="sidebar">
      <div class="brand">
        <div class="brand-mark">JAP</div>
        <div>
          <strong>裁判文书标注平台</strong>
          <span>Judgment Annotation</span>
        </div>
      </div>
      <el-menu :default-active="route.path" router class="nav">
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">{{ item.label }}</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <div>
          <div class="page-title">{{ title }}</div>
          <div class="page-subtitle">四阶段流程：创建任务 -> 标注 -> 裁定 -> 导出</div>
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
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

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
  if (auth.user?.canCreateTask) {
    return [
      { path: '/dashboard', label: '任务概览' },
      { path: '/tasks', label: '任务管理' }
    ]
  }
  return [
    { path: '/dashboard', label: '我的任务' },
    { path: '/tasks', label: '参与任务' }
  ]
})

function logout() {
  auth.logout()
  router.push('/login')
}
</script>
