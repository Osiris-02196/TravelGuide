import { defineStore } from 'pinia'
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  getSessionList,
  deleteSession,
  createSession as apiCreateSession,
  chatWithAi,
  getSessionMessages,
  type AiChatSession,
} from '@/api/ai'

/** 单条聊天消息 */
export interface ChatMessage {
  id: string
  role: 'user' | 'ai'
  content: string
  timestamp: number
}

export const useAiChatStore = defineStore('aiChat', () => {
  // ========== 会话列表相关 ==========
  const sessionList = ref<AiChatSession[]>([])
  const currentSessionId = ref<string>('') // 当前 UI 选中的 sessionId

  // ========== 聊天消息 ==========
  const chatMessages = ref<ChatMessage[]>([])
  const loading = ref(false)

  /** 生成唯一 ID */
  function generateId(): string {
    return Date.now().toString(36) + Math.random().toString(36).slice(2)
  }

  /** 获取会话列表 */
  async function fetchSessionList(userId: number | string) {
    try {
      const res = await getSessionList(userId)
      if (res.data?.code === 0 && Array.isArray(res.data.data)) {
        sessionList.value = res.data.data
        // 如果没有选中任何会话，默认选中第一个并加载消息
        if (!currentSessionId.value && sessionList.value.length > 0) {
          currentSessionId.value = sessionList.value[0].sessionId
          await fetchMessages(currentSessionId.value)
        }
      }
    } catch {
      message.error('获取会话列表失败')
    }
  }

  /** 加载指定会话的消息列表 */
  async function fetchMessages(sessionId: string) {
    try {
      const res = await getSessionMessages(sessionId)
      if (res.data?.code === 0 && Array.isArray(res.data.data)) {
        chatMessages.value = res.data.data.map((msg) => ({
          id: String(msg.id),
          role: msg.role as 'user' | 'ai',
          content: msg.content,
          timestamp: new Date(msg.createTime).getTime(),
        }))
      }
    } catch {
      console.error('加载会话消息失败')
    }
  }

  /** 创建新会话 */
  async function createSession(userId: number | string) {
    try {
      const res = await apiCreateSession(userId)
      if (res.data?.code === 0 && res.data?.data) {
        sessionList.value.unshift(res.data.data)
        currentSessionId.value = res.data.data.sessionId
        chatMessages.value = []
      }
    } catch {
      message.error('创建会话失败')
    }
  }

  /** 删除会话 */
  async function removeSession(sessionId: string, userId: number | string) {
    // 根据 sessionId 找到对应的 id
    const session = sessionList.value.find((s) => s.sessionId === sessionId)
    if (!session) return

    try {
      const res = await deleteSession(session.id, userId)
      if (res.data?.data === true) {
        // 从列表中移除
        sessionList.value = sessionList.value.filter((s) => s.sessionId !== sessionId)

        // 如果删除的是当前选中的会话
        if (currentSessionId.value === sessionId) {
          currentSessionId.value = sessionList.value[0]?.sessionId || ''
          if (currentSessionId.value) {
            await fetchMessages(currentSessionId.value)
          } else {
            chatMessages.value = []
          }
        }
        message.success('删除成功')
      } else {
        message.error('删除失败')
      }
    } catch {
      message.error('删除会话出错')
    }
  }

  /** 发送消息 */
  async function sendMessage(content: string, userId: string) {
    if (!content.trim() || loading.value) return

    // 1. 添加用户消息到本地
    const userMsg: ChatMessage = {
      id: generateId(),
      role: 'user',
      content: content.trim(),
      timestamp: Date.now(),
    }
    chatMessages.value.push(userMsg)

    // 2. 开始请求 AI
    loading.value = true
    try {
      // 使用当前会话 ID，如果没有则回退到 "user:"+userId（兼容旧数据）
      const chatSessionId = currentSessionId.value || 'user:' + userId
      await chatWithAi(userId, content.trim(), chatSessionId)

      // 3. 请求成功后重新加载消息列表（从数据库获取完整消息）
      await fetchMessages(chatSessionId)

      // 4. 更新会话列表（可能新增了会话）
      await fetchSessionList(userId)
    } catch {
      message.error('AI 请求失败，请稍后重试')
    } finally {
      loading.value = false
    }
  }

  /** 切换会话（加载该会话的消息） */
  async function switchSession(sessionId: string) {
    currentSessionId.value = sessionId
    await fetchMessages(sessionId)
  }

  /** 清空当前聊天记录 */
  function clearMessages() {
    chatMessages.value = []
  }

  return {
    sessionList,
    currentSessionId,
    chatMessages,
    loading,
    fetchSessionList,
    fetchMessages,
    createSession,
    removeSession,
    sendMessage,
    switchSession,
    clearMessages,
  }
})
