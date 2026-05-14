<template>
  <div class="page-container">
    <div class="follow-page-wrapper">
      <!-- Tabs: 关注 / 粉丝 -->
      <div class="follow-tabs">
        <div
          :class="['follow-tab', { active: currentTab === 'following' }]"
          @click="switchTab('following')"
        >
          我的关注
        </div>
        <div
          :class="['follow-tab', { active: currentTab === 'followers' }]"
          @click="switchTab('followers')"
        >
          我的粉丝
        </div>
      </div>

      <!-- 用户列表 -->
      <a-spin :spinning="loading">
        <div v-if="userList.length > 0" class="follow-list">
          <div
            v-for="user in userList"
            :key="user.id"
            class="follow-item"
            @click="goToProfile(user.id!)"
          >
            <img
              :src="user.userAvatar || defaultAvatar"
              alt="avatar"
              class="follow-avatar"
              @error="handleAvatarError"
            />
            <span class="follow-name">{{ user.userName || user.userAccount }}</span>
          </div>
        </div>
        <div v-else-if="!loading" class="follow-empty">
          {{ currentTab === 'following' ? '还没有关注任何人' : '还没有粉丝' }}
        </div>
      </a-spin>

      <!-- Pagination -->
      <div v-if="total > pageSize" class="pagination-wrap">
        <a-pagination
          v-model:current="currentPage"
          :total="total"
          :page-size="pageSize"
          :show-size-changer="false"
          @change="fetchData"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listFollowing, listFollowers } from '@/api/followController'
import { useLoginUserStore } from '@/stores/loginUser'
import { message } from 'ant-design-vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const currentTab = ref<'following' | 'followers'>('following')
const userList = ref<API.UserVO[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

const defaultAvatar = 'https://api.dicebear.com/7.x/initials/svg?seed=User'

onMounted(() => {
  if (!loginUserStore.isLoggedIn) {
    message.warning('请先登录')
    router.push('/user/login')
    return
  }
  fetchData()
})

function switchTab(tab: 'following' | 'followers') {
  currentTab.value = tab
  currentPage.value = 1
  fetchData()
}

async function fetchData(page = 1) {
  if (!loginUserStore.loginUser?.id) return
  loading.value = true
  try {
    const fn = currentTab.value === 'following' ? listFollowing : listFollowers
    const res = await fn(String(loginUserStore.loginUser.id), {
      pageNum: page,
      pageSize,
    })
    if (res.data?.code === 0 && res.data?.data) {
      userList.value = res.data.data.records || []
      total.value = res.data.data.totalRow || 0
      currentPage.value = page
    }
  } catch {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

function goToProfile(userId: number | string) {
  router.push(`/user/${userId}/profile`)
}

function handleAvatarError(e: Event) {
  const img = e.target as HTMLImageElement
  if (img.src !== defaultAvatar) {
    img.src = defaultAvatar
  }
}
</script>

<style scoped>
.follow-page-wrapper {
  max-width: 600px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.follow-tabs {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
}

.follow-tab {
  font-size: 16px;
  color: #999;
  cursor: pointer;
  padding-bottom: 4px;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.follow-tab.active {
  color: #1a1a1a;
  font-weight: 600;
  border-bottom-color: #ff6b6b;
}

.follow-list {
  display: flex;
  flex-direction: column;
}

.follow-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.15s;
}

.follow-item:hover {
  background: #fafafa;
  margin: 0 -12px;
  padding: 12px 12px;
  border-radius: 8px;
}

.follow-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  background: #f0f0f0;
  flex-shrink: 0;
}

.follow-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.follow-empty {
  text-align: center;
  padding: 48px 0;
  color: #bbb;
  font-size: 14px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
