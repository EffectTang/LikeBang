<template>
  <div class="reset-page">
    <el-card class="reset-card">
      <template #header>
        <div class="card-header">
          <h2>设置新密码</h2>
          <span class="sub">通过邮件重置链接操作</span>
        </div>
      </template>

      <!-- 链接缺令牌：直接引导重新申请，不发无效请求 -->
      <template v-if="!token">
        <el-result icon="warning" title="重置链接不完整" sub-title="链接可能复制不全或已失效，请重新发起找回密码" />
        <div class="footer-links">
          <el-button type="primary" class="submit-btn" @click="router.push('/forgot-password')">重新申请</el-button>
          <el-button link type="primary" @click="router.push('/login')">返回登录</el-button>
        </div>
      </template>

      <template v-else-if="done">
        <el-result icon="success" title="密码重置成功" sub-title="请使用新密码登录，本次重置链接已失效" />
        <div class="footer-links">
          <el-button type="primary" class="submit-btn" @click="router.push('/login')">前往登录</el-button>
        </div>
      </template>

      <el-form
        v-else
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        size="large"
        @keyup.enter="submit"
      >
        <el-form-item prop="newPassword">
          <el-input
            v-model="form.newPassword"
            :prefix-icon="Lock"
            type="password"
            placeholder="新密码"
            show-password
            maxlength="32"
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            :prefix-icon="Lock"
            type="password"
            placeholder="确认新密码"
            show-password
            maxlength="32"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">
            重置密码
          </el-button>
        </el-form-item>
        <div class="footer-links">
          <el-button link type="primary" @click="router.push('/login')">返回登录</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 令牌仅来自邮件链接 query；未知来源页不展示表单
const token = computed(() => route.query.token || '')

const loading = ref(false)
const done = ref(false)
const formRef = ref()

const form = reactive({ newPassword: '', confirmPassword: '' })

const rules = computed(() => ({
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在6~32之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        value === form.newPassword ? callback() : callback(new Error('两次输入的密码不一致'))
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
    await userStore.completePasswordReset(token.value, form.newPassword)
    done.value = true
  } catch (e) {
    // 令牌无效/过期/新旧密码相同等错误已由 axios 拦截器统一弹提示，留在本页可重试或重新申请
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.reset-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2d3d 0%, #40bad5 100%);
}

.reset-card {
  width: 440px;
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

.footer-links {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
</style>
