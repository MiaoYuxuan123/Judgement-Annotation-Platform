<template>
  <router-view v-if="route.meta.public" />
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
            <div class="page-subtitle" v-if="auth.user?.role !== 'admin'">四阶段流程：创建任务 -> 标注 -> 裁定 -> 导出</div>
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
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const sidebarCollapsed = ref(false)

const titleMap = {
  '/dashboard': '工作台',
  '/users': '用户管理',
  '/documents': '文书总库',
  '/configs': '配置中心',
  '/tasks': '任务管理'
}

const title = computed(() => titleMap[route.path] || '业务流程')
const roleName = computed(() => ({ admin: '超级管理员', creator: '任务创建者', annotator: '标注员', reviewer: '裁定者' }[auth.user?.role] || '用户'))

const adminMenuItems = [
  { path: '/users', label: '用户管理' },
  { path: '/documents', label: '文书总库' },
  { path: '/configs', label: '配置中心' }
]

const userMenuItems = [
  { path: '/dashboard', label: '工作台' },
  { path: '/tasks', label: '任务管理' }
]

const menuItems = computed(() => auth.user?.role === 'admin' ? adminMenuItems : userMenuItems)

function logout() {
  auth.logout()
  router.push('/login')
}
</script>
