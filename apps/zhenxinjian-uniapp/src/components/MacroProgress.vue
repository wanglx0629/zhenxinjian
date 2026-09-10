<script setup lang="ts">
/**
 * 三宏 + 总热量三色进度（F09/F26，口径与 P12 记录页一致）
 * 作者: wanglx
 */
import { computed } from 'vue'
import { buildProgressItems } from '@/utils/macro'
import type { DietSummaryVO } from '@/api/diet'

const props = defineProps<{ summary: DietSummaryVO | null }>()

const items = computed(() => buildProgressItems(props.summary))
</script>

<template>
  <view v-if="items.length" class="macro-progress">
    <view v-for="item in items" :key="item.key" class="progress-row">
      <view class="progress-info">
        <text class="progress-label">{{ item.label }}</text>
        <text class="progress-value">{{ item.actual }}/{{ item.target }}{{ item.unit }}</text>
      </view>
      <view class="progress-bar">
        <view class="progress-fill" :style="{ width: item.displayRate + '%', background: item.color }"></view>
      </view>
      <view class="progress-status">
        <text v-if="item.overAmount > 0" class="over-text" :style="{ color: item.color }">已超标 {{ item.overAmount }}{{ item.unit }}</text>
        <text v-else class="rate-text" :style="{ color: item.color }">{{ Math.round(item.rate) }}%</text>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.progress-row {
  margin-bottom: 24rpx;
}

.progress-row:last-child {
  margin-bottom: 0;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8rpx;
}

.progress-label {
  font-size: 26rpx;
  color: $zhenxinjian-text;
}

.progress-value {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
}

.progress-bar {
  height: 16rpx;
  background: #f0f0f0;
  border-radius: 8rpx;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 8rpx;
  transition: width 0.3s;
}

.progress-status {
  margin-top: 8rpx;
  display: flex;
  justify-content: flex-end;
}

.rate-text {
  font-size: 22rpx;
}

.over-text {
  font-size: 22rpx;
  font-weight: 600;
}
</style>
