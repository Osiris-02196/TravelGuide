<template>
  <div class="ai-input-box">
    <a-textarea
      ref="textareaRef"
      v-model:value="inputText"
      :auto-size="{ minRows: 1, maxRows: 4 }"
      placeholder="输入消息..."
      :disabled="disabled"
      @press-enter="handleSend"
    />
    <a-button
      type="primary"
      shape="circle"
      :disabled="disabled || !inputText.trim()"
      @click="handleSend"
    >
      <template #icon>
        <SendOutlined />
      </template>
    </a-button>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { SendOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  disabled: boolean
}>()

const emit = defineEmits<{
  send: [content: string]
}>()

const inputText = ref('')
const textareaRef = ref<any>(null)

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || props.disabled) return
  emit('send', text)
  // 等待 Vue 完成所有同步更新（loading、消息列表等）后再清理输入框
  await nextTick()
  inputText.value = ''
  // 确保底层 textarea DOM 也被清理
  if (textareaRef.value) {
    const textarea = (textareaRef.value as any).resizableTextArea?.textArea
    if (textarea) textarea.value = ''
  }
}
</script>

<style scoped>
.ai-input-box {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #e5e5e5;
}

.ai-input-box :deep(.ant-btn) {
  flex-shrink: 0;
  margin-bottom: 2px;
}
</style>
