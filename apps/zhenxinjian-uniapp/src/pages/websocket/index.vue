<script setup lang="ts">
/**
 * WebSocket 房间页
 * 作者: luote (luote) - https://luote996.cn
 */
import { onShow } from '@dcloudio/uni-app'
import WsBoard from '@/components/WsBoard.vue'
import { getToken } from '@/utils/storage'

onShow(() => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
  }
})
</script>

<template>
  <view class="page">
    <WsBoard />
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
  box-sizing: border-box;
  background: $zhenxinjian-bg;
}
</style>
