<script setup lang="ts">
/**
 * 首页看板：欢迎区（区分游客/正式用户）
 * 作者: wanglx
 */
import { onShow } from '@dcloudio/uni-app'
import { computed } from 'vue'
import { useUserStore } from '@/store/user'
import { getToken } from '@/utils/storage'

const userStore = useUserStore()
const displayName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || '用户'
)

/** 游客体验期剩余天数（不足 1 天按 1 天计，负数视为已到期） */
const guestDaysLeft = computed(() => {
  const expireAt = userStore.userInfo?.guestExpireAt
  if (!expireAt || !userStore.isGuest) return null
  const diffMs = new Date(expireAt).getTime() - Date.now()
  if (diffMs <= 0) return 0
  return Math.max(1, Math.ceil(diffMs / 86400000))
})

onShow(() => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/auth/guide' })
    return
  }
  userStore.fetchUserInfo().catch(() => undefined)
})
</script>

<template>
  <view class="page">
    <view class="welcome card">
      <text class="eyebrow">臻心减</text>
      <text class="hello">你好，{{ displayName }}</text>
      <text v-if="userStore.isGuest" class="desc">
        游客体验中，剩余 {{ guestDaysLeft ?? 0 }} 天。授权微信登录后可继续使用并保留数据。
      </text>
      <text v-else class="desc">生活化减脂，从今天开始。饮食记录与食物库功能开发中。</text>
    </view>

    <view class="card chips-card">
      <text class="panel-title">能力速览</text>
      <view class="chips">
        <text class="chip">微信授权登录</text>
        <text class="chip">游客 3 天体验</text>
        <text class="chip">JWT 鉴权</text>
        <text class="chip">Token 续期</text>
      </view>
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
