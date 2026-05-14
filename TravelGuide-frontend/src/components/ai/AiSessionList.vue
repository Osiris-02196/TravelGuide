<template>
  <div class="ai-session-list">
    <div class="session-list-header">
      <span>对话历史</span>
      <a-button type="text" size="small" class="btn-new-session" @click="$emit('create')">
        <template #icon><PlusOutlined /></template>
      </a-button>
    </div>
    <a-list
      :data-source="sessionList"
      :loading="sessionLoading"
      size="small"
      class="session-list-content"
    >
      <template #renderItem="{ item }">
        <a-list-item
          class="session-item"
          :class="{ active: item.sessionId === activeSessionId }"
          @click="$emit('select', item.sessionId)"
        >
          <a-list-item-meta>
            <template #title>
              <span class="session-title">{{ item.title }}</span>
            </template>
            <template #description>
              <span class="session-time">{{ formatTime(item.updateTime) }}</span>
            </template>
          </a-list-item-meta>
          <template #actions>
            <a-popconfirm
              title="确定删除此会话？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="$emit('delete', item.sessionId)"
            >
              <DeleteOutlined class="session-delete-btn" />
            </a-popconfirm>
          </template>
        </a-list-item>
      </template>
      <template #empty>
        <a-empty description="暂无会话记录" style="margin-top: 60px" />
      </template>
    </a-list>
  </div>
</template>

<script setup lang="ts">
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import type { AiChatSession } from '@/api/ai'

defineProps<{
  sessionList: AiChatSession[]
  activeSessionId: string
  sessionLoading?: boolean
}>()

defineEmits<{
  select: [sessionId: string]
  delete: [sessionId: string]
  create: []
}>()

function formatTime(time: string): string {
  if (!time) return ''
  const d = dayjs(time)
  const now = dayjs()
  if (d.isSame(now, 'day')) return d.format('HH:mm')
  if (d.isSame(now, 'year')) return d.format('MM-DD HH:mm')
  return d.format('YYYY-MM-DD')
}
</script>

<style scoped>
.ai-session-list {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f7f7f8;
}

.session-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #e5e5e5;
}

.btn-new-session {
  font-size: 15px;
  color: #1677ff;
}

.session-list-content {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  cursor: pointer;
  padding: 10px 16px !important;
  transition: background 0.15s;
  border-bottom: 1px solid #eee;
}

.session-item:hover {
  background: #ececf1;
}

.session-item.active {
  background: #e0e0e6;
}

.session-title {
  font-size: 13px;
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.session-time {
  font-size: 11px;
  color: #999;
}

.session-delete-btn {
  color: #999;
  font-size: 14px;
  transition: color 0.15s;
}

.session-delete-btn:hover {
  color: #ff4d4f;
}
</style>
