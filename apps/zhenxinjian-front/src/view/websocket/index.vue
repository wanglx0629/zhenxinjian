<script setup lang="ts">
/**
 * WebSocket Demo 页（广播聊天板子）
 * 作者: wanglx
 */
import DemoWebSocket from '@/component/DemoWebSocket.vue'
</script>

<template>
  <div class="ws-page">
    <header class="page-head">
      <div>
        <h2 class="page-title">WebSocket 演示</h2>
        <p class="page-desc">实时广播聊天；Token 通过子协议传递，不会出现在 URL 中</p>
      </div>
    </header>
    <DemoWebSocket />
  </div>
</template>

<style scoped>
.ws-page {
  max-width: 960px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  padding: 4px 2px 0;
}

.page-title {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 600;
  color: var(--zhenxinjian-text);
}

.page-desc {
  margin: 0;
  font-size: 13px;
  color: var(--zhenxinjian-text-secondary);
  line-height: 1.5;
}
</style>
