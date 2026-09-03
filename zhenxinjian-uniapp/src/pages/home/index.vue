<script setup lang="ts">
/**
 * 首页看板：欢迎区 + WebSocket 连通状态
 * 作者: luote (luote) - https://luote996.cn
 */
import { onShow } from '@dcloudio/uni-app'
import { computed } from 'vue'
import { useUserStore } from '@/store/user'
import WsStatusCard from '@/components/WsStatusCard.vue'
import { getToken } from '@/utils/storage'

const userStore = useUserStore()
const displayName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || '用户'
)

onShow(() => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  userStore.fetchUserInfo().catch(() => undefined)
})
</script>

<template>
  <view class="page">
    <view class="welcome card">
      <text class="eyebrow">控制台</text>
      <text class="hello">你好，{{ displayName }}</text>
      <text class="desc">zhenxinjian 全栈脚手架已就绪。首页展示服务状态，实时聊天请使用消息页。</text>
    </view>

    <view class="card chips-card">
      <text class="panel-title">能力速览</text>
      <view class="chips">
        <text class="chip">JWT 鉴权</text>
        <text class="chip">图形验证码</text>
        <text class="chip">Token 续期</text>
        <text class="chip">WebSocket</text>
      </view>
    </view>

    <WsStatusCard />
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
  padding: 28rpx;
  margin-bottom: 24rpx;
}

.chips-card {
  margin-bottom: 24rpx;
}

.eyebrow {
  display: block;
  font-size: 22rpx;
  letter-spacing: 0.08em;
  color: $zhenxinjian-primary;
  margin-bottom: 12rpx;
}

.hello {
  display: block;
  font-size: 36rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 12rpx;
}

.desc {
  font-size: 26rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.6;
}

.panel-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 8rpx;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 16rpx;
}

.chip {
  padding: 10rpx 20rpx;
  border-radius: 12rpx;
  background: $zhenxinjian-primary-light;
  color: $zhenxinjian-primary;
  font-size: 24rpx;
}
</style>
