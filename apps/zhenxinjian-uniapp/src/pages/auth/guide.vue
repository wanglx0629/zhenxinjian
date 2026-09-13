<script setup lang="ts">
/**
 * P01 登录引导页：微信授权登录 / 先体验 3 天（游客）
 * 对应 PRD P01；游客逻辑见 specs/auth/guest-mode
 * 作者: wanglx
 */
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/user'
import { getToken } from '@/utils/storage'
import { canSubmit } from '@/utils/throttle'
import { track, trackPage } from '@/utils/track'
import { TRACK_EVENT } from '@/config/track-events'

const userStore = useUserStore()
const loading = ref(false)

onShow(() => {
  trackPage('pages/auth/guide')
  // 已有登录态（游客或正式）直接进首页
  if (getToken()) {
    uni.switchTab({ url: '/pages/home/index' })
  }
})

/** 微信一键授权登录（B-T24：uni.login → code → wechatLogin 收敛 store，页面只管 loading 与节流） */
async function handleWechatLogin() {
  if (loading.value) return
  if (!canSubmit()) {
    uni.showToast({ title: '操作过于频繁，请稍后再试', icon: 'none' })
    return
  }
  loading.value = true
  try {
    await userStore.loginByWechat()
  } finally {
    loading.value = false
  }
}

/** 先体验 3 天（游客签发，spec：游客身份签发） */
async function handleGuest() {
  if (loading.value) return
  if (!canSubmit()) {
    uni.showToast({ title: '操作过于频繁，请稍后再试', icon: 'none' })
    return
  }
  loading.value = true
  try {
    await userStore.loginAsGuest()
    track(TRACK_EVENT.LOGIN_GUEST)
    uni.switchTab({ url: '/pages/home/index' })
  } catch {
    uni.showToast({ title: '进入失败，请重试', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="page">
    <view class="hero">
      <view class="logo">臻心减</view>
      <text class="slogan">生活化减脂，从今天开始</text>
    </view>

    <view class="actions">
      <button class="btn-primary" :loading="loading" @click="handleWechatLogin">
        微信一键登录
      </button>
      <button class="btn-ghost" :disabled="loading" @click="handleGuest">
        先体验 3 天
      </button>
      <text class="tip">体验期内可随时授权，数据自动保留</text>
    </view>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 0 64rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: $zhenxinjian-bg;
}

.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 120rpx;
}

.logo {
  width: 160rpx;
  height: 160rpx;
  border-radius: 40rpx;
  background: $zhenxinjian-primary;
  color: $zhenxinjian-white;
  font-size: 44rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32rpx;
}

.slogan {
  font-size: 28rpx;
  color: $zhenxinjian-text-secondary;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.btn-primary {
  height: 88rpx;
  line-height: 88rpx;
  background: $zhenxinjian-primary;
  color: $zhenxinjian-white;
  border-radius: 12rpx;
  font-size: 30rpx;
}

.btn-ghost {
  height: 88rpx;
  line-height: 88rpx;
  background: $zhenxinjian-white;
  color: $zhenxinjian-primary;
  border: 1rpx solid $zhenxinjian-primary;
  border-radius: 12rpx;
  font-size: 30rpx;
}

.tip {
  text-align: center;
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
}
</style>
