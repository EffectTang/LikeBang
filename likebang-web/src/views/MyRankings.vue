<template>
  <div class="my-rankings">
    <div class="toolbar">
      <div class="toolbar-left">
        <h2 class="page-title">我的榜单</h2>
        <span class="page-sub">你发起的全部榜单，可直接编辑或删除</span>
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
        <el-button type="primary" :icon="Plus" @click="goCommunity">发起榜单</el-button>
      </div>
    </div>

    <!-- 状态筛选 -->
    <div class="status-bar">
      <el-radio-group v-model="query.status" @change="reload">
        <el-radio-button :value="null">全部</el-radio-button>
        <el-radio-button :value="1">已发布</el-radio-button>
        <el-radio-button :value="0">草稿</el-radio-button>
        <el-radio-button :value="2">已下架</el-radio-button>
      </el-radio-group>
    </div>

    <el-skeleton v-if="loading && list.length === 0" :rows="4" animated />
    <el-empty v-else-if="list.length === 0" description="还没有创建过榜单，去「发现榜单」发起第一个吧" />
    <div v-else class="card-grid" v-loading="loading">
      <el-card v-for="r in list" :key="r.id" class="ranking-card" shadow="hover">
        <div class="card-title" @click="goDetail(r.id)">
          <span class="title-text">{{ r.title }}</span>
          <el-tag size="small" :type="statusTagType(r.status)">{{ statusText(r.status) }}</el-tag>
          <el-tag v-if="r.visibility === 0" size="small" type="info">私有</el-tag>
          <el-tag v-else-if="r.visibility === 2" size="small" type="info">仅链接</el-tag>
        </div>
        <div class="card-desc">{{ r.description || '暂无描述' }}</div>
        <div class="card-meta">
          <span>🏆 Top {{ r.itemLimit }}</span>
          <span>💬 {{ r.itemCount }} 项</span>
          <span>👁 {{ r.viewCount }}</span>
          <span>🙋 {{ r.participantCount || 0 }} 人参与</span>
        </div>
        <div class="card-footer">
          <div class="footer-left">
            <el-tag v-if="r.categoryName" size="small" type="info">{{ r.categoryName }}</el-tag>
            <span class="time">{{ formatDate(r.createdAt) }}</span>
          </div>
          <div class="footer-ops">
            <el-button link type="primary" size="small" :icon="EditPen" @click="openEdit(r)">编辑</el-button>
            <el-button link type="danger" size="small" :icon="Delete" @click="handleDelete(r)">删除</el-button>
          </div>
        </div>
      </el-card>
    </div>

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

    <ranking-edit-dialog ref="editDialogRef" @updated="loadList" />
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, EditPen, Delete } from '@element-plus/icons-vue'
import { listMyRankings, deleteRanking } from '@/api/ranking'
import RankingEditDialog from '@/components/RankingEditDialog.vue'

const router = useRouter()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const editDialogRef = ref()

const query = reactive({
  current: 1,
  size: 12,
  keyword: '',
  status: null
})

// 后端全局 Long→String 序列化策略会把 total 下发成字符串，这里统一归一化为数字
const totalPages = computed(() =>
  Math.max(1, Math.ceil((Number(total.value) || 0) / query.size)))

async function loadList() {
  loading.value = true
  try {
    const res = await listMyRankings({
      current: query.current,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status ?? undefined
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

function statusText(status) {
  const map = { 0: '草稿', 1: '已发布', 2: '已下架' }
  return map[status] ?? status
}

function statusTagType(status) {
  const map = { 0: 'info', 1: 'success', 2: 'warning' }
  return map[status] ?? 'info'
}

function formatDate(value) {
  if (!value) return ''
  return String(value).slice(0, 10)
}

function goDetail(id) {
  router.push(`/rankings/${id}`)
}

function goCommunity() {
  router.push('/community')
}

function openEdit(ranking) {
  editDialogRef.value.open(ranking)
}

function handleDelete(ranking) {
  ElMessageBox.confirm(`确定要删除榜单「${ranking.title}」吗？删除后不可恢复。`, '提示', {
    type: 'warning'
  }).then(async () => {
    await deleteRanking(ranking.id)
    ElMessage.success('删除成功')
    loadList()
  }).catch(() => {})
}

onMounted(loadList)
</script>

<style scoped>
.my-rankings {
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
.status-bar {
  margin-bottom: 18px;
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
.ranking-card {
  border-radius: 10px;
}
.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 600;
  margin-bottom: 8px;
  cursor: pointer;
}
.title-text {
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
.footer-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.time {
  color: #909399;
  font-size: 12px;
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
