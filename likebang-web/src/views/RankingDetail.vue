<template>
  <div class="ranking-detail" v-loading="loading">
    <el-page-header class="back" @back="router.back()">
      <template #content>榜单详情</template>
    </el-page-header>

    <template v-if="detail">
      <!-- 榜单头部 -->
      <div class="detail-header">
        <img v-if="detail.coverUrl" :src="resolveImage(detail.coverUrl)" class="detail-cover" alt="封面" />
        <div class="detail-header-top">
          <h1 class="detail-title">{{ detail.title }}</h1>
          <div v-if="canDelete" class="header-ops">
            <el-button
              type="primary"
              plain
              size="small"
              :icon="EditPen"
              @click="openEdit"
            >编辑榜单</el-button>
            <el-button
              type="danger"
              plain
              size="small"
              :icon="Delete"
              @click="handleDelete"
            >删除榜单</el-button>
          </div>
        </div>
        <p class="detail-desc">{{ detail.description || '暂无描述' }}</p>
        <div class="detail-meta">
          <el-tag v-if="detail.categoryName" size="small" type="info">{{ detail.categoryName }}</el-tag>
          <span class="creator-link" @click="goUser(detail.creatorId)">发起人 @{{ detail.creatorNickname || '匿名' }}</span>
          <span>🏆 Top {{ detail.itemLimit }}</span>
          <span>👁 {{ detail.viewCount }} 次浏览</span>
        </div>
      </div>

      <!-- 排名项列表 -->
      <div class="item-list">
        <el-card v-for="item in detail.items" :key="item.id" class="item-card" shadow="never">
          <div class="item-head">
            <div class="rank-badge" :class="rankClass(item.currentRank)">
              {{ item.currentRank }}
            </div>
            <img v-if="item.imageUrl" :src="resolveImage(item.imageUrl)" class="item-thumb" alt="" @click="previewImage(resolveImage(item.imageUrl))" />
            <div class="item-info">
              <div class="item-name">{{ item.name }}</div>
              <!-- 出处副标题：仅展示来源作品名（不再附带类型标签，避免与榜单分类混淆） -->
              <div v-if="item.sourceName" class="item-source">
                ——《{{ item.sourceName }}》
              </div>
              <div v-if="item.description" class="item-desc">{{ item.description }}</div>
            </div>
            <div class="item-stats">
              <el-button
                round
                size="small"
                :type="item.myVoteType === 1 ? 'success' : 'default'"
                :plain="item.myVoteType !== 1"
                :loading="votingMap['i' + item.id]"
                @click="handleItemVote(item, 1)"
              >👍 认同 {{ item.agreeCount }}</el-button>
              <el-button
                round
                size="small"
                :type="item.myVoteType === -1 ? 'danger' : 'default'"
                :plain="item.myVoteType !== -1"
                :loading="votingMap['i' + item.id]"
                @click="handleItemVote(item, -1)"
              >👎 反对 {{ item.opposeCount }}</el-button>
            </div>
            <el-button
              v-if="canDelete"
              link
              type="primary"
              size="small"
              :icon="EditPen"
              class="item-edit"
              @click="openItemEdit(item)"
            >编辑</el-button>
            <el-button
              v-if="canDelete"
              link
              type="danger"
              size="small"
              :icon="Delete"
              class="item-del"
              @click="handleDeleteItem(item)"
            >删除项</el-button>
          </div>

          <!-- 理由列表：后端按认同数降序只带 Top N（N 管理员可在系统设置配置）；"查看详情"跳转排名项独立页面，不受条数限制 -->
          <div class="reasons">
            <div class="reasons-title">
              <el-icon><ChatLineSquare /></el-icon>
              推荐理由（{{ item.reasonCount ?? item.reasons?.length ?? 0 }}）
              <span v-if="item.reasons?.length" class="reasons-hint">按认同数排名，展示前 {{ item.reasons.length }} 条</span>
              <el-button
                link
                type="primary"
                size="small"
                @click="openReasons(item)"
              >查看详情</el-button>
            </div>
            <el-empty
              v-if="!item.reasons || item.reasons.length === 0"
              description="还没有理由"
              :image-size="40"
            />
            <!-- 理由条目：名次徽章按认同数排名（后端已按 agreeCount 降序返回，下标+1 即排名） -->
            <div v-for="(r, rIndex) in item.reasons" :key="r.id" class="reason">
              <span class="reason-rank" :class="reasonRankClass(rIndex)">{{ rIndex + 1 }}</span>
              <div class="reason-main">
                <!-- 行内编辑模式 -->
                <div v-if="editingReasonId === r.id" class="reason-edit">
                  <el-input
                    v-model="editingReasonContent"
                    type="textarea"
                    :rows="2"
                    maxlength="1000"
                    show-word-limit
                  />
                  <image-upload v-model="editingReasonImage" label="更换配图" />
                  <div class="reason-edit-actions">
                    <el-button size="small" @click="cancelEditReason">取消</el-button>
                    <el-button size="small" type="primary" :loading="savingReason" @click="saveEditReason">保存</el-button>
                  </div>
                </div>
                <!-- 正常展示模式 -->
                <template v-else>
                  <div class="reason-content">{{ r.content }}</div>
                  <!-- 理由配图：一期一图，点击看大图 -->
                  <div v-if="r.imageUrl" class="reason-image-wrap">
                    <img
                      :src="resolveImage(r.imageUrl)"
                      class="reason-image"
                      alt=""
                      @click="previewImage(resolveImage(r.imageUrl))"
                    />
                  </div>
                  <div class="reason-footer">
                    <div class="reason-meta">
                      <span>@{{ r.creatorNickname || '匿名' }}</span>
                      <span class="reason-vote">
                        <el-button
                          link
                          size="small"
                          :type="r.myVoteType === 1 ? 'success' : 'info'"
                          :loading="votingMap['r' + r.id]"
                          @click="handleReasonVote(r, 1)"
                        >👍 {{ r.agreeCount }}</el-button>
                        <el-button
                          link
                          size="small"
                          :type="r.myVoteType === -1 ? 'danger' : 'info'"
                          :loading="votingMap['r' + r.id]"
                          @click="handleReasonVote(r, -1)"
                        >👎 {{ r.opposeCount }}</el-button>
                      </span>
                    </div>
                    <div v-if="canEditReason(r)" class="reason-ops">
                      <el-button link size="small" :icon="EditPen" @click="startEditReason(r)">编辑</el-button>
                      <el-button link size="small" type="danger" :icon="Delete" @click="handleDeleteReason(r.id)">删除</el-button>
                    </div>
                  </div>
                </template>
              </div>
            </div>

            <!-- 添加理由（仅登录用户可见） -->
            <div v-if="userStore.isLogin" class="add-reason">
              <el-input
                :model-value="reasonDrafts[item.id] ?? ''"
                type="textarea"
                :rows="2"
                maxlength="1000"
                show-word-limit
                placeholder="说说你的理由…"
                @update:model-value="v => { reasonDrafts[item.id] = v }"
              />
              <div class="add-reason-image">
                <image-upload
                  :model-value="reasonImageDrafts[item.id] ?? ''"
                  label="添加配图（可选）"
                  @update:model-value="v => { reasonImageDrafts[item.id] = v }"
                />
              </div>
              <div class="add-reason-actions">
                <el-button
                  size="small"
                  type="primary"
                  :loading="addingReasonMap[item.id]"
                  :disabled="!reasonDrafts[item.id]?.trim()"
                  @click="submitAddReason(item.id)"
                >提交理由</el-button>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 新增排名项（仅本人或管理员；受 itemLimit 约束） -->
        <div v-if="canDelete" class="add-item">
          <template v-if="canAddItem">
            <div class="add-item-title">新增排名项</div>
            <el-input
              v-model="newItem.name"
              placeholder="排名项名称（必填）"
              maxlength="200"
              show-word-limit
              class="add-item-name"
            />
            <el-input
              v-model="newItem.description"
              type="textarea"
              :rows="2"
              placeholder="描述（可选）"
              maxlength="1000"
              show-word-limit
            />
            <image-upload v-model="newItem.imageUrl" label="添加配图（可选）" />
            <el-button
              type="primary"
              :loading="addingItem"
              :disabled="!newItem.name.trim()"
              @click="submitAddItem"
            >添加排名项</el-button>
          </template>
          <div v-else class="add-item-hint">
            已达名次上限（Top {{ detail.itemLimit }}），如需新增请先在「编辑榜单」中调高名次数量。
          </div>
        </div>
      </div>

      <!-- 编辑榜单（仅本人或管理员，入口在头部） -->
      <ranking-edit-dialog ref="editDialogRef" @updated="load" />

      <!-- 编辑排名项（仅本人或管理员，入口在每个排名项） -->
      <item-edit-dialog ref="itemEditDialogRef" @updated="load" />

      <!-- 排名项缩略图点击预览 -->
      <el-dialog v-model="previewVisible" width="600px" append-to-body>
        <img :src="previewUrl" style="width: 100%" alt="预览" />
      </el-dialog>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatLineSquare, Delete, EditPen } from '@element-plus/icons-vue'
import {
  getRankingDetail,
  deleteRanking,
  addReason as apiAddReason,
  updateReason as apiUpdateReason,
  deleteReason as apiDeleteReason,
  voteItem,
  cancelItemVote,
  voteReason,
  cancelReasonVote,
  addRankingItem,
  deleteRankingItem
} from '@/api/ranking'
import RankingEditDialog from '@/components/RankingEditDialog.vue'
import ItemEditDialog from '@/components/ItemEditDialog.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { resolveImage } from '@/utils/image'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const detail = ref(null)

// 排名项缩略图预览
const previewVisible = ref(false)
const previewUrl = ref('')

function previewImage(url) {
  previewUrl.value = url
  previewVisible.value = true
}

// 点击发起人进入他人主页；creatorId 缺失（历史数据）时不跳转
function goUser(id) {
  if (id) router.push(`/users/${id}`)
}

// ---- 榜单删除/编辑权限：同一套归属判定（本人或管理员），后端双重校验 ----
const canDelete = computed(() => {
  if (!detail.value) return false
  const myId = userStore.userInfo?.id
  const isOwner = myId != null && String(detail.value.creatorId) === String(myId)
  return userStore.isAdmin || isOwner
})

// ---- 排名项结构管理（新增/删除）：与"编辑/删除榜单"同一门控（本人或管理员）----
const activeItemCount = computed(() => detail.value?.items?.length ?? 0)
const canAddItem = computed(() =>
  canDelete.value && activeItemCount.value < (Number(detail.value?.itemLimit) || 0)
)
const newItem = reactive({ name: '', description: '', imageUrl: '' })
const addingItem = ref(false)

async function submitAddItem() {
  const name = newItem.name.trim()
  if (!name) {
    ElMessage.warning('请输入排名项名称')
    return
  }
  addingItem.value = true
  try {
    await addRankingItem(route.params.id, {
      name,
      description: newItem.description.trim() || undefined,
      imageUrl: newItem.imageUrl || undefined
    })
    newItem.name = ''
    newItem.description = ''
    newItem.imageUrl = ''
    ElMessage.success('已新增排名项')
    await load()
  } finally {
    addingItem.value = false
  }
}

async function handleDeleteItem(item) {
  try {
    await ElMessageBox.confirm(
      `确定删除排名项「${item.name}」吗？该项下的理由将一并隐藏（投票记录保留）。`,
      '提示', { type: 'warning' }
    )
  } catch { return }
  await deleteRankingItem(route.params.id, item.id)
  ElMessage.success('已删除排名项')
  await load()
}

// ---- 编辑榜单 ----
const editDialogRef = ref()

function openEdit() {
  editDialogRef.value.open(detail.value)
}

// ---- 编辑排名项 ----
const itemEditDialogRef = ref()

function openItemEdit(item) {
  itemEditDialogRef.value.open(item, route.params.id, detail.value?.categoryId)
}

// ---- 排名项详情页：主体+全量理由的独立页面（微博/贴吧式）----
function openReasons(item) {
  router.push(`/rankings/${route.params.id}/items/${item.id}`)
}

// ---- 理由编辑状态 ----
const editingReasonId = ref(null)       // 正在编辑的理由 ID，null 表示无
const editingReasonContent = ref('')    // 编辑框内容
const editingReasonImage = ref('')      // 编辑态配图：初始为原图，空串=清空
const savingReason = ref(false)         // 保存中 loading

// 每条理由的"新增草稿"，以 itemId 为 key
const reasonDrafts = reactive({})
// 每个 item 新理由的配图草稿，以 itemId 为 key
const reasonImageDrafts = reactive({})
// 每个 item 的"提交中"标志，以 itemId 为 key
const addingReasonMap = reactive({})

// 判断当前用户是否可以编辑/删除该理由（本人或管理员）
function canEditReason(r) {
  if (!userStore.userInfo) return false
  return userStore.isAdmin ||
    (r.creatorId != null && String(r.creatorId) === String(userStore.userInfo.id))
}

// 理由名次徽章配色：前三名高亮（名次 = 认同数降序下的位置）
function reasonRankClass(index) {
  if (index === 0) return 'rank-1'
  if (index === 1) return 'rank-2'
  if (index === 2) return 'rank-3'
  return ''
}

// ---- 理由操作 ----
function startEditReason(r) {
  editingReasonId.value = r.id
  editingReasonContent.value = r.content
  editingReasonImage.value = r.imageUrl || ''
}

function cancelEditReason() {
  editingReasonId.value = null
  editingReasonContent.value = ''
  editingReasonImage.value = ''
}

async function saveEditReason() {
  if (!editingReasonContent.value.trim()) {
    ElMessage.warning('理由内容不能为空')
    return
  }
  savingReason.value = true
  try {
    await apiUpdateReason(editingReasonId.value, editingReasonContent.value.trim(), editingReasonImage.value)
    ElMessage.success('修改成功')
    cancelEditReason()
    await load()
  } finally {
    savingReason.value = false
  }
}

async function handleDeleteReason(reasonId) {
  try {
    await ElMessageBox.confirm('确定要删除该理由吗？', '提示', { type: 'warning' })
  } catch { return }
  await apiDeleteReason(reasonId)
  ElMessage.success('删除成功')
  await load()
}

async function submitAddReason(itemId) {
  const content = reasonDrafts[itemId]?.trim()
  if (!content) return
  addingReasonMap[itemId] = true
  try {
    await apiAddReason(route.params.id, itemId, content, reasonImageDrafts[itemId] || undefined)
    reasonDrafts[itemId] = ''
    reasonImageDrafts[itemId] = ''
    ElMessage.success('理由已添加')
    await load()
  } finally {
    addingReasonMap[itemId] = false
  }
}

// ---- 投票：同类型再点=取消，异类型=换票，成功后就地更新不整页刷新 ----
const votingMap = reactive({})

async function handleItemVote(item, voteType) {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录后投票')
    return
  }
  const key = 'i' + item.id
  if (votingMap[key]) return
  votingMap[key] = true
  try {
    const cancel = item.myVoteType === voteType
    const res = cancel
      ? await cancelItemVote(route.params.id, item.id)
      : await voteItem(route.params.id, item.id, voteType)
    applyVoteResult(item, res?.data)
  } finally {
    votingMap[key] = false
  }
}

async function handleReasonVote(reason, voteType) {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录后投票')
    return
  }
  const key = 'r' + reason.id
  if (votingMap[key]) return
  votingMap[key] = true
  try {
    const cancel = reason.myVoteType === voteType
    const res = cancel
      ? await cancelReasonVote(reason.id)
      : await voteReason(reason.id, voteType)
    applyVoteResult(reason, res?.data)
  } finally {
    votingMap[key] = false
  }
}

function applyVoteResult(target, data) {
  if (!data) return
  target.myVoteType = data.myVoteType ?? null
  target.agreeCount = data.agreeCount
  target.opposeCount = data.opposeCount
  if (data.participantCount != null) target.participantCount = data.participantCount
  if (data.agreeRate != null) target.agreeRate = data.agreeRate
}

// ---- 榜单基础 ----
function rankClass(rank) {
  if (rank === 1) return 'rank-1'
  if (rank === 2) return 'rank-2'
  if (rank === 3) return 'rank-3'
  return ''
}

async function load() {
  loading.value = true
  try {
    const res = await getRankingDetail(route.params.id)
    detail.value = res.data
    // 为每个 item 初始化草稿 key（Vue 3 reactive 新增 key 也是响应式的）
    if (res.data?.items) {
      res.data.items.forEach(i => {
        if (reasonDrafts[i.id] === undefined) reasonDrafts[i.id] = ''
      })
    }
  } finally {
    loading.value = false
  }
}

function handleDelete() {
  ElMessageBox.confirm('确定要删除该榜单吗？删除后不可恢复。', '提示', {
    type: 'warning'
  }).then(async () => {
    await deleteRanking(route.params.id)
    ElMessage.success('删除成功')
    router.push('/community')
  }).catch(() => {})
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.ranking-detail {
  max-width: 860px;
  margin: 0 auto;
}
.back {
  margin-bottom: 16px;
}
.detail-header {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: #fff;
  padding: 24px;
  border-radius: 12px;
  margin-bottom: 20px;
}
.detail-cover {
  display: block;
  width: 100%;
  max-height: 260px;
  object-fit: cover;
  border-radius: 8px;
  margin-bottom: 16px;
}
.detail-header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.header-ops {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.header-ops .el-button + .el-button {
  margin-left: 0;
}
.detail-title {
  margin: 0 0 8px;
  font-size: 24px;
}
.detail-desc {
  margin: 0 0 14px;
  opacity: 0.92;
  line-height: 1.6;
}
.detail-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  flex-wrap: wrap;
}
.creator-link {
  color: #409eff;
  cursor: pointer;
}
.item-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.item-card {
  border-radius: 10px;
}
.item-head {
  display: flex;
  align-items: center;
  gap: 14px;
}
.rank-badge {
  width: 36px;
  height: 36px;
  line-height: 36px;
  text-align: center;
  border-radius: 50%;
  background: #909399;
  color: #fff;
  font-weight: 700;
  flex-shrink: 0;
}
.rank-1 { background: #f5a623; }
.rank-2 { background: #a0a4ab; }
.rank-3 { background: #cd7f32; }
.item-thumb {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
  cursor: zoom-in;
}
.item-info {
  flex: 1;
}
.item-name {
  font-size: 17px;
  font-weight: 600;
}
.item-source {
  color: #909399;
  font-size: 12px;
  margin-top: 2px;
}
.item-desc {
  color: #606266;
  font-size: 13px;
  margin-top: 4px;
}
.item-stats {
  display: flex;
  gap: 8px;
}
.item-stats .el-button + .el-button {
  margin-left: 0;
}
.item-del {
  flex-shrink: 0;
  margin-left: 4px;
}
.item-edit {
  flex-shrink: 0;
  margin-left: 4px;
}
.add-item {
  border: 1px dashed #dcdfe6;
  border-radius: 10px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.add-item-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.add-item-name {
  max-width: 420px;
}
.add-item-hint {
  color: #909399;
  font-size: 13px;
}
.reason-vote {
  display: inline-flex;
  align-items: center;
}
.reason-vote .el-button + .el-button {
  margin-left: 4px;
}
.reasons {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
}
.reasons-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}
.reasons-hint {
  font-size: 12px;
  color: #c0c4cc;
}
.reason {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  background: #f7f9fc;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 8px;
}
.reason-main {
  flex: 1;
  min-width: 0;
}
.reason-rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 11px;
  background: #c0c4cc;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}
.reason-rank.rank-1 { background: #f5a623; }
.reason-rank.rank-2 { background: #a0a4ab; }
.reason-rank.rank-3 { background: #cd7f32; }
.reason-content {
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
}
.reason-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
}
.reason-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}
.reason-ops {
  display: flex;
  gap: 4px;
}
/* 行内编辑 */
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
/* 添加理由 */
.add-reason {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
}
.add-reason-image {
  margin-top: 8px;
}
.reason-image-wrap {
  margin-top: 6px;
}
.reason-image {
  display: block;
  max-width: 220px;
  max-height: 160px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  cursor: zoom-in;
}
.add-reason-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
</style>
