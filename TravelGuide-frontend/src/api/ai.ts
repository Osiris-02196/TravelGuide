import request from '@/request'

/** AI 会话实体 */
export interface AiChatSession {
  id: number
  sessionId: string
  title: string
  updateTime: string
  createTime?: string
  userId?: number
}

/** AI 聊天消息 */
export interface AiChatMessage {
  id: number
  sessionId: string
  role: 'user' | 'ai'
  content: string
  createTime: string
}

/** 获取用户的所有会话列表 GET /ai/session/list */
export async function getSessionList(userId: number | string) {
  return request<API.BaseResponseListAiChatSession>('/ai/session/list', {
    method: 'GET',
    params: { userId },
  })
}

/** 创建新会话 POST /ai/session/create */
export async function createSession(userId: number | string) {
  return request<API.BaseResponseAiChatSession>('/ai/session/create', {
    method: 'POST',
    params: { userId },
  })
}

/** 删除会话 POST /ai/session/delete */
export async function deleteSession(id: number, userId: number | string) {
  return request<API.BaseResponseBoolean>('/ai/session/delete', {
    method: 'POST',
    params: { id, userId },
  })
}

/** AI 对话 POST /ai/chat（返回纯文本字符串） */
export async function chatWithAi(userId: string, message: string, sessionId?: string) {
  const params: Record<string, string> = { userId, message }
  if (sessionId) {
    params.sessionId = sessionId
  }
  return request<string>('/ai/chat', {
    method: 'POST',
    params,
  })
}

/** 获取会话的消息列表 GET /ai/session/messages */
export async function getSessionMessages(sessionId: string) {
  return request<API.BaseResponseListAiChatMessage>('/ai/session/messages', {
    method: 'GET',
    params: { sessionId },
  })
}
