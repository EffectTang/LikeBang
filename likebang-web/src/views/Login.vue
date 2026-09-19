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
      </el-tabs>

      <el-form
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
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
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
</style>
