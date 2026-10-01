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
      </div>
    </el-card>

    <!-- Tab 容器：一期仅「动态」实装，收藏/设置为占位 -->
    <el-tabs v-model="activeTab" class="space-tabs">
      <el-tab-pane label="动态" name="activity">
        <el-radio-group v-model="listType" class="list-switch" @change="reloadList">
          <el-radio-button value="created">我创建的</el-radio-button>
          <el-radio-button value="voted">我投票的</el-radio-button>
        </el-radio-group>

        <el-skeleton v-if="listLoading && list.length === 0" :rows="4" animated />
        <el-empty
          v-else-if="list.length === 0"
          :description="listType === 'created' ? '还没有创建过榜单，去「发现榜单」发起第一个吧' : '还没有参与投票的榜单'" />
        <div v-else class="card-grid" v-loading="listLoading">
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
              <span>💬 {{ r.itemCount }} 项</span>
              <span>👁 {{ r.viewCount }}</span>
            </div>
            <div class="card-footer">
              <el-tag v-if="r.categoryName" size="small" type="info">{{ r.categoryName }}</el-tag>
              <span class="time">{{ fmtDate(r.createdAt) }}</span>
            </div>
          </el-card>
        </div>

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
      </el-tab-pane>

      <el-tab-pane label="收藏" name="favorite" disabled>
        <el-empty description="收藏功能即将上线，敬请期待" />
      </el-tab-pane>

      <el-tab-pane label="设置" name="setting" disabled>
        <el-empty description="账号安全与通知设置即将上线" />
      </el-tab-pane>
    </el-tabs>

    <profile-edit-dialog ref="editDialogRef" @saved="onProfileSaved" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { EditPen } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { resolveImage } from '@/utils/image'
import { getSpaceStats, listMyRankings, listVotedRankings } from '@/api/ranking'
import ProfileEditDialog from '@/components/ProfileEditDialog.vue'

const router = useRouter()
const userStore = useUserStore()

// 资料区直接读本地登录缓存（fetchMe 已在 App 挂载时同步）
const profile = computed(() => userStore.userInfo || {})
const avatarSrc = computed(() => resolveImage(profile.value.avatarUrl))

const editDialogRef = ref()
const statsLoading = ref(false)
const stats = reactive({ rankingCount: 0, receivedVoteCount: 0, joinedRankingCount: 0 })

const activeTab = ref('activity')
const listType = ref('created') // created | voted
const listLoading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 12 })

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
  } finally {
    statsLoading.value = false
  }
}

async function loadList() {
  listLoading.value = true
  try {
    const params = { current: query.current, size: query.size }
    const api = listType.value === 'voted' ? listVotedRankings : listMyRankings
    const res = await api(params)
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
  /* 满宽抵消 el-card 左右内边距，配合 object-fit 居中裁剪（同发现页规则） */
  width: calc(100% + 40px);
  height: 150px;
  object-fit: cover;
  border-radius: 10px 10px 0 0;
  /* 左右必须 -20px 抵消正文内边距形成通栏；用 auto 会负margin失效导致左空右溢 */
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
}
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f0f0f0;
  padding-top: 10px;
  margin-top: 10px;
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
