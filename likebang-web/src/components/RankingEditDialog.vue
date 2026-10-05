<template>
  <el-dialog
    v-model="visible"
    title="编辑榜单"
    width="560px"
    :close-on-click-modal="false"
    @closed="onClosed"
  >
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="edit-tip"
      title="排名项与理由是社区共同积累的数据，编辑榜单仅修改主题信息；排名项管理将随后续版本开放"
    />
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="标题" prop="title">
        <el-input
          v-model.trim="form.title"
          placeholder="例如：大学最值得读的10本书"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 240px">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>

      <el-form-item label="榜单说明" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="2"
          maxlength="1000"
          show-word-limit
          placeholder="介绍一下这个榜单的主题与规则"
        />
      </el-form-item>

      <el-form-item label="封面图">
        <div class="cover-upload">
          <el-upload
            :file-list="coverDisplayList"
            :limit="coverLimit"
            list-type="picture-card"
            accept="image/jpeg,image/png,image/gif,image/webp"
            :before-upload="beforeImageUpload"
            :http-request="doUpload"
            :on-remove="removeCover"
            :on-preview="handlePreview"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="cover-hint">
            <span>最多 {{ coverLimit }} 张，当前 {{ form.coverUrls.length }}/{{ coverLimit }}；保存时按当前列表整组替换</span>
            <span class="tip">详情页将按此处顺序轮播展示</span>
          </div>
        </div>
        <el-dialog v-model="previewVisible" width="600px" append-to-body :show-close="true">
          <img :src="previewUrl" style="width: 100%" alt="预览" />
        </el-dialog>
      </el-form-item>

      <el-form-item label="名次数量" prop="itemLimit">
        <el-input-number v-model="form.itemLimit" :min="itemLimitMin" :max="50" />
        <span class="tip">当前已有 {{ form.itemCount }} 个排名项，不能调小至此数量以下</span>
      </el-form-item>

      <el-form-item label="可见性" prop="visibility">
        <el-radio-group v-model="form.visibility">
          <el-radio :value="1">公开</el-radio>
          <el-radio :value="2">仅链接可见</el-radio>
          <el-radio :value="0">私有</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { updateRanking, listCategories, getCoverLimit } from '@/api/ranking'
import { uploadImage } from '@/api/file'
import { resolveImage } from '@/utils/image'

const emit = defineEmits(['updated'])

const visible = ref(false)
const submitting = ref(false)
const formRef = ref()
const categories = ref([])
// 封面最大数量：优先用详情接口下发的 coverLimit，缺失时走 /rankings/cover-limit 拉取；
// 均失败则兜底为 1，保证至少可保留/上传 1 张
const coverLimit = ref(1)
const previewVisible = ref(false)
const previewUrl = ref('')

const form = reactive({
  id: null,
  title: '',
  description: '',
  // 封面相对路径列表（/uploads/xxx），数组顺序即详情页轮播顺序；提交时整组替换
  coverUrls: [],
  categoryId: null,
  itemLimit: 10,
  visibility: 1,
  itemCount: 0
})

// el-upload picture-card 展示列表：由 form.coverUrls 派生（事实源只有相对路径数组一份）
const coverDisplayList = computed(() =>
  form.coverUrls.map((u, i) => ({ name: `封面${i + 1}`, url: resolveImage(u) }))
)

const rules = {
  title: [
    { required: true, message: '请输入榜单标题', trigger: 'blur' },
    { max: 100, message: '标题不能超过100字', trigger: 'blur' }
  ],
  itemLimit: [{ required: true, message: '请设置名次数量', trigger: 'change' }]
}

// 名次数量下限：至少 3，且不得低于当前排名项数量（后端同规则兜底）
const itemLimitMin = computed(() => Math.max(3, form.itemCount || 0))

async function open(ranking) {
  form.id = ranking.id
  form.title = ranking.title || ''
  form.description = ranking.description || ''
  // 后端已含历史数据回落（无多图记录时 coverUrls 为单元素），再兜一层防旧接口缓存
  form.coverUrls = ranking.coverUrls?.length
    ? [...ranking.coverUrls]
    : (ranking.coverUrl ? [ranking.coverUrl] : [])
  form.categoryId = ranking.categoryId ?? null
  form.itemLimit = ranking.itemLimit ?? 10
  form.visibility = ranking.visibility ?? 1
  form.itemCount = ranking.itemCount ?? 0
  visible.value = true

  // 封面上限：详情接口已下发则直接用，否则拉配置接口（接口失败不阻断编辑）
  if (Number(ranking.coverLimit) > 0) {
    coverLimit.value = Number(ranking.coverLimit)
  } else {
    try {
      const res = await getCoverLimit()
      coverLimit.value = Number(res.data) || 1
    } catch (e) {
      coverLimit.value = 1
    }
  }

  // 分类列表懒加载一次，失败不阻塞编辑
  if (categories.value.length === 0) {
    try {
      const res = await listCategories()
      categories.value = res.data
    } catch (e) {
      categories.value = []
    }
  }
}

function onClosed() {
  formRef.value?.resetFields()
  form.id = null
  form.coverUrls = []
}

// 上传前置校验：格式 + 大小（与后端白名单/限流口径一致）
function beforeImageUpload(file) {
  if (!/^image\/(jpeg|png|gif|webp)$/.test(file.type)) {
    ElMessage.warning('仅支持 jpg/png/gif/webp 格式图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('单张图片不能超过 5MB')
    return false
  }
  return true
}

function handlePreview(file) {
  previewUrl.value = file.url
  previewVisible.value = true
}

async function doUpload(req) {
  try {
    const res = await uploadImage(req.file)
    const url = res.data
    if (form.coverUrls.length >= coverLimit.value) {
      ElMessage.warning(`封面图最多上传 ${coverLimit.value} 张`)
      return
    }
    form.coverUrls.push(url)
    if (form.coverUrls.length === coverLimit.value) {
      ElMessage.success(`已达封面数量上限（${coverLimit.value} 张）`)
    }
  } catch (e) {
    // 失败：不写入列表即可重试（request 拦截器已弹错）
  }
}

// 移除封面：按展示列表位置从事实源数组中剔除同下标路径
function removeCover(file) {
  const idx = coverDisplayList.value.findIndex(f => f.url === file.url)
  if (idx >= 0) form.coverUrls.splice(idx, 1)
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 封面至少保留 1 张（上限由后台系统设置驱动，下限固定为 1）；后端按同口径兜底校验
  if (form.coverUrls.length === 0) {
    ElMessage.warning('请至少保留 1 张封面图')
    return
  }

  submitting.value = true
  try {
    await updateRanking(form.id, {
      title: form.title,
      description: form.description ?? '',
      coverUrls: [...form.coverUrls],
      categoryId: form.categoryId,
      itemLimit: form.itemLimit,
      visibility: form.visibility
    })
    ElMessage.success('榜单已更新')
    visible.value = false
    emit('updated')
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.edit-tip {
  margin-bottom: 16px;
}
.cover-upload {
  width: 100%;
}
.cover-hint {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}
.tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}
</style>
