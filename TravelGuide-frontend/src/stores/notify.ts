import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUnreadCount, listMyNotifies, readAllNotifies } from '@/api/notifyController'

export const useNotifyStore = defineStore('notify', () => {
  const notifyList = ref<API.NotifyVO[]>([])
  const total = ref(0)
  const loading = ref(false)
  const unreadCount = ref(0)
  let ws: WebSocket | null = null

  function wsHost() {
    if (import.meta.env.DEV) {
      return 'localhost:8082'
    }
    return window.location.host
  }

  function disconnectWebSocket() {
    if (ws) {
      ws.close()
      ws = null
    }
  }

  function connectWebSocket(userId: number) {
    disconnectWebSocket()
    if (!Number.isFinite(userId)) return
    const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const url = `${scheme}://${wsHost()}/ws/notify/${userId}`
    try {
      ws = new WebSocket(url)
    } catch {
      ws = null
      return
    }
    ws.onmessage = (ev) => {
      try {
        const data = JSON.parse(ev.data as string)
        if (data?.type === 'unreadCount') {
          unreadCount.value = Number(data.unreadCount) || 0
          return
        }
        unreadCount.value = (unreadCount.value || 0) + 1
        const item: API.NotifyVO = {
          id: data.id,
          type: data.type,
          content: data.content,
          isRead: data.isRead ?? 0,
          createTime: data.createTime,
        }
        notifyList.value = [item, ...notifyList.value]
      } catch {
        void fetchUnreadCount()
      }
    }
  }

  async function fetchNotifies(pageNum: number, pageSize: number) {
    loading.value = true
    try {
      const res = await listMyNotifies({ pageNum, pageSize })
      if (res.data?.code === 0 && res.data?.data) {
        const page = res.data.data
        const records = page.records ?? []
        const rowTotal = page.totalRow ?? 0
        total.value = typeof rowTotal === 'number' ? rowTotal : Number(rowTotal)
        if (pageNum <= 1) {
          notifyList.value = records
        } else {
          notifyList.value = [...notifyList.value, ...records]
        }
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchUnreadCount() {
    try {
      const res = await getUnreadCount()
      if (res.data?.code === 0 && res.data?.data != null) {
        unreadCount.value = Number(res.data.data)
      }
    } catch {
      // 未登录等场景忽略
    }
  }

  async function markAllRead() {
    const res = await readAllNotifies()
    if (res.data?.code === 0) {
      notifyList.value = notifyList.value.map((n) => ({ ...n, isRead: 1 }))
      unreadCount.value = 0
    }
  }

  function $reset() {
    notifyList.value = []
    total.value = 0
    unreadCount.value = 0
    loading.value = false
    disconnectWebSocket()
  }

  return {
    notifyList,
    total,
    loading,
    unreadCount,
    fetchNotifies,
    fetchUnreadCount,
    markAllRead,
    connectWebSocket,
    disconnectWebSocket,
    $reset,
  }
})
