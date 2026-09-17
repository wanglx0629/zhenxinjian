<script setup lang="ts">
/**
 * P01 登录引导页：微信授权登录 / 先体验 3 天（游客）
 * 对应 PRD P01；游客逻辑见 specs/auth/guest-mode
 * 作者: wanglx
 */
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/user'
import { getToken } from '@/utils/storage'
import { canSubmit } from '@/utils/throttle'
import { track, trackPage } from '@/utils/track'
import { TRACK_EVENT } from '@/config/track-events'
import { iconSrc } from '@/utils/icons'

const avatarPlaceholderIcon = iconSrc('body', '#94A3B8')

const userStore = useUserStore()
const loading = ref(false)

/** chooseAvatar 返回的本地临时头像路径 */
const avatarPath = ref('')
/** nickname input 采集的昵称 */
const nickname = ref('')

/** 是否可发起授权登录（必须头像 + 昵称都已采集） */
const canLogin = computed(() => !!avatarPath.value && !!nickname.value.trim())

onShow(async () => {
  trackPage('pages/auth/guide')
  if (!getToken()) return
  // 已有登录态时：仅确认非游客（正式用户）才直接进首页；
  // 游客需停留本页完成微信授权，不能因持有游客 token 被弹走
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      // 拉取失败无法判定身份，停留本页
      return
    }
  }
  if (!userStore.isGuest) {
    uni.switchTab({ url: '/pages/home/index' })
  }
})

/** 微信 chooseAvatar 回调：e.detail.avatarUrl 为本地临时路径（wxfile:// 或 http://tmp/） */
function onChooseAvatar(e: { detail: { avatarUrl: string } }) {
  avatarPath.value = e.detail.avatarUrl
}

/** chooseAvatar 失败兜底：多为隐私授权被拒，重新拉起官方授权弹窗 */
function onChooseAvatarFail() {
  // #ifdef MP-WEIXIN
  if (typeof uni.requirePrivacyAuthorize === 'function') {
    uni.requirePrivacyAuthorize({})
  }
  // #endif
  uni.showToast({ title: '请先同意隐私协议后再选择头像', icon: 'none' })
}

/** 微信一键授权登录（先采集昵称头像，再 uni.login → uploadFile 一次请求落库） */
async function handleWechatLogin() {
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
    await userStore.loginByWechat({
      nickname: nickname.value.trim(),
      avatarPath: avatarPath.value
    })
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

    <view class="profile">
      <!-- 微信头像选择（open-type="chooseAvatar" 触发原生选择器） -->
      <button
        class="avatar-btn"
        open-type="chooseAvatar"
        @chooseavatar="onChooseAvatar"
        @error="onChooseAvatarFail"
      >
        <image v-if="avatarPath" class="avatar-img" :src="avatarPath" mode="aspectFill" />
        <view v-else class="avatar-placeholder">
          <image class="avatar-placeholder-icon" :src="avatarPlaceholderIcon" />
        </view>
      </button>
      <text class="profile-tip">{{ avatarPath ? '头像已选好啦，点可换' : '点击选择头像' }}</text>

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

    <view class="actions">
      <button
        class="btn-primary"
        :class="{ 'btn-disabled': !canLogin }"
        :loading="loading"
        :disabled="!canLogin || loading"
        @click="handleWechatLogin"
      >
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
  margin-bottom: 64rpx;
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

/* 头像 + 昵称采集区 */
.profile {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 56rpx;
}

.avatar-btn {
  width: 144rpx;
  height: 144rpx;
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
  background: $zhenxinjian-white;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-placeholder-icon {
  width: 64rpx;
  height: 64rpx;
}

.profile-tip {
  margin-top: 16rpx;
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
}

.nickname-input {
  margin-top: 32rpx;
  width: 100%;
  height: 88rpx;
  padding: 0 32rpx;
  box-sizing: border-box;
  background: $zhenxinjian-white;
  border: 2rpx solid $zhenxinjian-border;
  border-radius: $zhenxinjian-radius-md;
  font-size: 30rpx;
  color: $zhenxinjian-text;
}

.nickname-placeholder {
  color: $zhenxinjian-text-secondary;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

/* B-T29 漂移覆盖：无 width:100%（flex 布局自动撑满） */
.btn-primary {
  height: 88rpx;
  line-height: 88rpx;
  background: $zhenxinjian-primary;
  color: $zhenxinjian-white;
  border-radius: $zhenxinjian-radius-md;
  font-size: 30rpx;
}

.btn-disabled {
  background: $zhenxinjian-border;
  color: $zhenxinjian-text-secondary;
}

.btn-ghost {
  height: 88rpx;
  line-height: 88rpx;
  background: $zhenxinjian-white;
  color: $zhenxinjian-primary;
  border: 1rpx solid $zhenxinjian-primary;
  border-radius: $zhenxinjian-radius-md;
  font-size: 30rpx;
}

.tip {
  text-align: center;
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
}
</style>
