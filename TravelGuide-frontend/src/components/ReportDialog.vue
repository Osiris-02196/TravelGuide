<template>
  <a-modal
    v-model:open="visible"
    title="举报"
    :confirm-loading="submitting"
    ok-text="提交举报"
    cancel-text="取消"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-form layout="vertical">
      <a-form-item label="举报原因" required>
        <a-radio-group v-model:value="selectedReason" direction="vertical">
          <a-radio v-for="r in reasons" :key="r" :value="r">{{ r }}</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="详细说明（选填）">
        <a-textarea
          v-model:value="description"
          placeholder="请详细描述举报原因..."
          :rows="4"
          :maxlength="500"
          show-count
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { addReport } from '@/api/reportController'

const visible = ref(false)
const submitting = ref(false)
const selectedReason = ref('')
const description = ref('')

const reasons = ['色情低俗', '广告营销', '人身攻击', '违法违规', '虚假信息', '抄袭搬运', '其他']

let pendingTargetType = ''
let pendingTargetId: number | string | null = null
let pendingReportedUserId: number | string | null = null

function open(targetType: string, targetId: number | string, reportedUserId: number | string) {
  pendingTargetType = targetType
  pendingTargetId = targetId
  pendingReportedUserId = reportedUserId
  selectedReason.value = ''
  description.value = ''
  visible.value = true
}

async function handleSubmit() {
  if (!selectedReason.value) {
    message.warning('请选择举报原因')
    return
  }
  submitting.value = true
  try {
    const res = await addReport({
      targetType: pendingTargetType,
      targetId: pendingTargetId!,
      reportedUserId: pendingReportedUserId!,
      reason: selectedReason.value,
      description: description.value || undefined,
    })
    if (res.data?.code === 0) {
      message.success('举报提交成功')
      visible.value = false
    } else {
      message.error(res.data?.message || '举报提交失败')
    }
  } catch {
    message.error('举报提交失败')
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  visible.value = false
}

defineExpose({ open })
</script>
