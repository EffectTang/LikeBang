<template>
  <div class="my-space">
    <!-- 资料头卡：本人视角（复用他人主页公开区 + 编辑入口 + 私密统计） -->
    <el-card class="profile-card" shadow="never">
      <div class="profile-head">
        <el-avatar :size="72" :src="avatarSrc">
          {{ (profile.nickname || profile.username || 'U').charAt(0) }}
        </el-avatar>
        <div class="profile-info">
          <div class="profile-name">{{ profile.nickname || profile.username || '匿名用户' }}</div>
          <div class="profile-join">加入于 {{ fmtDate(profile.createdAt) }}</div>
        </div>
        <el-button class="edit-btn" plain :icon="EditPen" @click="openEdit">编辑资料</el-button>
      </div>
      <p class="profile-intro">{{ profile.intro || '还没有填写自我介绍，点「编辑资料」介绍一下自己吧' }}</p>

      <!-- 数据概览：后端全局 Long→String，计数须 Number() 归一化后展示 -->
      <div class="stats-bar" v-loading="statsLoading">
        <div class="stat">
          <span class="stat-num">{{ stats.rankingCount }}</span>
          <span class="stat-label">我发布的榜单</span>
        </div>
        <div class="stat">
          <span class="stat-num">{{ stats.receivedVoteCount }}</span>
          <span class="stat-label">收到的投票</span>
        </div>
        <div class="stat">
          <span class="stat-num">{{ stats.joinedRankingCount }}</span>
          <span class="stat-label">参与投票的榜单</span>
        </div>
        <div class="stat">
          <span class="stat-num">{{ stats.bookmarkCount }}</span>
          <span class="stat-label">我的收藏</span>
        </div>
      </div>
    </el-card>

    <!-- Tab 容器：动态/收藏两个列表共用同一份 list + 分页状态，当前 Tab 决定取哪个数据源 -->
    <el-tabs v-model="activeTab" class="space-tabs">
      <el-tab-pane label="动态" name="activity">
        <el-radio-group v-model="listType" class="list-switch" @change="reloadList">
          <el-radio-button value="created">我创建的</el-radio-button>
          <el-radio-button value="voted">我投票的</el-radio-button>
        </el-radio-group>

        <ranking-card-grid
          :list="list"
          :loading="listLoading"
          :empty-text="activityEmptyText"
          @open="goDetail"
        />
      </el-tab-pane>

      <el-tab-pane label="收藏" name="favorite">
        <ranking-card-grid
          :list="list"
          :loading="listLoading"
          empty-text="还没有收藏的榜单，去「发现榜单」点 ☆ 收藏"
          cancellable
          @open="goDetail"
          @cancel="handleCancelBookmark"
        />
      </el-tab-pane>

      <el-tab-pane label="设置" name="setting" disabled>
        <el-empty description="账号安全与通知设置即将上线" />
      </el-tab-pane>
    </el-tabs>

    <!-- 分页挂在 Tabs 下方：两个列表 Tab 共用同一份分页状态，无需各写一份 -->
    <div class="pager" v-if="total > query.size">
      <span class="page-info">第 {{ query.current }} / {{ totalPages }} 页 · 共 {{ total }} 条</span>
      <el-pagination
        v-model:current-page="query.current"
        :page-size="query.size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="loadList"
      />
    </div>

    <profile-edit-dialog ref="editDialogRef" @saved="onProfileSaved" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { resolveImage } from '@/utils/image'
import {
  getSpaceStats,
  listMyRankings,
  listVotedRankings,
  listBookmarkedRankings,
  cancelBookmark
} from '@/api/ranking'
import ProfileEditDialog from '@/components/ProfileEditDialog.vue'
import RankingCardGrid from '@/components/RankingCardGrid.vue'

const router = useRouter()
const userStore = useUserStore()

// 资料区直接读本地登录缓存（fetchMe 已在 App 挂载时同步）
const profile = computed(() => userStore.userInfo || {})
const avatarSrc = computed(() => resolveImage(profile.value.avatarUrl))

const editDialogRef = ref()
const statsLoading = ref(false)
const stats = reactive({ rankingCount: 0, receivedVoteCount: 0, joinedRankingCount: 0, bookmarkCount: 0 })

const activeTab = ref('activity')
const listType = ref('created') // created | voted（仅「动态」Tab 内切换）
const listLoading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 12 })

const activityEmptyText = computed(() =>
  listType.value === 'voted'
    ? '还没有参与投票的榜单'
    : '还没有创建过榜单，去「发现榜单」发起第一个吧')

// 三个列表视图（我创建的 / 我投票的 / 我收藏的）共用 list + 分页状态，Tab 决定当前数据源
function currentApi() {
  if (activeTab.value === 'favorite') return listBookmarkedRankings
  return listType.value === 'voted' ? listVotedRankings : listMyRankings
}

// 切 Tab 即换数据源：回到第一页重取，避免带着上一个 Tab 的页码跳页
watch(activeTab, () => reloadList())

const totalPages = computed(() =>
  Math.max(1, Math.ceil((Number(total.value) || 0) / query.size)))

function fmtDate(v) {
  return v ? String(v).slice(0, 10) : ''
}

async function loadStats() {
  statsLoading.value = true
  try {
    const res = await getSpaceStats()
    const d = res.data || {}
    // 全局 Long→String：归一化为数字再展示
    stats.rankingCount = Number(d.rankingCount) || 0
    stats.receivedVoteCount = Number(d.receivedVoteCount) || 0
    stats.joinedRankingCount = Number(d.joinedRankingCount) || 0
    stats.bookmarkCount = Number(d.bookmarkCount) || 0
  } finally {
    statsLoading.value = false
  }
}

async function loadList() {
  listLoading.value = true
  try {
    const params = { current: query.current, size: query.size }
    const res = await currentApi()(params)
    list.value = res.data.records
    total.value = Number(res.data.total) || 0
  } finally {
    listLoading.value = false
  }
}

function reloadList() {
  query.current = 1
  loadList()
}

// 取消收藏：就地剔除而非整页重载，避免列表跳回顶部丢滚动位置
async function handleCancelBookmark(row) {
  try {
    await ElMessageBox.confirm(`确定不再收藏「${row.title}」？`, '取消收藏', { type: 'warning' })
  } catch {
    return
  }
  await cancelBookmark(row.id)
  ElMessage.success('已取消收藏')
  list.value = list.value.filter(r => String(r.id) !== String(row.id))
  total.value = Math.max(0, total.value - 1)
  stats.bookmarkCount = Math.max(0, stats.bookmarkCount - 1)
}

function openEdit() {
  editDialogRef.value.open()
}

// 资料变更后刷新概览（昵称/头像即时反映，统计数字与资料无关但仍统一重取）
function onProfileSaved() {
  loadStats()
}

function goDetail(id) {
  router.push(`/rankings/${id}`)
}

onMounted(() => {
  loadStats()
  loadList()
})
</script>

<style scoped>
.my-space {
  max-width: 1080px;
  margin: 0 auto;
}
.profile-card {
  border-radius: 10px;
  margin-bottom: 20px;
}
.profile-head {
  display: flex;
  align-items: center;
  gap: 16px;
}
.profile-info {
  flex: 1;
}
.profile-name {
  font-size: 20px;
  font-weight: 600;
}
.profile-join {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}
.edit-btn {
  align-self: flex-start;
}
.profile-intro {
  margin: 16px 0 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}
.stats-bar {
  display: flex;
  gap: 40px;
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid #f0f0f0;
}
.stat {
  display: flex;
  flex-direction: column;
}
.stat-num {
  font-size: 22px;
  font-weight: 600;
  color: #409EFF;
}
.stat-label {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}
.list-switch {
  margin-bottom: 16px;
}

/* 卡片网格样式已随模板抽到 components/RankingCardGrid.vue，此处只保留本页专属布局 */
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
