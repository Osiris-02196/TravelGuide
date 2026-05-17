<template>
  <div class="page-container">
    <h3 style="margin-bottom: 20px; font-size: 18px">举报审核</h3>

    <a-table
      :columns="columns"
      :data-source="dataList"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'targetType'">
          <a-tag :color="record.targetType === 'strategy' ? 'blue' : 'purple'">
            {{ record.targetType === 'strategy' ? '攻略' : '评论' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="getStatusColor(record.status)">
            {{ getStatusText(record.status) }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-button type="primary" size="small" @click="handleViewDetail(record)">
            查看详情
          </a-button>
        </template>
      </template>
    </a-table>

    <!-- 举报详情抽屉 -->
    <a-drawer
      v-model:open="drawerVisible"
      title="举报详情"
      width="520"
      :footer-style="{ textAlign: 'right' }"
    >
      <template v-if="detailItem">
        <a-descriptions :column="1" size="small" bordered>
          <a-descriptions-item label="举报人">
            {{ detailItem.reporterName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="被举报用户">
            {{ detailItem.reportedUserName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="举报类型">
            {{ detailItem.targetType === 'strategy' ? '攻略' : '评论' }}
          </a-descriptions-item>
          <a-descriptions-item label="举报原因">
            {{ detailItem.reason }}
          </a-descriptions-item>
          <a-descriptions-item label="举报详细说明">
            <span style="color: #1677ff">{{ detailItem.description || '无' }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="被举报内容">
            <div class="target-content-row">
              <span class="target-content-text" style="color: #d46b08">{{ detailItem.targetContent || '-' }}</span>
              <a-button
                v-if="detailItem.targetType === 'strategy' && detailItem.strategyId"
                type="link"
                size="small"
                @click="goToTarget(detailItem.strategyId)"
              >
                查看攻略 <RightOutlined />
              </a-button>
              <a-button
                v-else-if="detailItem.targetType === 'comment' && detailItem.strategyId"
                type="link"
                size="small"
                @click="goToTarget(detailItem.strategyId, detailItem.targetId)"
              >
                定位评论 <RightOutlined />
              </a-button>
            </div>
          </a-descriptions-item>
          <a-descriptions-item
            v-if="detailItem.targetType === 'comment' && detailItem.strategyTitle"
            label="所属攻略"
          >
            <a
              v-if="detailItem.strategyId"
              @click="goToTarget(detailItem.strategyId, detailItem.targetId)"
            >
              {{ detailItem.strategyTitle }}
            </a>
            <span v-else>{{ detailItem.strategyTitle }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="举报时间">
            {{ formatTime(detailItem.createTime) }}
          </a-descriptions-item>
        </a-descriptions>

        <!-- 审核操作区域 -->
        <div v-if="detailItem.status === 'pending'" class="review-section">
          <a-divider />
          <h4>审核操作</h4>
          <a-form layout="vertical">
            <a-form-item label="审核说明" required>
              <a-textarea
                v-model:value="reviewRemark"
                placeholder="请填写审核说明..."
                :rows="4"
                :maxlength="200"
              />
            </a-form-item>
            <a-form-item>
              <a-space>
                <a-button
                  type="primary"
                  danger
                  :loading="reviewing"
                  @click="handleApproveClick"
                >
                  举报成立
                </a-button>
                <a-button :loading="reviewing" @click="handleReview('rejected')">
                  举报驳回
                </a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </div>

        <!-- 已审核结果展示 -->
        <div v-else class="review-result">
          <a-divider />
          <a-descriptions :column="1" size="small" bordered>
            <a-descriptions-item label="审核结果">
              <a-tag :color="getStatusColor(detailItem.status)">
                {{ getStatusText(detailItem.status) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="审核说明">
              <span style="color: #1677ff">{{ detailItem.reviewRemark || '-' }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="审核人">
              {{ detailItem.reviewAdminName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="审核时间">
              {{ formatTime(detailItem.reviewTime) }}
            </a-descriptions-item>
          </a-descriptions>
        </div>
      </template>

      <template #footer>
        <a-button @click="drawerVisible = false">关闭</a-button>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { RightOutlined } from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { listPendingReports, getReportDetail, reviewReport } from '@/api/reportController'
import dayjs from 'dayjs'

const router = useRouter()

const columns = [
  { title: '举报人', dataIndex: 'reporterName', key: 'reporter', width: 120 },
  { title: '被举报用户', dataIndex: 'reportedUserName', key: 'reportedUser', width: 120 },
  { title: '内容类型', key: 'targetType', width: 90 },
  { title: '举报原因', dataIndex: 'reason', key: 'reason', width: 120 },
  { title: '举报时间', dataIndex: 'createTime', key: 'time', width: 160 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'actions', width: 120, fixed: 'right' as const },
]

const dataList = ref<API.ReportVO[]>([])
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

// 详情抽屉
const drawerVisible = ref(false)
const detailItem = ref<API.ReportVO | null>(null)
const reviewRemark = ref('')
const reviewing = ref(false)

onMounted(() => fetchData())

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

function formatTime(time?: string) {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

async function fetchData(page = 1) {
  loading.value = true
  try {
    const res = await listPendingReports({ pageNum: page, pageSize })
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

async function handleViewDetail(record: API.ReportVO) {
  if (!record.id) return
  try {
    const res = await getReportDetail({ id: record.id })
    if (res.data?.code === 0 && res.data?.data) {
      detailItem.value = res.data.data
    } else {
      detailItem.value = record
    }
  } catch {
    detailItem.value = record
  }
  reviewRemark.value = ''
  drawerVisible.value = true
}

function handleApproveClick() {
  if (!detailItem.value?.id) return
  if (!reviewRemark.value.trim()) {
    message.warning('请填写审核说明')
    return
  }
  // 如果是评论举报，弹出确认弹窗
  if (detailItem.value.targetType === 'comment') {
    Modal.confirm({
      title: '该评论违规，是否删除该评论？',
      content: '选择"是"将删除该评论并警告用户；选择"否，仅警告该用户"将仅发送警告通知。',
      okText: '是',
      cancelText: '否，仅警告该用户',
      onOk: () => handleReview('approved', true),
      onCancel: () => handleReview('approved', false),
    })
  } else {
    // 策略举报直接审核通过
    handleReview('approved', false)
  }
}

async function handleReview(status: 'approved' | 'rejected', deleteTarget?: boolean) {
  // 如果是从直接调用进入（非弹窗路径），先校验
  if (deleteTarget === undefined) {
    if (!reviewRemark.value.trim()) {
      message.warning('请填写审核说明')
      return
    }
    if (!detailItem.value?.id) return
  }
  reviewing.value = true
  try {
    const body: API.ReportReviewRequest = {
      status,
      reviewRemark: reviewRemark.value.trim(),
    }
    if (deleteTarget !== undefined) {
      body.deleteTarget = deleteTarget
    }
    const res = await reviewReport(
      { id: detailItem.value.id },
      body,
    )
    if (res.data?.code === 0) {
      message.success(status === 'approved' ? '举报成立' : '举报已驳回')
      // 刷新列表（后端返回所有举报，已审核的记录仍会显示）
      fetchData(currentPage.value)
      if (detailItem.value) {
        detailItem.value.status = status
        detailItem.value.reviewRemark = reviewRemark.value.trim()
      }
    } else {
      message.error(res.data?.message || '操作失败')
    }
  } catch {
    message.error('操作失败')
  } finally {
    reviewing.value = false
  }
}

function goToStrategy(strategyId: string | number, commentId?: string | number) {
  const query: Record<string, string> = {}
  if (commentId !== undefined) {
    query.commentId = String(commentId)
  }
  router.push({
    name: 'strategyDetail',
    params: { id: String(strategyId) },
    query,
  })
}

function goToTarget(strategyId?: string | number, commentId?: string | number) {
  if (!strategyId) return
  goToStrategy(strategyId, commentId)
}
</script>

<style scoped>
.page-container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px;
}

.review-section {
  margin-top: 16px;
}

.target-content-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.target-content-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-section h4 {
  margin-bottom: 12px;
}

.review-result {
  margin-top: 16px;
}
</style>
