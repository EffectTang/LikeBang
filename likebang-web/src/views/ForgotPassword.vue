<template>
  <div class="reset-page">
    <el-card class="reset-card">
      <template #header>
        <div class="card-header">
          <h2>找回密码</h2>
          <span class="sub">通过注册邮箱重置密码</span>
        </div>
      </template>

      <!-- 申请成功：不透露邮箱是否注册，统一口径引导查收 -->
      <template v-if="sent">
        <el-result icon="success" title="申请已提交" sub-title="若该邮箱已注册账号，我们已发送重置邮件，请登录邮箱（含垃圾邮件箱）点击链接完成重置。链接 30 分钟内有效且仅可使用一次。" />
        <div class="footer-links">
          <el-button link type="primary" @click="sent = false">重新申请</el-button>
          <el-button link type="primary" @click="router.push('/login')">返回登录</el-button>
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
        <el-alert
          type="info"
          :closable="false"
          class="tip"
          title="重置邮件将发送到账号注册时填写的邮箱；微信登录账号请直接在小程序内登录"
        />
        <el-form-item prop="email">
          <el-input
            v-model.trim="form.email"
            :prefix-icon="Message"
            placeholder="注册邮箱"
            maxlength="128"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">
            发送重置邮件
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
import { useRouter } from 'vue-router'
import { Message } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const sent = ref(false)
const formRef = ref()

const form = reactive({ email: '' })

const rules = computed(() => ({
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}))

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    // 后端防账号探测恒返回成功；频控等业务错误已由 axios 拦截器弹提示并 reject
    await userStore.requestPasswordReset(form.email)
    sent.value = true
  } catch (e) {
    // 错误提示已在拦截器统一处理，此处仅结束 loading
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

.tip {
  margin-bottom: 16px;
}

.submit-btn {
  width: 100%;
}

.footer-links {
  display: flex;
  justify-content: center;
  gap: 8px;
}
</style>
