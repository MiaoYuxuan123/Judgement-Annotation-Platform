import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getDefaultRoute, isParticipant } from '../utils/defaultRoute'

const routes = [
  {
    path: '/',
    redirect: () => {
      const auth = useAuthStore()
      if (!auth.isLoggedIn) return '/login'
      return getDefaultRoute(auth)
    }
  },
  { path: '/login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
  { path: '/documents', component: () => import('../views/DocumentsView.vue') },
  { path: '/configs', component: () => import('../views/ConfigsView.vue') },
  { path: '/users', component: () => import('../views/UsersView.vue') },
  { path: '/dashboard', redirect: '/tasks' },
  { path: '/tasks', component: () => import('../views/TaskListEntry.vue') },
  { path: '/tasks/create', component: () => import('../views/creator/TaskCreateView.vue'), meta: { creatorOnly: true } },
  { path: '/tasks/create/documents', component: () => import('../views/creator/TaskDocumentPickerView.vue'), meta: { creatorOnly: true } },
  { path: '/tasks/create/members', component: () => import('../views/creator/TaskMemberPickerView.vue'), meta: { creatorOnly: true } },
  { path: '/tasks/:taskId/documents/add', component: () => import('../views/creator/TaskDocumentPickerView.vue'), meta: { creatorOnly: true } },
  { path: '/tasks/:taskId/members/add', component: () => import('../views/creator/TaskMemberPickerView.vue'), meta: { creatorOnly: true } },
  { path: '/configs/versions/:id/view', component: () => import('../views/GuideConfigView.vue') },
  { path: '/tasks/:id', redirect: (to) => ({ path: '/tasks', query: { taskId: to.params.id } }) },
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
  auth.ensureSession()
  if (!to.meta.public && !auth.isLoggedIn) {
    return to.path === '/login' ? true : { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && auth.isLoggedIn) return getDefaultRoute(auth)
  if (['/users', '/documents', '/configs'].includes(to.path) && auth.user?.role !== 'admin') {
    return '/tasks'
  }
  if (to.meta.creatorOnly && !auth.user?.canCreateTask) {
    return '/tasks'
  }
  if (to.path === '/dashboard' && isParticipant(auth)) return '/tasks'
  return true
})

export default router
