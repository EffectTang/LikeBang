<template>
  <!-- 登录页不使用后台布局 -->
  <router-view v-if="isLoginPage" />
  <el-container v-else class="layout-container">
    <el-header class="layout-header">
      <h1 class="logo">榜了个榜 · 观点排名社区</h1>
      <div class="header-user">
        <el-avatar :size="32" :src="avatarSrc" class="user-avatar" @click="openProfile">
          {{ (userStore.userInfo?.nickname || userStore.userInfo?.username || 'U').charAt(0) }}
        </el-avatar>
        <span class="nickname clickable" @click="openProfile">
          {{ userStore.userInfo?.nickname || userStore.userInfo?.username }}
        </span>
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
          <el-menu-item index="/my-rankings" v-if="userStore.isLogin">
            <el-icon><Collection /></el-icon>
            <span>我的榜单</span>
          </el-menu-item>
          <el-sub-menu index="admin" v-if="userStore.canManageUsers">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>管理后台</span>
            </template>
            <el-menu-item index="/admin/dashboard" v-if="userStore.isAdmin">
              <el-icon><DataBoard /></el-icon>
              <span>仪表盘</span>
            </el-menu-item>
            <el-menu-item index="/admin/categories" v-if="userStore.isAdmin">
              <el-icon><Grid /></el-icon>
              <span>分类管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/configs" v-if="userStore.isAdmin">
              <el-icon><Tools /></el-icon>
              <span>系统设置</span>
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

    <!-- 个人中心：查看/编辑昵称、头像、自我介绍（仅本人，userId 由后端登录态确定） -->
    <el-dialog v-model="profileVisible" title="个人中心" width="440px">
      <el-form label-width="80px">
        <el-form-item label="头像">
          <div class="avatar-edit">
            <el-avatar :size="64" :src="formAvatarSrc">
              {{ (form.nickname || userStore.userInfo?.username || 'U').charAt(0) }}
            </el-avatar>
            <el-upload :show-file-list="false" :http-request="onUpload" accept="image/*">
              <el-button size="small">上传头像</el-button>
            </el-upload>
            <el-button v-if="form.avatarUrl" size="small" text type="danger" @click="form.avatarUrl = ''">移除</el-button>
          </div>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input :value="userStore.userInfo?.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" maxlength="32" show-word-limit placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="自我介绍">
          <el-input v-model="form.intro" type="textarea" :rows="3" maxlength="200" show-word-limit
                    placeholder="介绍一下自己吧（留空则清空）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DataBoard, User, Compass, Setting, Grid, Collection, Tools } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { uploadImage } from '@/api/file'
import { resolveImage } from '@/utils/image'

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

// ---- 个人中心 ----
const profileVisible = ref(false)
const saving = ref(false)
const form = reactive({ nickname: '', avatarUrl: '', intro: '' })

const avatarSrc = computed(() => resolveImage(userStore.userInfo?.avatarUrl))
const formAvatarSrc = computed(() => resolveImage(form.avatarUrl))

function openProfile() {
  const u = userStore.userInfo || {}
  form.nickname = u.nickname || ''
  form.avatarUrl = u.avatarUrl || ''
  form.intro = u.intro || ''
  profileVisible.value = true
}

// 走站内上传接口，落库仅存相对路径（/uploads/xxx），渲染时再补 /api 前缀
async function onUpload({ file }) {
  try {
    const res = await uploadImage(file)
    form.avatarUrl = res.data
  } catch (e) {
    // 失败提示已由 request 拦截器统一处理
  }
}

async function saveProfile() {
  if (!form.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    // 头像/简介传空串=清空；昵称空则后端按不修改处理，这里已拦截
    await userStore.updateProfile({
      nickname: form.nickname.trim(),
      avatarUrl: form.avatarUrl,
      intro: form.intro
    })
    ElMessage.success('已保存')
    profileVisible.value = false
  } catch (e) {
    // 错误提示已由 request 拦截器统一处理
  } finally {
    saving.value = false
  }
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
.header-user .user-avatar,
.header-user .nickname.clickable {
  cursor: pointer;
}
.avatar-edit {
  display: flex;
  align-items: center;
  gap: 12px;
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
