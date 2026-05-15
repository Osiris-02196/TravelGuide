<template>
  <div class="page-container">
    <!-- Tabs -->
    <div class="page-tabs">
      <div
        :class="['page-tab tab-passed', { active: currentTab === 'passed' }]"
        @click="switchTab('passed')"
      >
        上传成功
      </div>
      <div
        :class="['page-tab tab-pending', { active: currentTab === 'pending' }]"
        @click="switchTab('pending')"
      >
        待审核
      </div>
      <div
        :class="['page-tab tab-rejected', { active: currentTab === 'rejected' }]"
        @click="switchTab('rejected')"
      >
        已拒绝
      </div>
      <div
        :class="['page-tab', { active: currentTab === 'collect' }]"
        @click="switchTab('collect')"
      >
        我的收藏
      </div>
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
        </div>

        <!-- 中部分：核心内容区 -->
        <div class="card-content-area">
          <!-- 攻略标题（加粗，最多2行，超出省略） -->
          <div class="card-title">{{ item.strategyTitle }}</div>

          <!-- 攻略内容摘要（截取前20字，灰色文本） -->
          <div class="card-summary">{{ truncateContent(item.strategyContent) }}</div>

          <!-- 图片（最多3张，200x200 横向排列） -->
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
            <span class="stat-item"><StarOutlined /> {{ item.collectCount || 0 }}</span>
          </span>
        </div>

        <!-- 删除/取消收藏按钮 -->
        <div v-if="currentTab === 'passed'" class="card-delete-row">
          <a-popconfirm title="确定删除此攻略？" ok-text="确定" cancel-text="取消" @confirm="handleDelete(item)">
            <a-button danger size="small">删除</a-button>
          </a-popconfirm>
        </div>
        <div v-else-if="currentTab === 'collect'" class="card-delete-row">
          <a-popconfirm title="确定取消收藏？" ok-text="确定" cancel-text="取消" @confirm="handleUncollect(item)">
            <a-button type="primary" danger size="small">取消收藏</a-button>
          </a-popconfirm>
        </div>
      </div>
    </div>

    <div v-else-if="!loading" class="empty-state">{{ currentTab === 'collect' ? '暂无收藏' : '暂无数据' }}</div>

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
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { HeartOutlined, StarOutlined, EnvironmentOutlined } from '@ant-design/icons-vue'
import { listMyStrategies, deleteStrategy, listUserCollectStrategies, uncollectStrategy } from '@/api/strategyController'
import { message } from 'ant-design-vue'

type TabKey = 'passed' | 'pending' | 'rejected' | 'collect'

const router = useRouter()
const currentTab = ref<TabKey>('passed')
const dataList = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

const defaultAvatar = 'https://api.dicebear.com/7.x/initials/svg?seed=User'

const SCROLL_KEY = 'MyStrategies_scrollTop'

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

function displayAvatar(item: any): string {
  const url = item.userAvatar?.trim()
  if (url) return url
  const seed = item.userName?.trim() || String(item.userId ?? 'User')
  return `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(seed)}`
}

function displayUserName(item: any): string {
  const name = item.userName?.trim()
  if (name) return name
  return item.userId != null && item.userId !== undefined ? `用户${item.userId}` : '用户'
}

function switchTab(tab: TabKey) {
  currentTab.value = tab
  currentPage.value = 1
  fetchData()
}

async function fetchData(page = 1) {
  loading.value = true
  try {
    if (currentTab.value === 'collect') {
      // 调用收藏列表接口
      const res = await listUserCollectStrategies({
        pageNum: page,
        pageSize: pageSize,
      })
      if (res.data?.code === 0 && res.data?.data) {
        total.value = res.data.data.totalRow || 0
        currentPage.value = page
        // 将 StrategyCollectVO 映射为模板可用的数据结构
        dataList.value = (res.data.data.records || []).map((item: API.StrategyCollectVO) => ({
          id: item.strategyId,
          strategyTitle: item.strategyTitle,
          imageUrls: item.imageUrls || (item.coverImage ? JSON.stringify([item.coverImage]) : ''),
          strategyContent: item.strategyContent || '',
          likeCount: item.likeCount,
          collectCount: item.collectCount,
          commentCount: 0,
          locations: item.locations || '',
          userName: item.userName,
          userAvatar: item.userAvatar,
          collectId: item.id, // 收藏记录ID，用于取消
        }))
      }
    } else {
      // 其他 tab 原有逻辑
      const params: API.StrategyQueryRequest = {
        pageNum: page,
        pageSize: pageSize,
        sortField: 'createTime',
        sortOrder: 'desc',
      }
      if (currentTab.value === 'passed') params.strategyStatus = 1
      else if (currentTab.value === 'pending') params.strategyStatus = 0
      else if (currentTab.value === 'rejected') params.strategyStatus = 2

      const res = await listMyStrategies(params)
      if (res.data?.code === 0 && res.data?.data) {
        dataList.value = res.data.data.records || []
        total.value = res.data.data.totalRow || 0
        currentPage.value = page
      }
    }
  } catch {
    message.error('加载失败')
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
  if (!locations) return ''
  try {
    const arr = JSON.parse(locations)
    if (Array.isArray(arr) && arr.length > 0) {
      return arr.slice(0, 2).join(' · ')
    }
    return ''
  } catch {
    return locations || ''
  }
}

function goToDetail(id?: string | number) {
  if (id !== undefined && id !== null && String(id) !== '') {
    router.push({ name: 'strategyDetail', params: { id: String(id) } })
  }
}

async function handleDelete(item: any) {
  if (!item.id) return
  try {
    const res = await deleteStrategy({ id: item.id })
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

async function handleUncollect(item: any) {
  if (!item.id) return
  try {
    const res = await uncollectStrategy({ id: item.id })
    if (res.data?.code === 0) {
      message.success('取消收藏成功')
      fetchData(currentPage.value)
    } else {
      message.error(res.data?.message || '取消收藏失败')
    }
  } catch {
    message.error('取消收藏失败')
  }
}

function parseTags(tagsStr?: string): string[] {
  if (!tagsStr) return []
  return tagsStr.split(',').filter(Boolean)
}

function handleImgError(e: Event) {
  ;(e.target as HTMLImageElement).style.display = 'none'
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

/* ===== 删除/取消收藏按钮行 ===== */
.card-delete-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
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
