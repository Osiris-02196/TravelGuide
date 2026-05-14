<template>
  <div class="detail-page">
    <div class="detail-card">
      <!-- Sticky top bar: back button only -->
      <div class="detail-top-bar">
        <LeftOutlined class="back-arrow" @click="goBack" />
      </div>

      <!-- Scrollable content area -->
      <div class="detail-scroll-area">
        <a-spin :spinning="loading">
          <!-- Images - single rectangle with navigation arrows -->
          <div class="detail-image-container">
            <div v-if="allImages.length > 0" class="detail-image-wrapper">
              <div
                v-if="allImages.length > 1"
                class="detail-image-arrow detail-image-arrow-left"
                @click="prevImage"
              >
                <LeftOutlined />
              </div>
              <img :src="allImages[currentImageIndex]" alt="strategy img" @error="handleImgError" />
              <div
                v-if="allImages.length > 1"
                class="detail-image-arrow detail-image-arrow-right"
                @click="nextImage"
              >
                <RightOutlined />
              </div>
              <span v-if="allImages.length > 1" class="detail-image-counter">
                {{ currentImageIndex + 1 }} / {{ allImages.length }}
              </span>
            </div>
            <div v-else class="detail-no-image">暂无图片</div>
          </div>

          <!-- User avatar + name row (below images, left aligned) -->
          <div v-if="detail" class="detail-user-row" @click="goToAuthorProfile">
            <img
              :src="displayAvatar(detail)"
              alt="avatar"
              class="user-avatar"
              @error="handleAvatarError"
            />
            <span class="user-name">{{ displayUserName(detail) }}</span>
          </div>

          <!-- Tags -->
          <div v-if="detailTags.length > 0" class="detail-tags">
            <a-tag v-for="tag in detailTags" :key="tag" color="orange">{{ tag }}</a-tag>
          </div>

          <!-- Title -->
          <h1 class="detail-title">
            {{ detail?.strategyTitle }}
            <span v-if="detail?.isOfficial === 1" class="official-badge">官方推荐</span>
          </h1>

          <!-- Content -->
          <div class="detail-body">{{ detail?.strategyContent }}</div>

          <!-- Comments -->
          <div class="comment-section">
            <div class="comment-header">
              <h3>
                评论区
                <span v-if="commentTotal > 0" class="comment-count">({{ commentTotal }})</span>
              </h3>
              <div class="comment-tabs">
                <span
                  :class="['comment-tab', { active: commentSort === 'createTime' }]"
                  @click="switchCommentTab('createTime')"
                >最新</span>
                <span
                  :class="['comment-tab', { active: commentSort === 'likeCount' }]"
                  @click="switchCommentTab('likeCount')"
                >最热</span>
              </div>
            </div>

            <div v-if="comments.length > 0">
              <div v-for="c in comments" :key="c.id" class="comment-item">
                <div class="comment-item-header">
                  <img :src="c.userAvatar || defaultAvatar" alt="avatar" class="comment-avatar" />
                  <span class="comment-item-name">{{ c.userName || '匿名用户' }}</span>
                  <span class="comment-item-time">
                    {{ c.createTime ? dayjs(c.createTime).format('YYYY-MM-DD HH:mm') : '' }}
                  </span>
                  <span class="comment-like-btn" @click="handleCommentLike(c)">
                    <HeartOutlined :style="{ color: c._liked ? '#ff4d4f' : '#ccc' }" />
                    {{ c.likeCount || 0 }}
                  </span>
                </div>
                <div class="comment-item-content">{{ c.content }}</div>
              </div>
            </div>
            <div v-else style="text-align: center; padding: 24px; color: #bbb; font-size: 13px">
              暂无评论，快来抢沙发~
            </div>
          </div>
        </a-spin>
      </div>

      <!-- Fixed bottom bar: input + actions -->
      <div class="detail-bottom-bar">
        <div v-if="loginUserStore.isLoggedIn" class="bottom-bar-inner">
          <input
            v-model="commentText"
            class="bottom-input"
            placeholder="写下你的评论..."
            @keyup.enter="submitComment"
          />
          <div class="bottom-actions">
            <span class="action-btn" @click="handleLike">
              <HeartOutlined :style="{ color: liked ? '#ff4d4f' : '#999' }" />
              {{ likeCount }}
            </span>
            <span class="action-btn" @click="handleCollect">
              <StarOutlined :style="{ color: collected ? '#faad14' : '#999' }" />
              {{ collectCount }}
            </span>
            <a-button
              type="primary"
              size="small"
              :loading="submittingComment"
              @click="submitComment"
              class="bottom-submit-btn"
            >
              发表
            </a-button>
          </div>
        </div>
        <div v-else class="bottom-bar-login">
          <a @click="goToLogin">登录</a>后可评论
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HeartOutlined, StarOutlined, LeftOutlined, RightOutlined } from '@ant-design/icons-vue'
import { getStrategyDetail, likeStrategy, collectStrategy } from '@/api/strategyController'
import { addComment, listComments, likeComment } from '@/api/commentController'
import { useLoginUserStore } from '@/stores/loginUser'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const detail = ref<API.StrategyVO | null>(null)
const loading = ref(false)
const liked = ref(false)
const collected = ref(false)
const likeCount = ref(0)
const collectCount = ref(0)
const commentText = ref('')
const submittingComment = ref(false)
const comments = ref<API.CommentVO[]>([])
const commentSort = ref<string>('createTime')
const commentPage = ref(1)
const commentTotal = ref(0)

const defaultAvatar = 'https://api.dicebear.com/7.x/initials/svg?seed=User'

const allImages = ref<string[]>([])
const currentImageIndex = ref(0)

function routeStrategyId(): string | null {
  const raw = route.params.id
  const v = Array.isArray(raw) ? raw[0] : raw
  if (v === undefined || v === null || String(v).trim() === '') return null
  return String(v)
}

const detailTags = computed(() => {
  const tags = detail.value?.strategyTags
  if (!tags) return []
  return tags.split(',').filter(Boolean)
})

onMounted(async () => {
  const idStr = routeStrategyId()
  if (!idStr) return
  await fetchDetail(idStr)
  await fetchComments(idStr)
})

async function fetchDetail(id: string) {
  loading.value = true
  try {
    const res = await getStrategyDetail({ id })
    if (res.data?.code === 0 && res.data?.data) {
      detail.value = res.data.data
      currentImageIndex.value = 0
      likeCount.value = res.data.data.likeCount || 0
      collectCount.value = res.data.data.collectCount || 0
      // Parse images
      if (res.data.data.imageUrls) {
        try {
          const urls = JSON.parse(res.data.data.imageUrls)
          allImages.value = Array.isArray(urls) ? urls : []
        } catch {
          allImages.value = res.data.data.imageUrls.split(',').filter(Boolean)
        }
      }
    }
  } catch {
    message.error('加载攻略详情失败')
  } finally {
    loading.value = false
  }
}

async function fetchComments(strategyId: string) {
  try {
    const res = await listComments({
      strategyId: strategyId as any,
      pageNum: commentPage.value,
      pageSize: 20,
      sortField: commentSort.value,
      sortOrder: 'desc',
    })
    if (res.data?.code === 0 && res.data?.data) {
      comments.value = res.data.data.records || []
      commentTotal.value = res.data.data.totalRow || 0
    }
  } catch {
    // ignore
  }
}

function switchCommentTab(tab: string) {
  if (tab === commentSort.value) return
  commentSort.value = tab
  commentPage.value = 1
  const idStr = routeStrategyId()
  if (idStr) fetchComments(idStr)
}

async function handleLike() {
  if (!loginUserStore.isLoggedIn) {
    message.warning('请先登录')
    return
  }
  if (!detail.value?.id) return
  try {
    await likeStrategy({ id: detail.value.id })
    liked.value = !liked.value
    likeCount.value += liked.value ? 1 : -1
    if (likeCount.value < 0) likeCount.value = 0
  } catch {
    // ignore
  }
}

async function handleCollect() {
  if (!loginUserStore.isLoggedIn) {
    message.warning('请先登录')
    return
  }
  if (!detail.value?.id) return
  try {
    await collectStrategy({ id: detail.value.id })
    collected.value = !collected.value
    collectCount.value += collected.value ? 1 : -1
    if (collectCount.value < 0) collectCount.value = 0
    message.success(collected.value ? '收藏成功' : '已取消收藏')
  } catch {
    // ignore
  }
}

async function submitComment() {
  if (!commentText.value.trim()) {
    message.warning('请输入评论内容')
    return
  }
  const idStr = routeStrategyId()
  if (!idStr) {
    message.error('攻略ID不存在')
    return
  }
  submittingComment.value = true
  try {
    const res = await addComment({
      strategyId: idStr as any,
      content: commentText.value.trim(),
    })
    if (res.data?.code === 0) {
      message.success('评论成功')
      commentText.value = ''
      await fetchComments(idStr)
    } else {
      message.error(res.data?.message || '评论失败')
    }
  } catch {
    message.error('评论失败，请稍后重试')
  } finally {
    submittingComment.value = false
  }
}

async function handleCommentLike(c: any) {
  if (!loginUserStore.isLoggedIn) {
    message.warning('请先登录')
    return
  }
  if (!c.id) return
  try {
    await likeComment({ id: c.id })
    c._liked = !c._liked
    c.likeCount = (c.likeCount || 0) + (c._liked ? 1 : -1)
    if (c.likeCount < 0) c.likeCount = 0
  } catch {
    // ignore
  }
}

function goToLogin() {
  router.push('/user/login')
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

function prevImage() {
  if (allImages.value.length === 0) return
  currentImageIndex.value =
    currentImageIndex.value > 0 ? currentImageIndex.value - 1 : allImages.value.length - 1
}

function nextImage() {
  if (allImages.value.length === 0) return
  currentImageIndex.value =
    currentImageIndex.value < allImages.value.length - 1 ? currentImageIndex.value + 1 : 0
}

function goBack() {
  router.back()
}

function goToAuthorProfile() {
  const userId = detail.value?.userId
  if (userId !== undefined && userId !== null) {
    router.push(`/user/${userId}/profile`)
  }
}
</script>

<style scoped>
.detail-page {
  height: 100%;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.detail-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  width: 800px;
  margin: 0 auto;
  background: #fff;
  overflow: hidden;
  border-radius: 12px;
}

/* ===== Top Bar ===== */
.detail-top-bar {
  position: sticky;
  top: 0;
  z-index: 10;
  flex-shrink: 0;
  padding: 10px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}

.back-arrow {
  font-size: 22px;
  color: #333;
  cursor: pointer;
  display: inline-block;
  line-height: 1;
}

/* ===== Scrollable Content ===== */
.detail-scroll-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  min-height: 0;
}

/* Images - single rectangle with arrows */
.detail-image-container {
  width: 100%;
  margin-bottom: 16px;
}

.detail-image-wrapper {
  position: relative;
  width: 100%;
  height: 450px;
  background: #f5f5f5;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-image-wrapper img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.detail-image-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  background: rgba(0, 0, 0, 0.35);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 2;
  font-size: 15px;
  transition: background 0.2s;
  user-select: none;
}

.detail-image-arrow:hover {
  background: rgba(0, 0, 0, 0.55);
}

.detail-image-arrow-left {
  left: 10px;
}

.detail-image-arrow-right {
  right: 10px;
}

.detail-image-counter {
  position: absolute;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
  z-index: 2;
}

.detail-no-image {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 200px;
  background: #f5f5f5;
  color: #bbb;
  font-size: 14px;
  border-radius: 8px;
  margin-bottom: 16px;
}

/* User info row (below images, left aligned) */
.detail-user-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 12px 0 16px;
  cursor: pointer;
}

.detail-user-row:hover .user-name {
  color: #ff6b6b;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  background: #f0f0f0;
  flex-shrink: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

/* Tags */
.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

/* Title */
.detail-title {
  font-size: 20px;
  font-weight: 600;
  color: #222;
  margin: 0 0 16px;
  line-height: 1.4;
}

.official-badge {
  display: inline-block;
  font-size: 11px;
  color: #1890ff;
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 4px;
  padding: 0 6px;
  margin-left: 6px;
  vertical-align: middle;
  font-weight: 400;
}

/* Content */
.detail-body {
  font-size: 15px;
  color: #444;
  line-height: 1.8;
  margin-bottom: 24px;
  white-space: pre-wrap;
  word-break: break-word;
}

/* ===== Comments ===== */
.comment-section {
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.comment-header h3 {
  margin: 0;
  font-size: 16px;
}

.comment-count {
  font-weight: 400;
  font-size: 13px;
  color: #999;
}

.comment-tabs {
  display: flex;
  gap: 12px;
}

.comment-tab {
  cursor: pointer;
  font-size: 14px;
  color: #999;
  padding: 2px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.comment-tab.active {
  color: #1890ff;
  font-weight: 500;
}

.comment-tab:hover {
  color: #1890ff;
}

.comment-item {
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  background: #f0f0f0;
  flex-shrink: 0;
}

.comment-item-name {
  font-size: 13px;
  color: #333;
  font-weight: 500;
  flex-shrink: 0;
}

.comment-item-time {
  flex: 1;
  font-size: 12px;
  color: #bbb;
}

.comment-like-btn {
  cursor: pointer;
  font-size: 18px;
  color: #999;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  user-select: none;
}

.comment-like-btn:hover {
  color: #ff4d4f;
}

.comment-item-content {
  font-size: 14px;
  color: #444;
  line-height: 1.6;
  margin-top: 6px;
  padding-left: 36px;
  word-break: break-word;
}

/* ===== Bottom Bar ===== */
.detail-bottom-bar {
  position: sticky;
  bottom: 0;
  z-index: 10;
  flex-shrink: 0;
  padding: 8px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
}

.bottom-bar-inner {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bottom-input {
  flex: 1;
  height: 36px;
  border: 1px solid #d9d9d9;
  border-radius: 18px;
  padding: 0 16px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  min-width: 0;
}

.bottom-input:focus {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

.bottom-input::placeholder {
  color: #bbb;
}

.bottom-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.action-btn {
  cursor: pointer;
  font-size: 14px;
  color: #999;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  white-space: nowrap;
  user-select: none;
}

.action-btn:hover {
  color: #1890ff;
}

.bottom-submit-btn {
  flex-shrink: 0;
  border-radius: 18px;
  height: 32px;
  padding: 0 16px;
  font-size: 13px;
}

.bottom-bar-login {
  text-align: center;
  color: #888;
  font-size: 13px;
  padding: 6px 0;
}
</style>
