<script setup lang="ts">
/**
 * P15 游客到期强制授权页：全屏不可关闭
 * navigationStyle: custom + onBackPress 拦截物理/手势返回（spec：到期强制授权拦截）
 * 作者: wanglx
 */
import { ref } from 'vue'
import { onBackPress, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/user'
import { canSubmit } from '@/utils/throttle'
import { track, trackPage } from '@/utils/track'

const userStore = useUserStore()
const loading = ref(false)

// 拦截物理返回 / 手势返回，不允许回到功能页面
onBackPress(() => true)

onShow(() => {
  trackPage('pages/auth/expire')
})

/** 立即授权：wx.login() 取新 code，携 guestKey 完成迁移（spec：游客期内登录并迁移） */
async function handleAuth() {
  if (loading.value) return
  if (!canSubmit()) {
    uni.showToast({ title: '操作过于频繁，请稍后再试', icon: 'none' })
    return
  }
  loading.value = true
  try {
    // 注意：部分平台 Promise 形式返回 [err, res] 数组，需兼容取值
    const result: unknown = await uni.login({ provider: 'weixin' })
    const loginRes = (Array.isArray(result) ? result[1] : result) as { code?: string }
    const code = loginRes?.code
    if (!code) {
      track('login_fail')
      uni.showToast({ title: '获取登录凭证失败，请重试', icon: 'none' })
      return
    }
    await userStore.loginByWechat(code)
    track('login_wechat')
    uni.showToast({ title: '授权成功，数据已保留', icon: 'success' })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/home/index' })
    }, 400)
  } catch {
    track('login_fail')
    uni.showToast({ title: '授权失败，请重试', icon: 'none' })
  } finally {
    loading.value = false
  }
}

/** 放弃数据退出：清本地游客登录态回引导页（服务端数据保留至 7 天清空期限） */
function handleAbandon() {
  uni.showModal({
    title: '放弃数据',
    content: '放弃后本地登录态将被清除，7 天内重新授权仍可找回数据。确认放弃？',
    confirmText: '放弃数据',
    cancelText: '再想想',
    success: (res) => {
      if (res.confirm) {
        userStore.abandonGuest()
      }
    }
  })
}
</script>

<template>
  <view class="page">
    <view class="panel">
      <text class="icon">&#9200;</text>
      <text class="title">体验期已结束</text>
      <text class="desc">你的 3 天免费体验已到期。授权微信登录即可继续使用，体验期内的数据将自动保留到你的账号。</text>

      <button class="btn-primary" :loading="loading" @click="handleAuth">
        立即授权，继续使用
      </button>
      <button class="btn-ghost" :disabled="loading" @click="handleAbandon">
        放弃数据，退出
      </button>
    </view>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 0 64rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  background: $zhenxinjian-bg;
}

.panel {
  width: 100%;
  background: $zhenxinjian-white;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 16rpx;
  padding: 64rpx 48rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.icon {
  font-size: 96rpx;
  margin-bottom: 24rpx;
}

.title {
  font-size: 36rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 20rpx;
}

.desc {
  font-size: 26rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.6;
  margin-bottom: 48rpx;
}

.btn-primary {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: $zhenxinjian-primary;
  color: $zhenxinjian-white;
  border-radius: 12rpx;
  font-size: 30rpx;
}

.btn-ghost {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  margin-top: 24rpx;
  margin-bottom: 0;
  background: $zhenxinjian-white;
  color: #f56c6c;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  font-size: 30rpx;
}
</style>
