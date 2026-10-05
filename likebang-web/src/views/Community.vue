<template>
  <div class="community">
    <!-- 顶部操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <h2 class="page-title">{{ pageTitle }}</h2>
        <span class="page-sub">{{ pageSub }}</span>
      </div>
      <div class="toolbar-right">
        <div class="search-group">
          <span class="search-label">发起人</span>
          <el-input
            v-model="query.creatorNickname"
            placeholder="按昵称"
            clearable
            class="creator-input"
            @keyup.enter="onSearch"
            @clear="onSearch"
          />
          <span class="search-label">标题/榜单</span>
          <!-- 搜索联想弹出层 -->
          <el-popover
            placement="bottom-end"
            :width="380"
            trigger="manual"
            :visible="suggestVisible && !!query.keyword"
            popper-class="suggest-popover"
          >
            <template #reference>
              <el-input
                v-model="query.keyword"
                placeholder="搜索榜单、用户、排名项..."
                clearable
                class="search-input"
                @input="onKeywordInput"
                @keyup.enter="onSearch"
                @clear="onClear"
              />
            </template>

          <!-- 联想面板：分组展示聚合结果，点击直接跳转，底部"查看全部结果"进入搜索态 -->
          <div class="suggest-panel">
            <el-skeleton v-if="suggestLoading" :rows="3" animated />
            <template v-else>
              <div v-if="suggest.rankings?.length" class="suggest-group">
                <div class="suggest-head"><span>榜单</span></div>
                <div v-for="r in suggest.rankings" :key="'rk'+r.id" class="suggest-item" @click="goRanking(r.id)">
                  <span v-html="highlight(r.title, query.keyword)"></span>
                  <span class="suggest-sub">@{{ r.creatorNickname || '匿名' }}</span>
                </div>
              </div>
              <div v-if="suggest.users?.length" class="suggest-group">
                <div class="suggest-head"><span>用户</span></div>
                <div v-for="u in suggest.users" :key="'us'+u.id" class="suggest-item" @click="goUser(u.id)">
                  <el-avatar :size="20" :src="resolveImage(u.avatarUrl)" />
                  <span v-html="highlight(u.nickname, query.keyword)"></span>
                </div>
              </div>
              <div v-if="suggest.items?.length" class="suggest-group">
                <div class="suggest-head"><span>排名项</span></div>
                <div v-for="i in suggest.items" :key="'it'+i.itemId" class="suggest-item" @click="goItem(i.rankingId, i.itemId)">
                  <span v-html="highlight(i.name, query.keyword)"></span>
                  <span class="suggest-sub">· 《{{ i.rankingTitle }}》</span>
                </div>
              </div>
              <div v-if="suggest.works?.length" class="suggest-group">
                <div class="suggest-head"><span>作品名</span></div>
                <div v-for="w in suggest.works" :key="'wk'+w" class="suggest-item" @click="backSearchWork(w)">
                  《{{ w }}》
                </div>
              </div>
              <div v-if="suggestEmpty" class="suggest-empty">无匹配结果</div>
              <div v-else class="suggest-footer">
                <el-button link type="primary" size="small" @click="onSearch">查看全部结果 →</el-button>
              </div>
            </template>
          </div>
          </el-popover>
          <el-button type="primary" :icon="Search" @click="onSearch">搜索</el-button>
          <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreate">发起榜单</el-button>
      </div>
    </div>

    <!-- 态3：聚焦列表顶部导航 + 二级过滤器 -->
    <template v-if="mode === 'section'">
      <div class="section-nav">
        <el-button link :icon="ArrowLeft" @click="backToSearch">{{ backLabel }}</el-button>
        <span class="section-keyword">
          <template v-if="query.keyword">关键词：<strong>{{ query.keyword }}</strong></template>
          <template v-if="query.creatorNickname">{{ query.keyword ? ' · ' : '' }}发起人：<strong>{{ query.creatorNickname }}</strong></template>
          <template v-if="!query.keyword && !query.creatorNickname">全部榜单</template>
        </span>
      </div>
      <!-- Phase2：排名项聚焦时支持按作品名二次过滤 -->
      <div v-if="activeSection === 'items'" class="section-filter">
        <span class="filter-label">按作品名筛选：</span>
        <el-input
          v-model="sectionFilter.sourceName"
          placeholder="输入作品名"
          size="small"
          clearable
          class="filter-input"
          @change="onSectionFilterChange"
          @keyup.enter="onSectionFilterChange"
        />
      </div>
    </template>

    <!-- 态1 browse：分类筛选栏 -->
    <div v-if="mode === 'browse'" class="category-bar">
      <el-tag
        :effect="query.categoryId == null ? 'dark' : 'plain'"
        class="cat-tag"
        @click="selectCategory(null)"
      >全部</el-tag>
      <el-tag
        v-for="c in categories"
        :key="c.id"
        :effect="String(query.categoryId) === String(c.id) ? 'dark' : 'plain'"
        class="cat-tag"
        @click="selectCategory(c.id)"
      >{{ c.name }}</el-tag>
    </div>

    <!-- 态2 search：分类结果页 -->
    <template v-if="mode === 'search'">
      <el-skeleton v-if="searchLoading" :rows="6" animated />
      <template v-else>
        <!-- 相关榜单 -->
        <section v-if="searchResults.rankings.length" class="result-section">
          <div class="section-head">
            <h3>相关榜单 <span class="count-badge">{{ searchResults.rankingsTotal }}</span></h3>
            <el-button v-if="searchResults.rankingsTotal > searchResults.rankings.length"
              link type="primary" @click="enterSection('rankings')">查看全部 →</el-button>
          </div>
          <div class="card-grid">
            <el-card v-for="r in searchResults.rankings" :key="r.id"
              class="ranking-card" shadow="hover" @click="goRanking(r.id)">
              <img v-if="r.coverUrl" :src="resolveImage(r.coverUrl)" class="card-cover" alt="封面" />
              <div class="card-title" v-html="highlight(r.title, query.keyword)"></div>
              <div class="card-desc">{{ r.description || '暂无描述' }}</div>
              <div class="card-meta">
                <span>🏆 Top {{ r.itemLimit }}</span>
                <span>👁 {{ r.viewCount }}</span>
                <span>💬 {{ r.itemCount }} 项</span>
              </div>
              <div class="card-footer">
                <el-tag v-if="r.categoryName" size="small" type="info">{{ r.categoryName }}</el-tag>
                <span class="creator" @click.stop="goUser(r.creatorId)">
                  @<span v-html="highlight(r.creatorNickname || '匿名', query.keyword)"></span>
                </span>
              </div>
            </el-card>
          </div>
        </section>

        <!-- 相关用户 -->
        <section v-if="searchResults.users.length" class="result-section">
          <div class="section-head">
            <h3>相关用户 <span class="count-badge">{{ searchResults.usersTotal }}</span></h3>
            <el-button link type="primary" @click="enterSection('users')">查看全部 →</el-button>
          </div>
          <div class="card-grid">
            <el-card v-for="u in searchResults.users" :key="u.id"
              class="user-card" shadow="hover" @click="goUser(u.id)">
              <div class="user-card-body">
                <el-avatar :size="48" :src="resolveImage(u.avatarUrl)" />
                <div class="user-card-info">
                  <div class="user-name" v-html="highlight(u.nickname, query.keyword)"></div>
                  <div class="user-intro">{{ u.intro || '暂无简介' }}</div>
                </div>
              </div>
            </el-card>
          </div>
        </section>

        <!-- 相关排名项 -->
        <section v-if="searchResults.items.length" class="result-section">
          <div class="section-head">
            <h3>相关排名项 <span class="count-badge">{{ searchResults.itemsTotal }}</span></h3>
            <el-button link type="primary" @click="enterSection('items')">查看全部 →</el-button>
          </div>
          <div class="item-list">
            <el-card v-for="i in searchResults.items" :key="i.itemId"
              class="item-row" shadow="hover" @click="goItem(i.rankingId, i.itemId)">
              <div class="item-row-body">
                <img v-if="i.imageUrl" :src="resolveImage(i.imageUrl)" class="item-thumb" alt="" />
                <div v-else class="item-thumb item-thumb-empty">🖼</div>
                <div class="item-row-info">
                  <div class="item-name" v-html="highlight(i.name, query.keyword)"></div>
                  <div class="item-sub">
                    属于
                    <span class="item-ranking" @click.stop="goRanking(i.rankingId)">《{{ i.rankingTitle }}》</span>
                    <span v-if="i.creatorNickname" class="item-creator">· @{{ i.creatorNickname }}</span>
                  </div>
                </div>
                <div v-if="i.sourceName" class="item-source">《{{ i.sourceName }}》</div>
              </div>
            </el-card>
          </div>
        </section>

        <!-- 相关作品名 -->
        <section v-if="searchResults.works.length" class="result-section">
          <div class="section-head"><h3>相关作品名</h3></div>
          <div class="work-cloud">
            <el-tag v-for="w in searchResults.works" :key="w"
              class="work-tag" size="large" @click="backSearchWork(w)">
              《<span v-html="highlight(w, query.keyword)"></span>》
            </el-tag>
          </div>
        </section>

        <!-- 搜索态：全空 -->
        <el-empty v-if="searchAllEmpty && !searchLoading" description="没有找到相关内容，换个词试试" />
      </template>
    </template>

    <!-- 态1 browse + 态3 section:rankings：榜单卡片流（共用同一渲染块） -->
    <template v-else-if="mode === 'browse' || activeSection === 'rankings'">
      <el-skeleton v-if="loading && list.length === 0" :rows="4" animated />
      <el-empty v-else-if="list.length === 0"
        :description="mode === 'browse' ? '暂无公开榜单' : '没有找到相关榜单'" />
      <div v-else class="card-grid" v-loading="loading">
        <el-card v-for="r in list" :key="r.id"
          class="ranking-card" shadow="hover" @click="goRanking(r.id)">
          <img v-if="r.coverUrl" :src="resolveImage(r.coverUrl)" class="card-cover" alt="封面" />
          <div class="card-title" v-html="mode !== 'browse' ? highlight(r.title, query.keyword) : r.title"></div>
          <div class="card-desc">{{ r.description || '暂无描述' }}</div>
          <div class="card-meta">
            <span>🏆 Top {{ r.itemLimit }}</span>
            <span>👁 {{ r.viewCount }}</span>
            <span>💬 {{ r.itemCount }} 项</span>
          </div>
          <div class="card-footer">
            <el-tag v-if="r.categoryName" size="small" type="info">{{ r.categoryName }}</el-tag>
            <span class="creator" @click.stop="goUser(r.creatorId)">
              @<span v-html="mode !== 'browse' ? highlight(r.creatorNickname || '匿名', query.keyword) : (r.creatorNickname || '匿名')"></span>
            </span>
          </div>
        </el-card>
      </div>
    </template>

    <!-- 态3 section:users -->
    <template v-else-if="activeSection === 'users'">
      <el-skeleton v-if="loading && list.length === 0" :rows="4" animated />
      <el-empty v-else-if="list.length === 0" description="没有找到相关用户，换个昵称试试" />
      <div v-else class="card-grid" v-loading="loading">
        <el-card v-for="u in list" :key="u.id"
          class="user-card" shadow="hover" @click="goUser(u.id)">
          <div class="user-card-body">
            <el-avatar :size="56" :src="resolveImage(u.avatarUrl)" />
            <div class="user-card-info">
              <div class="user-name" v-html="highlight(u.nickname, query.keyword)"></div>
              <div class="user-intro">{{ u.intro || '暂无简介' }}</div>
            </div>
          </div>
        </el-card>
      </div>
    </template>

    <!-- 态3 section:items -->
    <template v-else-if="activeSection === 'items'">
      <el-skeleton v-if="loading && list.length === 0" :rows="4" animated />
      <el-empty v-else-if="list.length === 0" description="没有找到相关排名项" />
      <div v-else class="item-list" v-loading="loading">
        <el-card v-for="i in list" :key="i.itemId"
          class="item-row" shadow="hover" @click="goItem(i.rankingId, i.itemId)">
          <div class="item-row-body">
            <img v-if="i.imageUrl" :src="resolveImage(i.imageUrl)" class="item-thumb" alt="" />
            <div v-else class="item-thumb item-thumb-empty">🖼</div>
            <div class="item-row-info">
              <div class="item-name" v-html="highlight(i.name, query.keyword)"></div>
              <div class="item-sub">
                属于
                <span class="item-ranking" @click.stop="goRanking(i.rankingId)">《{{ i.rankingTitle }}》</span>
                <span v-if="i.creatorNickname" class="item-creator">· @{{ i.creatorNickname }}</span>
              </div>
            </div>
            <div v-if="i.sourceName" class="item-source">《{{ i.sourceName }}》</div>
          </div>
        </el-card>
      </div>
    </template>

    <!-- 态3 section:works -->
    <template v-else>
      <el-skeleton v-if="loading && list.length === 0" :rows="2" animated />
      <el-empty v-else-if="list.length === 0" description="暂无来源作品名数据" />
      <div v-else class="work-cloud" v-loading="loading">
        <el-tag v-for="w in list" :key="w"
          class="work-tag" size="large" @click="backSearchWork(w)">
          《<span v-html="highlight(w, query.keyword)"></span>》
        </el-tag>
      </div>
    </template>

    <!-- 分页：browse 态不分页（全量卡片流），search 态各区块自带，section 态底部单页 -->
    <div v-if="mode !== 'search' && total > 0 && activeSection !== 'works'" class="pager">
      <span class="page-info">第 {{ query.current }} / {{ totalPages }} 页 · 共 {{ total }} 条</span>
      <el-pagination
        v-model:current-page="query.current"
        :page-size="query.size"
        :page-sizes="[12, 24, 48]"
        :total="total"
        layout="sizes, prev, pager, next, jumper"
        background
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>

    <ranking-create-dialog ref="createDialogRef" :categories="categories" @created="onCreated" />
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus, ArrowLeft, RefreshLeft } from '@element-plus/icons-vue'
import { listPublicRankings, listCategories, searchSourceNames, searchItems } from '@/api/ranking'
import { searchAll, searchByType } from '@/api/search'
import { resolveImage } from '@/utils/image'
import RankingCreateDialog from '@/components/RankingCreateDialog.vue'

const router = useRouter()

/* ------------------------------------------------------------------
 * 页面状态机：mode 决定当前展示哪个态
 *   browse  — 无搜索词，浏览全部公开榜单
 *   search  — 有搜索词，展示四维分类结果（态2）
 *   section — 从搜索结果"查看全部"进入，单维聚焦分页列表（态3）
 * ------------------------------------------------------------------ */
const mode = ref('browse')          // 'browse' | 'search' | 'section'
const activeSection = ref('rankings') // 'rankings' | 'users' | 'items' | 'works'

/* ---- browse & section 共用数据 ---- */
const loading = ref(false)
const list = ref([])
const total = ref(0)

/* ---- 态2 search 分类结果 ---- */
const searchLoading = ref(false)
const searchResults = reactive({
  rankings: [], rankingsTotal: 0,
  users: [],    usersTotal: 0,
  items: [],    itemsTotal: 0,
  works:   []
})

/* ---- 共用查询参数 ---- */
const categories = ref([])
const createDialogRef = ref()
const query = reactive({
  current: 1,
  size: 12,
  keyword: '',
  creatorNickname: '',
  categoryId: null
})

/* ---- Phase2 二级过滤器 ---- */
/* ---- 态3 排名项二级过滤（发起人已提升至工具栏 query.creatorNickname） ---- */
const sectionFilter = reactive({
  sourceName: ''         // 排名项聚焦：按来源作品名 AND 过滤
})

/* ---- 计算属性 ---- */
const pageTitle = computed(() =>
  mode.value === 'browse' ? '发现榜单' : mode.value === 'search' ? '搜索结果' : sectionTitle.value)
const pageSub = computed(() =>
  mode.value === 'browse' ? '看看大家在排什么，也发起一个你的观点榜'
    : mode.value === 'search' ? '以下结果按维度分类展示'
    : '')
const sectionTitleMap = { rankings: '榜单', users: '用户', items: '排名项', works: '作品名' }
const sectionTitle = computed(() => `相关${sectionTitleMap[activeSection.value] || ''}`)

const totalPages = computed(() =>
  Math.max(1, Math.ceil((Number(total.value) || 0) / query.size)))

const searchAllEmpty = computed(() =>
  !searchResults.rankings.length && !searchResults.users.length &&
  !searchResults.items.length && !searchResults.works.length)

const isLoggedIn = () => !!localStorage.getItem('lb_token')

const backLabel = computed(() =>
  query.keyword?.trim() ? '返回搜索结果' : '返回发现榜单')

/* ------------------------------------------------------------------
 * 数据加载
 * ------------------------------------------------------------------ */

// 态1 browse：加载公开榜单（无关键词，全量浏览）
async function loadBrowse() {
  loading.value = true
  try {
    const res = await listPublicRankings({
      current: query.current,
      size: query.size,
      categoryId: query.categoryId || undefined
    })
    list.value = res.data.records
    total.value = Number(res.data.total) || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 态2 search：并行调用各维度接口，填充分类结果
async function loadSearchResults(kw) {
  searchLoading.value = true
  // 重置
  searchResults.rankings = []; searchResults.rankingsTotal = 0
  searchResults.users    = []; searchResults.usersTotal    = 0
  searchResults.items    = []; searchResults.itemsTotal    = 0
  searchResults.works    = []

  const tasks = [
    listPublicRankings({ keyword: kw, current: 1, size: 8 })
      .then(res => {
        searchResults.rankings = res.data.records
        searchResults.rankingsTotal = Number(res.data.total) || 0
      })
  ]
  if (isLoggedIn()) {
    tasks.push(
      searchByType('USER', { keyword: kw, current: 1, size: 4 })
        .then(res => {
          searchResults.users = res.data.records
          searchResults.usersTotal = Number(res.data.total) || 0
        }),
      searchByType('ITEM', { keyword: kw, current: 1, size: 4 })
        .then(res => {
          searchResults.items = res.data.records
          searchResults.itemsTotal = Number(res.data.total) || 0
        }),
      searchSourceNames(kw)
        .then(res => { searchResults.works = res.data || [] })
    )
  }
  try {
    await Promise.all(tasks)
  } catch (e) {
    // 单维失败不影响其他维（Promise.all 已保证成功的那些已赋值）
  } finally {
    searchLoading.value = false
  }
}

// 态3 section：按 activeSection 加载聚焦列表
async function loadSection() {
  loading.value = true
  const kw = query.keyword?.trim() || ''
  try {
    if (activeSection.value === 'rankings') {
      const res = await listPublicRankings({
        keyword: kw || undefined,
        creatorNickname: query.creatorNickname?.trim() || undefined,
        current: query.current,
        size: query.size
      })
      list.value = res.data.records
      total.value = Number(res.data.total) || 0
    } else if (activeSection.value === 'users') {
      const res = await searchByType('USER', {
        keyword: kw, current: query.current, size: query.size
      })
      list.value = res.data.records
      total.value = Number(res.data.total) || 0
    } else if (activeSection.value === 'items') {
      const res = await searchItems({
        keyword: kw,
        sourceName: sectionFilter.sourceName?.trim() || undefined,
        current: query.current,
        size: query.size
      })
      list.value = res.data.records
      total.value = Number(res.data.total) || 0
    } else if (activeSection.value === 'works') {
      const res = await searchSourceNames(kw || undefined)
      list.value = res.data || []
      total.value = list.value.length
    }
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/* ------------------------------------------------------------------
 * 模式切换
 * ------------------------------------------------------------------ */

// 搜索框 Enter 或点击搜索按钮
function onSearch() {
  suggestVisible.value = false
  const kw = query.keyword?.trim()
  const cr = query.creatorNickname?.trim()
  query.current = 1
  if (cr) {
    // 有发起人 → 榜单聚焦列表：keyword 与 creatorNickname AND 交叉（后端 Phase2）
    mode.value = 'section'
    activeSection.value = 'rankings'
    sectionFilter.sourceName = ''
    loadSection()
    return
  }
  if (!kw) {
    // 两者皆空 → 回 browse
    mode.value = 'browse'
    loadBrowse()
    return
  }
  // 仅关键词 → 分类结果页
  mode.value = 'search'
  loadSearchResults(kw)
}

// 搜索框 clear 按钮
function onClear() {
  query.keyword = ''
  // 若发起人框仍有值则保留过滤（重跑 onSearch），否则回 browse
  onSearch()
}

// “重置”（全量）：一键清空所有搜索输入框（发起人/标题/排名项作品名）与分类筛选，回 browse
function onReset() {
  query.keyword = ''
  query.creatorNickname = ''
  query.categoryId = null
  sectionFilter.sourceName = ''
  suggestVisible.value = false
  mode.value = 'browse'
  query.current = 1
  loadBrowse()
}

// 从态2 点"查看全部" → 进入态3
function enterSection(section) {
  activeSection.value = section
  mode.value = 'section'
  query.current = 1
  sectionFilter.sourceName = ''
  loadSection()
}

// 从态3 返回：有关键词回分类结果页（放宽发起人过滤），否则回 browse
function backToSearch() {
  const kw = query.keyword?.trim()
  query.creatorNickname = ''
  if (kw) {
    mode.value = 'search'
    loadSearchResults(kw)
  } else {
    mode.value = 'browse'
    query.current = 1
    loadBrowse()
  }
}

// Phase2：section 态二级过滤变更时重新加载
function onSectionFilterChange() {
  query.current = 1
  loadSection()
}

/* ------------------------------------------------------------------
 * browse/section 分页
 * ------------------------------------------------------------------ */
function onPageChange(newPage) {
  query.current = newPage
  if (mode.value === 'section') {
    loadSection()
  } else {
    loadBrowse()
  }
}

function onSizeChange(val) {
  query.size = val
  query.current = 1
  if (mode.value === 'section') {
    loadSection()
  } else if (mode.value === 'browse') {
    loadBrowse()
  }
}

/* ------------------------------------------------------------------
 * browse 态分类筛选
 * ------------------------------------------------------------------ */
function selectCategory(id) {
  query.categoryId = id
  query.current = 1
  loadBrowse()
}

/* ------------------------------------------------------------------
 * 导航跳转
 * ------------------------------------------------------------------ */
function goRanking(id) {
  suggestVisible.value = false
  router.push(`/rankings/${id}`)
}

function goUser(id) {
  if (!id) return
  suggestVisible.value = false
  router.push(`/users/${id}`)
}

function goItem(rankingId, itemId) {
  suggestVisible.value = false
  router.push(`/rankings/${rankingId}/items/${itemId}`)
}

// 点击作品名：回填关键词并进入排名项聚焦列表
function backSearchWork(w) {
  query.keyword = w
  suggestVisible.value = false
  mode.value = 'section'
  activeSection.value = 'items'
  query.current = 1
  sectionFilter.sourceName = ''
  loadSection()
}

function openCreate() {
  createDialogRef.value?.open()
}

function onCreated(id) {
  if (mode.value === 'browse') {
    query.current = 1
    loadBrowse()
  } else {
    onSearch()
  }
  if (id) router.push(`/rankings/${id}`)
}

/* ------------------------------------------------------------------
 * 输入联想面板（保持原逻辑，去掉维度选择器）
 * ------------------------------------------------------------------ */
const suggestVisible = ref(false)
const suggestLoading = ref(false)
const suggest = ref({ rankings: [], users: [], items: [], works: [] })
let debounceTimer = null

const suggestEmpty = computed(() => {
  const s = suggest.value
  return !(s.rankings?.length || s.users?.length || s.items?.length || s.works?.length)
})

function onKeywordInput() {
  if (debounceTimer) clearTimeout(debounceTimer)
  if (!query.keyword?.trim()) {
    suggestVisible.value = false
    return
  }
  suggestVisible.value = true
  debounceTimer = setTimeout(loadSuggest, 300)
}

async function loadSuggest() {
  if (!isLoggedIn() || !query.keyword?.trim()) return
  suggestLoading.value = true
  try {
    const res = await searchAll(query.keyword)
    suggest.value = res.data || { rankings: [], users: [], items: [], works: [] }
  } catch (e) {
    // 联想失败静默忽略
  } finally {
    suggestLoading.value = false
  }
}

/* ------------------------------------------------------------------
 * 关键词高亮（转义防 XSS 再用原生 <mark> 包裹）
 * ------------------------------------------------------------------ */
function escapeHtml(s) {
  return String(s ?? '').replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]))
}
function escapeRegExp(s) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}
function highlight(text, kw) {
  const safe = escapeHtml(text)
  const k = (kw || '').trim()
  if (!k) return safe
  const re = new RegExp(escapeRegExp(escapeHtml(k)), 'gi')
  return safe.replace(re, m => `<mark>${m}</mark>`)
}

/* ------------------------------------------------------------------
 * 初始化
 * ------------------------------------------------------------------ */
onMounted(async () => {
  try {
    const res = await listCategories()
    categories.value = res.data
  } catch (e) {
    // 分类加载失败不阻塞
  }
  loadBrowse()
})
</script>

<style scoped>
.community {
  max-width: 1080px;
  margin: 0 auto;
}

/* 顶部工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
.page-title { margin: 0; font-size: 22px; }
.page-sub { color: #909399; font-size: 13px; }
.toolbar-right { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.search-group { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.search-label { color: #606266; font-size: 13px; white-space: nowrap; }
.creator-input { width: 120px; }
.search-input { width: 240px; }

/* 态3 聚焦导航 */
.section-nav {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 10px 16px;
  background: #f5f7fa;
  border-radius: 8px;
}
.section-keyword { color: #606266; font-size: 14px; }

/* Phase2 二级过滤条 */
.section-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  padding: 8px 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}
.filter-label { color: #606266; font-size: 13px; white-space: nowrap; }
.filter-input { width: 160px; }

/* browse 态分类栏 */
.category-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 18px;
}
.cat-tag { cursor: pointer; }

/* 态2 搜索结果区块 */
.result-section {
  margin-bottom: 32px;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}
.section-head h3 { margin: 0; font-size: 16px; color: #303133; }
.count-badge {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 8px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 10px;
  font-size: 12px;
  font-weight: normal;
  vertical-align: middle;
}

/* 榜单卡片网格 */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}
.ranking-card { cursor: pointer; border-radius: 10px; }
.card-cover {
  display: block;
  width: calc(100% + 40px);
  height: 150px;
  object-fit: cover;
  border-radius: 10px 10px 0 0;
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
.card-meta { display: flex; gap: 16px; color: #909399; font-size: 12px; margin-bottom: 12px; }
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f0f0f0;
  padding-top: 10px;
}
.creator { color: #409eff; font-size: 13px; cursor: pointer; }

/* 用户卡片 */
.user-card { cursor: pointer; border-radius: 10px; }
.user-card-body { display: flex; align-items: center; gap: 14px; }
.user-card-info { min-width: 0; }
.user-name { font-size: 16px; font-weight: 600; margin-bottom: 6px; }
.user-intro {
  color: #909399; font-size: 13px;
  overflow: hidden; display: -webkit-box;
  -webkit-line-clamp: 2; -webkit-box-orient: vertical;
}

/* 排名项行卡 */
.item-list { display: flex; flex-direction: column; gap: 12px; }
.item-row { cursor: pointer; border-radius: 10px; }
.item-row-body { display: flex; align-items: center; gap: 12px; }
.item-thumb { width: 48px; height: 48px; border-radius: 8px; object-fit: cover; flex-shrink: 0; }
.item-thumb-empty {
  display: flex; align-items: center; justify-content: center;
  background: #f5f7fa; font-size: 20px;
}
.item-row-info { flex: 1; min-width: 0; }
.item-name { font-size: 15px; font-weight: 600; margin-bottom: 4px; }
.item-sub { color: #909399; font-size: 12px; }
.item-ranking { color: #409eff; cursor: pointer; }
.item-creator { margin-left: 6px; }
.item-source { color: #67c23a; font-size: 12px; flex-shrink: 0; }

/* 作品名标签云 */
.work-cloud { display: flex; flex-wrap: wrap; gap: 12px; }
.work-tag { cursor: pointer; }

/* 分页 */
.pager {
  display: flex; align-items: center; justify-content: center;
  gap: 12px; margin-top: 24px;
}
.page-info { color: #606266; font-size: 13px; white-space: nowrap; }

/* 高亮 */
mark {
  background: #ffe58f; color: inherit;
  padding: 0 1px; border-radius: 2px;
}
</style>

<style>
/* 联想面板样式：非 scoped，popper 挂在 body 下 */
.suggest-popover { padding: 8px 0 !important; }
.suggest-panel { max-height: 380px; overflow-y: auto; padding: 0 8px; }
.suggest-group { margin-bottom: 8px; }
.suggest-head {
  font-size: 12px; color: #909399;
  padding: 4px 8px; border-bottom: 1px solid #f0f0f0;
}
.suggest-item {
  display: flex; align-items: center; gap: 6px;
  padding: 7px 8px; font-size: 14px;
  cursor: pointer; border-radius: 6px;
}
.suggest-item:hover { background: #f5f7fa; }
.suggest-sub { color: #909399; font-size: 12px; }
.suggest-empty { padding: 16px; text-align: center; color: #909399; font-size: 13px; }
.suggest-footer {
  padding: 8px 12px;
  border-top: 1px solid #f0f0f0;
  text-align: center;
}
</style>
