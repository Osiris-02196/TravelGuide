<template>
  <div id="userLoginPage">
    <h2 class="title">旅游攻略指南 - 用户登录</h2>
    <div class="desc">旅游没有方向，就选旅游攻略指南</div>
    <a-form :model="formState" name="login" autocomplete="off" @finish="handleSubmit">
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
      <div class="tips">
        没有账号？
        <RouterLink to="/user/register">去注册</RouterLink>
      </div>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%" :loading="submitting"
        >登录</a-button
        >
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { userLogin } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})
const submitting = ref(false)

async function handleSubmit() {
  submitting.value = true
  try {
    const res = await userLogin(formState)
    if (res.data?.code === 0) {
      message.success('登录成功')
      loginUserStore.setLoginUser(res.data.data ?? null)
      router.push('/')
    } else {
      message.error(res.data?.message || '登录失败')
    }
  } catch {
    message.error('登录失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>
