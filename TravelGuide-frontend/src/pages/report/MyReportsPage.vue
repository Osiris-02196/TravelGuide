<template>
  <div class="my-reports-page">
    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="reports" tab="我的举报" />
      <a-tab-pane key="warnings" tab="警告" />
    </a-tabs>

    <!-- 我的举报 Tab -->
    <div v-if="activeTab === 'reports'" class="report-list">
      <a-spin :spinning="loading">
        <div v-if="reportList.length > 0">
          <div v-for="item in reportList" :key="item.id" class="report-card" :data-id="item.id">
            <div class="report-card-header">
              <span class="report-card-target">
                <a-tag :color="item.targetType === 'strategy' ? 'blue' : 'purple'">
                  {{ item.targetType === 'strategy' ? '攻略' : '评论' }}
                </a-tag>
              </span>
              <span class="report-card-status">
                <a-tag :color="getStatusColor(item.status)">
                  {{ getStatusText(item.status) }}
                </a-tag>
              </span>
            </div>
            <div class="report-card-body">
              <div class="report-card-reason">
                <strong>举报原因：</strong>{{ item.reason }}
              </div>
              <div v-if="item.description" class="report-card-desc">
                {{ truncateText(item.description, 80) }}
              </div>
            </div>
            <div class="report-card-footer">
              <span class="report-card-time">{{ formatTime(item.createTime) }}</span>
              <a-button type="link" size="small" @click="showDetail(item)">查看详情</a-button>
            </div>
          </div>
        </div>
        <a-empty v-else description="暂无举报记录" />
      </a-spin>
      <div v-if="reportTotal > reportList.length" class="load-more">
        <a-button :loading="loadingMore" @click="loadMore">加载更多</a-button>
      </div>
    </div>

    <!-- 警告 Tab -->
    <div v-if="activeTab === 'warnings'" class="warning-list">
      <a-spin :spinning="warningLoading">
        <div v-if="warningList.length > 0">
          <div v-for="item in warningList" :key="item.id" class="warning-card">
            <div class="warning-card-header">
              <a-tag :color="item.targetType === 1 ? 'blue' : 'purple'">
                {{ item.targetType === 1 ? '攻略' : '评论' }}
              </a-tag>
              <span class="warning-card-time">{{ formatTime(item.createTime) }}</span>
            </div>
            <div class="warning-card-body">
              <p>{{ item.content }}</p>
              <a-button
                v-if="item.targetType === 1 || item.targetType === 2"
                type="link"
                size="small"
                @click="viewWarningContent(item)"
              >
                <template v-if="item.targetType === 1">查看攻略</template>
                <template v-else>查看评论</template>
                <RightOutlined style="font-size: 12px" />
              </a-button>
            </div>
          </div>
        </div>
        <a-empty v-else description="暂无警告记录" />
      </a-spin>
      <div v-if="warningTotal > warningList.length" class="load-more">
        <a-button :loading="warningLoadingMore" @click="loadMoreWarnings">加载更多</a-button>
      </div>
    </div>

    <!-- 举报详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="举报详情"
      :footer="null"
      width="560px"
    >
      <div v-if="detailItem" class="report-detail">
        <a-descriptions :column="1" size="small" bordered>
          <a-descriptions-item label="举报类型">
            {{ detailItem.targetType === 'strategy' ? '攻略' : '评论' }}
          </a-descriptions-item>
          <a-descriptions-item label="举报原因">{{ detailItem.reason }}</a-descriptions-item>
          <a-descriptions-item label="举报详情">
            {{ detailItem.description || '无' }}
          </a-descriptions-item>
          <a-descriptions-item label="举报时间">
            {{ formatTime(detailItem.createTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="举报状态">
            <a-tag :color="getStatusColor(detailItem.status)">
              {{ getStatusText(detailItem.status) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item v-if="detailItem.reviewRemark" label="管理员审核说明">
            {{ detailItem.reviewRemark }}
          </a-descriptions-item>
          <a-descriptions-item v-if="detailItem.reviewTime" label="审核时间">
            {{ formatTime(detailItem.reviewTime) }}
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { RightOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { listMyReports, getReportDetail } from '@/api/reportController'
import { listMyNotifies } from '@/api/notifyController'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const activeTab = ref('reports')

// 我的举报
const reportList = ref<API.ReportVO[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const reportPage = ref(1)
const reportTotal = ref(0)
const pageSize = 10

// 警告
const warningList = ref<API.NotifyVO[]>([])
const warningLoading = ref(false)
const warningLoadingMore = ref(false)
const warningPage = ref(1)
const warningTotal = ref(0)

// 详情弹窗
const detailVisible = ref(false)
const detailItem = ref<API.ReportVO | null>(null)

function getStatusColor(status?: string) {
  const map: Record<string, string> = {
    pending: 'orange',
    approved: 'red',
    rejected: 'default',
  }
  return map[status || ''] || 'default'
}

function getStatusText(status?: string) {
  const map: Record<string, string> = {
    pending: '待审核',
    approved: '举报成立',
    rejected: '举报驳回',
  }
  return map[status || ''] || '未知'
}

function truncateText(text: string, max: number) {
  if (!text) return ''
  return text.length > max ? text.slice(0, max) + '...' : text
}

function formatTime(time?: string) {
  if (!time) return ''
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

async function fetchReports(page: number) {
  if (page === 1) loading.value = true
  try {
    const res = await listMyReports({ pageNum: page, pageSize })
    if (res.data?.code === 0 && res.data?.data) {
      const data = res.data.data
      if (page === 1) {
        reportList.value = data.records || []
      } else {
        reportList.value.push(...(data.records || []))
      }
      reportTotal.value = data.totalRow || 0
      reportPage.value = page
    }
  } catch {
    message.error('加载举报列表失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadMore() {
  loadingMore.value = true
  await fetchReports(reportPage.value + 1)
}

async function showDetail(item: API.ReportVO) {
  try {
    const res = await getReportDetail({ id: item.id! })
    if (res.data?.code === 0 && res.data?.data) {
      detailItem.value = res.data.data
      detailVisible.value = true
    }
  } catch {
    // fallback: show basic info
    detailItem.value = item
    detailVisible.value = true
  }
}

async function fetchWarnings(page: number) {
  if (page === 1) warningLoading.value = true
  try {
    // 同时查询 violation（违规删除）和 warning（警告）两种类型
    const [res1, res2] = await Promise.all([
      listMyNotifies({ pageNum: page, pageSize, type: 'violation' }),
      listMyNotifies({ pageNum: page, pageSize, type: 'warning' }),
    ])
    const allRecords: API.NotifyVO[] = []
    let totalRow = 0
    if (res1.data?.code === 0 && res1.data?.data) {
      allRecords.push(...(res1.data.data.records || []))
      totalRow += res1.data.data.totalRow || 0
    }
    if (res2.data?.code === 0 && res2.data?.data) {
      allRecords.push(...(res2.data.data.records || []))
      totalRow += res2.data.data.totalRow || 0
    }
    // 按 createTime 降序排列
    allRecords.sort((a, b) => {
      const ta = a.createTime ? new Date(a.createTime).getTime() : 0
      const tb = b.createTime ? new Date(b.createTime).getTime() : 0
      return tb - ta
    })
    if (page === 1) {
      warningList.value = allRecords
    } else {
      warningList.value.push(...allRecords)
    }
    warningTotal.value = totalRow
    warningPage.value = page
  } catch {
    message.error('加载警告列表失败')
  } finally {
    warningLoading.value = false
    warningLoadingMore.value = false
  }
}

async function loadMoreWarnings() {
  warningLoadingMore.value = true
  await fetchWarnings(warningPage.value + 1)
}

/** 查看被警告/违规的内容 */
function viewWarningContent(item: API.NotifyVO) {
  const targetType = item.targetType
  const targetId = item.targetId ? String(item.targetId) : ''
  const strategyId = item.strategyId ? String(item.strategyId) : ''

  if (targetType === 1) {
    router.push(`/strategy/${targetId}`)
  } else if (targetType === 2) {
    const sid = strategyId || targetId
    router.push(`/strategy/${sid}?commentId=${targetId}`)
  }
}

/** 监听 highlightId 参数变化，高亮对应的举报项（支持从通知列表多次点击跳转） */
watch(
  () => route.query.highlightId,
  (highlightId) => {
    if (!highlightId) return
    activeTab.value = 'reports'
    const doHighlight = () => {
      const el = document.querySelector(`.report-card[data-id="${highlightId}"]`)
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' })
        el.classList.add('highlight-flash')
        setTimeout(() => el.classList.remove('highlight-flash'), 2000)
        return true
      }
      return false
    }
    // 重试最多 5 次（间隔 500ms），等待数据加载完成
    let attempts = 0
    const timer = setInterval(() => {
      attempts++
      if (doHighlight() || attempts >= 5) clearInterval(timer)
    }, 500)
  },
  { immediate: true },
)

// 监听 tab 切换，首次切换到 tab 时加载数据
let reportsLoaded = false
let warningsLoaded = false
watch(activeTab, (tab) => {
  if (tab === 'reports' && !reportsLoaded) {
    reportsLoaded = true
    fetchReports(1)
  }
  if (tab === 'warnings' && !warningsLoaded) {
    warningsLoaded = true
    fetchWarnings(1)
  }
}, { immediate: true })
</script>

<style scoped>
.my-reports-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.report-card,
.warning-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.2s;
}
.report-card:hover,
.warning-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.report-card-header,
.warning-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.report-card-body {
  margin-bottom: 10px;
}

.report-card-reason {
  font-size: 14px;
  margin-bottom: 6px;
}

.report-card-desc {
  font-size: 13px;
  color: #888;
}

.report-card-footer,
.warning-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.report-card-time,
.warning-card-time {
  font-size: 12px;
  color: #aaa;
}

.warning-card-body p {
  margin: 0;
  font-size: 14px;
  color: #555;
  flex: 1;
  min-width: 0;
}

.load-more {
  text-align: center;
  padding: 16px;
}

.report-detail {
  padding: 8px 0;
}

/* 高亮闪烁动画 */
.report-card.highlight-flash {
  animation: reportHighlightFade 2s ease-out;
  border-radius: 8px;
}

@keyframes reportHighlightFade {
  0% {
    background-color: #fff566;
    box-shadow: 0 0 12px rgba(255, 245, 102, 0.6);
  }
  100% {
    background-color: #fff;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  }
}

.warning-card-body {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
