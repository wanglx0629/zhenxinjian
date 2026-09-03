<script setup lang="ts">
/**
 * 我的页面
 * 作者: luote (luote) - https://luote996.cn
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
const roleText = computed(() => userStore.userInfo?.role || '-')

onShow(() => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  userStore.fetchUserInfo().catch(() => undefined)
})

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
        <text class="name">{{ displayName }}</text>
        <text class="meta">@{{ userStore.userInfo?.username || '-' }}</text>
        <text class="role">角色 {{ roleText }}</text>
      </view>
    </view>

    <view class="card list">
      <view class="row">
        <text class="label">邮箱</text>
        <text class="value">{{ userStore.userInfo?.email || '未填写' }}</text>
      </view>
      <view class="row">
        <text class="label">手机</text>
        <text class="value">{{ userStore.userInfo?.phone || '未填写' }}</text>
      </view>
      <view class="row">
        <text class="label">品牌</text>
        <text class="value">luote · luote996.cn</text>
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

.name {
  font-size: 34rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.meta,
.role {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
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
