<template>
  <div class="page-container">
    <div class="create-form-wrapper">
      <h3 style="margin-bottom: 24px; font-size: 18px">修改资料</h3>
      <a-form :model="formState" @finish="handleSubmit">
        <!-- Avatar -->
        <a-form-item label="头像">
          <a-upload
            v-model:file-list="fileList"
            list-type="picture-card"
            :max-count="1"
            :before-upload="beforeUpload"
            :custom-request="customUpload"
            @remove="handleRemoveAvatar"
          >
            <div v-if="fileList.length === 0">
              <PlusOutlined />
              <div style="margin-top: 8px">上传</div>
            </div>
          </a-upload>
        </a-form-item>

        <!-- UserName -->
        <a-form-item label="用户名称" name="userName">
          <a-input
            v-model:value="formState.userName"
            placeholder="请输入新的用户名"
            :maxlength="50"
          />
        </a-form-item>

        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" :loading="submitting">保存</a-button>
            <a-button @click="handleCancel">取消</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import request from '@/request'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const fileList = ref<any[]>([])
const newAvatarUrl = ref('')
const submitting = ref(false)

const formState = reactive({
  userName: loginUserStore.loginUser?.userName || '',
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
    const res = await request<API.BaseResponseString>('/user/upload/avatar', {
      method: 'POST',
      data: formData,
    })
    if (res.data?.code === 0 && res.data?.data) {
      newAvatarUrl.value = res.data.data
      onSuccess(res.data, file)
    } else {
      onError(new Error(res.data?.message || '上传失败'))
    }
  } catch (err) {
    onError(err)
  }
}

function handleRemoveAvatar() {
  newAvatarUrl.value = ''
}

async function handleSubmit() {
  submitting.value = true
  try {
    const res = await request<API.BaseResponseBoolean>('/user/update/profile', {
      method: 'POST',
      data: {
        userName: formState.userName,
        userAvatar: newAvatarUrl.value || undefined,
      },
    })
    if (res.data?.code === 0) {
      message.success('修改成功')
      if (loginUserStore.loginUser) {
        if (formState.userName) loginUserStore.loginUser.userName = formState.userName
        if (newAvatarUrl.value) loginUserStore.loginUser.userAvatar = newAvatarUrl.value
      }
      router.push('/profile')
    } else {
      message.error(res.data?.message || '修改失败')
    }
  } catch {
    message.error('修改失败')
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
  max-width: 600px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
</style>
