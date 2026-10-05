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

      <el-form-item label="封面图">
        <div class="cover-upload">
          <el-upload
            :file-list="coverDisplayList"
            :limit="coverLimit"
            list-type="picture-card"
            accept="image/jpeg,image/png,image/gif,image/webp"
            :before-upload="beforeImageUpload"
            :http-request="req => doUpload(req, 'cover')"
            :on-remove="removeCover"
            :on-preview="handlePreview"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="cover-hint">
            <span>最多 {{ coverLimit }} 张，当前 {{ form.coverUrls.length }}/{{ coverLimit }}；可点击封面右上角 × 移除</span>
            <span class="tip">详情页将按上传顺序轮播展示</span>
          </div>
        </div>
        <el-dialog v-model="previewVisible" width="600px" append-to-body :show-close="true">
          <img :src="previewUrl" style="width: 100%" alt="预览" />
        </el-dialog>
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
              <!-- 来源信息：仅分类开启来源能力时展示（台词/歌词/书摘等摘录型榜单），均为可选 -->
              <div v-if="sourceEnabled" class="item-source-row">
                <el-select
                  v-model="item.sourceType"
                  placeholder="类型"
                  clearable
                  class="source-type"
                >
                  <el-option
                    v-for="(label, key) in sourceTypeOptions"
                    :key="key"
                    :label="label"
                    :value="key"
                  />
                </el-select>
                <el-autocomplete
                  v-model.trim="item.sourceName"
                  :fetch-suggestions="fetchSourceSuggestions"
                  placeholder="来源作品名称（可选），如：让子弹飞"
                  maxlength="100"
                  clearable
                  class="source-name"
                />
              </div>
              <el-input
                v-if="sourceEnabled"
                v-model="item.sourceDesc"
                placeholder="来源补充说明（可选），如出现的场景/章节"
                maxlength="500"
              />
              <el-input
                v-model="item.reason"
                type="textarea"
                :autosize="{ minRows: 1, maxRows: 3 }"
                placeholder="你的推荐理由（可选）"
                maxlength="1000"
              />
              <div class="item-image-upload">
                <template v-if="item.imageUrl">
                  <img :src="resolveImage(item.imageUrl)" class="item-image-thumb" alt="" />
                  <el-button link type="danger" size="small" :icon="Delete" @click="item.imageUrl = ''">移除图片</el-button>
                </template>
                <el-upload
                  v-else
                  :show-file-list="false"
                  :disabled="!item.name"
                  :before-upload="beforeImageUpload"
                  :http-request="req => doUpload(req, index)"
                  accept="image/jpeg,image/png,image/gif,image/webp"
                >
                  <el-button link type="primary" size="small" :icon="Plus">上传图片</el-button>
                </el-upload>
                <span v-if="!item.name" class="tip">先填名称后可上传图片</span>
              </div>
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
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { createRanking, getSourceTypes, searchSourceNames, getCoverLimit } from '@/api/ranking'
import { uploadImage } from '@/api/file'
import { resolveImage } from '@/utils/image'
import { useUserStore } from '@/store/user'

const props = defineProps({
  categories: { type: Array, default: () => [] }
})
const emit = defineEmits(['created'])

const visible = ref(false)
const submitting = ref(false)
const formRef = ref()
// 封面最大数量：后台“系统设置”配置 ranking.detail.cover_limit，接口拉取；
// 拉取失败（如未登录 401）兜底为 1，保证至少可上传 1 张
const coverLimit = ref(1)
const previewVisible = ref(false)
const previewUrl = ref('')
const userStore = useUserStore()

// 单元素初始值工厂：来源字段随展开收起保留草稿，统一在此声明防遗漏
// 注意：const 无提升，必须声明在 form 之前（form 初始化时即调用）
const newItem = () => ({
  name: '', reason: '', imageUrl: '',
  sourceType: '', sourceName: '', sourceDesc: ''
})

const form = reactive({
  title: '',
  description: '',
  categoryId: null,
  itemLimit: 10,
  visibility: 1,
  // 封面相对路径列表（/uploads/xxx），数组顺序即详情页轮播顺序
  coverUrls: [],
  items: [newItem()]
})

// el-upload picture-card 展示列表：由 form.coverUrls 派生（事实源只有相对路径数组一份）
const coverDisplayList = computed(() =>
  form.coverUrls.map((u, i) => ({ name: `封面${i + 1}`, url: resolveImage(u) }))
)

// 来源能力由分类配置驱动（sourceEnabled=1），不在前端硬编码分类判断
const sourceEnabled = computed(() => {
  const cat = props.categories.find(c => String(c.id) === String(form.categoryId))
  return cat?.sourceEnabled === 1
})

// 来源类型字典：首次需要时拉取，失败不阻断（下拉退化为空选项，仍可手填作品名）
const sourceTypeOptions = ref({})
watch(sourceEnabled, async enabled => {
  if (!enabled || Object.keys(sourceTypeOptions.value).length > 0) return
  try {
    const res = await getSourceTypes()
    sourceTypeOptions.value = res.data || {}
  } catch (e) {
    // 未登录/服务异常：拦截器已弹错，留空字典即可
  }
}, { immediate: true })

// 作品名站内补全：输入时实时查历史去重列表；未登录不发起请求避免反复 401
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
  loadCoverLimit()
}

// 封面上限走配置中心单一事实源，不在前端硬编码（同来源字典白名单思路）
async function loadCoverLimit() {
  if (!userStore.isLogin) return
  try {
    const res = await getCoverLimit()
    coverLimit.value = Number(res.data) || 1
  } catch (e) {
    // 拉取失败保持兜底上限，不阻断表单
  }
}

function onClosed() {
  formRef.value?.resetFields()
  form.items = [newItem()]
  form.coverUrls = []
  form.categoryId = null
  form.itemLimit = 10
  form.visibility = 1
}

function addItem() {
  form.items.push(newItem())
}

function removeItem(index) {
  form.items.splice(index, 1)
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

// 自定义上传：走 /files/image 拿相对路径回填封面列表；target='cover' 为榜单封面，否则为对应下标排名项图
async function doUpload(req, target) {
  try {
    const res = await uploadImage(req.file)
    const url = res.data
    if (target === 'cover') {
      if (form.coverUrls.length >= coverLimit.value) {
        ElMessage.warning(`封面图最多上传 ${coverLimit.value} 张`)
        return
      }
      form.coverUrls.push(url)
      if (form.coverUrls.length === coverLimit.value) {
        ElMessage.success(`已达封面数量上限（${coverLimit.value} 张）`)
      }
    } else {
      form.items[target].imageUrl = url
    }
  } catch (e) {
    // 失败：不写入列表即可重试（request 拦截器已弹错）
    if (target !== 'cover') {
      form.items[target].imageUrl = ''
    }
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
      // 封面多图：可选（不传即无封面），上限由后端按配置中心同口径校验
      coverUrls: form.coverUrls,
      categoryId: form.categoryId,
      itemLimit: form.itemLimit,
      visibility: form.visibility,
      items: items.map(i => ({
        name: i.name,
        reason: i.reason,
        imageUrl: i.imageUrl || null,
        // 分类未开启来源能力时不提交来源字段（后端也会按分类配置整组丢弃）
        ...(sourceEnabled.value && {
          sourceType: i.sourceType || null,
          sourceName: i.sourceName || null,
          sourceDesc: i.sourceDesc || null
        })
      }))
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
.item-image-upload {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 32px;
}
.item-image-thumb {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}
.item-source-row {
  display: flex;
  gap: 8px;
}
.source-type {
  width: 120px;
  flex-shrink: 0;
}
.source-name {
  flex: 1;
}
</style>
