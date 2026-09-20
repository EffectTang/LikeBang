<template>
  <!-- 登录页不使用后台布局 -->
  <router-view v-if="isLoginPage" />
  <el-container v-else class="layout-container">
    <el-header class="layout-header">
      <h1 class="logo">榜了个榜 · 观点排名社区</h1>
      <div class="header-user">
        <span class="nickname">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
        <el-button link type="primary" @click="logout">退出登录</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" class="layout-aside">
        <el-menu
          :default-active="activeMenu"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF">
          <el-menu-item index="/community">
            <el-icon><Compass /></el-icon>
            <span>发现榜单</span>
          </el-menu-item>
          <el-sub-menu index="admin" v-if="userStore.isAdmin">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>管理后台</span>
            </template>
            <el-menu-item index="/admin/dashboard">
              <el-icon><DataBoard /></el-icon>
              <span>仪表盘</span>
            </el-menu-item>
            <el-menu-item index="/admin/categories">
              <el-icon><Grid /></el-icon>
              <span>分类管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/users">
              <el-icon><User /></el-icon>
              <span>用户管理</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DataBoard, User, Compass, Setting, Grid } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 榜单详情页高亮“发现榜单”菜单
const activeMenu = computed(() =>
  route.path.startsWith('/rankings') ? '/community' : route.path
)
const isLoginPage = computed(() => route.path === '/login')

// 刷新后从后端拉取最新用户信息，保证 role 与 localStorage 缓存一致（避免菜单/权限显示错乱）
onMounted(() => {
  if (userStore.isLogin) {
    userStore.fetchMe().catch(() => {})
  }
})

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.layout-header {
  background: #409EFF;
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-user {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-user .nickname {
  font-size: 14px;
}
.logo {
  margin: 0;
  font-size: 20px;
}
.layout-aside {
  background: #304156;
}
.layout-main {
  background: #f0f2f5;
  padding: 20px;
}
</style>
