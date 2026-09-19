<template>
  <div class="ranking-detail" v-loading="loading">
    <el-page-header class="back" @back="router.back()">
      <template #content>榜单详情</template>
    </el-page-header>

    <template v-if="detail">
      <!-- 榜单头部 -->
      <div class="detail-header">
        <h1 class="detail-title">{{ detail.title }}</h1>
        <p class="detail-desc">{{ detail.description || '暂无描述' }}</p>
        <div class="detail-meta">
          <el-tag v-if="detail.categoryName" size="small" type="info">{{ detail.categoryName }}</el-tag>
          <span>发起人 @{{ detail.creatorNickname || '匿名' }}</span>
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
            <div class="item-info">
              <div class="item-name">{{ item.name }}</div>
              <div v-if="item.description" class="item-desc">{{ item.description }}</div>
            </div>
            <div class="item-stats">
              <el-tag type="success" size="small">认同 {{ item.agreeCount }}</el-tag>
              <el-tag type="danger" size="small" effect="plain">反对 {{ item.opposeCount }}</el-tag>
            </div>
          </div>

          <!-- 理由列表 -->
          <div class="reasons">
            <div class="reasons-title">
              <el-icon><ChatLineSquare /></el-icon>
              推荐理由（{{ item.reasons?.length || 0 }}）
            </div>
            <el-empty
              v-if="!item.reasons || item.reasons.length === 0"
              description="还没有理由"
              :image-size="40"
            />
            <div v-for="r in item.reasons" :key="r.id" class="reason">
              <div class="reason-content">{{ r.content }}</div>
              <div class="reason-meta">
                <span>@{{ r.creatorNickname || '匿名' }}</span>
                <span>👍 {{ r.agreeCount }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatLineSquare } from '@element-plus/icons-vue'
import { getRankingDetail } from '@/api/ranking'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref(null)

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
  } finally {
    loading.value = false
  }
}

onMounted(load)
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
.item-info {
  flex: 1;
}
.item-name {
  font-size: 17px;
  font-weight: 600;
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
.reason {
  background: #f7f9fc;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 8px;
}
.reason-content {
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
}
.reason-meta {
  display: flex;
  gap: 16px;
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}
</style>
