<script setup lang="ts">
/**
 * P15 游客到期强制授权页：全屏不可关闭
 * navigationStyle: custom + onBackPress 拦截物理/手势返回（spec：到期强制授权拦截）
 * 作者: wanglx
 */
import { computed, ref } from 'vue'
import { onBackPress, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/user'
import { canSubmit } from '@/utils/throttle'
import { trackPage } from '@/utils/track'

const userStore = useUserStore()
const loading = ref(false)

/** chooseAvatar 返回的本地临时头像路径 */
const avatarPath = ref('')
/** nickname input 采集的昵称 */
const nickname = ref('')

/** 是否可发起授权登录（必须头像 + 昵称都已采集） */
const canLogin = computed(() => !!avatarPath.value && !!nickname.value.trim())

// 拦截物理返回 / 手势返回，不允许回到功能页面
onBackPress(() => true)

onShow(() => {
  trackPage('pages/auth/expire')
})

/** 微信 chooseAvatar 回调：e.detail.avatarUrl 为本地临时路径 */
function onChooseAvatar(e: { detail: { avatarUrl: string } }) {
  avatarPath.value = e.detail.avatarUrl
}

/** 立即授权（先采集昵称头像，再 uni.login → uploadFile 一次请求落库并触发游客迁移） */
async function handleAuth() {
  if (loading.value) return
  if (!canLogin.value) {
    uni.showToast({ title: '请先选头像、填昵称', icon: 'none' })
    return
  }
  if (!canSubmit()) {
    uni.showToast({ title: '操作过于频繁，请稍后再试', icon: 'none' })
    return
  }
  loading.value = true
  try {
    await userStore.loginByWechat(
      {
        nickname: nickname.value.trim(),
        avatarPath: avatarPath.value
      },
      { successText: '授权成功，数据已保留', failText: '授权失败，请重试' }
    )
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

      <view class="profile">
        <!-- 微信头像选择（open-type="chooseAvatar" 触发原生选择器） -->
        <button class="avatar-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
          <image v-if="avatarPath" class="avatar-img" :src="avatarPath" mode="aspectFill" />
          <view v-else class="avatar-placeholder">
            <text class="avatar-emoji">😊</text>
          </view>
        </button>
        <text class="profile-tip">{{ avatarPath ? '头像已选好啦，点可换' : '👆 点击选择头像' }}</text>

        <!-- 微信昵称填写（type="nickname" 聚焦拉起带微信昵称建议的键盘） -->
        <input
          v-model="nickname"
          class="nickname-input"
          type="nickname"
          placeholder="点击输入昵称"
          placeholder-class="nickname-placeholder"
          maxlength="32"
        />
      </view>

      <button
        class="btn-primary"
        :class="{ 'btn-disabled': !canLogin }"
        :loading="loading"
        :disabled="!canLogin || loading"
        @click="handleAuth"
      >
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
  margin-bottom: 40rpx;
}

/* 头像 + 昵称采集区 */
.profile {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 40rpx;
}

.avatar-btn {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  padding: 0;
  margin: 0;
  background: transparent;
  border: 4rpx dashed $zhenxinjian-border;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;

  &::after {
    border: none;
  }
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  background: $zhenxinjian-bg;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-emoji {
  font-size: 56rpx;
}

.profile-tip {
  margin-top: 16rpx;
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
}

.nickname-input {
  margin-top: 24rpx;
  width: 100%;
  height: 88rpx;
  padding: 0 32rpx;
  box-sizing: border-box;
  background: $zhenxinjian-bg;
  border: 2rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  font-size: 30rpx;
  color: $zhenxinjian-text;
}

.nickname-placeholder {
  color: $zhenxinjian-text-secondary;
}

.btn-disabled {
  background: $zhenxinjian-border !important;
  color: $zhenxinjian-text-secondary !important;
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
