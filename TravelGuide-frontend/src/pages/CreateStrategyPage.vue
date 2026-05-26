<template>
  <div class="page-container">
    <div class="create-form-wrapper">
      <h3 style="margin-bottom: 24px; font-size: 18px">新建攻略</h3>
      <a-form :model="formState" :rules="rules" ref="formRef" @finish="handleSubmit">
        <!-- Upload Images -->
        <a-form-item label="上传图片" name="imageUrls">
          <a-upload
            v-model:file-list="fileList"
            list-type="picture-card"
            :max-count="9"
            :before-upload="beforeUpload"
            :custom-request="customUpload"
            @remove="handleRemove"
          >
            <div v-if="fileList.length < 9">
              <PlusOutlined />
              <div style="margin-top: 8px">上传</div>
            </div>
          </a-upload>
          <div style="color: #888; font-size: 12px; margin-top: 4px">
            支持多张图片上传，最多9张
          </div>
          <div style="margin-top: 8px">
            <a-button @click="handlePhoneUpload" :disabled="uploadingFromPhone" icon="phone">
              手机扫码上传
            </a-button>
          </div>
        </a-form-item>

        <!-- 手机扫码上传弹窗 -->
        <a-modal
          v-model:visible="showQrModal"
          title="手机扫码上传"
          :footer="null"
          :mask-closable="false"
          :closable="!uploadingFromPhone"
          @cancel="handleCloseQrModal"
          width="360px"
        >
          <div style="text-align: center; padding: 16px 0">
            <div v-if="qrDataUrl" style="margin-bottom: 16px">
              <img :src="qrDataUrl" alt="扫码上传" style="width: 240px; height: 240px" />
            </div>
            <div v-else style="height: 240px; display: flex; align-items: center; justify-content: center">
              <a-spin />
            </div>
            <p style="color: #666; margin-bottom: 4px">请使用手机扫描二维码上传图片</p>
            <p v-if="uploadingFromPhone" style="color: #52c41a">
              <a-spin size="small" style="margin-right: 6px" />手机正在上传中…
            </p>
            <p v-else style="color: #999; font-size: 12px">二维码有效期30分钟，上传完成后页面将自动同步</p>
          </div>
        </a-modal>

        <!-- Title -->
        <a-form-item label="攻略标题" name="strategyTitle">
          <a-input
            v-model:value="formState.strategyTitle"
            placeholder="请输入攻略标题"
            :maxlength="100"
          />
        </a-form-item>

        <!-- Content -->
        <a-form-item label="攻略内容" name="strategyContent">
          <a-textarea
            v-model:value="formState.strategyContent"
            placeholder="请描述你的旅游攻略..."
            :rows="8"
            :maxlength="5000"
            show-count
          />
        </a-form-item>

        <!-- Tags -->
        <a-form-item label="攻略标签">
          <a-checkbox-group v-model:value="formState.strategyTags">
            <a-checkbox value="美食专栏">美食专栏</a-checkbox>
            <a-checkbox value="自然专栏">自然专栏</a-checkbox>
            <a-checkbox value="人文专栏">人文专栏</a-checkbox>
            <a-checkbox value="建筑专栏">建筑专栏</a-checkbox>
            <a-checkbox value="艺术专栏">艺术专栏</a-checkbox>
          </a-checkbox-group>
        </a-form-item>

        <!-- Location -->
        <a-form-item label="选择地点">
          <a-cascader
            v-model:value="formState.locations"
            :options="provinceOptions"
            placeholder="选择省市"
            change-on-select
            style="width: 100%"
          />
        </a-form-item>

        <!-- Route Planning -->
        <a-form-item label="路线规划">
          <RouteMapEditor @change="handleRouteChange" />
        </a-form-item>

        <!-- Buttons -->
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" :loading="submitting">上传</a-button>
            <a-button @click="handleCancel">取消</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { addStrategy, uploadImage } from '@/api/strategyController'
import { createUploadSession, deleteUploadSession, getSessionImages } from '@/api/uploadSessionController'
import { listProvinces, listCities } from '@/api/locationController'
import QRCode from 'qrcode'
import RouteMapEditor, { type RouteResultData } from '@/components/RouteMapEditor.vue'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const fileList = ref<any[]>([])
const uploadedUrls = ref<string[]>([])

// 手机扫码上传状态
const showQrModal = ref(false)
const uploadToken = ref('')
const qrDataUrl = ref('')
const uploadingFromPhone = ref(false)
/** 轮询定时器 */
let pollTimer: ReturnType<typeof setInterval> | null = null

const formState = reactive<{
  strategyTitle: string
  strategyContent: string
  strategyTags: string[]
  locations: string[]
  routeData: string
}>({
  strategyTitle: '',
  strategyContent: '',
  strategyTags: [],
  locations: [],
  routeData: '', // 存储路线规划数据的JSON字符串
})

const rules = {
  strategyTitle: [{ required: true, message: '请输入攻略标题' }],
  strategyContent: [{ required: true, message: '请输入攻略内容' }],
}

interface CascaderOption {
  value: string | number | undefined
  label: string | undefined
  isLeaf: boolean
  children?: CascaderOption[]
}

const provinceOptions = ref<CascaderOption[]>([])

onMounted(async () => {
  try {
    const res = await listProvinces()
    if (res.data?.code === 0 && res.data?.data) {
      // 并行加载所有城市的子级数据
      const provinceOptionsWithChildren = await Promise.all(
        res.data.data.map(async (p) => {
          const option: CascaderOption = {
            value: p.id,
            label: p.locationName,
            isLeaf: false,
          }
          try {
            const cityRes = await listCities({ provinceId: p.id as number })
            if (cityRes.data?.code === 0 && cityRes.data?.data) {
              option.children = cityRes.data.data.map((c) => ({
                value: c.id,
                label: c.locationName,
                isLeaf: true,
              }))
            }
          } catch {
            // 单个省份加载失败不影响其他省份
          }
          return option
        })
      )
      provinceOptions.value = provinceOptionsWithChildren
    }
  } catch {
    // ignore
  }
})

function beforeUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    message.error('只能上传图片文件')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    message.error('图片大小不能超过5MB')
    return false
  }
  return true
}

async function customUpload(options: any) {
  const { file, onSuccess, onError } = options
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await uploadImage(formData as any)
    if (res.data?.code === 0 && res.data?.data) {
      uploadedUrls.value.push(res.data.data)
      onSuccess(res.data, file)
    } else {
      onError(new Error(res.data?.message || '上传失败'))
    }
  } catch (err) {
    onError(err)
  }
}

function handleRemove(file: any) {
  const url = file.url || file.response?.data
  if (url) {
    const idx = uploadedUrls.value.indexOf(url)
    if (idx > -1) uploadedUrls.value.splice(idx, 1)
  }
}

async function handleSubmit() {
  submitting.value = true
  try {
    const res = await addStrategy({
      strategyTitle: formState.strategyTitle,
      strategyContent: formState.strategyContent,
      imageUrls: uploadedUrls.value,
      strategyTags: formState.strategyTags.length > 0 ? formState.strategyTags : undefined,
      locations: formState.locations.length > 0 ? formState.locations.map(String) : undefined,
      routeData: formState.routeData || undefined,
    })
    if (res.data?.code === 0) {
      message.success('攻略上传成功，等待审核')
      router.push('/my-strategies')
    } else {
      message.error(res.data?.message || '上传失败')
    }
  } catch {
    message.error('上传失败')
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  router.back()
}

/** 手机扫码上传：创建会话 → 生成 QR → 监听 WS */
async function handlePhoneUpload() {
  try {
    const res = await createUploadSession()
    if (res.data?.code !== 0 || !res.data?.data?.token) {
      message.error(res.data?.message || '创建上传会话失败')
      return
    }
    const token = res.data.data.token as string
    uploadToken.value = token
    showQrModal.value = true

    // 生成二维码：优先使用 VITE_PUBLIC_URL（局域网 IP），否则取当前 origin
    const baseUrl = import.meta.env.VITE_PUBLIC_URL || window.location.origin
    const mobileUrl = `${baseUrl}/mobile-upload?token=${token}`
    qrDataUrl.value = await QRCode.toDataURL(mobileUrl, {
      width: 240,
      margin: 2,
      color: { dark: '#000000', light: '#ffffff' },
    })
    // 开始轮询，兜底拉取手机上传的图片
    startPolling()
  } catch {
    message.error('创建上传会话失败')
  }
}

/** 轮询拉取已上传的图片列表，检测会话完成状态 */
async function startPolling() {
  stopPolling()
  const fn = async () => {
    if (!uploadToken.value) return
    try {
      const res = await getSessionImages(uploadToken.value)
      const data = res.data?.data
      if (!data) return

      // 检测会话是否已完成（WebSocket uploadComplete 可能丢失）
      if (data.completed === true) {
        uploadingFromPhone.value = false
        stopPolling()
        showQrModal.value = false
        uploadToken.value = ''
        qrDataUrl.value = ''
        return
      }

      const urls = data.imageUrls as string[] | undefined
      if (!urls || urls.length === 0) return
      for (const url of urls) {
        if (uploadedUrls.value.includes(url)) continue
        uploadingFromPhone.value = true
        uploadedUrls.value.push(url)
        fileList.value.push({
          uid: `phone-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
          name: url.split('/').pop() || `phone-upload-${fileList.value.length}`,
          status: 'done',
          url,
        })
      }
    } catch {
      // ignore
    }
  }
  // 立即执行一次 + 每 3 秒轮询
  await fn()
  pollTimer = setInterval(fn, 3000)
}
function stopPolling() {
  if (pollTimer !== null) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}
async function handleCloseQrModal() {
  showQrModal.value = false
  // 如果还在上传中，不删除会话（防止误操作）
  if (!uploadingFromPhone.value && uploadToken.value) {
    try {
      await deleteUploadSession(uploadToken.value)
    } catch {
      // ignore
    }
  }
  uploadToken.value = ''
  qrDataUrl.value = ''
  uploadingFromPhone.value = false
  stopPolling()
}

/** 处理 WebSocket 推送的上传事件 */
function handleWsUpload(e: Event) {
  const detail = (e as CustomEvent).detail
  if (!detail || detail.token !== uploadToken.value) return

  if (detail.type === 'uploadProgress') {
    uploadingFromPhone.value = true
    const imageUrl = detail.imageUrl as string
    // 避免重复
    if (uploadedUrls.value.includes(imageUrl)) return
    uploadedUrls.value.push(imageUrl)
    // 同步更新 fileList 用于展示
    fileList.value.push({
      uid: `phone-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
      name: imageUrl.split('/').pop() || `phone-upload-${fileList.value.length}`,
      status: 'done',
      url: imageUrl,
    })
  } else if (detail.type === 'uploadComplete') {
    uploadingFromPhone.value = false
    message.success(`手机上传完成，共 ${uploadedUrls.value.length} 张图片`)
    stopPolling()
    showQrModal.value = false
    uploadToken.value = ''
    qrDataUrl.value = ''
  }
}

onMounted(() => {
  window.addEventListener('ws-upload', handleWsUpload)
})

onUnmounted(() => {
  window.removeEventListener('ws-upload', handleWsUpload)
  stopPolling()
})

function handleRouteChange(data: RouteResultData | null) {
  formState.routeData = data ? JSON.stringify(data) : ''
}
</script>

<style scoped>
.create-form-wrapper {
  max-width: 720px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
</style>
