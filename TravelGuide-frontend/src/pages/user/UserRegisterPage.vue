<template>
  <div id="userRegisterPage">
    <h2 class="title">旅游攻略指南 - 用户注册</h2>
    <div class="desc">旅游没有方向，就选旅游攻略指南</div>
    <a-form :model="formState" name="register" autocomplete="off" @finish="handleSubmit">
      <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
      </a-form-item>
      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 8, message: '密码不能小于 8 位' },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
      </a-form-item>
      <a-form-item
        name="checkPassword"
        :rules="[
          { required: true, message: '请确认密码' },
          { validator: validateConfirmPassword },
        ]"
      >
        <a-input-password v-model:value="formState.checkPassword" placeholder="请确认密码" />
      </a-form-item>
      <div class="tips">
        已有账号？
        <RouterLink to="/user/login">去登录</RouterLink>
      </div>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%" :loading="submitting">注册</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { userRigister } from '@/api/userController'

const router = useRouter()

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})
const submitting = ref(false)

function validateConfirmPassword(_rule: any, value: string) {
  if (value !== formState.userPassword) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

async function handleSubmit() {
  submitting.value = true
  try {
    const res = await userRigister(formState)
    if (res.data?.code === 0) {
      message.success('注册成功，请登录')
      router.push('/user/login')
    } else {
      message.error(res.data?.message || '注册失败')
    }
  } catch {
    message.error('注册失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>
