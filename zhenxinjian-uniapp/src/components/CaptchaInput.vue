<script setup lang="ts">
/**
 * 图形验证码组件
 * 作者: luote (luote) - https://luote996.cn
 */
import { ref, onMounted } from 'vue'
import { getCaptcha } from '@/api/auth'

const captcha = defineModel<string>({ default: '' })
const captchaUuid = defineModel<string>('uuid', { default: '' })

const captchaImage = ref('')
const loading = ref(false)

async function loadCaptcha() {
  if (loading.value) {
    return
  }
  loading.value = true
  try {
    const result = await getCaptcha()
    captchaUuid.value = result.uuid
    captchaImage.value = result.image
    captcha.value = ''
  } catch {
    captchaImage.value = ''
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)

defineExpose({ loadCaptcha })
</script>

<template>
  <view class="captcha-field">
    <input
      v-model="captcha"
      class="captcha-input"
      type="text"
      maxlength="6"
      placeholder="验证码"
      placeholder-class="placeholder"
    />
    <view class="captcha-image-wrap" @click="loadCaptcha">
      <image
        v-if="captchaImage && !loading"
        class="captcha-img"
        :src="captchaImage"
        mode="aspectFit"
      />
      <text v-else class="captcha-placeholder">{{ loading ? '加载中' : '点击刷新' }}</text>
    </view>
  </view>
</template>

<style scoped lang="scss">
.captcha-field {
  display: flex;
  align-items: center;
  gap: 20rpx;
  width: 100%;
}

.captcha-input {
  flex: 1;
  min-width: 0;
  height: 88rpx;
  padding: 0 28rpx;
  background: $zhenxinjian-white;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: $zhenxinjian-text;
}

.placeholder {
  color: $zhenxinjian-text-secondary;
}

.captcha-image-wrap {
  flex-shrink: 0;
  // 与后端 LineCaptcha 120x40（3:1）一致，避免裁切
  width: 264rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx;
  border: 1rpx solid $zhenxinjian-border;
  background: $zhenxinjian-white;
  overflow: hidden;
}

.captcha-img {
  width: 100%;
  height: 100%;
  display: block;
}

.captcha-placeholder {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
}
</style>
