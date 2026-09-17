<script setup lang="ts">
/**
 * 通用空态组件：SVG 插画（data-uri <image> 渲染）+ 标题 + 描述 + 可选主 CTA
 * 背景：mp-weixin 不支持内联 <svg>；插画整体编码为 data-uri，H5/小程序一致
 * 作者: wanglx
 */
import { iconSrc } from '@/utils/icons'

const props = withDefaults(
  defineProps<{
    /** 插画类型：bowl 餐食 / calendar 周期 / search 搜索 / body 身体数据 */
    type?: 'bowl' | 'calendar' | 'search' | 'body'
    title: string
    desc?: string
    btnText?: string
  }>(),
  { type: 'bowl' }
)

const emit = defineEmits<{
  (e: 'action'): void
}>()

/** 多色插画 SVG（120 viewBox），整体 data-uri 后由 <image> 渲染 */
const ILLUSTRATIONS: Record<string, string> = {
  bowl:
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120" fill="none">' +
    '<circle cx="60" cy="60" r="52" fill="#F4F8F6"/>' +
    '<path d="M30 62h60c0 14-10 24-22 26v6H52v-6c-12-2-22-12-22-26z" fill="#E3EFE9"/>' +
    '<path d="M30 62h60" stroke="#00AC7C" stroke-width="3" stroke-linecap="round"/>' +
    '<path d="M42 52c2-6 8-10 14-10M60 46c4-4 10-5 14-2" stroke="#33BD96" stroke-width="3" stroke-linecap="round"/>' +
    '<circle cx="48" cy="70" r="3" fill="#FFB020"/><circle cx="62" cy="74" r="3" fill="#FFB020"/>' +
    '<circle cx="74" cy="68" r="3" fill="#FFB020"/>' +
    '<path d="M50 96h20" stroke="#00AC7C" stroke-width="3" stroke-linecap="round"/></svg>',
  calendar:
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120" fill="none">' +
    '<circle cx="60" cy="60" r="52" fill="#F4F8F6"/>' +
    '<rect x="30" y="36" width="60" height="52" rx="8" fill="#fff" stroke="#00AC7C" stroke-width="3"/>' +
    '<path d="M30 50h60" stroke="#00AC7C" stroke-width="3"/>' +
    '<path d="M44 30v10M76 30v10" stroke="#00AC7C" stroke-width="4" stroke-linecap="round"/>' +
    '<rect x="40" y="58" width="12" height="10" rx="2" fill="#E3EFE9"/>' +
    '<rect x="56" y="58" width="12" height="10" rx="2" fill="#FFB020"/>' +
    '<rect x="72" y="58" width="12" height="10" rx="2" fill="#E3EFE9"/>' +
    '<rect x="40" y="72" width="12" height="10" rx="2" fill="#E3EFE9"/>' +
    '<rect x="56" y="72" width="12" height="10" rx="2" fill="#E3EFE9"/></svg>',
  search:
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120" fill="none">' +
    '<circle cx="60" cy="60" r="52" fill="#F4F8F6"/>' +
    '<circle cx="54" cy="54" r="20" fill="#fff" stroke="#00AC7C" stroke-width="4"/>' +
    '<path d="M69 69l14 14" stroke="#00AC7C" stroke-width="5" stroke-linecap="round"/>' +
    '<path d="M46 54h16M54 46v16" stroke="#E3EFE9" stroke-width="4" stroke-linecap="round"/>' +
    '<circle cx="88" cy="34" r="4" fill="#FFB020"/><circle cx="30" cy="84" r="3" fill="#33BD96"/></svg>',
  body:
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120" fill="none">' +
    '<circle cx="60" cy="60" r="52" fill="#F4F8F6"/>' +
    '<circle cx="60" cy="42" r="12" fill="#E3EFE9" stroke="#00AC7C" stroke-width="3"/>' +
    '<path d="M38 88c0-14 10-22 22-22s22 8 22 22" fill="#E3EFE9" stroke="#00AC7C" stroke-width="3"/>' +
    '<path d="M88 74l6 6 10-12" stroke="#FFB020" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/></svg>'
}

function illusSrc(type: string) {
  return `data:image/svg+xml;utf8,${encodeURIComponent(ILLUSTRATIONS[type] || ILLUSTRATIONS.bowl)}`
}

/** CTA 箭头（白底 token 色） */
const btnArrow = iconSrc('chevronRight', '#ffffff')
</script>

<template>
  <view class="empty-state">
    <image class="illus" :src="illusSrc(props.type)" />
    <text class="empty-title">{{ title }}</text>
    <text v-if="desc" class="empty-desc">{{ desc }}</text>
    <view v-if="btnText" class="empty-btn" hover-class="empty-btn-hover" @click="emit('action')">
      <text class="empty-btn-text">{{ btnText }}</text>
      <image class="empty-btn-arrow" :src="btnArrow" />
    </view>
  </view>
</template>

<style scoped lang="scss">
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 32rpx;
}

.illus {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 24rpx;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 12rpx;
}

.empty-desc {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
  text-align: center;
  line-height: 1.6;
  margin-bottom: 28rpx;
}

.empty-btn {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 14rpx 44rpx;
  background: $zhenxinjian-gradient-cta;
  border-radius: $zhenxinjian-radius-pill;
  box-shadow: 0 4rpx 12rpx rgba(255, 176, 32, 0.24);
}

.empty-btn-hover {
  background: $zhenxinjian-cta-active;
  transform: scale(0.97);
}

.empty-btn-text {
  color: $zhenxinjian-white;
  font-size: 26rpx;
  font-weight: 600;
}

.empty-btn-arrow {
  width: 24rpx;
  height: 24rpx;
}
</style>
