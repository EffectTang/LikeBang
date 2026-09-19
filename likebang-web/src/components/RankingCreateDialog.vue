<template>
  <el-dialog
    v-model="visible"
    title="发起排行榜"
    width="720px"
    :close-on-click-modal="false"
    @open="onOpen"
    @closed="onClosed"
  >
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
        <el-select v-model="form.categoryId" placeholder="请选择分类（可选）" clearable style="width: 240px">
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
        <el-input-number v-model="form.itemLimit" :min="3" :max="50" />
        <span class="tip">榜单最多可容纳的排名项数量</span>
      </el-form-item>

      <el-form-item label="排名项" prop="items" required>
        <div class="items-editor">
          <div v-for="(item, index) in form.items" :key="index" class="item-row">
            <div class="item-index">{{ index + 1 }}</div>
            <div class="item-fields">
              <el-input
                v-model.trim="item.name"
                placeholder="名称，如《百年孤独》"
                maxlength="200"
                class="item-name"
              />
              <el-input
                v-model="item.reason"
                type="textarea"
                :autosize="{ minRows: 1, maxRows: 3 }"
                placeholder="你的推荐理由（可选）"
                maxlength="1000"
              />
            </div>
            <el-button
              link
              type="danger"
              :icon="Delete"
              :disabled="form.items.length <= 1"
              @click="removeItem(index)"
            />
          </div>
          <el-button
            type="primary"
            plain
            :icon="Plus"
            :disabled="form.items.length >= form.itemLimit"
            @click="addItem"
          >
            添加一项（{{ form.items.length }}/{{ form.itemLimit }}）
          </el-button>
        </div>
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
      <el-button type="primary" :loading="submitting" @click="submit">发布榜单</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { createRanking } from '@/api/ranking'

const props = defineProps({
  categories: { type: Array, default: () => [] }
})
const emit = defineEmits(['created'])

const visible = ref(false)
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  title: '',
  description: '',
  categoryId: null,
  itemLimit: 10,
  visibility: 1,
  items: [{ name: '', reason: '' }]
})

const rules = {
  title: [
    { required: true, message: '请输入榜单标题', trigger: 'blur' },
    { max: 100, message: '标题不能超过100字', trigger: 'blur' }
  ],
  itemLimit: [{ required: true, message: '请设置名次数量', trigger: 'change' }]
}

function open() {
  visible.value = true
}

function onOpen() {
  // 外部数据（categories）变化不影响已填内容，此处仅重置一次性提交态
  submitting.value = false
}

function onClosed() {
  formRef.value?.resetFields()
  form.items = [{ name: '', reason: '' }]
  form.categoryId = null
  form.itemLimit = 10
  form.visibility = 1
}

function addItem() {
  form.items.push({ name: '', reason: '' })
}

function removeItem(index) {
  form.items.splice(index, 1)
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const items = form.items.filter(i => i.name)
  if (items.length < 2) {
    ElMessage.warning('请至少填写 2 个排名项名称')
    return
  }
  if (items.length > form.itemLimit) {
    ElMessage.warning(`排名项数量不能超过 ${form.itemLimit}`)
    return
  }

  submitting.value = true
  try {
    const res = await createRanking({
      title: form.title,
      description: form.description,
      categoryId: form.categoryId,
      itemLimit: form.itemLimit,
      visibility: form.visibility,
      items: items.map(i => ({ name: i.name, reason: i.reason }))
    })
    ElMessage.success('榜单发布成功')
    visible.value = false
    emit('created', res.data)
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}
.items-editor {
  width: 100%;
}
.item-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 10px;
}
.item-index {
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 12px;
  flex-shrink: 0;
  margin-top: 4px;
}
.item-fields {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
</style>
