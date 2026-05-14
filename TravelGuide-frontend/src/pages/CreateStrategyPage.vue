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
        </a-form-item>

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
            :load-data="loadCityData"
            placeholder="选择省市"
            change-on-select
            style="width: 100%"
          />
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { addStrategy, uploadImage } from '@/api/strategyController'
import { listProvinces, listCities } from '@/api/locationController'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const fileList = ref<any[]>([])
const uploadedUrls = ref<string[]>([])

const formState = reactive<{
  strategyTitle: string
  strategyContent: string
  strategyTags: string[]
  locations: string[]
}>({
  strategyTitle: '',
  strategyContent: '',
  strategyTags: [],
  locations: [],
})

const rules = {
  strategyTitle: [{ required: true, message: '请输入攻略标题' }],
  strategyContent: [{ required: true, message: '请输入攻略内容' }],
}

const provinceOptions = ref<any[]>([])

onMounted(async () => {
  try {
    const res = await listProvinces()
    if (res.data?.code === 0 && res.data?.data) {
      provinceOptions.value = res.data.data.map((p) => ({
        value: p.id,
        label: p.locationName,
        isLeaf: false,
      }))
    }
  } catch {
    // ignore
  }
})

async function loadCityData(selectedOptions: any) {
  const targetOption = selectedOptions[selectedOptions.length - 1]
  targetOption.loading = true
  try {
    const provinceId = typeof targetOption.value === 'number' ? targetOption.value : Number(targetOption.value)
    const res = await listCities({ provinceId })
    if (res.data?.code === 0 && res.data?.data) {
      targetOption.children = res.data.data.map((c) => ({
        value: c.id,
        label: c.locationName,
        isLeaf: true,
      }))
    }
  } catch {
    // ignore
  } finally {
    targetOption.loading = false
  }
}

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
