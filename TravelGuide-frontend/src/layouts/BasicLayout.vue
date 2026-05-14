<template>
  <div class="basic-layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-logo">
        <img src="@/assets/logo.png" alt="logo" />
        <span>旅游攻略指南</span>
      </div>
      <nav class="sidebar-nav">
        <div class="menu-links">
          <router-link to="/">
            <HomeOutlined />
            <span>首页</span>
          </router-link>
          <router-link to="/my-strategies">
            <FileTextOutlined />
            <span>我的攻略</span>
          </router-link>
          <router-link to="/my-follows">
            <HeartOutlined />
            <span>我的关注</span>
          </router-link>
          <router-link to="/create-strategy">
            <PlusCircleOutlined />
            <span>新建攻略</span>
          </router-link>
          <template v-if="loginUserStore.isAdmin">
            <router-link to="/admin/pending-strategies">
              <AuditOutlined />
              <span>待审核攻略</span>
            </router-link>
            <router-link to="/admin/users">
              <TeamOutlined />
              <span>用户管理</span>
            </router-link>
            <router-link to="/admin/all-strategies">
              <AppstoreOutlined />
              <span>所有攻略</span>
            </router-link>
          </template>
        </div>

        <!-- AI 对话入口按钮 -->
        <div class="ai-entry" @click="openAiPanel">
          <div class="ai-entry-btn">
            <RobotOutlined />
          </div>
          <span>AI 助手</span>
        </div>
      </nav>
    </aside>

    <!-- AI 对话面板 -->
    <AiChatPanel :open="aiPanelOpen" @close="aiPanelOpen = false" />

    <!-- Main Area -->
    <div class="main-area">
      <!-- Top Header -->
      <header class="top-header">
        <div class="top-header-left">
          <a-input-search
            v-model:value="searchText"
            class="search-input"
            placeholder="搜索攻略标题、地点..."
            @search="handleSearch"
          />
          <a-cascader
            v-model:value="selectedLocation"
            :options="provinceOptions"
            :load-data="loadCityData"
            placeholder="选择城市"
            change-on-select
            @change="handleLocationChange"
          />
        </div>
        <div class="top-header-right">
          <template v-if="loginUserStore.isLoggedIn && loginUserStore.loginUser">
            <NotifyBell />
            <a-dropdown :trigger="['hover']">
              <div class="user-dropdown-trigger">
                <img
                  :src="loginUserStore.loginUser.userAvatar || defaultAvatar"
                  alt="avatar"
                />
                <span>{{ loginUserStore.loginUser.userName || loginUserStore.loginUser.userAccount }}</span>
                <DownOutlined style="font-size: 10px; color: #999" />
              </div>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="profile" @click="goToProfile">
                    <UserOutlined /> 个人主页
                  </a-menu-item>
                  <a-menu-item key="logout" @click="handleLogout">
                    <LogoutOutlined /> 退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
          <template v-else>
            <a-button type="primary" @click="goToLogin">登录</a-button>
          </template>
        </div>
      </header>

      <!-- Content -->
      <main class="main-content">
        <router-view v-slot="{ Component }">
          <keep-alive :include="['home']">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  HomeOutlined,
  FileTextOutlined,
  PlusCircleOutlined,
  HeartOutlined,
  AuditOutlined,
  TeamOutlined,
  AppstoreOutlined,
  DownOutlined,
  UserOutlined,
  LogoutOutlined,
  RobotOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { listProvinces, listCities } from '@/api/locationController'
import { userLogout } from '@/api/userController'
import NotifyBell from '@/components/NotifyBell.vue'
import AiChatPanel from '@/components/ai/AiChatPanel.vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { useAiChatStore } from '@/stores/aiChat'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()
const aiChatStore = useAiChatStore()

const defaultAvatar = 'https://api.dicebear.com/7.x/initials/svg?seed=User'
const searchText = ref('')
const selectedLocation = ref<string[]>([])
const provinceOptions = ref<any[]>([])
const aiPanelOpen = ref(false)

onMounted(async () => {
  loadProvinces()
})

async function loadProvinces() {
  try {
    const res = await listProvinces()
    if (res.data?.code === 0 && res.data?.data) {
      provinceOptions.value = res.data.data.map((p: API.LocationVO) => ({
        value: p.id,
        label: p.locationName,
        isLeaf: false,
      }))
    }
  } catch {
    console.error('加载省份失败')
  }
}

async function loadCityData(selectedOptions: any) {
  const targetOption = selectedOptions[selectedOptions.length - 1]
  targetOption.loading = true
  try {
    const provinceId = typeof targetOption.value === 'number' ? targetOption.value : Number(targetOption.value)
    const res = await listCities({ provinceId })
    if (res.data?.code === 0 && res.data?.data) {
      targetOption.children = res.data.data.map((c: API.LocationVO) => ({
        value: c.id,
        label: c.locationName,
        isLeaf: true,
      }))
    }
  } catch {
    console.error('加载城市失败')
  } finally {
    targetOption.loading = false
  }
}

function handleSearch(value: string) {
  if (!value.trim()) return
  router.push({ name: 'home', query: { keyword: value.trim() } })
}

function handleLocationChange(value: string[]) {
  if (!value || value.length === 0) return
  // Only navigate when a city (second level) is selected, not when province (first level) is clicked
  if (value.length < 2) return
  const provinceOpt = provinceOptions.value.find(
    (p) => String(p.value) === String(value[0])
  )
  if (!provinceOpt || !provinceOpt.children) return
  const cityOpt = provinceOpt.children.find(
    (c: any) => String(c.value) === String(value[1])
  )
  if (cityOpt) {
    router.push({
      name: 'locationStrategies',
      params: { locationId: String(cityOpt.value) },
      query: { name: cityOpt.label },
    })
  }
}

function openAiPanel() {
  aiPanelOpen.value = true
  // 打开面板时加载会话列表
  const userId = loginUserStore.loginUser?.id
  if (userId) {
    aiChatStore.fetchSessionList(userId)
  }
}

function goToProfile() {
  router.push('/profile')
}

async function handleLogout() {
  try {
    await userLogout()
  } catch {
    // ignore
  }
  loginUserStore.logout()
  message.success('已退出登录')
  router.push('/')
}

function goToLogin() {
  router.push('/user/login')
}
</script>
