<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <h1 class="logo">LikeBang 管理系统</h1>
    </el-header>
    <el-container>
      <el-aside width="200px" class="layout-aside">
        <el-menu
          :default-active="activeMenu"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF">
          <el-menu-item index="/dashboard">
            <el-icon><DataBoard /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
          <el-menu-item index="/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { DataBoard, User } from '@element-plus/icons-vue'

const route = useRoute()
const activeMenu = computed(() => route.path)
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
