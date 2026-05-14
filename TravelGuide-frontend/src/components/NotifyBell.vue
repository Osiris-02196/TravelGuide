<template>
  <a-popover
    v-model:open="visible"
    trigger="click"
    placement="bottomRight"
    :overlay-style="{ maxWidth: '420px', width: '380px' }"
    overlay-class-name="notify-popover"
  >
    <!-- 铃铛按钮 -->
    <a-badge :count="unreadCount" :overflow-count="99" :dot="false" style="cursor: pointer">
      <BellOutlined style="font-size: 20px" />
    </a-badge>

    <template #content>
      <div class="notify-panel">
        <!-- 头部 -->
        <div class="notify-header">
          <span style="font-weight: 600; font-size: 15px">消息通知</span>
          <a-button type="link" size="small" :disabled="unreadCount === 0" @click="handleReadAll">
            全部已读
          </a-button>
        </div>

        <!-- 列表 -->
        <a-spin :spinning="loading" style="min-height: 80px">
          <div v-if="notifyList.length === 0 && !loading" class="empty-tip">暂无消息</div>
          <div class="notify-list" v-else @scroll="handleScroll" ref="listRef">
            <div
              v-for="item in notifyList"
              :key="item.id"
              class="notify-item"
              :class="{ unread: item.isRead === 0 }"
              @click="handleClickItem(item)"
            >
              <div class="notify-item-content">
                {{ item.content }}
              </div>
              <div class="notify-item-footer">
                <span class="notify-time">{{ formatTime(item.createTime) }}</span>
                <a-badge v-if="item.isRead === 0" status="error" :dot="true" />
              </div>
            </div>
            <!-- 加载更多 -->
            <div v-if="hasMore" style="text-align: center; padding: 8px">
              <a-spin size="small" />
            </div>
          </div>
        </a-spin>
      </div>
    </template>
  </a-popover>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { BellOutlined } from '@ant-design/icons-vue'
import { useNotifyStore } from '@/stores/notify'
import { useLoginUserStore } from '@/stores/loginUser'
import dayjs from 'dayjs'

const notifyStore = useNotifyStore()
const loginUserStore = useLoginUserStore()

const visible = ref(false)
const listRef = ref<HTMLElement | null>(null)
const pageNum = ref(1)
const pageSize = 20

const notifyList = ref<API.NotifyVO[]>([])
const unreadCount = ref(0)
const loading = ref(false)
const hasMore = ref(false)

// 同步 store 中的状态
watch(
  () => notifyStore.notifyList,
  (val) => {
    notifyList.value = val
  },
  { immediate: true },
)
watch(
  () => notifyStore.unreadCount,
  (val) => {
    unreadCount.value = val
  },
  { immediate: true },
)
watch(
  () => notifyStore.loading,
  (val) => {
    loading.value = val
  },
  { immediate: true },
)

// 面板打开时拉取数据
watch(visible, (open) => {
  if (open) {
    pageNum.value = 1
    notifyStore.fetchNotifies(pageNum.value, pageSize).then(() => {
      hasMore.value = notifyStore.notifyList.length < notifyStore.total
    })
    notifyStore.fetchUnreadCount()
  }
})

function formatTime(time?: string) {
  if (!time) return ''
  const d = dayjs(time)
  const now = dayjs()
  if (d.isSame(now, 'day')) {
    return d.format('HH:mm')
  } else if (d.isSame(now, 'year')) {
    return d.format('MM-DD HH:mm')
  }
  return d.format('YYYY-MM-DD')
}

function handleReadAll() {
  notifyStore.markAllRead()
}

function handleClickItem(_item: API.NotifyVO) {
  // 可以跳转到对应详情页，此处暂不处理
}

function handleScroll(e: Event) {
  const target = e.target as HTMLElement
  if (target.scrollHeight - target.scrollTop - target.clientHeight < 20) {
    if (hasMore.value && !loading.value) {
      pageNum.value++
      notifyStore.fetchNotifies(pageNum.value, pageSize).then(() => {
        hasMore.value = notifyStore.notifyList.length < notifyStore.total
      })
    }
  }
}

// 登录后连接 WebSocket，退出后断开
watch(
  () => loginUserStore.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      notifyStore.connectWebSocket(loginUserStore.loginUser.id)
      notifyStore.fetchUnreadCount()
    } else {
      notifyStore.disconnectWebSocket()
      unreadCount.value = 0
      notifyList.value = []
      notifyStore.$reset()
    }
  },
  { immediate: true },
)

onUnmounted(() => {
  notifyStore.disconnectWebSocket()
})
</script>

<style scoped>
.notify-panel {
  display: flex;
  flex-direction: column;
}

.notify-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 8px;
}

.notify-list {
  max-height: 360px;
  overflow-y: auto;
}

.notify-item {
  padding: 10px 2px;
  border-bottom: 1px solid #fafafa;
  cursor: pointer;
  transition: background 0.2s;
}

.notify-item:hover {
  background: #fafafa;
}

.notify-item.unread {
  background: #f6ffed;
}

.notify-item-content {
  font-size: 13px;
  line-height: 1.5;
  word-break: break-all;
}

.notify-item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
}

.notify-time {
  font-size: 12px;
  color: #999;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 24px 0;
  font-size: 13px;
}
</style>
