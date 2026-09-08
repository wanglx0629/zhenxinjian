<script setup lang="ts">
/**
 * 记录页（占位页，Change 5 实现：饮食记录列表）
 * 作者: wanglx
 */
</script>

<template>
  <view class="page">
    <view class="card">
      <text class="title">记录</text>
      <text class="desc">饮食记录功能开发中，敬请期待</text>
    </view>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
  box-sizing: border-box;
  background: $zhenxinjian-bg;
}

.card {
  background: $zhenxinjian-white;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 16rpx;
  padding: 48rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.title {
  font-size: 32rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 16rpx;
}

.desc {
  font-size: 26rpx;
  color: $zhenxinjian-text-secondary;
}
</style>
