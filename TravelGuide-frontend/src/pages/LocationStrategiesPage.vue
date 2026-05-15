<template>
  <div class="page-container">
    <h2 style="margin-bottom: 20px; font-size: 20px">
      📍 {{ locationName }} 的攻略
    </h2>

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

    <!-- Official Pinned（横向长卡片） -->
    <div v-if="officialList.length > 0 && currentPage === 1" style="margin-bottom: 24px">
      <div style="font-size: 14px; font-weight: 600; color: #ff6b6b; margin-bottom: 12px">
        🔥 官方推荐
      </div>
      <div class="strategy-list">
        <div
          v-for="item in officialList"
          :key="item.id"
          class="strategy-card-horizontal"
          @click="goToDetail(item.id)"
        >
          <!-- 上部分：用户信息区 -->
          <div class="card-user-row">
            <img
              :src="displayAvatar(item)"
              alt="avatar"
              class="card-user-avatar"
              @error="handleAvatarError"
            />
            <span class="card-user-name">{{ displayUserName(item) }}</span>
            <span class="official-badge-tag">官方</span>
          </div>

          <!-- 中部分：核心内容区 -->
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

          <!-- 标签行 -->
          <div class="card-tags">
            <a-tag v-for="tag in parseTags(item.strategyTags)" :key="tag" color="orange">
              {{ tag }}
            </a-tag>
          </div>

          <!-- 下部分：信息区 -->
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
    </div>

    <!-- Normal List（横向长卡片） -->
    <div v-if="dataList.length > 0" class="strategy-list">
      <div
        v-for="item in dataList"
        :key="item.id"
        class="strategy-card-horizontal"
        @click="goToDetail(item.id)"
      >
        <!-- 上部分：用户信息区 -->
        <div class="card-user-row">
          <img
            :src="displayAvatar(item)"
            alt="avatar"
            class="card-user-avatar"
            @error="handleAvatarError"
          />
          <span class="card-user-name">{{ displayUserName(item) }}</span>
        </div>

        <!-- 中部分：核心内容区 -->
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

        <!-- 标签行 -->
        <div class="card-tags">
          <a-tag v-for="tag in parseTags(item.strategyTags)" :key="tag" color="orange">
            {{ tag }}
          </a-tag>
        </div>

        <!-- 下部分：信息区 -->
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

    <div v-if="dataList.length === 0 && officialList.length === 0 && !loading" class="empty-state">
      该地点暂无攻略
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
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HeartOutlined, MessageOutlined, EnvironmentOutlined } from '@ant-design/icons-vue'
import { listStrategiesByLocation } from '@/api/strategyController'

const route = useRoute()
const router = useRouter()

const locationId = computed(() => (route.params.locationId as string) || '')
const locationName = computed(() => {
  const q = route.query.name
  const raw = Array.isArray(q) ? q[0] : q
  if (raw && typeof raw === 'string') {
    try {
      return decodeURIComponent(raw)
    } catch {
      return raw
    }
  }
  return locationId.value ? `地点 #${locationId.value}` : ''
})

const currentTab = ref<'latest' | 'hot'>('latest')
const dataList = ref<API.StrategyVO[]>([])
const officialList = ref<API.StrategyVO[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

const defaultAvatar = 'https://api.dicebear.com/7.x/initials/svg?seed=User'

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

async function reloadLists() {
  currentPage.value = 1
  await Promise.all([fetchOfficial(), fetchData(1)])
}

const SCROLL_KEY = 'LocStrategies_scrollTop'

onBeforeUnmount(() => {
  const el = document.querySelector('.main-content')
  if (el) sessionStorage.setItem(SCROLL_KEY, String(el.scrollTop))
})

function restoreScroll() {
  try {
    const saved = sessionStorage.getItem(SCROLL_KEY)
    if (saved && Number(saved) > 0) {
      nextTick(() => {
        const el = document.querySelector('.main-content')
        if (el) el.scrollTop = Number(saved)
      })
    }
  } finally {
    sessionStorage.removeItem(SCROLL_KEY)
  }
}

onMounted(async () => {
  await reloadLists()
  restoreScroll()
})

watch(
  () => [route.params.locationId, route.query.name] as const,
  () => {
    if (!locationId.value) return
    reloadLists()
  }
)

function switchTab(tab: 'latest' | 'hot') {
  currentTab.value = tab
  currentPage.value = 1
  fetchData()
}

async function fetchOfficial() {
  if (!locationId.value) {
    officialList.value = []
    return
  }
  try {
    const res = await listStrategiesByLocation({
      pageNum: 1,
      pageSize: 10,
      location: locationId.value,
      isOfficial: 1,
    })
    if (res.data?.code === 0 && res.data?.data) {
      officialList.value = res.data.data.records || []
    }
  } catch {
    // ignore
  }
}

async function fetchData(page = 1) {
  if (!locationId.value) {
    dataList.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const params: API.StrategyQueryRequest = {
      pageNum: page,
      pageSize: pageSize,
      location: locationId.value,
    }
    if (currentTab.value === 'hot') {
      params.sortField = 'hotScore'
      params.sortOrder = 'desc'
    } else {
      params.sortField = 'createTime'
      params.sortOrder = 'desc'
    }
    params.isOfficial = 0

    const res = await listStrategiesByLocation(params)
    if (res.data?.code === 0 && res.data?.data) {
      dataList.value = res.data.data.records || []
      total.value = res.data.data.totalRow || 0
      currentPage.value = page
    }
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

function getFirstThreeImages(imageUrls?: string): string[] {
  if (!imageUrls) return []
  try {
    const urls = JSON.parse(imageUrls)
    return Array.isArray(urls) ? urls.slice(0, 3) : []
  } catch {
    return imageUrls?.split(',').slice(0, 3) || []
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

function parseTags(tagsStr?: string): string[] {
  if (!tagsStr) return []
  return tagsStr.split(',').filter(Boolean)
}

function handleImgError(e: Event) {
  (e.target as HTMLImageElement).style.display = 'none'
}

function handleAvatarError(e: Event) {
  const img = e.target as HTMLImageElement
  if (img.src !== defaultAvatar) {
    img.src = defaultAvatar
  }
}
</script>

<style scoped>
/* ===== 列表容器（一行一个，flex纵向排列） ===== */
.strategy-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

/* ===== 横向长卡片 ===== */
.strategy-card-horizontal {
  width: 100%;
  max-width: 720px;
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

/* ===== 上部分：用户信息区 ===== */
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

.official-badge-tag {
  font-size: 11px;
  color: #ff6b6b;
  border: 1px solid #ff6b6b;
  border-radius: 3px;
  padding: 0 5px;
  line-height: 18px;
  margin-left: auto;
  font-weight: 500;
}

/* ===== 中部分：核心内容区 ===== */
.card-content-area {
  margin-bottom: 0;
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
  margin-bottom: 0;
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

/* ===== 标签行 ===== */
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 10px;
  margin-bottom: 6px;
}

/* ===== 下部分：信息区（左右两端对齐） ===== */
.card-info-row-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
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

/* ===== Tabs 样式 ===== */
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

/* ===== 空状态 & 分页 ===== */
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
