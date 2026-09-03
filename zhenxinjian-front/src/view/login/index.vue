<script setup lang="ts">
/**
 * 登录页
 * 作者: luote (luote) - https://luote996.cn
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
    <div class="auth-card">
      <div class="auth-header">
        <TeLogo />
        <h2>登录 zhenxinjian</h2>
        <p class="subtitle">luote996.cn</p>
      </div>
      <el-form :model="form" label-width="0" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <CaptchaInput
            ref="captchaRef"
            v-model="form.captcha"
            v-model:uuid="form.captchaUuid"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" :disabled="loading" native-type="submit" class="submit-btn">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="auth-footer">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ecf5ff 0%, #f5f7fa 50%, #ffffff 100%);
}

.auth-card {
  width: 400px;
  padding: 40px;
  background: var(--zhenxinjian-white);
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.1);
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-header h2 {
  margin-top: 12px;
  color: var(--zhenxinjian-text);
  font-size: 22px;
}

.subtitle {
  color: var(--zhenxinjian-text-secondary);
  font-size: 13px;
  margin-top: 4px;
}

.submit-btn {
  width: 100%;
}

.auth-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: var(--zhenxinjian-text-secondary);
}
</style>
