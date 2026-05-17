<template>
  <div class="page-container">
    <div class="page-header">
      <h3 style="margin: 0; font-size: 18px">所有攻略</h3>
      <a-input-search
        v-model:value="searchTitle"
        placeholder="搜索攻略标题"
        style="width: 240px"
        @search="handleSearch"
        @change="handleSearchChange"
      />
    </div>

    <!-- Strategy List（横向长卡片，一行一个） -->
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
          <a-tag :color="getStatusColor(item.strategyStatus)" style="margin-left: auto">
            {{ getStatusText(item.strategyStatus) }}
          </a-tag>
          <span v-if="item.strategyStatus === 1" class="official-badge" style="margin-left: 8px">
            {{ item.isOfficial === 1 ? '官方推荐' : '' }}
          </span>
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

        <!-- 下部分：信息区（左右两端对齐） -->
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

        <!-- 管理员操作区 -->
        <div class="card-actions-row" @click.stop>
          <a-space>
            <a-button
              v-if="item.isOfficial !== 1"
              type="primary"
              size="small"
              @click="handleSetOfficial(item, 1)"
            >
              设为推荐
            </a-button>
            <a-button
              v-if="item.isOfficial === 1"
              size="small"
              @click="handleSetOfficial(item, 0)"
            >
              取消推荐
            </a-button>
            <a-popconfirm title="确定删除此攻略？" ok-text="确定" cancel-text="取消" @confirm="handleAdminDelete(item)">
              <a-button danger size="small">删除</a-button>
            </a-popconfirm>
          </a-space>
        </div>
      </div>
    </div>

    <!-- Empty -->
    <div v-else-if="!loading" class="empty-state">暂无攻略</div>

    <!-- Pagination -->
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
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { HeartOutlined, MessageOutlined, EnvironmentOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { listPassedStrategies, setOfficial, adminDeleteStrategy } from '@/api/strategyController'

const router = useRouter()

const dataList = ref<API.StrategyVO[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)
const searchTitle = ref('')

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: false,
})

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

const SCROLL_KEY = 'AllStrategies_scrollTop'

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
  await fetchData()
  restoreScroll()
})

async function fetchData(page = 1) {
  loading.value = true
  try {
    const params: API.StrategyQueryRequest = {
      pageNum: page,
      pageSize: pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    }
    if (searchTitle.value.trim()) {
      params.strategyTitle = searchTitle.value.trim()
    }
    const res = await listPassedStrategies(params)
    if (res.data?.code === 0 && res.data?.data) {
      dataList.value = res.data.data.records || []
      total.value = res.data.data.totalRow || 0
      pagination.current = page
      pagination.total = total.value
      currentPage.value = page
    }
  } catch {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  fetchData(1)
}

function handleSearchChange() {
  if (!searchTitle.value.trim()) {
    fetchData(1)
  }
}

async function handleSetOfficial(record: API.StrategyVO, isOfficial: number) {
  if (!record.id) return
  try {
    const res = await setOfficial({ id: record.id, isOfficial })
    if (res.data?.code === 0) {
      message.success(isOfficial === 1 ? '已设为推荐' : '已取消推荐')
      fetchData(currentPage.value)
    } else {
      message.error(res.data?.message || '操作失败')
    }
  } catch {
    message.error('操作失败')
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

function parseTags(tagsStr?: string): string[] {
  if (!tagsStr) return []
  return tagsStr.split(',').filter(Boolean)
}

function formatLocation(locations?: string): string {
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

function getStatusColor(status?: number): string {
  const map: Record<number, string> = { 0: 'orange', 1: 'green', 2: 'red' }
  return map[status ?? 0] || 'default'
}

function getStatusText(status?: number): string {
  const map: Record<number, string> = { 0: '待审核', 1: '已通过', 2: '已拒绝' }
  return map[status ?? 0] || '未知'
}

function goToDetail(id?: string | number) {
  if (id !== undefined && id !== null && String(id) !== '') {
    router.push({ name: 'strategyDetail', params: { id: String(id) } })
  }
}

function handleImgError(e: Event) {
  (e.target as HTMLImageElement).style.display = 'none'
}

async function handleAdminDelete(record: API.StrategyVO) {
  if (!record.id) return
  try {
    const res = await adminDeleteStrategy({ id: record.id })
    if (res.data?.code === 0) {
      message.success('删除成功')
      fetchData(currentPage.value)
    } else {
      message.error(res.data?.message || '删除失败')
    }
  } catch {
    message.error('删除失败')
  }
}

function handleAvatarError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = defaultAvatar
}
</script>

<style scoped>
/* ===== 页面头部 ===== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

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

.official-badge {
  font-size: 11px;
  color: #fff;
  background: #ff6b6b;
  padding: 1px 6px;
  border-radius: 3px;
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

/* ===== 管理员操作区 ===== */
.card-actions-row {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
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
