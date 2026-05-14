<template>
  <div class="page-container">
    <h3 style="margin-bottom: 20px; font-size: 18px">用户管理</h3>

    <a-table
      :columns="columns"
      :data-source="dataList"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'avatar'">
          <img
            :src="record.userAvatar || defaultAvatar"
            style="width: 32px; height: 32px; border-radius: 50%; object-fit: cover"
            @error="handleImgError"
          />
        </template>
        <template v-else-if="column.key === 'userStatus'">
          <a-tag :color="getStatusColor(record.userStatus)">
            {{ getStatusText(record.userStatus) }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-space>
            <a-button
              v-if="record.userStatus !== 1"
              type="primary"
              size="small"
              @click="handleUpdateStatus(record, 1)"
            >
              正常
            </a-button>
            <a-button
              v-if="record.userStatus !== 2"
              size="small"
              @click="handleUpdateStatus(record, 2)"
            >
              禁言
            </a-button>
            <a-button
              v-if="record.userStatus !== 3"
              danger
              size="small"
              @click="handleUpdateStatus(record, 3)"
            >
              封号
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { listUserVoByPage, updateUserStatus } from '@/api/userController'

const defaultAvatar = 'https://api.dicebear.com/7.x/initials/svg?seed=User'

const columns = [
  { title: '头像', key: 'avatar', width: 70 },
  { title: '账号', dataIndex: 'userAccount', key: 'userAccount', width: 140 },
  { title: '用户名', dataIndex: 'userName', key: 'userName', width: 140 },
  { title: '角色', dataIndex: 'userRole', key: 'userRole', width: 80 },
  { title: '状态', dataIndex: 'userStatus', key: 'userStatus', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'actions', width: 240, fixed: 'right' as const },
]

const dataList = ref<API.UserVO[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: false,
})

onMounted(() => fetchData())

async function fetchData(page = 1) {
  loading.value = true
  try {
    const res = await listUserVoByPage({ pageNum: page, pageSize: pageSize })
    if (res.data?.code === 0 && res.data?.data) {
      dataList.value = res.data.data.records || []
      total.value = res.data.data.totalRow || 0
      pagination.current = page
      pagination.total = total.value
      currentPage.value = page
    }
  } catch {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

function handleTableChange(pag: any) {
  fetchData(pag.current)
}

async function handleUpdateStatus(record: API.UserVO, status: number) {
  if (!record.id) return
  const statusMap: Record<number, string> = { 1: '正常', 2: '禁言', 3: '封号' }
  try {
    const res = await updateUserStatus({ id: record.id, userStatus: status })
    if (res.data?.code === 0) {
      message.success(`已修改为${statusMap[status]}`)
      fetchData(currentPage.value)
    } else {
      message.error(res.data?.message || '操作失败')
    }
  } catch {
    message.error('操作失败')
  }
}

function getStatusColor(status?: number): string {
  const map: Record<number, string> = { 1: 'green', 2: 'orange', 3: 'red' }
  return map[status ?? 0] || 'default'
}

function getStatusText(status?: number): string {
  const map: Record<number, string> = { 1: '正常', 2: '禁言', 3: '封号' }
  return map[status ?? 0] || '未知'
}

function handleImgError(e: Event) {
  ;(e.target as HTMLImageElement).src = defaultAvatar
}
</script>
