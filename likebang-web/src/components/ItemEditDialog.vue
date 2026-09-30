<template>
  <el-dialog
    v-model="visible"
    title="编辑排名项"
    width="560px"
    :close-on-click-modal="false"
    @closed="onClosed"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="名称" prop="name">
        <el-input
          v-model.trim="form.name"
          placeholder="如《百年孤独》"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="2"
          maxlength="1000"
          show-word-limit
          placeholder="对这个排名项的说明（可留空）"
        />
      </el-form-item>

      <el-form-item label="配图">
        <image-upload v-model="form.imageUrl" label="上传配图" />
      </el-form-item>

      <!-- 来源信息：仅分类开启来源能力时展示（台词/歌词/书摘等摘录型榜单），均为可选 -->
      <template v-if="sourceEnabled">
        <el-form-item label="来源类型">
          <el-select v-model="form.sourceType" placeholder="请选择（可选）" clearable style="width: 200px">
            <el-option v-for="(label, key) in sourceTypeOptions" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源作品">
          <el-autocomplete
            v-model.trim="form.sourceName"
            :fetch-suggestions="fetchSourceSuggestions"
            placeholder="作品名称（可选），如：让子弹飞"
            maxlength="100"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="来源说明">
          <el-input
            v-model="form.sourceDesc"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="如台词出现的场景/章节（可选）"
          />
        </el-form-item>
      </template>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { updateRankingItem, listCategories, getSourceTypes, searchSourceNames } from '@/api/ranking'
import ImageUpload from '@/components/ImageUpload.vue'
import { useUserStore } from '@/store/user'

const emit = defineEmits(['updated'])

const visible = ref(false)
const submitting = ref(false)
const formRef = ref()
const userStore = useUserStore()

const rankingId = ref(null)
const itemId = ref(null)
const categoryId = ref(null)

const form = reactive({
  name: '',
  description: '',
  imageUrl: '',
  sourceType: '',
  sourceName: '',
  sourceDesc: ''
})

const rules = {
  name: [
    { required: true, message: '请输入排名项名称', trigger: 'blur' },
    { max: 200, message: '名称不能超过200字', trigger: 'blur' }
  ]
}

// 分类列表（判断来源能力 + 未来扩展），懒加载一次
const categories = ref([])

// 来源能力由分类配置驱动（sourceEnabled=1），不硬编码分类判断
const sourceEnabled = computed(() => {
  const cat = categories.value.find(c => String(c.id) === String(categoryId.value))
  return cat?.sourceEnabled === 1
})

// 来源类型字典：需要时拉取一次，失败不阻断（下拉留空仍可手填作品名）
const sourceTypeOptions = ref({})
watch(sourceEnabled, async enabled => {
  if (!enabled || Object.keys(sourceTypeOptions.value).length > 0) return
  try {
    const res = await getSourceTypes()
    sourceTypeOptions.value = res.data || {}
  } catch (e) {
    // 拦截器已弹错，留空字典即可
  }
}, { immediate: true })

// 作品名站内补全：未登录不发起请求，避免反复 401
async function fetchSourceSuggestions(queryString, cb) {
  if (!userStore.isLogin) {
    cb([])
    return
  }
  try {
    const res = await searchSourceNames(queryString || undefined)
    cb((res.data || []).map(name => ({ value: name })))
  } catch (e) {
    cb([])
  }
}

async function open(item, id, catId) {
  rankingId.value = id
  itemId.value = item.id
  categoryId.value = catId ?? null
  form.name = item.name || ''
  form.description = item.description || ''
  form.imageUrl = item.imageUrl || ''
  form.sourceType = item.sourceType || ''
  form.sourceName = item.sourceName || ''
  form.sourceDesc = item.sourceDesc || ''
  visible.value = true

  if (categories.value.length === 0) {
    try {
      const res = await listCategories()
      categories.value = res.data || []
    } catch (e) {
      categories.value = []
    }
  }
}

function onClosed() {
  formRef.value?.resetFields()
  rankingId.value = null
  itemId.value = null
  categoryId.value = null
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await updateRankingItem(rankingId.value, itemId.value, {
      name: form.name,
      description: form.description ?? '',
      imageUrl: form.imageUrl ?? '',
      // 分类未开启来源能力时不提交来源字段（后端也会按分类配置整组置空）
      ...(sourceEnabled.value && {
        sourceType: form.sourceType || '',
        sourceName: form.sourceName || '',
        sourceDesc: form.sourceDesc || ''
      })
    })
    ElMessage.success('排名项已更新')
    visible.value = false
    emit('updated')
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>
