<script setup lang="ts">
/**
 * 首页：欢迎区 + WebSocket 连通状态 + 示例图表
 * 作者: luote (luote) - https://luote996.cn
 */
import { computed } from 'vue'
import { useUserStore } from '@/store/user'
import DemoChart from '@/component/DemoChart.vue'
import WsStatusCard from '@/component/WsStatusCard.vue'

const userStore = useUserStore()

const displayName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || '用户'
)
</script>

<template>
  <div class="home">
    <section class="hero">
      <div class="hero-copy">
        <p class="eyebrow">控制台</p>
        <h2 class="hero-title">你好，{{ displayName }}</h2>
        <p class="hero-desc">
          zhenxinjian 全栈脚手架已就绪。首页展示服务状态与示例数据，实时聊天请使用 WebSocket 页。
        </p>
      </div>
      <div class="hero-meta">
        <div class="meta-item">
          <span class="meta-label">角色</span>
          <span class="meta-value">{{ userStore.userInfo?.role || '--' }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">站点</span>
          <span class="meta-value">luote996.cn</span>
        </div>
      </div>
    </section>

    <WsStatusCard />

    <section class="chart-panel">
      <div class="panel-head">
        <h3 class="panel-title">示例数据</h3>
        <p class="panel-desc">可替换为业务接口返回的统计图</p>
      </div>
      <DemoChart />
    </section>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1080px;
  margin: 0 auto;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: stretch;
  padding: 24px 26px;
  border-radius: 12px;
  border: 1px solid var(--zhenxinjian-border);
  background:
    linear-gradient(135deg, rgba(64, 158, 255, 0.08), transparent 42%),
    var(--zhenxinjian-white);
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--zhenxinjian-primary);
}

.hero-title {
  margin: 0 0 10px;
  font-size: 24px;
  font-weight: 600;
  color: var(--zhenxinjian-text);
  line-height: 1.3;
}

.hero-desc {
  margin: 0;
  max-width: 520px;
  font-size: 14px;
  line-height: 1.65;
  color: var(--zhenxinjian-text-secondary);
}

.hero-meta {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 12px;
  min-width: 160px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--zhenxinjian-border);
}

.meta-label {
  font-size: 12px;
  color: var(--zhenxinjian-text-secondary);
}

.meta-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--zhenxinjian-text);
}

.chart-panel {
  background: var(--zhenxinjian-white);
  border: 1px solid var(--zhenxinjian-border);
  border-radius: 12px;
  padding: 18px 20px 12px;
}

.panel-head {
  margin-bottom: 4px;
}

.panel-title {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: var(--zhenxinjian-text);
}

.panel-desc {
  margin: 0;
  font-size: 13px;
  color: var(--zhenxinjian-text-secondary);
}

@media (max-width: 768px) {
  .hero {
    flex-direction: column;
  }

  .hero-meta {
    flex-direction: row;
    min-width: 0;
  }

  .meta-item {
    flex: 1;
  }
}
</style>
