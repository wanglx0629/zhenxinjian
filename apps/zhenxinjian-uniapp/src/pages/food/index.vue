<script setup lang="ts">
/**
 * 食物库页（占位页，Change 4 实现：食物搜索与详情）
 * 作者: wanglx
 */
</script>

<template>
  <view class="page">
    <view class="card">
      <text class="title">食物库</text>
      <text class="desc">食物库功能开发中，敬请期待</text>
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
