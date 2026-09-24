<script setup lang="ts">
/**
 * P16/F15 模式切换确认弹窗：周期日期、第 N/总天数、已消耗/总量碳水、不可恢复警示
 * 「确认切换并重启周期 / 取消，继续当前周期」；确认由调用方调既有切换接口，取消无副作用
 * 作者: wanglx
 */
import { computed } from 'vue'
import type { CyclePlanVO } from '@/api/cycle'

const props = defineProps<{
  /** 是否显示 */
  visible: boolean
  /** 当前进行中周期摘要（F15 明细展示；空时字段兜底 '-'） */
  plan?: CyclePlanVO | null
}>()

const emit = defineEmits<{
  /** 确认切换（调用方执行既有 PUT /api/body/mode） */
  (e: 'confirm'): void
  /** 取消（关闭弹窗，周期不变） */
  (e: 'cancel'): void
}>()

/** 周期日期（起止完整；缺失兜底 '-'） */
const dateRange = computed(() => {
  const p = props.plan
  if (!p?.startDate || !p?.endDate) return '-'
  return `${p.startDate} 至 ${p.endDate}`
})

/** 第 N/总天数（今日不在周期内仅展示总天数） */
const dayProgress = computed(() => {
  const p = props.plan
  if (!p?.totalDays) return '-'
  return p.dayIndex ? `第 ${p.dayIndex}/${p.totalDays} 天` : `共 ${p.totalDays} 天`
})

/** 已消耗/总量碳水 g */
const carbProgress = computed(() => {
  const p = props.plan
  const consumed = p?.consumedCarb ?? null
  const total = p?.carbPoolTotal ?? null
  if (consumed === null && total === null) return '-'
  return `${consumed ?? 0}g / ${total ?? 0}g`
})

function onConfirm() {
  emit('confirm')
}

function onCancel() {
  emit('cancel')
}
</script>

<template>
  <view v-if="visible" class="modal-mask" @click="onCancel">
    <view class="modal" @click.stop>
      <text class="modal-title">切换为 532 模式？</text>
      <!-- F15 周期明细 -->
      <view class="summary">
        <view class="summary-row">
          <text class="summary-label">当前周期</text>
          <text class="summary-value">{{ dateRange }}</text>
        </view>
        <view class="summary-row">
          <text class="summary-label">进行至</text>
          <text class="summary-value">{{ dayProgress }}</text>
        </view>
        <view class="summary-row">
          <text class="summary-label">已消耗碳水</text>
          <text class="summary-value">{{ carbProgress }}</text>
        </view>
      </view>
      <text class="modal-warn">当前周期将立即终止，当期进度清空且不可恢复，切换后按 532 模式重新开始。</text>
      <view class="modal-actions">
        <view class="modal-btn cancel" @click="onCancel">取消，继续当前周期</view>
        <view class="modal-btn confirm" @click="onConfirm">确认切换并重启周期</view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal {
  width: 600rpx;
  background: $zhenxinjian-white;
  border-radius: $zhenxinjian-radius-lg;
  box-shadow: $zhenxinjian-shadow-pop;
  padding: 48rpx 32rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 24rpx;
}

/* F15 周期明细 */
.summary {
  width: 100%;
  background: $zhenxinjian-bg;
  border-radius: $zhenxinjian-radius-md;
  padding: 20rpx 24rpx;
  margin-bottom: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary-label {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
}

.summary-value {
  font-size: 24rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.modal-warn {
  font-size: 24rpx;
  color: $zhenxinjian-danger;
  line-height: 1.6;
  margin-bottom: 32rpx;
  text-align: center;
}

.modal-actions {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  width: 100%;
}

.modal-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border-radius: $zhenxinjian-radius-md;
  font-size: 28rpx;
  box-sizing: border-box;
}

.modal-btn.cancel {
  border: 1rpx solid $zhenxinjian-border;
  color: $zhenxinjian-text;
}

.modal-btn.confirm {
  background: $zhenxinjian-danger;
  color: $zhenxinjian-white;
}
</style>
