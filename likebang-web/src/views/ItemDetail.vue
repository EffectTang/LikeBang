<template>
  <div class="item-detail">
    <el-page-header class="back" @back="router.back()">
      <template #content>排名项详情</template>
    </el-page-header>

    <!-- 主体：排名项（借鉴微博帖子/贴吧主楼：主体在上，理由如跟帖在下） -->
    <el-card v-loading="itemLoading" class="main-card" shadow="never">
      <div class="main-head">
        <div class="main-cover">
          <img v-if="item?.imageUrl" :src="item.imageUrl" alt="" />
          <div v-else class="cover-placeholder">
            <el-icon :size="28"><Picture /></el-icon>
          </div>
        </div>
        <div class="main-info">
          <div class="main-name">
            <span class="rank-badge" :class="rankClass(item?.currentRank)">{{ item?.currentRank }}</span>
            {{ item?.name }}
          </div>
          <div v-if="item?.description" class="main-desc">{{ item.description }}</div>
          <div class="main-meta">
            <span>@{{ item?.creatorNickname || '匿名' }}</span>
            <span>{{ item?.reasonCount ?? 0 }} 条理由</span>
          </div>
        </div>
        <div class="main-vote">
          <el-button
            round
            :type="item?.myVoteType === 1 ? 'success' : 'default'"
            :plain="item?.myVoteType !== 1"
            :loading="votingItem"
            @click="handleItemVote(1)"
          >👍 认同 {{ item?.agreeCount ?? 0 }}</el-button>
          <el-button
            round
            :type="item?.myVoteType === -1 ? 'danger' : 'default'"
            :plain="item?.myVoteType !== -1"
            :loading="votingItem"
            @click="handleItemVote(-1)"
          >👎 反对 {{ item?.opposeCount ?? 0 }}</el-button>
        </div>
      </div>
    </el-card>

    <!-- 理由区 -->
    <div class="reason-section">
      <!-- 发表理由（微博发帖式，置顶输入框） -->
      <el-card v-if="userStore.isLogin" class="publish-card" shadow="never">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="3"
          maxlength="1000"
          show-word-limit
          placeholder="说说你的理由，支持 1000 字…"
        />
        <div class="publish-actions">
          <el-button
            type="primary"
            round
            :loading="addingReason"
            :disabled="!draft.trim()"
            @click="submitReason"
          >发表理由</el-button>
        </div>
      </el-card>
      <el-card v-else class="publish-card login-tip" shadow="never">
        <span>登录后可以发表理由、参与投票</span>
      </el-card>

      <div class="section-head">
        <span class="section-title">全部理由（{{ total }}）</span>
        <span class="section-hint">按认同数从高到低排列</span>
      </div>

      <div v-loading="loading" class="reason-list">
        <el-empty
          v-if="!loading && reasons.length === 0"
          description="还没有理由，来发布第一条吧"
        />
        <el-card
          v-for="(r, idx) in reasons"
          :key="r.id"
          class="reason-card"
          shadow="never"
        >
          <!-- 行内编辑 -->
          <div v-if="editingId === r.id" class="reason-edit">
            <el-input v-model="editingContent" type="textarea" :rows="3" maxlength="1000" show-word-limit />
            <div class="reason-edit-actions">
              <el-button size="small" @click="cancelEdit">取消</el-button>
              <el-button size="small" type="primary" :loading="saving" @click="saveEdit">保存</el-button>
            </div>
          </div>
          <template v-else>
            <div class="reason-line">
              <span class="reason-floor">{{ idx + 1 }}</span>
              <div class="reason-content">{{ r.content }}</div>
            </div>
            <div class="reason-footer">
              <div class="reason-meta">
                <span>@{{ r.creatorNickname || '匿名' }}</span>
                <span>{{ fmtDate(r.createdAt) }}</span>
              </div>
              <div class="reason-right">
                <el-button
                  link
                  size="small"
                  class="comment-toggle"
                  @click="toggleComments(r)"
                >💬 {{ r.commentCount || 0 }} 回复</el-button>
                <span class="reason-vote">
                  <el-button
                    round
                    size="small"
                    :type="r.myVoteType === 1 ? 'success' : 'default'"
                    :plain="r.myVoteType !== 1"
                    :loading="votingMap[r.id]"
                    @click="handleReasonVote(r, 1)"
                  >👍 {{ r.agreeCount }}</el-button>
                  <el-button
                    round
                    size="small"
                    :type="r.myVoteType === -1 ? 'danger' : 'default'"
                    :plain="r.myVoteType !== -1"
                    :loading="votingMap[r.id]"
                    @click="handleReasonVote(r, -1)"
                  >👎 {{ r.opposeCount }}</el-button>
                </span>
                <span v-if="canEditReason(r)" class="reason-ops">
                  <el-button link size="small" :icon="EditPen" @click="startEdit(r)">编辑</el-button>
                  <el-button link size="small" type="danger" :icon="Delete" @click="handleDelete(r)">删除</el-button>
                </span>
              </div>
            </div>

            <!-- 楼中楼：评论仅在理由卡内折叠展开，时间正序（贴吧风），首次展开拉取 -->
            <div v-if="commentsMap[r.id]?.visible" class="comments">
              <div v-loading="commentsMap[r.id].loading" class="comment-list">
                <div v-for="c in commentsMap[r.id].list" :key="c.id" class="comment">
                  <span class="comment-author">@{{ c.creatorNickname || '匿名' }}</span>
                  <span class="comment-content">{{ c.content }}</span>
                  <span class="comment-time">{{ fmtDate(c.createdAt) }}</span>
                  <el-button
                    v-if="canDeleteComment(c)"
                    link
                    size="small"
                    type="danger"
                    @click="handleDeleteComment(r, c)"
                  >删除</el-button>
                </div>
                <div
                  v-if="commentsMap[r.id].list.length < commentsMap[r.id].total"
                  class="comment-more"
                >
                  <el-button
                    link
                    size="small"
                    @click="loadComments(r.id, commentsMap[r.id].current + 1)"
                  >查看更多回复</el-button>
                </div>
              </div>
              <div class="comment-input">
                <el-input
                  v-model="commentsMap[r.id].draft"
                  size="small"
                  maxlength="500"
                  placeholder="回复这条理由…"
                  @keyup.enter="submitComment(r)"
                />
                <el-button
                  size="small"
                  type="primary"
                  :loading="commentsMap[r.id].submitting"
                  :disabled="!commentsMap[r.id].draft?.trim()"
                  @click="submitComment(r)"
                >回复</el-button>
              </div>
            </div>
          </template>
        </el-card>
      </div>

      <div v-if="hasMore" class="load-more">
        <el-button :loading="loading" @click="loadMore">加载更多</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Delete, EditPen } from '@element-plus/icons-vue'
import {
  getRankingItem,
  listItemReasons,
  addReason as apiAddReason,
  updateReason as apiUpdateReason,
  deleteReason as apiDeleteReason,
  addReasonComment,
  listReasonComments,
  deleteReasonComment,
  voteItem,
  cancelItemVote,
  voteReason,
  cancelReasonVote
} from '@/api/ranking'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const rankingId = route.params.id
const itemId = route.params.itemId
const pageSize = 10

// ---- 主体 ----
const item = ref(null)
const itemLoading = ref(false)
const votingItem = ref(false)

async function loadItem() {
  itemLoading.value = true
  try {
    const res = await getRankingItem(rankingId, itemId)
    item.value = res.data
  } finally {
    itemLoading.value = false
  }
}

async function handleItemVote(voteType) {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录后投票')
    return
  }
  if (votingItem.value) return
  votingItem.value = true
  try {
    const cancel = item.value.myVoteType === voteType
    const res = cancel
      ? await cancelItemVote(rankingId, itemId)
      : await voteItem(rankingId, itemId, voteType)
    const data = res?.data
    if (data && item.value) {
      item.value.myVoteType = data.myVoteType ?? null
      item.value.agreeCount = data.agreeCount
      item.value.opposeCount = data.opposeCount
    }
  } finally {
    votingItem.value = false
  }
}

function rankClass(rank) {
  if (rank === 1) return 'rank-1'
  if (rank === 2) return 'rank-2'
  if (rank === 3) return 'rank-3'
  return ''
}

// ---- 理由流：追加式分页（内容流不用页码跳页，回避实时排序下的翻页漂移）----
const reasons = ref([])
const total = ref(0)
const current = ref(1)
const loading = ref(false)

const hasMore = computed(() => reasons.value.length < total.value)

async function loadPage(page) {
  loading.value = true
  try {
    const res = await listItemReasons(rankingId, itemId, { current: page, size: pageSize })
    const records = res.data.records || []
    // total 为后端 Long→String，需 Number 归一化
    total.value = Number(res.data.total) || 0
    current.value = page
    reasons.value = page === 1 ? records : reasons.value.concat(records)
  } finally {
    loading.value = false
  }
}

function loadMore() {
  loadPage(current.value + 1)
}

function fmtDate(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}

// ---- 发表理由 ----
const draft = ref('')
const addingReason = ref(false)

async function submitReason() {
  const content = draft.value.trim()
  if (!content) return
  addingReason.value = true
  try {
    await apiAddReason(rankingId, itemId, content)
    draft.value = ''
    ElMessage.success('理由已发布')
    // 新理由认同数为 0 按排序在列表尾部，回到第一页刷新并同步总数
    await loadPage(1)
  } finally {
    addingReason.value = false
  }
}

// ---- 理由投票 ----
const votingMap = reactive({})

async function handleReasonVote(reason, voteType) {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录后投票')
    return
  }
  const key = reason.id
  if (votingMap[key]) return
  votingMap[key] = true
  try {
    const cancel = reason.myVoteType === voteType
    const res = cancel
      ? await cancelReasonVote(reason.id)
      : await voteReason(reason.id, voteType)
    const data = res?.data
    if (data) {
      reason.myVoteType = data.myVoteType ?? null
      reason.agreeCount = data.agreeCount
      reason.opposeCount = data.opposeCount
    }
  } finally {
    votingMap[key] = false
  }
}

// ---- 理由编辑/删除（权限与榜单页一致：本人或管理员）----
const editingId = ref(null)
const editingContent = ref('')
const saving = ref(false)

function canEditReason(r) {
  if (!userStore.userInfo) return false
  return userStore.isAdmin ||
    (r.creatorId != null && String(r.creatorId) === String(userStore.userInfo.id))
}

// ---- 楼中楼评论：每个理由一份独立状态，首次展开才拉取 ----
const commentPageSize = 10
const commentsMap = reactive({})

function ensureBox(reasonId) {
  if (!commentsMap[reasonId]) {
    commentsMap[reasonId] = {
      visible: false, loaded: false, loading: false, submitting: false,
      list: [], total: 0, current: 1, draft: ''
    }
  }
  return commentsMap[reasonId]
}

function toggleComments(r) {
  const box = ensureBox(r.id)
  box.visible = !box.visible
  if (box.visible && !box.loaded) {
    loadComments(r.id, 1)
  }
}

async function loadComments(reasonId, page) {
  const box = ensureBox(reasonId)
  box.loading = true
  try {
    const res = await listReasonComments(reasonId, { current: page, size: commentPageSize })
    const records = res.data.records || []
    // total 为后端 Long→String，需 Number 归一化
    box.total = Number(res.data.total) || 0
    box.current = page
    box.list = page === 1 ? records : box.list.concat(records)
    box.loaded = true
  } finally {
    box.loading = false
  }
}

async function submitComment(r) {
  const box = ensureBox(r.id)
  const content = (box.draft || '').trim()
  if (!content || box.submitting) return
  box.submitting = true
  try {
    const res = await addReasonComment(r.id, content)
    box.draft = ''
    // 时间正序新评论在末尾：已加载到最后一页才本地追加，否则仅更新计数避免与服务端返回重复
    if (box.list.length >= box.total) {
      box.list.push(res.data)
    }
    box.total += 1
    r.commentCount = (r.commentCount || 0) + 1
  } finally {
    box.submitting = false
  }
}

function canDeleteComment(c) {
  if (!userStore.userInfo) return false
  return userStore.isAdmin ||
    (c.creatorId != null && String(c.creatorId) === String(userStore.userInfo.id))
}

function handleDeleteComment(reason, c) {
  ElMessageBox.confirm('确定要删除该评论吗？', '提示', { type: 'warning' })
    .then(async () => {
      await deleteReasonComment(c.id)
      const box = ensureBox(reason.id)
      box.list = box.list.filter(x => x.id !== c.id)
      box.total = Math.max(0, box.total - 1)
      reason.commentCount = Math.max(0, (reason.commentCount || 0) - 1)
      ElMessage.success('删除成功')
    })
    .catch(() => {})
}

function startEdit(r) {
  editingId.value = r.id
  editingContent.value = r.content
}

function cancelEdit() {
  editingId.value = null
  editingContent.value = ''
}

async function saveEdit() {
  if (!editingContent.value.trim()) {
    ElMessage.warning('理由内容不能为空')
    return
  }
  saving.value = true
  try {
    await apiUpdateReason(editingId.value, editingContent.value.trim())
    ElMessage.success('修改成功')
    cancelEdit()
    await loadPage(1)
  } finally {
    saving.value = false
  }
}

function handleDelete(r) {
  ElMessageBox.confirm('确定要删除该理由吗？', '提示', { type: 'warning' })
    .then(async () => {
      await apiDeleteReason(r.id)
      ElMessage.success('删除成功')
      await loadPage(1)
    })
    .catch(() => {})
}

onMounted(() => {
  loadItem()
  loadPage(1)
})
</script>

<style scoped>
.item-detail {
  max-width: 860px;
  margin: 0 auto;
}
.back {
  margin-bottom: 16px;
}
.main-card {
  border-radius: 12px;
  margin-bottom: 16px;
}
.main-head {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.main-cover {
  flex-shrink: 0;
  width: 96px;
  height: 96px;
  border-radius: 10px;
  overflow: hidden;
  background: #f5f7fa;
}
.main-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}
.main-info {
  flex: 1;
  min-width: 0;
}
.main-name {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.rank-badge {
  width: 28px;
  height: 28px;
  line-height: 28px;
  text-align: center;
  border-radius: 50%;
  background: #909399;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}
.rank-1 { background: #f5a623; }
.rank-2 { background: #a0a4ab; }
.rank-3 { background: #cd7f32; }
.main-desc {
  margin-top: 8px;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}
.main-meta {
  margin-top: 8px;
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}
.main-vote {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.publish-card {
  border-radius: 10px;
  margin-bottom: 14px;
}
.publish-card.login-tip {
  text-align: center;
  color: #909399;
  font-size: 13px;
}
.publish-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}
.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 10px;
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.section-hint {
  font-size: 12px;
  color: #c0c4cc;
}
.reason-card {
  border-radius: 10px;
  margin-bottom: 10px;
}
.reason-line {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.reason-floor {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 12px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 12px;
  font-weight: 600;
}
.reason-content {
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}
.reason-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}
.reason-meta {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: #909399;
}
.reason-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.reason-vote {
  display: inline-flex;
  align-items: center;
}
.reason-vote .el-button + .el-button {
  margin-left: 6px;
}
.reason-ops {
  display: flex;
  gap: 4px;
}
.comment-toggle {
  margin-right: 4px;
  color: #909399;
}
.comments {
  margin-top: 12px;
  padding: 10px 12px;
  background: #f7f8fa;
  border-radius: 8px;
}
.comment-list {
  min-height: 8px;
}
.comment {
  display: flex;
  align-items: baseline;
  gap: 10px;
  font-size: 13px;
  line-height: 1.7;
  padding: 4px 0;
}
.comment-author {
  flex-shrink: 0;
  color: #409eff;
  font-weight: 500;
}
.comment-content {
  flex: 1;
  min-width: 0;
  color: #303133;
  word-break: break-word;
}
.comment-time {
  flex-shrink: 0;
  color: #c0c4cc;
  font-size: 12px;
}
.comment-more {
  text-align: center;
  margin-top: 4px;
}
.comment-input {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
.reason-edit {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.reason-edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.load-more {
  text-align: center;
  margin: 14px 0 30px;
}
</style>
