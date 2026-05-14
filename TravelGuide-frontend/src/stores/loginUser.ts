import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getLoginUser } from '@/api/userController'

export const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<API.LoginUserVO | null>(null)
  const loading = ref(false)

  const isLoggedIn = computed(() => !!loginUser.value)
  const isAdmin = computed(() => loginUser.value?.userRole === 'admin' || loginUser.value?.userRole === 'superadmin')

  async function fetchLoginUser() {
    if (loading.value) return
    loading.value = true
    try {
      const res = await getLoginUser()
      if (res.data?.code === 0 && res.data?.data) {
        loginUser.value = res.data.data
      } else {
        loginUser.value = null
      }
    } catch {
      loginUser.value = null
    } finally {
      loading.value = false
    }
  }

  function setLoginUser(user: API.LoginUserVO | null) {
    loginUser.value = user
  }

  function logout() {
    loginUser.value = null
  }

  return { loginUser, loading, isLoggedIn, isAdmin, fetchLoginUser, setLoginUser, logout }
})
