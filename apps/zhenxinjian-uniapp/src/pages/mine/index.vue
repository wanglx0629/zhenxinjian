<script setup lang="ts">
/**
 * 我的页面：游客展示体验状态，正式用户展示账号信息
 * 作者: wanglx
 */
import { onShow } from '@dcloudio/uni-app'
import { computed } from 'vue'
import TeLogo from '@/components/TeLogo.vue'
import { useUserStore } from '@/store/user'
import { getToken } from '@/utils/storage'

const userStore = useUserStore()
const displayName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || '用户'
)

/** 游客体验期剩余天数（不足 1 天按 1 天计） */
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

/** 退出登录（正式用户走登出接口；游客直接清本地态） */
async function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确认退出登录？',
    success: async (res) => {
      if (res.confirm) {
        await userStore.logout()
      }
    }
  })
}
</script>

<template>
  <view class="page">
    <view class="profile card">
      <TeLogo :size="96" />
      <view class="info">
        <view class="name-row">
          <text class="name">{{ displayName }}</text>
          <text v-if="userStore.isGuest" class="badge">游客</text>
        </view>
        <text v-if="userStore.isGuest" class="meta">体验剩余 {{ guestDaysLeft ?? 0 }} 天</text>
        <text v-else class="meta">@{{ userStore.userInfo?.username || '-' }}</text>
      </view>
    </view>

    <view class="card list">
      <template v-if="!userStore.isGuest">
        <view class="row">
          <text class="label">邮箱</text>
          <text class="value">{{ userStore.userInfo?.email || '未填写' }}</text>
        </view>
        <view class="row">
          <text class="label">手机</text>
          <text class="value">{{ userStore.userInfo?.phone || '未填写' }}</text>
        </view>
      </template>
      <view v-else class="guest-tip">
        <text class="tip-text">
          体验期内可随时授权微信登录，数据将自动保留到你的账号。
        </text>
      </view>
      <view class="row">
        <text class="label">品牌</text>
        <text class="value">wanglx</text>
      </view>
    </view>

    <button class="logout" @click="handleLogout">退出登录</button>
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

.profile {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.info {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.name {
  font-size: 34rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.badge {
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  background: $zhenxinjian-primary-light;
  color: $zhenxinjian-primary;
  font-size: 22rpx;
}

.meta {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
}

.guest-tip {
  padding: 20rpx 0;
  border-bottom: 1rpx solid $zhenxinjian-border;
}

.tip-text {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.6;
}

.row {
  display: flex;
  justify-content: space-between;
  padding: 22rpx 0;
  border-bottom: 1rpx solid $zhenxinjian-border;
}

.row:last-child {
  border-bottom: none;
}

.label {
  color: $zhenxinjian-text-secondary;
  font-size: 26rpx;
}

.value {
  color: $zhenxinjian-text;
  font-size: 26rpx;
}

.logout {
  margin-top: 16rpx;
  height: 88rpx;
  line-height: 88rpx;
  background: $zhenxinjian-white;
  color: #f56c6c;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  font-size: 30rpx;
}
</style>
