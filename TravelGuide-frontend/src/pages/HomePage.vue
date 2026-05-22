<template>
  <div class="home-layout">
    <!-- Left Content Area -->
    <div class="home-main">
      <!-- Tabs -->
      <div class="page-tabs">
        <div
          :class="['page-tab', { active: currentTab === 'latest' }]"
          @click="switchTab('latest')"
        >
          最新
        </div>
        <div
          :class="['page-tab', { active: currentTab === 'hot' }]"
          @click="switchTab('hot')"
        >
          推荐
        </div>
      </div>

      <!-- Active tag filter indicator -->
      <div v-if="activeTag" class="active-tag-bar">
        <a-tag color="#ff6b6b" closable @close="clearTag">
          {{ activeTag }}
        </a-tag>
      </div>

      <!-- Strategy List -->
      <div v-if="dataList.length > 0" class="strategy-list">
        <div
          v-for="item in dataList"
          :key="item.id"
          class="strategy-card-horizontal"
          @click="goToDetail(item.id)"
        >
          <!-- User info row -->
          <div class="card-user-row" @click.stop="goToAuthorProfile(item)">
            <img
              :src="displayAvatar(item)"
              alt="avatar"
              class="card-user-avatar"
              @error="handleAvatarError"
            />
            <span class="card-user-name">{{ displayUserName(item) }}</span>
          </div>

          <!-- Content area -->
          <div class="card-content-area">
            <div class="card-title">{{ item.strategyTitle }}</div>
            <div class="card-summary">{{ truncateContent(item.strategyContent) }}</div>
            <div v-if="getFirstThreeImages(item.imageUrls).length > 0" class="card-images-row">
              <div
                v-for="(img, idx) in getFirstThreeImages(item.imageUrls)"
                :key="idx"
                class="card-image-wrap"
              >
                <img
                  :src="img"
                  :alt="'img' + idx"
                  @error="handleImgError"
                />
              </div>
            </div>
            <div v-else class="card-no-img">暂无图片</div>
          </div>

          <!-- Info row: tags, location, stats -->
          <div class="card-info-row">
            <span class="card-tags">
              <a-tag v-for="tag in parseTags(item.strategyTags)" :key="tag" color="orange">
                {{ tag }}
              </a-tag>
            </span>
          </div>
          <div class="card-info-row-bottom">
            <span class="card-location">
              <EnvironmentOutlined />
              {{ formatLocation(item.locations) }}
            </span>
            <span class="card-stats">
              <span class="stat-item"><HeartOutlined /> {{ item.likeCount || 0 }}</span>
              <span class="stat-item"><MessageOutlined /> {{ item.commentCount || 0 }}</span>
            </span>
          </div>
        </div>
      </div>

      <div v-else-if="!loading" class="empty-state">
        暂无论略，快去新建一个吧~
      </div>

      <div v-if="total > 0" class="pagination-wrap">
        <a-pagination
          v-model:current="currentPage"
          :total="total"
          :page-size="pageSize"
          :show-size-changer="false"
          @change="fetchData"
        />
      </div>
    </div>

    <!-- Right Sidebar: Tag Columns -->
    <aside class="home-sidebar">
      <div class="sidebar-card">
        <h4 class="sidebar-title">攻略专栏</h4>
        <div class="tag-list">
          <div
            v-for="tag in columnTags"
            :key="tag"
            :class="['tag-item', { active: activeTag === tag }]"
            @click="selectTag(tag)"
          >
            {{ tag }}
          </div>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated, onDeactivated, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { HeartOutlined, MessageOutlined, EnvironmentOutlined } from '@ant-design/icons-vue'
import { listPassedStrategies, listHotStrategies } from '@/api/strategyController'

const router = useRouter()
const route = useRoute()

const currentTab = ref<'latest' | 'hot'>('latest')
const dataList = ref<API.StrategyVO[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)
const searchText = ref('')

const columnTags = ['美食专栏', '自然专栏', '人文专栏', '建筑专栏', '艺术专栏']
const activeTag = ref<string>('')

const defaultAvatar = 'https://api.dicebear.com/7.x/initials/svg?seed=User'

onMounted(() => {
  const keyword = route.query.keyword as string
  if (keyword) {
    searchText.value = keyword
  }
  const tag = route.query.tag as string
  if (tag) {
    activeTag.value = tag
  }
  fetchData()
})

// 滚动位置管理：在页面活跃时持续跟踪 .main-content 的滚动位置，
// 返回时可恢复到离开时的位置
let mainScrollTop = 0

function onMainScroll() {
  const el = document.querySelector('.main-content')
  if (el) mainScrollTop = el.scrollTop
}

onActivated(() => {
  // 页面被激活时，重新加载数据（keep-alive 缓存了组件，onMounted 只会执行一次）
  currentPage.value = 1
  fetchData()

  // 跟踪滚动位置
  const el = document.querySelector('.main-content')
  if (el) el.addEventListener('scroll', onMainScroll, { passive: true })

  // 在 DOM 布局完成后恢复到离开时的滚动位置
  if (mainScrollTop > 0) {
    nextTick(() => {
      const scrollEl = document.querySelector('.main-content')
      if (scrollEl) scrollEl.scrollTop = mainScrollTop
    })
  }
})

onDeactivated(() => {
  // 离开时停止跟踪（避免干扰其他页面的滚动）
  const el = document.querySelector('.main-content')
  if (el) el.removeEventListener('scroll', onMainScroll)
})

// 监听搜索关键词变化（路由查询参数变化时重新拉取数据）
watch(() => route.query.keyword, (newKeyword) => {
  searchText.value = (newKeyword as string) || ''
  currentPage.value = 1
  fetchData()
})

function switchTab(tab: 'latest' | 'hot') {
  currentTab.value = tab
  currentPage.value = 1
  fetchData()
}

function selectTag(tag: string) {
  if (activeTag.value === tag) {
    activeTag.value = ''
  } else {
    activeTag.value = tag
  }
  currentPage.value = 1
  // Update URL query param for shareability
  if (activeTag.value) {
    router.replace({ query: { ...route.query, tag: activeTag.value } })
  } else {
    const { tag: _, ...rest } = route.query
    router.replace({ query: rest })
  }
  fetchData()
}

function clearTag() {
  activeTag.value = ''
  currentPage.value = 1
  const { tag: _, ...rest } = route.query
  router.replace({ query: rest })
  fetchData()
}

async function fetchData(page = 1) {
  loading.value = true
  try {
    const params: API.StrategyQueryRequest = {
      pageNum: page,
      pageSize,
    }
    if (searchText.value) {
      params.keyword = searchText.value
    }
    if (activeTag.value) {
      params.strategyTags = activeTag.value
    }
    let res
    if (currentTab.value === 'latest') {
      params.sortField = 'createTime'
      params.sortOrder = 'desc'
      res = await listPassedStrategies(params)
    } else {
      res = await listHotStrategies(params)
    }
    if (res.data?.code === 0 && res.data?.data) {
      dataList.value = res.data.data.records || []
      total.value = res.data.data.totalRow || 0
      currentPage.value = page
    }
  } catch {
    console.error('加载攻略失败')
  } finally {
    loading.value = false
  }
}

function parseTags(tagsStr?: string): string[] {
  if (!tagsStr) return []
  return tagsStr.split(',').filter(Boolean)
}

function displayAvatar(item: API.StrategyVO): string {
  const url = item.userAvatar?.trim()
  if (url) return url
  const seed = item.userName?.trim() || String(item.userId ?? 'User')
  return `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(seed)}`
}

function displayUserName(item: API.StrategyVO): string {
  const name = item.userName?.trim()
  if (name) return name
  return item.userId != null && item.userId !== undefined ? `用户${item.userId}` : '用户'
}

function getFirstThreeImages(imageUrls?: string): string[] {
  if (!imageUrls) return []
  try {
    const urls = JSON.parse(imageUrls)
    if (Array.isArray(urls)) return urls.slice(0, 3)
    return []
  } catch {
    return imageUrls.split(',').slice(0, 3)
  }
}

function truncateContent(content?: string): string {
  if (!content) return ''
  return content.length > 20 ? content.substring(0, 20) + '...' : content
}

function formatLocation(locations?: string): string {
  if (!locations) return '未知地点'
  try {
    const arr = JSON.parse(locations)
    if (Array.isArray(arr) && arr.length > 0) {
      return arr.slice(0, 2).join(' · ')
    }
    return '未知地点'
  } catch {
    return locations || '未知地点'
  }
}

function goToDetail(id?: string | number) {
  if (id !== undefined && id !== null && String(id) !== '') {
    router.push({ name: 'strategyDetail', params: { id: String(id) } })
  }
}

function goToAuthorProfile(item: API.StrategyVO) {
  const userId = item.userId
  if (userId !== undefined && userId !== null) {
    router.push(`/user/${userId}/profile`)
  }
}

function handleImgError(e: Event) {
  const img = e.target as HTMLImageElement
  img.style.display = 'none'
}

function handleAvatarError(e: Event) {
  const img = e.target as HTMLImageElement
  if (img.src !== defaultAvatar) {
    img.src = defaultAvatar
  }
}
</script>

<style scoped>
/* ===== Layout ===== */
.home-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.home-main {
  flex: 1;
  min-width: 0;
}

.home-sidebar {
  width: 180px;
  flex-shrink: 0;
  position: sticky;
  top: 0;
}

/* ===== Sidebar Card ===== */
.sidebar-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding: 16px;
}

.sidebar-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 12px;
}

.tag-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tag-item {
  font-size: 13px;
  color: #666;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 6px;
  transition: all 0.15s;
}

.tag-item:hover {
  background: #fff2f0;
  color: #ff6b6b;
}

.tag-item.active {
  background: #fff2f0;
  color: #ff6b6b;
  font-weight: 500;
}

/* ===== Active tag bar ===== */
.active-tag-bar {
  margin-bottom: 12px;
}

/* ===== Tabs ===== */
.page-tabs {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
}

.page-tab {
  font-size: 16px;
  color: #999;
  cursor: pointer;
  padding-bottom: 4px;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.page-tab.active {
  color: #1a1a1a;
  font-weight: 600;
  border-bottom-color: #ff6b6b;
}

.page-tab:hover {
  color: #1a1a1a;
}

/* ===== Strategy List ===== */
.strategy-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.strategy-card-horizontal {
  width: 680px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  padding: 16px;
  cursor: pointer;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
  box-sizing: border-box;
}

.strategy-card-horizontal:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.card-user-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  cursor: pointer;
}

.card-user-row:hover .card-user-name {
  color: #ff6b6b;
}

.card-user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  background: #f0f0f0;
}

.card-user-name {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}


.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 6px;
}

.card-summary {
  font-size: 13px;
  color: #999;
  line-height: 1.4;
  margin-bottom: 10px;
}

.card-images-row {
  display: flex;
  gap: 24px;
}

.card-image-wrap {
  width: 200px;
  height: 200px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-image-wrap img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.card-no-img {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 200px;
  background: #f5f5f5;
  color: #bbb;
  font-size: 13px;
  border-radius: 6px;
}

/* Tags row */
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 6px;
}

/* Info row */
.card-info-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.card-info-row-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.card-location {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
  font-size: 12px;
}

.card-stats {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 3px;
  color: #999;
  font-size: 12px;
}

.empty-state {
  text-align: center;
  padding: 48px 0;
  color: #bbb;
  font-size: 14px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
