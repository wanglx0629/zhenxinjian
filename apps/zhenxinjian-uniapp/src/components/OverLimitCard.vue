<script setup lang="ts">
/**
 * 超标预警卡（F26）：超标 chips + 按超标项动态微调建议 + 免责注；无超标不渲染
 * 作者: wanglx
 */
import { computed } from 'vue'
import { buildAdviceList, buildProgressItems } from '@/utils/macro'
import { OVER_ADVICE } from '@/config/constants'
import type { DietSummaryVO } from '@/api/diet'

const props = defineProps<{ summary: DietSummaryVO | null }>()

const items = computed(() => buildProgressItems(props.summary))
/** 超标项（chips 展示） */
const overItems = computed(() => items.value.filter(i => i.overAmount > 0))
/** 微调建议列表 */
const adviceList = computed(() => buildAdviceList(items.value))
</script>

<template>
  <view v-if="overItems.length" class="over-card">
    <text class="over-title">⚠️ 今日摄入超标</text>
    <view class="chips">
      <text v-for="item in overItems" :key="item.key" class="chip">
        {{ item.label }}已超标 {{ item.overAmount }}{{ item.unit }}
      </text>
    </view>
    <view class="advice-box">
      <text v-for="(advice, i) in adviceList" :key="i" class="advice-text">{{ advice }}</text>
      <text class="advice-disclaimer">{{ OVER_ADVICE.disclaimer }}</text>
    </view>
  </view>
</template>

<style scoped lang="scss">
.over-card {
  background: #fff7f7;
  border: 1rpx solid #fbd5d5;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}

.over-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #f56c6c;
  margin-bottom: 20rpx;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.chip {
  padding: 8rpx 20rpx;
  border-radius: 12rpx;
  background: #fef0f0;
  color: #f56c6c;
  font-size: 22rpx;
  font-weight: 600;
}

.advice-box {
  background: #fff;
  border-radius: 12rpx;
  padding: 20rpx;
}

.advice-text {
  display: block;
  font-size: 24rpx;
  color: $zhenxinjian-text;
  line-height: 1.8;
  margin-bottom: 12rpx;
}

.advice-text:last-of-type {
  margin-bottom: 0;
}

.advice-disclaimer {
  display: block;
  font-size: 20rpx;
  color: $zhenxinjian-text-secondary;
  margin-top: 16rpx;
}
</style>
