<script setup lang="ts">
/**
 * 注册页
 * 作者: luote (luote) - https://luote996.cn
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'
import TeLogo from '@/component/TeLogo.vue'
import CaptchaInput from '@/component/CaptchaInput.vue'

const router = useRouter()

const form = ref({
  username: '',
  password: '',
  nickname: '',
  email: '',
  captcha: '',
  captchaUuid: ''
})
const loading = ref(false)
const captchaRef = ref<InstanceType<typeof CaptchaInput>>()
const SUBMIT_INTERVAL = 2000
const lastSubmitAt = ref(0)

async function handleRegister() {
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
  if (form.value.password.length < 8 || form.value.password.length > 32) {
    ElMessage.warning('密码长度需为 8-32 位')
    return
  }
  loading.value = true
  lastSubmitAt.value = now
  try {
    await register(form.value)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
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
        <h2>注册 zhenxinjian</h2>
        <p class="subtitle">luote996.cn</p>
      </div>
      <el-form :model="form" label-width="0" @submit.prevent="handleRegister">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码（8-32 位）" show-password />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.nickname" placeholder="昵称（可选）" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.email" placeholder="邮箱（可选）" />
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
            class="submit-btn"
            native-type="submit"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>
      <div class="auth-footer">
        已有账号？<router-link to="/login">立即登录</router-link>
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
