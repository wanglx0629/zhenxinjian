<script setup lang="ts">
/**
 * 图形验证码组件（输入框 + 右侧验证码图片）
 * 作者: wanglx
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
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)

defineExpose({ loadCaptcha })
</script>

<template>
  <div class="captcha-field">
    <el-input
      v-model="captcha"
      class="captcha-input"
      placeholder="验证码"
      maxlength="6"
      clearable
    />
    <div
      class="captcha-image-wrap"
      :class="{ 'is-loading': loading }"
      title="点击刷新验证码"
      @click="loadCaptcha"
    >
      <img
        v-if="captchaImage && !loading"
        :src="captchaImage"
        class="captcha-img"
        alt="验证码"
      />
      <span v-else class="captcha-placeholder">{{ loading ? '加载中' : '点击刷新' }}</span>
    </div>
  </div>
</template>

<style scoped>
.captcha-field {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.captcha-input {
  flex: 1;
  min-width: 0;
}

.captcha-image-wrap {
  flex-shrink: 0;
  /* 与后端 LineCaptcha 120x40（3:1）一致，避免移动端裁切 */
  width: 120px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 4px;
  border: 1px solid var(--zhenxinjian-border);
  background: var(--zhenxinjian-white);
  overflow: hidden;
  user-select: none;
}

.captcha-image-wrap:hover {
  border-color: var(--zhenxinjian-primary);
}

.captcha-image-wrap.is-loading {
  cursor: wait;
}

.captcha-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.captcha-placeholder {
  font-size: 12px;
  color: var(--zhenxinjian-text-secondary);
}
</style>
