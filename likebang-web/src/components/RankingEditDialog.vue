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
import { updateRanking, listCategories } from '@/api/ranking'

const emit = defineEmits(['updated'])

const visible = ref(false)
const submitting = ref(false)
const formRef = ref()
const categories = ref([])

const form = reactive({
  id: null,
  title: '',
  description: '',
  categoryId: null,
  itemLimit: 10,
  visibility: 1,
  itemCount: 0
})

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
  form.categoryId = ranking.categoryId ?? null
  form.itemLimit = ranking.itemLimit ?? 10
  form.visibility = ranking.visibility ?? 1
  form.itemCount = ranking.itemCount ?? 0
  visible.value = true

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
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await updateRanking(form.id, {
      title: form.title,
      description: form.description ?? '',
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
.tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}
</style>
