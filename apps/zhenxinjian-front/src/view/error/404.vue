<script setup lang="ts">
/**
 * 404 页面
 * 作者: wanglx
 */
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()

function goHome() {
  router.push('/')
}
</script>

<template>
  <div class="not-found">
    <div class="nf-card hud-corners tech-topline">
      <div class="nf-code num">404</div>
      <div class="nf-title">页面不存在或已被移除</div>
      <div class="nf-desc">请检查地址是否正确，或返回首页继续操作</div>
      <el-button type="primary" :icon="ArrowLeft" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.nf-card {
  position: relative;
  text-align: center;
  background: var(--zhenxinjian-white);
  border: 1px solid var(--zhenxinjian-border);
  border-radius: var(--zhenxinjian-radius-xl);
  padding: 60px 48px;
  box-shadow: var(--zhenxinjian-shadow-card);
  max-width: 420px;
}

.nf-code {
  font-size: 96px;
  font-weight: 800;
  line-height: 1;
  background: var(--zhenxinjian-gradient-brand);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: -2px;
}

.nf-title {
  margin-top: 16px;
  font-size: 18px;
  font-weight: 600;
  color: var(--zhenxinjian-text);
}

.nf-desc {
  margin: 12px 0 28px;
  font-size: 14px;
  color: var(--zhenxinjian-text-secondary);
}
</style>
