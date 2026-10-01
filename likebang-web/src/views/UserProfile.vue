<template>
  <div class="user-profile" v-loading="loading">
    <el-page-header class="back" @back="router.back()">
      <template #content>用户主页</template>
    </el-page-header>

    <!-- 资料头部（仅脱敏字段：昵称/头像/简介/加入时间） -->
    <el-card v-if="profile" class="profile-card" shadow="never">
      <div class="profile-head">
        <el-avatar :size="72" :src="resolveImage(profile.avatarUrl)">
          {{ (profile.nickname || 'U').charAt(0) }}
        </el-avatar>
        <div class="profile-info">
          <div class="profile-name">{{ profile.nickname || '匿名用户' }}</div>
          <div class="profile-join">加入于 {{ fmtDate(profile.createdAt) }}</div>
        </div>
      </div>
      <p class="profile-intro">{{ profile.intro || 'TA 还没有填写自我介绍' }}</p>
    </el-card>

    <!-- TA 的公开榜单 -->
    <div class="sec-title">TA 的公开榜单（{{ total }}）</div>
    <el-empty v-if="!loading && list.length === 0" description="TA 还没有公开榜单" />
    <div v-else class="card-grid">
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPublicProfile } from '@/api/user'
import { listUserRankings } from '@/api/ranking'
import { resolveImage } from '@/utils/image'

const route = useRoute()
const router = useRouter()

const userId = route.params.id
const loading = ref(false)
const profile = ref(null)
const list = ref([])
const total = ref(0)

const query = reactive({ current: 1, size: 12 })

// 后端全局 Long→String：total 归一化为数字再算页数
const totalPages = computed(() =>
  Math.max(1, Math.ceil((Number(total.value) || 0) / query.size)))

function fmtDate(v) {
  return v ? String(v).slice(0, 10) : ''
}

async function loadProfile() {
  try {
    const res = await getPublicProfile(userId)
    profile.value = res.data
  } catch (e) {
    // 用户不存在/已禁用：request 拦截器已提示，这里保持空态
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await listUserRankings(userId, { current: query.current, size: query.size })
    list.value = res.data.records
    total.value = Number(res.data.total) || 0
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  router.push(`/rankings/${id}`)
}

onMounted(() => {
  loadProfile()
  loadList()
})
</script>

<style scoped>
.user-profile {
  max-width: 1080px;
  margin: 0 auto;
}
.back {
  margin-bottom: 16px;
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
.profile-name {
  font-size: 20px;
  font-weight: 600;
}
.profile-join {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}
.profile-intro {
  margin: 16px 0 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}
.sec-title {
  font-size: 16px;
  font-weight: 600;
  margin: 8px 0 14px;
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
