<template>
  <div class="page-container">
    <div class="page-title">我的攻略</div>

    <a-table
      :data-source="dataList"
      :columns="columns"
      :loading="loading"
      :row-key="(record: any) => record.id"
      :pagination="{
        current: currentPage,
        pageSize: pageSize,
        total: total,
        showSizeChanger: false,
      }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'image'">
          <div class="table-image-wrap">
            <img
              v-if="getFirstImage(record.imageUrls)"
              :src="getFirstImage(record.imageUrls)"
              alt="cover"
              @error="handleImgError"
            />
            <span v-else class="no-image">暂无</span>
          </div>
        </template>
        <template v-if="column.key === 'title'">
          <a class="title-link" @click="goToDetail(record.id)">{{ record.strategyTitle }}</a>
        </template>
        <template v-if="column.key === 'location'">
          {{ formatLocation(record.locations) }}
        </template>
        <template v-if="column.key === 'updateTime'">
          {{ formatTime(record.updateTime) }}
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColor(record.strategyStatus)">{{ statusLabel(record.strategyStatus) }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-popconfirm title="确定删除此攻略？" ok-text="确定" cancel-text="取消" @confirm="handleDelete(record)">
            <a-button danger size="small">删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listMyStrategies, deleteStrategy } from '@/api/strategyController'
import { message } from 'ant-design-vue'

const router = useRouter()
const dataList = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

const columns = [
  { title: '图片', key: 'image', width: 100, align: 'center' },
  { title: '标题', key: 'title', ellipsis: true },
  { title: '地点', key: 'location', width: 150, ellipsis: true },
  { title: '更新时间', key: 'updateTime', width: 170 },
  { title: '状态', key: 'status', width: 90, align: 'center' },
  { title: '操作', key: 'action', width: 80, align: 'center' },
]

onMounted(async () => {
  await fetchData()
})

function onTableChange(pag: any) {
  if (pag.current !== currentPage.value) {
    currentPage.value = pag.current
    fetchData(pag.current)
  }
}

async function fetchData(page = 1) {
  loading.value = true
  try {
    const params: API.StrategyQueryRequest = {
      pageNum: page,
      pageSize: pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    }
    const res = await listMyStrategies(params)
    if (res.data?.code === 0 && res.data?.data) {
      dataList.value = res.data.data.records || []
      total.value = res.data.data.totalRow || 0
      currentPage.value = page
    }
  } catch {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

function getFirstImage(imageUrls?: string): string | undefined {
  if (!imageUrls) return undefined
  try {
    const urls = JSON.parse(imageUrls)
    if (Array.isArray(urls) && urls.length > 0) return urls[0]
    return undefined
  } catch {
    const parts = imageUrls.split(',').filter(Boolean)
    return parts.length > 0 ? parts[0] : undefined
  }
}

function formatLocation(locations?: string): string {
  if (!locations) return ''
  try {
    const arr = JSON.parse(locations)
    if (Array.isArray(arr) && arr.length > 0) {
      return arr.slice(0, 2).join(' · ')
    }
    return ''
  } catch {
    return locations || ''
  }
}

function formatTime(time?: string): string {
  if (!time) return ''
  try {
    const d = new Date(time)
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  } catch {
    return time
  }
}

function statusColor(status?: number): string {
  if (status === 1) return 'green'
  if (status === 0) return 'orange'
  if (status === 2) return 'red'
  return 'default'
}

function statusLabel(status?: number): string {
  if (status === 1) return '已通过'
  if (status === 0) return '待审核'
  if (status === 2) return '已拒绝'
  return '未知'
}

function goToDetail(id?: string | number) {
  if (id !== undefined && id !== null && String(id) !== '') {
    router.push({ name: 'strategyDetail', params: { id: String(id) } })
  }
}

async function handleDelete(item: any) {
  if (!item.id) return
  try {
    const res = await deleteStrategy({ id: item.id })
    if (res.data?.code === 0) {
      message.success('删除成功')
      fetchData(currentPage.value)
    } else {
      message.error(res.data?.message || '删除失败')
    }
  } catch {
    message.error('删除失败')
  }
}

function handleImgError(e: Event) {
  const img = e.target as HTMLImageElement
  img.style.display = 'none'
}
</script>

<style scoped>
.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 20px;
}

.table-image-wrap {
  width: 72px;
  height: 54px;
  border-radius: 4px;
  overflow: hidden;
  background: #f5f5f5;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.table-image-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.no-image {
  font-size: 12px;
  color: #bbb;
}

.title-link {
  color: #1a1a1a;
  cursor: pointer;
}

.title-link:hover {
  color: #ff6b6b;
}

:deep(.ant-table-wrapper) {
  background: #fff;
  border-radius: 8px;
}
</style>
