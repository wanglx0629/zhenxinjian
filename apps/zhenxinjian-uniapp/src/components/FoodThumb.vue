<script setup lang="ts">
/**
 * 食物缩略图：有图显图、无图按分类色块占位（纯色块 + 品类简称，不新增美术资源，不出现裂图）
 * JS 侧色值取 V2 色板同族浅底/深字（SCSS 变量在 <script> 不可用，见 design-system §7）
 * 作者: wanglx
 */
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  /** 图片 URL（空则按分类占位） */
  image?: string
  /** 分类编号 01–10 */
  categoryCode?: string
  /** 尺寸：sm 88rpx 列表 / lg 128rpx 详情头图 */
  size?: 'sm' | 'lg'
}>(), {
  image: '',
  categoryCode: '',
  size: 'sm'
})

/** 分类占位配色（bg 浅色块 + text 深色字 + 品类简称） */
const CATEGORY_STYLE: Record<string, { bg: string; text: string; label: string }> = {
  '01': { bg: '#F3EAD9', text: '#7A4E1D', label: '主食' },
  '02': { bg: '#FDECEC', text: '#D63333', label: '肉类' },
  '03': { bg: '#FFF4D6', text: '#9A6B00', label: '蛋奶' },
  '04': { bg: '#E7F3F7', text: '#1F6E85', label: '水产' },
  '05': { bg: '#EAF6E9', text: '#3E7E3A', label: '豆类' },
  '06': { bg: '#E6F5EF', text: '#008A63', label: '蔬菜' },
  '07': { bg: '#E0F2EF', text: '#0F766E', label: '菌藻' },
  '08': { bg: '#FFF1E3', text: '#C2410C', label: '水果' },
  '09': { bg: '#F1EAE2', text: '#8A5A2B', label: '坚果' },
  '10': { bg: '#EDF1EF', text: '#475569', label: '油脂' }
}

/** 兜底（分类缺失/非法时） */
const DEFAULT_STYLE = { bg: '#EDF1EF', text: '#94A3B8', label: '食物' }

const placeholder = computed(() => CATEGORY_STYLE[props.categoryCode] ?? DEFAULT_STYLE)
</script>

<template>
  <image
    v-if="image"
    class="food-thumb"
    :class="size"
    :src="image"
    mode="aspectFill"
  />
  <view
    v-else
    class="food-thumb placeholder"
    :class="size"
    :style="{ background: placeholder.bg }"
  >
    <text class="thumb-text" :class="size" :style="{ color: placeholder.text }">
      {{ placeholder.label }}
    </text>
  </view>
</template>

<style scoped lang="scss">
.food-thumb {
  flex-shrink: 0;
  background: $zhenxinjian-bg;
  border-radius: $zhenxinjian-radius-md;

  &.sm {
    width: 88rpx;
    height: 88rpx;
  }

  &.lg {
    width: 128rpx;
    height: 128rpx;
    border-radius: $zhenxinjian-radius-lg;
  }
}

.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb-text {
  font-size: 22rpx;

  &.lg {
    font-size: 26rpx;
  }
}
</style>
