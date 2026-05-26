<template>
  <div class="page-container">
    <div class="profile-wrapper">
      <!-- Profile Header -->
      <div class="profile-header">
        <img
          :src="profileUser.userAvatar || defaultAvatar"
          alt="avatar"
          class="profile-avatar"
          @error="handleAvatarError"
        />
        <div class="profile-name">{{ profileUser.userName || '未设置昵称' }}</div>
        <div class="profile-account">账号：{{ profileUser.userAccount }}</div>
        <div v-if="userStatusInfo" class="profile-status">
          <a-tag :color="userStatusInfo.color">{{ userStatusInfo.label }}</a-tag>
        </div>

        <!-- Follow stats -->
        <div class="follow-stats">
          <span class="follow-stat" @click="goToFollowing">
            <strong>{{ followCount }}</strong> 关注
          </span>
          <span class="follow-stat" @click="goToFollowers">
            <strong>{{ followerCount }}</strong> 粉丝
          </span>
        </div>

        <!-- Actions -->
        <div class="profile-actions">
          <a-button v-if="isSelf" type="primary" @click="goToEdit">修改资料</a-button>
          <a-button
            v-else-if="loginUserStore.isLoggedIn"
            :type="isFollowed ? 'default' : 'primary'"
            :loading="followLoading"
            @click="handleToggleFollow"
          >
            {{ isFollowed ? '已关注' : '关注' }}
          </a-button>
        </div>
      </div>

      <!-- Strategy Tabs -->
      <div class="profile-tabs">
        <div
          :class="['profile-tab', { active: strategyTab === 'strategies' }]"
          @click="switchStrategyTab('strategies')"
        >
          {{ isSelf ? '我的攻略' : '投稿' }}
        </div>
        <div
          v-if="isSelf"
          :class="['profile-tab', { active: strategyTab === 'collect' }]"
          @click="switchStrategyTab('collect')"
        >
          我的收藏
        </div>
      </div>

      <!-- Strategy List -->
      <template v-if="strategyTab === 'strategies'">
        <a-spin :spinning="strategyLoading">
          <div v-if="strategyList.length > 0" class="strategy-list">
            <div
              v-for="item in strategyList"
              :key="item.id"
              class="strategy-card-horizontal"
              @click="goToDetail(item.id)"
            >
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
              <div class="card-tags">
                <a-tag v-for="tag in parseTags(item.strategyTags)" :key="tag" color="orange">
                  {{ tag }}
                </a-tag>
              </div>
              <div class="card-info-row-bottom">
                <span class="card-stats">
                  <span class="stat-item"><HeartOutlined /> {{ item.likeCount || 0 }}</span>
                  <span class="stat-item"><MessageOutlined /> {{ item.commentCount || 0 }}</span>
                </span>
              </div>
            </div>
          </div>
          <div v-else-if="!strategyLoading" class="empty-state">
            {{ isSelf ? '还没有发布攻略' : '该用户还没有投稿' }}
          </div>
        </a-spin>
        <div v-if="strategyTotal > 0" class="pagination-wrap">
          <a-pagination
            v-model:current="strategyPage"
            :total="strategyTotal"
            :page-size="strategyPageSize"
            :show-size-changer="false"
            @change="fetchStrategies"
          />
        </div>
      </template>

      <!-- Collect List -->
      <template v-if="strategyTab === 'collect'">
        <a-spin :spinning="collectLoading">
          <div v-if="collectList.length > 0" class="strategy-list">
            <div
              v-for="item in collectList"
              :key="item.collectId"
              class="strategy-card-horizontal"
              @click="goToDetail(item.id)"
            >
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
              <div class="card-tags">
                <a-tag v-for="tag in parseTags(item.strategyTags)" :key="tag" color="orange">
                  {{ tag }}
                </a-tag>
              </div>
              <div class="card-info-row-bottom card-info-row-bottom-collect">
                <span class="card-location">
                  <EnvironmentOutlined />
                  {{ formatLocation(item.locations) }}
                </span>
                <span class="card-stats">
                  <span class="stat-item"><HeartOutlined /> {{ item.likeCount || 0 }}</span>
                  <span class="stat-item"><StarOutlined /> {{ item.collectCount || 0 }}</span>
                </span>
              </div>
              <div class="card-delete-row" @click.stop>
                <a-popconfirm title="确定取消收藏？" ok-text="确定" cancel-text="取消" @confirm="handleUncollect(item)">
                  <a-button type="primary" danger size="small">取消收藏</a-button>
                </a-popconfirm>
              </div>
            </div>
          </div>
          <div v-else-if="!collectLoading" class="empty-state">暂无收藏</div>
        </a-spin>
        <div v-if="collectTotal > 0" class="pagination-wrap">
          <a-pagination
            v-model:current="collectPage"
            :total="collectTotal"
            :page-size="collectPageSize"
            :show-size-changer="false"
            @change="fetchCollectList"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { HeartOutlined, MessageOutlined, EnvironmentOutlined, StarOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { getUserProfile } from '@/api/userController'
import { listUserStrategies, listMyStrategies, listUserCollectStrategies, uncollectStrategy } from '@/api/strategyController'
import { toggleFollow, checkFollowed, getFollowCounts } from '@/api/followController'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

const defaultAvatar = 'https://api.dicebear.com/7.x/initials/svg?seed=User'

// Determine if viewing own profile or another user's
// Keep as string to avoid JS number precision loss with snowflake IDs
const targetUserId = computed(() => {
  const raw = route.params.userId
  if (raw) {
    const v = Array.isArray(raw) ? raw[0] : raw
    if (v) return v
  }
  return null
})

const isSelf = computed(() => {
  if (targetUserId.value === null || targetUserId.value === undefined) return true
  return String(targetUserId.value) === String(loginUserStore.loginUser?.id)
})

// Profile data
const profileUser = ref<API.UserVO>({
  id: undefined,
  userAccount: '',
  userName: '',
  userAvatar: '',
})

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '正常', color: 'green' },
  2: { label: '禁言', color: 'orange' },
  3: { label: '封号', color: 'red' },
}

const userStatusInfo = computed(() => {
  const status = profileUser.value?.userStatus
  if (status !== undefined && status !== null && STATUS_MAP[status]) {
    return STATUS_MAP[status]
  }
  return null
})

// Follow state
const isFollowed = ref(false)
const followCount = ref(0)
const followerCount = ref(0)
const followLoading = ref(false)

// Strategy list
const strategyTab = ref('strategies')
const strategyList = ref<API.StrategyVO[]>([])
const strategyLoading = ref(false)
const strategyPage = ref(1)
const strategyPageSize = 20
const strategyTotal = ref(0)

// Collect list
const collectList = ref<any[]>([])
const collectLoading = ref(false)
const collectPage = ref(1)
const collectPageSize = 20
const collectTotal = ref(0)

const SCROLL_KEY = 'UserProfile_scrollTop'

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
  await loadProfile()
  await fetchStrategies()
  restoreScroll()
})

watch(() => route.params.userId, async () => {
  await loadProfile()
  await fetchStrategies()
})

async function loadProfile() {
  if (isSelf.value) {
    // Own profile - use store data
    const user = loginUserStore.loginUser
    if (user) {
      profileUser.value = {
        id: user.id,
        userAccount: user.userAccount,
        userName: user.userName,
        userAvatar: user.userAvatar,
        userStatus: user.userStatus,
      }
      // Load follow counts for self too
      if (user.id) {
        loadFollowInfo(String(user.id))
      }
    }
  } else if (targetUserId.value) {
    // Other user's profile - fetch from API
    try {
      const res = await getUserProfile({ id: targetUserId.value })
      if (res.data?.code === 0 && res.data?.data) {
        profileUser.value = res.data.data
        loadFollowInfo(targetUserId.value)
        // Check if current user follows this user
        if (loginUserStore.isLoggedIn) {
          checkFollowStatus(targetUserId.value)
        }
      }
    } catch {
      message.error('加载用户信息失败')
    }
  }
}

async function loadFollowInfo(userId: string) {
  try {
    const res = await getFollowCounts(userId)
    if (res.data?.code === 0 && res.data?.data) {
      followCount.value = res.data.data.followCount || 0
      followerCount.value = res.data.data.followerCount || 0
    }
  } catch {
    // ignore
  }
}

async function checkFollowStatus(userId: string) {
  try {
    const res = await checkFollowed(userId)
    if (res.data?.code === 0) {
      isFollowed.value = res.data?.data ?? false
    }
  } catch {
    // ignore
  }
}

async function handleToggleFollow() {
  if (!targetUserId.value || !loginUserStore.isLoggedIn) return
  followLoading.value = true
  try {
    const res = await toggleFollow(targetUserId.value)
    if (res.data?.code === 0 && res.data?.data) {
      const followed = res.data.data.followed as boolean
      isFollowed.value = followed
      followerCount.value += followed ? 1 : -1
      if (followerCount.value < 0) followerCount.value = 0
    }
  } catch {
    message.error('操作失败')
  } finally {
    followLoading.value = false
  }
}

function switchStrategyTab(tab: string) {
  strategyTab.value = tab
  strategyPage.value = 1
  if (tab === 'collect') {
    fetchCollectList()
  } else {
    fetchStrategies()
  }
}

async function fetchStrategies(page = 1) {
  const userId = targetUserId.value ?? String(loginUserStore.loginUser?.id ?? '')
  if (!userId) return
  strategyLoading.value = true
  try {
    let res
    if (isSelf.value) {
      // Own profile - show all strategies
      res = await listMyStrategies({
        pageNum: page,
        pageSize: strategyPageSize,
      })
    } else {
      // Other user - show approved strategies only
      res = await listUserStrategies(userId, {
        pageNum: page,
        pageSize: strategyPageSize,
      })
    }
    if (res.data?.code === 0 && res.data?.data) {
      strategyList.value = res.data.data.records || []
      strategyTotal.value = res.data.data.totalRow || 0
      strategyPage.value = page
    }
  } catch {
    // ignore
  } finally {
    strategyLoading.value = false
  }
}

async function fetchCollectList(page = 1) {
  collectLoading.value = true
  try {
    const res = await listUserCollectStrategies({
      pageNum: page,
      pageSize: collectPageSize,
    })
    if (res.data?.code === 0 && res.data?.data) {
      collectTotal.value = res.data.data.totalRow || 0
      collectPage.value = page
      collectList.value = (res.data.data.records || []).map((item: API.StrategyCollectVO) => ({
        id: item.strategyId,
        strategyTitle: item.strategyTitle,
        imageUrls: item.imageUrls || (item.coverImage ? JSON.stringify([item.coverImage]) : ''),
        strategyContent: item.strategyContent || '',
        likeCount: item.likeCount,
        collectCount: item.collectCount,
        locations: item.locations || '',
        strategyTags: item.strategyTags,
        collectId: item.id,
      }))
    }
  } catch {
    message.error('加载收藏失败')
  } finally {
    collectLoading.value = false
  }
}

async function handleUncollect(item: any) {
  if (!item.id) return
  try {
    const res = await uncollectStrategy({ id: item.id })
    if (res.data?.code === 0) {
      message.success('取消收藏成功')
      fetchCollectList(collectPage.value)
    } else {
      message.error(res.data?.message || '取消收藏失败')
    }
  } catch {
    message.error('取消收藏失败')
  }
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

// Navigation
function goToEdit() {
  router.push('/profile/edit')
}

function goToDetail(id?: string | number) {
  if (id !== undefined && id !== null && String(id) !== '') {
    router.push({ name: 'strategyDetail', params: { id: String(id) } })
  }
}

function goToFollowing() {
  router.push('/my-follows')
}

function goToFollowers() {
  router.push('/my-follows')
}

// Image/content helpers
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

function parseTags(tagsStr?: string): string[] {
  if (!tagsStr) return []
  return tagsStr.split(',').filter(Boolean)
}

function truncateContent(content?: string): string {
  if (!content) return ''
  return content.length > 20 ? content.substring(0, 20) + '...' : content
}

// Error handlers
function handleAvatarError(e: Event) {
  const img = e.target as HTMLImageElement
  if (img.src !== defaultAvatar) img.src = defaultAvatar
}

function handleImgError(e: Event) {
  (e.target as HTMLImageElement).style.display = 'none'
}
</script>

<style scoped>
.profile-wrapper {
  max-width: 720px;
  margin: 0 auto;
}

.profile-header {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 16px;
}

.profile-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  background: #f0f0f0;
  margin-bottom: 12px;
}

.profile-name {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.profile-account {
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}

.profile-status {
  margin-bottom: 12px;
}

.follow-stats {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin-bottom: 16px;
}

.follow-stat {
  cursor: pointer;
  font-size: 14px;
  color: #666;
}

.follow-stat strong {
  color: #1a1a1a;
}

.follow-stat:hover {
  color: #ff6b6b;
}

.profile-actions {
  display: flex;
  justify-content: center;
}

.profile-tabs {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
  padding: 0 16px;
}

.profile-tab {
  font-size: 16px;
  color: #999;
  cursor: pointer;
  padding-bottom: 4px;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.profile-tab.active {
  color: #1a1a1a;
  font-weight: 600;
  border-bottom-color: #ff6b6b;
}

/* Strategy list cards - same style as HomePage */
.strategy-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.strategy-card-horizontal {
  width: 100%;
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

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 10px;
  margin-bottom: 6px;
}

.card-info-row-bottom {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  font-size: 12px;
  color: #999;
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

.card-location {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
  font-size: 12px;
}

.card-delete-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
}

.card-info-row-bottom-collect {
  justify-content: space-between;
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
