import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue')
  },
  {
    path: '/users',
    name: 'Users',
    component: () => import('@/views/user/UserList.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
