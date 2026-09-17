<script setup lang="ts">
/**
 * 品牌 Logo 组件（能量渐变底 + 白色 Te 字标）
 * 作者: wanglx
 */
withDefaults(defineProps<{ size?: number }>(), { size: 36 })
</script>

<template>
  <div class="te-logo" :style="{ width: size + 'px', height: size + 'px' }">
    <span class="te-text" :style="{ fontSize: size * 0.44 + 'px' }">Te</span>
  </div>
</template>

<style scoped>
.te-logo {
  background: var(--zhenxinjian-gradient-brand);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.te-text {
  color: #fff;
  font-weight: 700;
  letter-spacing: -1px;
}
</style>
