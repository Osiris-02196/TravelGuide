<template>
  <div class="detail-page">
    <div class="detail-card">
      <!-- Sticky top bar: back button only -->
      <div class="detail-top-bar">
        <LeftOutlined class="back-arrow" @click="goBack" />
        <a-dropdown v-if="loginUserStore.isLoggedIn && detail" trigger="click">
          <MoreOutlined class="more-icon" />
          <template #overlay>
            <a-menu>
              <a-menu-item key="report" @click="handleReportStrategy">
                举报
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
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

          <!-- Route Map -->
          <div v-if="routeDataStr" class="route-map-section">
            <a-button
              class="route-map-toggle-btn"
              @click="showRouteMap = !showRouteMap"
            >
              <EnvironmentOutlined />
              {{ showRouteMap ? '收起路线规划' : '查看作者路线规划' }}
            </a-button>
            <RouteMapViewer
              v-if="showRouteMap"
              :route-data="parseRouteData(routeDataStr)"
            />
          </div>

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
                <div :id="`comment-${c.id}`" class="comment-item-header">
                  <img :src="c.userAvatar || defaultAvatar" alt="avatar" class="comment-avatar" @click="goToUserProfile(c.userId)" />
                  <span class="comment-item-name" @click="goToUserProfile(c.userId)">{{ c.userName || '匿名用户' }}</span>
                  <span class="comment-item-time">
                    {{ c.createTime ? dayjs(c.createTime).format('YYYY-MM-DD HH:mm') : '' }}
                  </span>
                  <span class="comment-like-btn" @click="handleCommentLike(c)">
                    <HeartOutlined :style="{ color: c._liked ? '#ff4d4f' : '#ccc' }" />
                    {{ c.likeCount || 0 }}
                  </span>
                  <a-dropdown v-if="loginUserStore.isLoggedIn" trigger="click">
                    <MoreOutlined class="comment-more-icon" />
                    <template #overlay>
                      <a-menu>
                        <a-menu-item key="report" @click="handleReportComment(c)">
                          举报
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </div>
                <div class="comment-item-content">{{ c.content }}</div>
                <div class="comment-actions">
                  <span v-if="loginUserStore.isLoggedIn" class="comment-action-btn" @click="startReply(c)">回复</span>
                </div>
                <!-- 回复列表 -->
                <div v-if="c.replyCount && c.replyCount > 0" class="reply-toggle-area">
                  <span v-if="!expandedSet.has(String(c.id))" class="reply-expand-btn" @click="toggleReplies(c.id)">
                    <DownOutlined /> 共 {{ c.replyCount }} 条回复
                  </span>
                  <span v-else class="reply-expand-btn" @click="toggleReplies(c.id)">
                    <UpOutlined /> 收起
                  </span>
                </div>
                <div v-if="expandedSet.has(String(c.id))" class="replies-section">
                  <a-spin :spinning="!!loadingReplyMap[String(c.id)]" size="small">
                    <div v-for="r in (replyMap[String(c.id)] || [])" :key="r.id" class="reply-item">
                      <div class="reply-item-header">
                        <img :src="r.userAvatar || defaultAvatar" alt="avatar" class="reply-avatar" @click="goToUserProfile(r.userId)" />
                        <span class="comment-item-name" @click="goToUserProfile(r.userId)">{{ r.userName || '匿名用户' }}</span>
                        <span class="comment-item-time">
                          {{ r.createTime ? dayjs(r.createTime).format('YYYY-MM-DD HH:mm') : '' }}
                        </span>
                        <span class="comment-like-btn" @click="handleCommentLike(r)">
                          <HeartOutlined :style="{ color: r._liked ? '#ff4d4f' : '#ccc' }" />
                          {{ r.likeCount || 0 }}
                        </span>
                      </div>
                      <div class="reply-item-content">
                        <span v-if="r.replyToUserName" class="reply-at-username">回复 @{{ r.replyToUserName }}：</span>
                        {{ r.content }}
                      </div>
                      <div class="comment-actions">
                        <span v-if="loginUserStore.isLoggedIn" class="comment-action-btn" @click="startReply(r)">回复</span>
                      </div>
                    </div>
                    <div v-if="replyMap[String(c.id)] && replyMap[String(c.id)].length < (replyTotalMap[String(c.id)] || 0)"
                         class="load-more-replies">
                      <span @click="loadMoreReplies(c.id)">查看更多回复</span>
                      <span class="reply-collapse-btn" @click="toggleReplies(c.id)">收起</span>
                    </div>
                  </a-spin>
                </div>
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
          <div class="bottom-input-wrapper">
            <div v-if="replyingTo" class="reply-indicator">
              <span class="reply-to-label">回复 @{{ replyingTo.replyToUserName }}</span>
              <CloseOutlined class="reply-cancel-btn" @click="cancelReply" />
            </div>
            <input
              v-model="commentText"
              class="bottom-input"
              :placeholder="replyingTo ? `回复 @${replyingTo.replyToUserName}...` : '写下你的评论...'"
              @keyup.enter="submitComment"
            />
          </div>
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

    <!-- 举报弹窗 -->
    <ReportDialog ref="reportDialogRef" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HeartOutlined, StarOutlined, LeftOutlined, RightOutlined, MoreOutlined, CloseOutlined, DownOutlined, UpOutlined, EnvironmentOutlined } from '@ant-design/icons-vue'
import { getStrategyDetail, likeStrategy, collectStrategy } from '@/api/strategyController'
import { addComment, listComments, likeComment, listReplies } from '@/api/commentController'
import { useLoginUserStore } from '@/stores/loginUser'
import ReportDialog from '@/components/ReportDialog.vue'
import RouteMapViewer from '@/components/RouteMapViewer.vue'
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

const reportDialogRef = ref<InstanceType<typeof ReportDialog> | null>(null)

// 回复状态
const replyingTo = ref<{ parentId: string | number; replyToUserId: string | number; replyToUserName: string } | null>(null)
const replyMap = reactive<Record<string, API.CommentVO[]>>({})
const replyPageMap = reactive<Record<string, number>>({})
const replyTotalMap = reactive<Record<string, number>>({})
const loadingReplyMap = reactive<Record<string, boolean>>({})
const expandedSet = reactive<Set<string>>(new Set())

const defaultAvatar = 'https://api.dicebear.com/7.x/initials/svg?seed=User'

const allImages = ref<string[]>([])
const currentImageIndex = ref(0)
const showRouteMap = ref(false)

function routeStrategyId(): string | null {
  const raw = route.params.id
  const v = Array.isArray(raw) ? raw[0] : raw
  if (v === undefined || v === null || String(v).trim() === '') return null
  return String(v)
}

const routeDataStr = computed(() => {
  const d = detail.value as any
  return d?.routeData || ''
})

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
  scrollToComment()
})

// 监听路由参数变化（组件复用时重新加载数据），例如通知列表点击跳转到不同攻略
watch(
  () => route.params.id,
  (newId, oldId) => {
    const newStr = Array.isArray(newId) ? newId[0] : newId
    const oldStr = Array.isArray(oldId) ? oldId[0] : oldId
    if (!newStr || !newStr.trim() || newStr === oldStr) return
    currentImageIndex.value = 0
    comments.value = []
    commentPage.value = 1
    commentTotal.value = 0
    fetchDetail(newStr)
    fetchComments(newStr).then(() => scrollToComment())
  },
)

// 仅 commentId 变化时重新滚动定位
watch(
  () => route.query.commentId,
  (newCommentId, oldCommentId) => {
    if (newCommentId && newCommentId !== oldCommentId) {
      scrollToComment()
    }
  },
)

function scrollToComment() {
  const commentId = route.query.commentId
  if (!commentId) return
  setTimeout(() => {
    const el = document.getElementById(`comment-${commentId}`)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      el.classList.add('highlight-flash')
      setTimeout(() => { el.classList.remove('highlight-flash') }, 2000)
    }
  }, 500)
}

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
  } finally {
    // 清除回复状态
    for (const key of Object.keys(replyMap)) {
      delete replyMap[key]
      delete replyPageMap[key]
      delete replyTotalMap[key]
      delete loadingReplyMap[key]
    }
    expandedSet.clear()
  }
}

function switchCommentTab(tab: string) {
  if (tab === commentSort.value) return
  commentSort.value = tab
  commentPage.value = 1
  const idStr = routeStrategyId()
  if (idStr) fetchComments(idStr)
}

// ===== 回复功能 =====
async function fetchReplies(parentId: string | number, pageNum: number = 1) {
  const key = String(parentId)
  loadingReplyMap[key] = true
  try {
    const res = await listReplies({
      parentId,
      pageNum,
      pageSize: 5,
    })
    if (res.data?.code === 0 && res.data?.data) {
      const records = res.data.data.records || []
      if (pageNum === 1) {
        replyMap[key] = records
      } else {
        replyMap[key] = [...(replyMap[key] || []), ...records]
      }
      replyPageMap[key] = res.data.data.pageNumber || 1
      replyTotalMap[key] = res.data.data.totalRow || 0
    }
  } catch {
    // ignore
  } finally {
    loadingReplyMap[key] = false
  }
}

function toggleReplies(commentId: string | number | undefined) {
  if (commentId == null) return
  const key = String(commentId)
  if (expandedSet.has(key)) {
    expandedSet.delete(key)
  } else {
    expandedSet.add(key)
    if (!replyMap[key] || replyMap[key].length === 0) {
      fetchReplies(commentId)
    }
  }
}

function loadMoreReplies(commentId: string | number | undefined) {
  if (commentId == null) return
  const key = String(commentId)
  const nextPage = (replyPageMap[key] || 1) + 1
  fetchReplies(commentId, nextPage)
}

function startReply(c: API.CommentVO) {
  if (!c.id || !c.userId) return
  replyingTo.value = {
    parentId: c.parentId || c.id,
    replyToUserId: c.userId,
    replyToUserName: c.userName || '匿名用户',
  }
}

function cancelReply() {
  replyingTo.value = null
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
    const body: API.CommentAddRequest = {
      strategyId: idStr as any,
      content: commentText.value.trim(),
    }
    if (replyingTo.value) {
      body.parentId = replyingTo.value.parentId
      body.replyToUserId = replyingTo.value.replyToUserId
    }
    const res = await addComment(body)
    if (res.data?.code === 0) {
      message.success('评论成功')
      commentText.value = ''
      replyingTo.value = null
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

function handleReportStrategy() {
  if (!detail.value?.id || !detail.value?.userId) return
  reportDialogRef.value?.open(
    'strategy',
    detail.value.id,
    detail.value.userId,
  )
}

function handleReportComment(c: API.CommentVO) {
  if (!c.id || !c.userId) return
  reportDialogRef.value?.open('comment', c.id, c.userId)
}

function parseRouteData(json: string) {
  try {
    return JSON.parse(json)
  } catch {
    return null
  }
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

function goToUserProfile(userId: string | number | undefined) {
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
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.back-arrow {
  font-size: 22px;
  color: #333;
  cursor: pointer;
  display: inline-block;
  line-height: 1;
}

.more-icon {
  font-size: 20px;
  color: #999;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}
.more-icon:hover {
  background: #f0f0f0;
  color: #333;
}

.comment-more-icon {
  font-size: 16px;
  color: #ccc;
  cursor: pointer;
  padding: 2px;
  border-radius: 4px;
  flex-shrink: 0;
}
.comment-more-icon:hover {
  background: #f0f0f0;
  color: #666;
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

/* Route Map Toggle */
.route-map-section {
  margin-bottom: 24px;
}
.route-map-toggle-btn {
  width: 100%;
  height: 44px;
  font-size: 14px;
  border-radius: 8px;
  border: 1px dashed #1890ff;
  color: #1890ff;
  background: #f0f9ff;
  transition: all 0.2s;
}
.route-map-toggle-btn:hover {
  background: #e6f7ff;
  border-color: #40a9ff;
  color: #40a9ff;
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
  cursor: pointer;
}

.comment-item-name {
  font-size: 13px;
  color: #333;
  font-weight: 500;
  flex-shrink: 0;
  cursor: pointer;
}

.comment-item-name:hover {
  color: #ff6b6b;
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

/* ===== Reply Actions ===== */
.comment-actions {
  display: flex;
  gap: 12px;
  padding-left: 36px;
  margin-top: 4px;
}

.comment-action-btn {
  font-size: 12px;
  color: #999;
  cursor: pointer;
  user-select: none;
}

.comment-action-btn:hover {
  color: #1890ff;
}

/* ===== Reply Toggle ===== */
.reply-toggle-area {
  padding-left: 36px;
  margin-top: 6px;
}

.reply-expand-btn {
  font-size: 13px;
  color: #1890ff;
  cursor: pointer;
  user-select: none;
}

.reply-expand-btn:hover {
  color: #40a9ff;
}

/* ===== Replies Section ===== */
.replies-section {
  margin-top: 8px;
  padding-left: 36px;
  border-left: 2px solid #f0f0f0;
  margin-left: 14px;
}

.reply-item {
  padding: 8px 0;
  border-bottom: 1px solid #f8f8f8;
}

.reply-item:last-child {
  border-bottom: none;
}

.reply-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reply-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  background: #f0f0f0;
  flex-shrink: 0;
  cursor: pointer;
}

.reply-item-content {
  font-size: 14px;
  color: #444;
  line-height: 1.6;
  margin-top: 4px;
  padding-left: 32px;
  word-break: break-word;
}

.reply-at-username {
  color: #1890ff;
  font-size: 13px;
}

.load-more-replies {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 13px;
  color: #1890ff;
}

.load-more-replies span {
  cursor: pointer;
  user-select: none;
}

.load-more-replies span:hover {
  color: #40a9ff;
}

.reply-collapse-btn {
  color: #999;
}

.reply-collapse-btn:hover {
  color: #333 !important;
}

/* ===== Bottom Bar Input Wrapper ===== */
.bottom-input-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.reply-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 4px;
}

.reply-to-label {
  font-size: 12px;
  color: #1890ff;
}

.reply-cancel-btn {
  font-size: 12px;
  color: #999;
  cursor: pointer;
}

.reply-cancel-btn:hover {
  color: #333;
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

<!-- 评论高亮闪烁动画（非 scoped，class 通过 JS 动态添加） -->
<style>
.comment-item-header.highlight-flash {
  animation: highlightFade 2s ease-out;
  border-radius: 6px;
  padding: 4px 8px;
  margin: -4px -8px;
}

@keyframes highlightFade {
  0% {
    background-color: #fff566;
    box-shadow: 0 0 8px rgba(255, 245, 102, 0.6);
  }
  100% {
    background-color: transparent;
    box-shadow: none;
  }
}
</style>
