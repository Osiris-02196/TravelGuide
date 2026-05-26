<template>
  <div class="mobile-upload-page">
    <div class="upload-container">
      <h2 class="page-title">手机上传</h2>
      <p class="page-desc">选择图片上传到电脑</p>

      <!-- 错误状态 -->
      <div v-if="errorMsg" class="error-state">
        <a-result status="error" :title="errorMsg" />
      </div>

      <!-- 正常上传 -->
      <template v-else>
        <!-- 图片预览区 -->
        <div v-if="selectedFiles.length > 0" class="preview-grid">
          <div
            v-for="(file, index) in selectedFiles"
            :key="index"
            class="preview-item"
            :class="{ uploading: file.status === 'uploading', done: file.status === 'done', fail: file.status === 'fail' }"
          >
            <img :src="file.previewUrl" alt="预览" />
            <div class="preview-overlay">
              <span v-if="file.status === 'uploading'"><a-spin size="small" /></span>
              <span v-else-if="file.status === 'done'" class="done-icon">✓</span>
              <span v-else-if="file.status === 'fail'" class="fail-icon">✕</span>
            </div>
            <div v-if="file.status === 'fail'" class="retry-hint">上传失败</div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <div class="empty-icon">📷</div>
          <p>请选择要上传的图片</p>
        </div>

        <!-- 进度信息 -->
        <div v-if="totalCount > 0" class="progress-info">
          已上传 {{ doneCount }} / {{ totalCount }} 张
          <a-progress
            :percent="Math.round((doneCount / totalCount) * 100)"
            :show-info="false"
            stroke-color="#52c41a"
            size="small"
          />
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <input
            ref="fileInputRef"
            type="file"
            accept="image/*"
            multiple
            style="display: none"
            @change="handleFileChange"
          />
          <a-button
            block
            size="large"
            :disabled="isUploading"
            @click="triggerFileSelect"
          >
            选择图片
          </a-button>
          <a-button
            v-if="selectedFiles.length > 0"
            type="primary"
            block
            size="large"
            :loading="isUploading"
            :disabled="allDone"
            @click="startUpload"
          >
            {{ isUploading ? '上传中…' : allDone ? '上传完成' : '开始上传' }}
          </a-button>
          <a-button
            v-if="allDone"
            type="primary"
            block
            size="large"
            @click="handleComplete"
          >
            完成上传
          </a-button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { uploadToSession, completeSession } from '@/api/uploadSessionController'

const route = useRoute()

// Token from URL query
const token = ref('')
const errorMsg = ref('')

interface SelectedFile {
  file: File
  previewUrl: string
  status: 'pending' | 'uploading' | 'done' | 'fail'
}

const fileInputRef = ref<HTMLInputElement | null>(null)
const selectedFiles = ref<SelectedFile[]>([])
const isUploading = ref(false)

const totalCount = computed(() => selectedFiles.value.length)
const doneCount = computed(() => selectedFiles.value.filter((f) => f.status === 'done').length)
const allDone = computed(() => totalCount.value > 0 && doneCount.value === totalCount.value)

onMounted(() => {
  token.value = (route.query.token as string) || ''
  if (!token.value) {
    errorMsg.value = '缺少上传凭证，请重新扫描二维码'
  }
})

function triggerFileSelect() {
  fileInputRef.value?.click()
}

function handleFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const files = input.files
  if (!files || files.length === 0) return

  const newFiles: SelectedFile[] = []
  for (let i = 0; i < files.length; i++) {
    const file = files[i]
    if (!file.type.startsWith('image/')) continue
    newFiles.push({
      file,
      previewUrl: URL.createObjectURL(file),
      status: 'pending',
    })
  }
  selectedFiles.value = [...selectedFiles.value, ...newFiles]
  // Reset input so same file can be re-selected
  input.value = ''
}

/**
 * 在手机上使用 Canvas 压缩图片：
 * 限制最长边 1920px，导出 JPEG quality 0.7
 */
function compressImage(file: File): Promise<Blob> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    const url = URL.createObjectURL(file)
    img.onload = () => {
      URL.revokeObjectURL(url)
      let { width, height } = img
      const MAX = 1920
      if (width > MAX || height > MAX) {
        if (width > height) {
          height = Math.round((height / width) * MAX)
          width = MAX
        } else {
          width = Math.round((width / height) * MAX)
          height = MAX
        }
      }
      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        reject(new Error('无法创建 Canvas'))
        return
      }
      ctx.drawImage(img, 0, 0, width, height)
      canvas.toBlob(
        (blob) => {
          if (blob) resolve(blob)
          else reject(new Error('图片压缩失败'))
        },
        'image/jpeg',
        0.7,
      )
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('图片加载失败'))
    }
    img.src = url
  })
}

async function startUpload() {
  if (!token.value || isUploading.value) return
  isUploading.value = true

  for (const item of selectedFiles.value) {
    if (item.status === 'done') continue
    item.status = 'uploading'
    try {
      // 1. 压缩
      const compressedBlob = await compressImage(item.file)
      // 2. 转 FormData 上传
      const formData = new FormData()
      formData.append('file', compressedBlob, item.file.name.replace(/\.[^.]+$/, '.jpg'))
      const res = await uploadToSession(token.value, formData as any)
      if (res.data?.code === 0 && res.data?.data) {
        item.status = 'done'
      } else {
        item.status = 'fail'
      }
    } catch {
      item.status = 'fail'
    }
  }

  isUploading.value = false
}

async function handleComplete() {
  if (!token.value) return
  try {
    const res = await completeSession(token.value)
    if (res.data?.code === 0) {
      message.success('上传完成，请返回电脑查看')
    }
  } catch {
    message.error('通知完成失败，但图片已上传')
  }
}
</script>

<style scoped>
.mobile-upload-page {
  min-height: 100dvh;
  background: #f5f5f5;
  display: flex;
  justify-content: center;
  padding: 16px;
  box-sizing: border-box;
}

.upload-container {
  width: 100%;
  max-width: 480px;
}

.page-title {
  text-align: center;
  font-size: 20px;
  margin: 8px 0 4px;
  color: #333;
}

.page-desc {
  text-align: center;
  font-size: 14px;
  color: #999;
  margin-bottom: 20px;
}

.error-state {
  margin-top: 60px;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 16px;
}

.preview-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid #e8e8e8;
}

.preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-overlay {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  font-weight: bold;
}

.done-icon {
  color: #52c41a;
}

.fail-icon {
  color: #ff4d4f;
}

.retry-hint {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(255, 77, 79, 0.8);
  color: #fff;
  font-size: 12px;
  text-align: center;
  padding: 2px;
}

.empty-state {
  text-align: center;
  padding: 48px 0;
  color: #bbb;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 12px;
}

.progress-info {
  text-align: center;
  font-size: 13px;
  color: #666;
  margin-bottom: 16px;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
