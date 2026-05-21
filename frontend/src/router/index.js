import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
  { path: '/dashboard', component: () => import('../views/DashboardView.vue') },
  { path: '/users', component: () => import('../views/UsersView.vue') },
  { path: '/documents', component: () => import('../views/DocumentsView.vue') },
  { path: '/configs', component: () => import('../views/ConfigsView.vue') },
  { path: '/tasks', component: () => import('../views/TasksView.vue') },
  { path: '/tasks/:id', component: () => import('../views/TaskDetailView.vue') },
  { path: '/tasks/:id/data', component: () => import('../views/DataSelectView.vue') },
  { path: '/annotate/:taskId/:dataId', component: () => import('../views/AnnotateView.vue') },
  { path: '/review/:taskId', component: () => import('../views/ReviewView.vue') },
  { path: '/results/:taskId', component: () => import('../views/ResultsView.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) return '/login'
  if (to.path === '/login' && auth.isLoggedIn) return '/dashboard'
  if (to.path === '/dashboard' && auth.user?.role === 'admin') return '/documents'
  if (['/users', '/documents', '/configs'].includes(to.path) && auth.user?.role !== 'admin') return '/tasks'
  return true
})

export default router
