<template>
  <!-- 个人资料编辑：仅本人，userId 由后端登录态确定（沿用 4.12 防提权约定） -->
  <el-dialog v-model="visible" title="编辑资料" width="440px">
    <el-form label-width="80px">
      <el-form-item label="头像">
        <div class="avatar-edit">
          <el-avatar :size="64" :src="avatarSrc">
            {{ (form.nickname || username || 'U').charAt(0) }}
          </el-avatar>
          <el-upload :show-file-list="false" :http-request="onUpload" accept="image/*">
            <el-button size="small">上传头像</el-button>
          </el-upload>
          <el-button v-if="form.avatarUrl" size="small" text type="danger" @click="form.avatarUrl = ''">移除</el-button>
        </div>
      </el-form-item>
      <el-form-item label="用户名">
        <el-input :value="username" disabled />
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
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { uploadImage } from '@/api/file'
import { resolveImage } from '@/utils/image'

const emit = defineEmits(['saved'])
const userStore = useUserStore()

const visible = ref(false)
const saving = ref(false)
const form = reactive({ nickname: '', avatarUrl: '', intro: '' })

const username = computed(() => userStore.userInfo?.username)
const avatarSrc = computed(() => resolveImage(form.avatarUrl))

// 由父组件调用：以当前登录用户资料初始化表单后打开
function open() {
  const u = userStore.userInfo || {}
  form.nickname = u.nickname || ''
  form.avatarUrl = u.avatarUrl || ''
  form.intro = u.intro || ''
  visible.value = true
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

async function save() {
  if (!form.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    // 头像/简介传空串=清空；后端返回最新用户信息并刷新本地缓存
    await userStore.updateProfile({
      nickname: form.nickname.trim(),
      avatarUrl: form.avatarUrl,
      intro: form.intro
    })
    ElMessage.success('已保存')
    visible.value = false
    emit('saved')
  } catch (e) {
    // 错误提示已由 request 拦截器统一处理
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.avatar-edit {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
