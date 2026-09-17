<script setup lang="ts">
/**
 * 登录页（浅色科技风：点阵网格 + 能量光晕 + HUD 角标）
 * 作者: wanglx
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import TeLogo from '@/component/TeLogo.vue'
import CaptchaInput from '@/component/CaptchaInput.vue'

const router = useRouter()
const userStore = useUserStore()

const form = ref({
  username: '',
  password: '',
  captcha: '',
  captchaUuid: ''
})
const loading = ref(false)
const captchaRef = ref<InstanceType<typeof CaptchaInput>>()
// 登录提交最小间隔（毫秒），防止恶意连续点击
const SUBMIT_INTERVAL = 2000
const lastSubmitAt = ref(0)

async function handleLogin() {
  // 请求进行中直接忽略
  if (loading.value) {
    return
  }
  const now = Date.now()
  if (now - lastSubmitAt.value < SUBMIT_INTERVAL) {
    ElMessage.warning('操作过于频繁，请稍后再试')
    return
  }
  if (!form.value.username || !form.value.password || !form.value.captcha) {
    ElMessage.warning('请填写完整信息')
    return
  }
  loading.value = true
  lastSubmitAt.value = now
  try {
    await userStore.login(form.value)
    ElMessage.success('登录成功')
    router.push('/')
  } catch {
    captchaRef.value?.loadCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <!-- 科技装饰层 -->
    <div class="bg-grid" />
    <div class="bg-glow glow-1" />
    <div class="bg-glow glow-2" />

    <div class="auth-card hud-corners tech-topline">
      <div class="auth-header">
        <TeLogo :size="40" />
        <h2>臻心减 运营控制台</h2>
        <p class="subtitle">生活化减脂 · 数据驱动的健康管理</p>
      </div>
      <el-form :model="form" label-width="0" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="'User'" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="'Lock'"
            show-password
            size="large"
          />
        </el-form-item>
        <el-form-item>
          <CaptchaInput
            ref="captchaRef"
            v-model="form.captcha"
            v-model:uuid="form.captchaUuid"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            :disabled="loading"
            native-type="submit"
            size="large"
            class="submit-btn"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  overflow: hidden;
  background: var(--zhenxinjian-bg);
}

/* 点阵网格 */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(16, 49, 43, 0.12) 1px, transparent 1px);
  background-size: 22px 22px;
  mask-image: radial-gradient(ellipse 80% 70% at 50% 45%, black 30%, transparent 75%);
}

/* 能量光晕 */
.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
}

.glow-1 {
  width: 420px;
  height: 420px;
  background: rgba(0, 172, 124, 0.14);
  top: -120px;
  left: -80px;
}

.glow-2 {
  width: 380px;
  height: 380px;
  background: rgba(255, 176, 32, 0.1);
  bottom: -120px;
  right: -60px;
}

.auth-card {
  position: relative;
  width: 400px;
  max-width: 100%;
  padding: 38px 36px 28px;
  background: rgba(255, 255, 255, 0.96);
  border-radius: var(--zhenxinjian-radius-xl);
  box-shadow: var(--zhenxinjian-shadow-glow);
  border: 1px solid var(--zhenxinjian-primary-border);
}

.auth-header {
  text-align: center;
  margin-bottom: 28px;
}

.auth-header h2 {
  margin-top: 14px;
  color: var(--zhenxinjian-text);
  font-size: 20px;
  font-weight: 700;
}

.subtitle {
  color: var(--zhenxinjian-text-secondary);
  font-size: 13px;
  margin-top: 8px;
}

.submit-btn {
  width: 100%;
  font-weight: 600;
  letter-spacing: 2px;
}
</style>
