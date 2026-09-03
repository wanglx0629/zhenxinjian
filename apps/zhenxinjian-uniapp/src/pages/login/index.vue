<script setup lang="ts">
/**
 * 登录页
 * 作者: luote (luote) - https://luote996.cn
 */
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import TeLogo from '@/components/TeLogo.vue'
import CaptchaInput from '@/components/CaptchaInput.vue'
import { useUserStore } from '@/store/user'
import { canSubmit } from '@/utils/throttle'
import { getToken } from '@/utils/storage'

const userStore = useUserStore()
const form = ref({
  username: '',
  password: '',
  captcha: '',
  captchaUuid: ''
})
const loading = ref(false)
const captchaRef = ref<InstanceType<typeof CaptchaInput>>()

onShow(() => {
  if (getToken()) {
    uni.switchTab({ url: '/pages/home/index' })
  }
})


async function handleLogin() {
  if (loading.value) {
    return
  }
  if (!canSubmit()) {
    uni.showToast({ title: '操作过于频繁，请稍后再试', icon: 'none' })
    return
  }
  if (!form.value.username || !form.value.password || !form.value.captcha) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' })
    return
  }
  loading.value = true
  try {
    await userStore.login(form.value)
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/home/index' })
    }, 400)
  } catch {
    captchaRef.value?.loadCaptcha()
  } finally {
    loading.value = false
  }
}

function goRegister() {
  uni.navigateTo({ url: '/pages/register/index' })
}
</script>

<template>
  <view class="auth-page">
    <view class="auth-card">
      <view class="auth-header">
        <TeLogo :size="80" />
        <text class="title">登录 zhenxinjian</text>
        <text class="subtitle">luote996.cn</text>
      </view>

      <view class="field">
        <input
          v-model="form.username"
          class="input"
          placeholder="用户名"
          placeholder-class="placeholder"
        />
      </view>
      <view class="field">
        <input
          v-model="form.password"
          class="input"
          password
          placeholder="密码"
          placeholder-class="placeholder"
        />
      </view>
      <view class="field">
        <CaptchaInput
          ref="captchaRef"
          v-model="form.captcha"
          v-model:uuid="form.captchaUuid"
        />
      </view>

      <button class="submit" :loading="loading" :disabled="loading" @click="handleLogin">登录</button>
      <view class="footer">
        <text class="muted">还没有账号？</text>
        <text class="link" @click="goRegister">立即注册</text>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48rpx;
  box-sizing: border-box;
  background: linear-gradient(135deg, #ecf5ff 0%, #f5f7fa 50%, #ffffff 100%);
}

.auth-card {
  width: 100%;
  max-width: 680rpx;
  padding: 56rpx 40rpx 48rpx;
  background: $zhenxinjian-white;
  border-radius: 24rpx;
  box-shadow: 0 8rpx 40rpx rgba(64, 158, 255, 0.12);
}

.auth-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 48rpx;
}

.title {
  margin-top: 24rpx;
  font-size: 40rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.subtitle {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
}

.field {
  margin-bottom: 28rpx;
}

.input {
  width: 100%;
  height: 88rpx;
  padding: 0 28rpx;
  box-sizing: border-box;
  background: $zhenxinjian-white;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: $zhenxinjian-text;
}

.placeholder {
  color: $zhenxinjian-text-secondary;
}

.submit {
  margin-top: 12rpx;
  height: 88rpx;
  line-height: 88rpx;
  background: $zhenxinjian-primary;
  color: #fff;
  border-radius: 12rpx;
  font-size: 30rpx;
  border: none;
}

.submit[disabled] {
  opacity: 0.7;
}

.footer {
  margin-top: 32rpx;
  text-align: center;
  font-size: 26rpx;
}

.muted {
  color: $zhenxinjian-text-secondary;
}

.link {
  color: $zhenxinjian-primary;
  margin-left: 8rpx;
}
</style>
