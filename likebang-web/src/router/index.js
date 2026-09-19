import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/community'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/community',
    name: 'Community',
    component: () => import('@/views/Community.vue')
  },
  {
    path: '/rankings/:id',
    name: 'RankingDetail',
    component: () => import('@/views/RankingDetail.vue')
  },
  {
    path: '/admin',
    redirect: '/admin/dashboard'
  },
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: () => import('@/views/Dashboard.vue')
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('@/views/user/UserList.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：未登录跳转登录页，已登录访问登录页跳转社区首页
router.beforeEach(to => {
  const token = localStorage.getItem('lb_token')
  if (!to.meta?.public && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && token) {
    return { path: '/community' }
  }
  return true
})

export default router
