<template>
  <!-- 榜单卡片网格：「我的空间-动态」与「收藏」两个 Tab 结构一致，抽出来避免复制一份模板与样式 -->
  <el-skeleton v-if="loading && list.length === 0" :rows="4" animated />
  <el-empty v-else-if="list.length === 0" :description="emptyText" />
  <div v-else class="card-grid" v-loading="loading">
    <el-card
      v-for="r in list"
      :key="r.id"
      class="ranking-card"
      shadow="hover"
      @click="emit('open', r.id)"
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
        <!-- 取消收藏挂在卡片内：@click.stop 阻断冒泡，否则点按钮会连带跳进榜单详情 -->
        <el-button v-if="cancellable" link type="danger" size="small" @click.stop="emit('cancel', r)">
          取消收藏
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { resolveImage } from '@/utils/image'

defineProps({
  list: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  emptyText: { type: String, default: '暂无数据' },
  cancellable: { type: Boolean, default: false }
})

const emit = defineEmits(['open', 'cancel'])

function fmtDate(v) {
  return v ? String(v).slice(0, 10) : ''
}
</script>

<style scoped>
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
  gap: 12px;
}
.time {
  color: #909399;
  font-size: 12px;
  flex: 1;
}
</style>
