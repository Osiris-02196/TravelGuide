<template>
  <a-drawer
    :open="visible"
    placement="left"
    :width="780"
    :closable="false"
    :body-style="{ padding: 0 }"
    class="ai-drawer"
    @close="handleClose"
  >
    <div class="ai-panel">
      <!-- 左侧：会话列表 -->
      <div class="ai-panel-left">
        <AiSessionList
          :session-list="store.sessionList"
          :active-session-id="store.currentSessionId"
          @select="handleSelectSession"
          @delete="handleDeleteSession"
          @create="handleCreateSession"
        />
      </div>

      <!-- 右侧：聊天 -->
      <div class="ai-panel-right">
        <!-- 顶部关闭按钮 -->
        <div class="chat-header">
          <span class="chat-header-title">AI 旅行助手</span>
          <a-button type="text" @click="handleClose">
            <CloseOutlined />
          </a-button>
        </div>
        <AiChatWindow
          :messages="store.chatMessages"
          :loading="store.loading"
          @send="handleSend"
        />
      </div>
    </div>
  </a-drawer>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { CloseOutlined } from '@ant-design/icons-vue'
import { useAiChatStore } from '@/stores/aiChat'
import { useLoginUserStore } from '@/stores/loginUser'
import AiSessionList from './AiSessionList.vue'
import AiChatWindow from './AiChatWindow.vue'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const store = useAiChatStore()
const loginUserStore = useLoginUserStore()

const visible = computed(() => props.open)

// 打开面板时加载会话列表
watch(visible, (open) => {
  if (open) {
    const userId = loginUserStore.loginUser?.id
    if (userId) {
      store.fetchSessionList(userId)
    }
  }
})

function handleClose() {
  emit('close')
}

function handleSelectSession(sessionId: string) {
  store.switchSession(sessionId)
}

function handleDeleteSession(sessionId: string) {
  const userId = loginUserStore.loginUser?.id
  if (userId) {
    store.removeSession(sessionId, userId)
  }
}

function handleCreateSession() {
  const userId = loginUserStore.loginUser?.id
  if (userId) {
    store.createSession(userId)
  }
}

function handleSend(content: string) {
  const userId = loginUserStore.loginUser?.id
  if (userId) {
    store.sendMessage(content, String(userId))
  }
}
</script>

<style scoped>
.ai-drawer :deep(.ant-drawer-body) {
  padding: 0;
  height: 100%;
  overflow: hidden;
}

.ai-panel {
  display: flex;
  height: 100%;
}

.ai-panel-left {
  width: 260px;
  flex-shrink: 0;
  border-right: 1px solid #e5e5e5;
}

.ai-panel-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid #e5e5e5;
  background: #fff;
}

.chat-header-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}
</style>
