<template>
  <div class="community">
    <!-- 顶部操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <h2 class="page-title">发现榜单</h2>
        <span class="page-sub">看看大家在排什么，也发起一个你的观点榜</span>
      </div>
      <div class="toolbar-right">
        <el-input
          v-model="query.keyword"
          placeholder="搜索榜单标题 / 描述"
          clearable
          :prefix-icon="Search"
          class="search-input"
          @keyup.enter="reload"
          @clear="reload"
        />
        <el-button type="primary" :icon="Plus" @click="openCreate">发起榜单</el-button>
      </div>
    </div>

    <!-- 分类筛选 -->
    <div class="category-bar">
      <el-tag
        :effect="query.categoryId == null ? 'dark' : 'plain'"
        class="cat-tag"
        @click="selectCategory(null)"
      >
        全部
      </el-tag>
      <el-tag
        v-for="c in categories"
        :key="c.id"
        :effect="query.categoryId === c.id ? 'dark' : 'plain'"
        class="cat-tag"
        @click="selectCategory(c.id)"
      >
        {{ c.name }}
      </el-tag>
    </div>

    <!-- 榜单卡片列表 -->
    <el-skeleton v-if="loading && list.length === 0" :rows="4" animated />
    <el-empty v-else-if="list.length === 0" description="还没有榜单，快来发起第一个吧" />
    <div v-else class="card-grid" v-loading="loading">
      <el-card
        v-for="r in list"
        :key="r.id"
        class="ranking-card"
        shadow="hover"
        @click="goDetail(r.id)"
      >
        <img v-if="r.coverUrl" :src="resolveImage(r.coverUrl)" class="card-cover" alt="封面" />
        <div class="card-title">{{ r.title }}</div>
        <div class="card-desc">{{ r.description || '暂无描述' }}</div>
        <div class="card-meta">
          <span>🏆 Top {{ r.itemLimit }}</span>
          <span>👁 {{ r.viewCount }}</span>
          <span>💬 {{ r.itemCount }} 项</span>
        </div>
        <div class="card-footer">
          <el-tag v-if="r.categoryName" size="small" type="info">{{ r.categoryName }}</el-tag>
          <span class="creator">@{{ r.creatorNickname || '匿名' }}</span>
        </div>
      </el-card>
    </div>

    <!-- 分页 -->
    <div class="pager" v-if="total > 0">
      <span class="page-info">第 {{ query.current }} / {{ totalPages }} 页 · 共 {{ total }} 条</span>
      <el-pagination
        v-model:current-page="query.current"
        :page-size="query.size"
        :page-sizes="[12, 24, 48]"
        :total="total"
        layout="sizes, prev, pager, next, jumper"
        background
        @current-change="loadList"
        @size-change="onSizeChange"
      />
    </div>

    <ranking-create-dialog ref="createDialogRef" :categories="categories" @created="onCreated" />
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus } from '@element-plus/icons-vue'
import { listPublicRankings, listCategories } from '@/api/ranking'
import { resolveImage } from '@/utils/image'
import RankingCreateDialog from '@/components/RankingCreateDialog.vue'

const router = useRouter()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const categories = ref([])
const createDialogRef = ref()

const query = reactive({
  current: 1,
  size: 12,
  keyword: '',
  categoryId: null
})

// 后端全局 Long→String 序列化策略会把 total 下发成字符串，这里统一归一化为数字
const totalPages = computed(() =>
  Math.max(1, Math.ceil((Number(total.value) || 0) / query.size)))

async function loadList() {
  loading.value = true
  try {
    const res = await listPublicRankings({
      current: query.current,
      size: query.size,
      keyword: query.keyword || undefined,
      categoryId: query.categoryId || undefined
    })
    list.value = res.data.records
    total.value = Number(res.data.total) || 0
  } finally {
    loading.value = false
  }
}

function reload() {
  query.current = 1
  loadList()
}

// 切换每页条数：回到第一页重新拉取
function onSizeChange(val) {
  query.size = val
  reload()
}

function selectCategory(id) {
  query.categoryId = id
  reload()
}

function goDetail(id) {
  router.push(`/rankings/${id}`)
}

function openCreate() {
  createDialogRef.value.open()
}

function onCreated(id) {
  reload()
  if (id) router.push(`/rankings/${id}`)
}

onMounted(async () => {
  try {
    const res = await listCategories()
    categories.value = res.data
  } catch (e) {
    // 分类加载失败不阻塞榜单浏览
  }
  loadList()
})
</script>

<style scoped>
.community {
  max-width: 1080px;
  margin: 0 auto;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
.page-title {
  margin: 0;
  font-size: 22px;
}
.page-sub {
  color: #909399;
  font-size: 13px;
}
.toolbar-right {
  display: flex;
  gap: 12px;
}
.search-input {
  width: 260px;
}
.category-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 18px;
}
.cat-tag {
  cursor: pointer;
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
.ranking-card {
  cursor: pointer;
  border-radius: 10px;
}
.card-cover {
  display: block;
  /* 满宽 = 卡片宽（抵消左右内边距），配合 object-fit: cover 让图片内容居中裁剪 */
  width: calc(100% + 40px);
  height: 150px;
  object-fit: cover;
  border-radius: 10px 10px 0 0;
  /* 左右必须 -20px 抵消 el-card 正文内边距形成通栏；用 auto 会把负margin抵消、图框无法拉到边缘导致左空右溢不对称 */
  margin: -20px -20px 12px;
}
.card-title {
  font-size: 17px;
  font-weight: 600;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-desc {
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
  height: 40px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 12px;
}
.card-meta {
  display: flex;
  gap: 16px;
  color: #909399;
  font-size: 12px;
  margin-bottom: 12px;
}
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f0f0f0;
  padding-top: 10px;
}
.creator {
  color: #409eff;
  font-size: 13px;
}
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
}
.page-info {
  color: #606266;
  font-size: 13px;
  white-space: nowrap;
}
</style>
