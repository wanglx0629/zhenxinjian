<script setup lang="ts">
/**
 * Te Logo 组件
 * 作者: wanglx
 */
defineProps<{
  size?: number
}>()
</script>

<template>
  <view class="te-logo" :style="{ width: (size || 72) + 'rpx', height: (size || 72) + 'rpx' }">
    <text class="te-text">Te</text>
  </view>
</template>

<style scoped lang="scss">
.te-logo {
  background: linear-gradient(135deg, $zhenxinjian-primary, $zhenxinjian-primary-light);
  border-radius: $zhenxinjian-radius-lg;
  display: flex;
  align-items: center;
  justify-content: center;
}

.te-text {
  color: $zhenxinjian-white;
  font-size: 28rpx;
  font-weight: 700;
  letter-spacing: -2rpx;
}
</style>
