<template>
  <div class="ai-chat-window">
    <!-- 消息列表区域 -->
    <div ref="messageContainer" class="chat-messages">
      <template v-if="messages.length === 0">
        <div class="chat-empty">
          <div class="empty-icon">
            <RobotOutlined />
          </div>
          <div class="empty-text">开始和 AI 对话吧</div>
        </div>
      </template>
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="message-row"
        :class="msg.role === 'user' ? 'message-user' : 'message-ai'"
      >
        <a-avatar
          v-if="msg.role === 'ai'"
          :src="aiAvatar"
          :size="32"
          class="msg-avatar"
        />
        <div class="msg-bubble" :class="msg.role === 'user' ? 'bubble-user' : 'bubble-ai'">
          <div class="msg-content">{{ msg.content }}</div>
        </div>
        <a-avatar
          v-if="msg.role === 'user'"
          :src="userAvatar"
          :size="32"
          class="msg-avatar"
        />
      </div>
      <!-- AI 思考中 -->
      <div v-if="loading" class="message-row message-ai">
        <a-avatar :src="aiAvatar" :size="32" class="msg-avatar" />
        <div class="msg-bubble bubble-ai thinking-bubble">
          <a-spin size="small" />
          <span style="margin-left: 8px; color: #888">思考中...</span>
        </div>
      </div>
    </div>

    <!-- 底部输入框 -->
    <AiInputBox :disabled="loading" @send="$emit('send', $event)" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, computed } from 'vue'
import { RobotOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import AiInputBox from './AiInputBox.vue'
import type { ChatMessage } from '@/stores/aiChat'

const props = defineProps<{
  messages: ChatMessage[]
  loading: boolean
}>()

defineEmits<{
  send: [content: string]
}>()

// 头像
const aiAvatar = 'https://api.dicebear.com/7.x/bottts/svg?seed=AI'
const loginUserStore = useLoginUserStore()
const userAvatar = computed(() => {
  return loginUserStore.loginUser?.userAvatar || 'https://api.dicebear.com/7.x/initials/svg?seed=User'
})

const messageContainer = ref<HTMLElement | null>(null)

// 当消息变化时自动滚动到底部
watch(
  () => props.messages.length,
  async () => {
    await nextTick()
    scrollToBottom()
  }
)

function scrollToBottom() {
  if (messageContainer.value) {
    messageContainer.value.scrollTop = messageContainer.value.scrollHeight
  }
}
</script>


<style scoped>
.ai-chat-window {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 16px;
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #ccc;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-text {
  font-size: 14px;
  color: #aaa;
}

.message-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
  gap: 8px;
}

.message-user {
  justify-content: flex-end;
}

.message-ai {
  justify-content: flex-start;
}

.msg-avatar {
  flex-shrink: 0;
}

.msg-bubble {
  max-width: 72%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}

.bubble-user {
  background: #1677ff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.bubble-ai {
  background: #f0f0f0;
  color: #333;
  border-bottom-left-radius: 4px;
}

.thinking-bubble {
  display: flex;
  align-items: center;
  padding: 10px 16px;
}

.msg-content {
  font-size: 14px;
}
</style>
