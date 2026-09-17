<script setup lang="ts">
/**
 * 超标预警卡（F26）：超标 chips + 按超标项动态微调建议 + 免责注；无超标不渲染
 * 图标经 data-uri <image> 渲染，H5/mp-weixin 一致
 * 作者: wanglx
 */
import { computed } from 'vue'
import { buildAdviceList, buildProgressItems } from '@/utils/macro'
import { OVER_ADVICE } from '@/config/constants'
import type { DietSummaryVO } from '@/api/diet'
import { iconSrc } from '@/utils/icons'

const props = defineProps<{ summary: DietSummaryVO | null }>()

const items = computed(() => buildProgressItems(props.summary))
/** 超标项（chips 展示） */
const overItems = computed(() => items.value.filter(i => i.overAmount > 0))
/** 微调建议列表 */
const adviceList = computed(() => buildAdviceList(items.value))

const alertIcon = iconSrc('alert', '#d63333')
const infoIcon = iconSrc('info', '#8a8f99')
const bulletIcon = iconSrc('sparkles', '#d63333')
</script>

<template>
  <view class="over-card">
    <view class="over-title-row">
      <image class="over-title-icon" :src="alertIcon" />
      <text class="over-title">今日摄入超标</text>
    </view>
    <view class="chips">
      <text v-for="item in overItems" :key="item.key" class="chip">
        {{ item.label }}已超标 {{ item.overAmount }}{{ item.unit }}
      </text>
    </view>
    <view class="advice-box">
      <view v-for="(advice, i) in adviceList" :key="i" class="advice-item">
        <image class="advice-bullet" :src="bulletIcon" />
        <text class="advice-text">{{ advice }}</text>
      </view>
      <view class="advice-disclaimer-row">
        <image class="advice-disclaimer-icon" :src="infoIcon" />
        <text class="advice-disclaimer">{{ OVER_ADVICE.disclaimer }}</text>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.over-card {
  background: $zhenxinjian-danger-bg;
  border: 1rpx solid $zhenxinjian-danger-border;
  border-radius: $zhenxinjian-radius-lg;
  padding: 32rpx;
  margin-bottom: 24rpx;
}

.over-title-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-bottom: 20rpx;
}

.over-title-icon {
  width: 32rpx;
  height: 32rpx;
}

.over-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $zhenxinjian-danger-deep;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.chip {
  padding: 8rpx 20rpx;
  border-radius: $zhenxinjian-radius-md;
  background: $zhenxinjian-danger-border;
  color: $zhenxinjian-danger-deep;
  font-size: 22rpx;
  font-weight: 600;
}

.advice-box {
  background: $zhenxinjian-white;
  border-radius: $zhenxinjian-radius-md;
  padding: 20rpx;
}

.advice-item {
  display: flex;
  align-items: flex-start;
  gap: 10rpx;
  margin-bottom: 12rpx;
}

.advice-item:last-of-type {
  margin-bottom: 0;
}

.advice-bullet {
  width: 26rpx;
  height: 26rpx;
  margin-top: 4rpx;
  flex-shrink: 0;
}

.advice-text {
  flex: 1;
  font-size: 24rpx;
  color: $zhenxinjian-text;
  line-height: 1.8;
}

.advice-disclaimer-row {
  display: flex;
  align-items: flex-start;
  gap: 8rpx;
  margin-top: 16rpx;
}

.advice-disclaimer-icon {
  width: 24rpx;
  height: 24rpx;
  margin-top: 2rpx;
  flex-shrink: 0;
}

.advice-disclaimer {
  flex: 1;
  font-size: 20rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.5;
}
</style>
