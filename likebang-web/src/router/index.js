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
    path: '/my-rankings',
    name: 'MyRankings',
    component: () => import('@/views/MyRankings.vue')
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
    component: () => import('@/views/Dashboard.vue'),
    meta: { adminOnly: true }
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('@/views/user/UserList.vue'),
    meta: { adminOnly: true }
  },
  {
    path: '/admin/categories',
    name: 'AdminCategories',
    component: () => import('@/views/admin/CategoryList.vue'),
    meta: { adminOnly: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：未登录跳转登录页，已登录访问登录页跳转社区首页，非管理员拦截后台
router.beforeEach(to => {
  const token = localStorage.getItem('lb_token')
  if (!to.meta?.public && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && token) {
    return { path: '/community' }
  }
  if (to.meta?.adminOnly) {
    const user = JSON.parse(localStorage.getItem('lb_user') || 'null')
    if (user?.role !== 1) {
      return { path: '/community' }
    }
  }
  return true
})

export default router
