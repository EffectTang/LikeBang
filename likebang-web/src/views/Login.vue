<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <h2>榜了个榜</h2>
          <span class="sub">观点排名社区</span>
        </div>
      </template>

      <el-tabs v-model="mode">
        <el-tab-pane label="登录" name="login" />
        <el-tab-pane label="注册" name="register" />
        <el-tab-pane label="扫码登录" name="wechat" />
      </el-tabs>

      <el-form
        v-if="mode !== 'wechat'"
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        size="large"
        @keyup.enter="submit"
      >
        <el-form-item prop="username">
          <el-input
            v-model.trim="form.username"
            :prefix-icon="User"
            placeholder="用户名"
            maxlength="32"
          />
        </el-form-item>

        <el-form-item v-if="mode === 'register'" prop="nickname">
          <el-input
            v-model.trim="form.nickname"
            :prefix-icon="Avatar"
            placeholder="昵称"
            maxlength="32"
          />
        </el-form-item>

        <el-form-item v-if="mode === 'register'" prop="email">
          <el-input
            v-model.trim="form.email"
            :prefix-icon="Message"
            placeholder="邮箱"
            maxlength="128"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            type="password"
            placeholder="密码"
            show-password
            maxlength="32"
          />
        </el-form-item>

        <el-form-item v-if="mode === 'register'" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            :prefix-icon="Lock"
            type="password"
            placeholder="确认密码"
            show-password
            maxlength="32"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="submit-btn"
            :loading="loading"
            @click="submit"
          >
            {{ mode === 'login' ? '登 录' : '注 册' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div v-if="mode === 'wechat'" class="scan-panel">
        <div v-if="scanState === 'loading'" class="scan-tip">二维码加载中…</div>
        <template v-else-if="scanState === 'pending'">
          <img class="scan-qr" :src="qrImage" alt="登录二维码" />
          <p class="scan-tip">请使用微信扫描二维码，并在小程序内确认登录</p>
          <p class="scan-count">{{ countdown }} 秒后失效</p>
          <el-button link type="primary" @click="startScan">刷新二维码</el-button>
        </template>
        <div v-else class="scan-tip">
          <p>二维码已失效</p>
          <el-button type="primary" class="refresh-btn" @click="startScan">刷新二维码</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, computed, watch, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Avatar, Message } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const mode = ref('login')
const loading = ref(false)
const formRef = ref()

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const rules = computed(() => ({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    ...(mode.value === 'register'
      ? [{
          pattern: /^[a-zA-Z0-9_]{4,32}$/,
          message: '用户名只能包含字母、数字、下划线，长度4~32',
          trigger: 'blur'
        }]
      : [])
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在6~32之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        value === form.password ? callback() : callback(new Error('两次输入的密码不一致'))
      },
      trigger: 'blur'
    }
  ]
}))

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    if (mode.value === 'login') {
      await userStore.login({ username: form.username, password: form.password })
    } else {
      await userStore.register({
        username: form.username,
        nickname: form.nickname,
        email: form.email,
        password: form.password
      })
    }
    ElMessage.success(mode.value === 'login' ? '登录成功' : '注册成功')
    router.push(route.query.redirect || '/community')
  } finally {
    loading.value = false
  }
}

// ===== 微信扫码登录 =====
const scanState = ref('pending') // loading | pending | expired
const qrImage = ref('')
const countdown = ref(0)
let sceneId = ''
let pollTimer = null
let countdownTimer = null

function stopScanTimers() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
}

async function startScan() {
  stopScanTimers()
  scanState.value = 'loading'
  try {
    const data = await userStore.scanQr()
    sceneId = data.sceneId
    qrImage.value = data.qrBase64
    countdown.value = data.expiresIn
    scanState.value = 'pending'
    // 每 1.5s 轮询一次扫码状态
    pollTimer = setInterval(pollScanStatus, 1500)
    // 本地倒计时兜底，到点主动置失效（与后端 PENDING TTL 一致）
    countdownTimer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) {
        stopScanTimers()
        scanState.value = 'expired'
      }
    }, 1000)
  } catch (e) {
    scanState.value = 'expired'
  }
}

async function pollScanStatus() {
  if (!sceneId) return
  try {
    const data = await userStore.scanStatus(sceneId)
    if (data.status === 'CONFIRMED') {
      stopScanTimers()
      userStore.setAuth({ token: data.token, userInfo: data.userInfo })
      ElMessage.success('登录成功')
      router.push(route.query.redirect || '/community')
    } else if (data.status === 'EXPIRED') {
      stopScanTimers()
      scanState.value = 'expired'
    }
  } catch (e) {
    // 单次轮询失败忽略，等下一次；真正到期由倒计时兜底停止
  }
}

watch(mode, (val) => {
  if (val === 'wechat') startScan()
  else stopScanTimers()
})

onBeforeUnmount(stopScanTimers)
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2d3d 0%, #40bad5 100%);
}

.login-card {
  width: 400px;
  border-radius: 12px;
}

.card-header h2 {
  margin: 0;
  display: inline-block;
}

.card-header .sub {
  margin-left: 12px;
  color: #909399;
  font-size: 13px;
}

.submit-btn {
  width: 100%;
}

.scan-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0 4px;
}

.scan-qr {
  width: 200px;
  height: 200px;
  border-radius: 8px;
}

.scan-tip {
  margin-top: 12px;
  color: #606266;
  font-size: 14px;
  text-align: center;
}

.scan-count {
  margin-top: 4px;
  color: #909399;
  font-size: 13px;
}

.refresh-btn {
  margin-top: 8px;
}
</style>
