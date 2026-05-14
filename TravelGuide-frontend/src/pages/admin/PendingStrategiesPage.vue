<template>
  <div class="page-container">
    <h3 style="margin-bottom: 20px; font-size: 18px">待审核攻略</h3>

    <a-table
      :columns="columns"
      :data-source="dataList"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'images'">
          <div style="display: flex; gap: 4px">
            <img
              v-for="(img, idx) in getFirstImage(record.imageUrls)"
              :key="idx"
              :src="img"
              style="width: 48px; height: 48px; object-fit: cover; border-radius: 4px"
              @error="handleImgError"
            />
            <span v-if="getImageCount(record.imageUrls) === 0">无图片</span>
          </div>
        </template>
        <template v-else-if="column.key === 'content'">
          {{ truncateContent(record.strategyContent) }}
        </template>
        <template v-else-if="column.key === 'locations'">
          {{ record.locations || '-' }}
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-space>
            <a-button size="small" @click="handleViewDetail(record)">查看</a-button>
            <a-button type="primary" size="small" @click="handleAudit(record, 1)">通过</a-button>
            <a-button danger size="small" @click="handleAudit(record, 2)">拒绝</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { listPendingStrategies, auditStrategy } from '@/api/strategyController'

const router = useRouter()

const columns = [
  { title: '图片', key: 'images', width: 120 },
  { title: '标题', dataIndex: 'strategyTitle', key: 'title', width: 180 },
  { title: '内容', dataIndex: 'strategyContent', key: 'content', ellipsis: true },
  { title: '地点', key: 'locations', width: 120 },
  { title: '提交时间', dataIndex: 'createTime', key: 'time', width: 160 },
  { title: '操作', key: 'actions', width: 220, fixed: 'right' as const },
]

const dataList = ref<API.StrategyVO[]>([])
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
    const res = await listPendingStrategies({ pageNum: page, pageSize: pageSize })
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

async function handleAudit(record: API.StrategyVO, status: 1 | 2) {
  if (!record.id) return
  const statusText = status === 1 ? '通过' : '拒绝'
  try {
    const res = await auditStrategy({ id: record.id, status })
    if (res.data?.code === 0) {
      message.success(`已${statusText}`)
      fetchData(currentPage.value)
    } else {
      message.error(res.data?.message || '操作失败')
    }
  } catch {
    message.error('操作失败')
  }
}

function getFirstImage(imageUrls?: string): string[] {
  if (!imageUrls) return []
  try {
    const urls = JSON.parse(imageUrls)
    return Array.isArray(urls) ? urls.slice(0, 1) : []
  } catch {
    return imageUrls.split(',').slice(0, 1)
  }
}

function getImageCount(imageUrls?: string): number {
  if (!imageUrls) return 0
  try {
    const urls = JSON.parse(imageUrls)
    return Array.isArray(urls) ? urls.length : 0
  } catch {
    return imageUrls.split(',').length
  }
}

function truncateContent(content?: string): string {
  if (!content) return ''
  return content.length > 30 ? content.substring(0, 30) + '...' : content
}

function handleImgError(e: Event) {
  (e.target as HTMLImageElement).style.display = 'none'
}

function handleViewDetail(record: API.StrategyVO) {
  if (record.id) {
    router.push({ name: 'strategyDetail', params: { id: String(record.id) } })
  }
}
</script>
