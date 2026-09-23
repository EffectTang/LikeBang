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
    path: '/rankings/:id/items/:itemId',
    name: 'ItemDetail',
    component: () => import('@/views/ItemDetail.vue')
  },
  {
    path: '/admin',
    redirect: '/admin/dashboard'
  },
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { roles: [1] }
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('@/views/user/UserList.vue'),
    // 管理员(1) + 运营管理员(2) 可进；页内再按角色隐藏越权操作按钮
    meta: { roles: [1, 2] }
  },
  {
    path: '/admin/categories',
    name: 'AdminCategories',
    component: () => import('@/views/admin/CategoryList.vue'),
    meta: { roles: [1] }
  },
  {
    path: '/admin/configs',
    name: 'AdminConfigs',
    component: () => import('@/views/admin/ConfigList.vue'),
    meta: { roles: [1] }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：未登录跳转登录页，已登录访问登录页跳转社区首页，后台页按 meta.roles 拦截
router.beforeEach(to => {
  const token = localStorage.getItem('lb_token')
  if (!to.meta?.public && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && token) {
    return { path: '/community' }
  }
  if (to.meta?.roles) {
    const user = JSON.parse(localStorage.getItem('lb_user') || 'null')
    if (!to.meta.roles.includes(user?.role)) {
      return { path: '/community' }
    }
  }
  return true
})

export default router
