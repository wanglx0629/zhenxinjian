<script setup lang="ts">
/**
 * 看板 KPI 卡片（DESIGN-T03 自 dashboard God 组分抽）：图标/标题/主值/副值/渐变色 props 化，
 * 仅承载展示，取值与口径由父级或 composable 编排。
 * 作者: wanglx
 */
import type { Component } from 'vue'

type Tone = 'leaf' | 'amber' | 'cyan' | 'mix'

withDefaults(
  defineProps<{
    /** 主值（undefined 兜底展示 —） */
    value?: string | number
    /** 副值（有值时以 / 分隔展示，如「累计用户 / 饮食记录」） */
    sub?: string | number
    /** 卡名 */
    label: string
    /** 图标组件（Element Plus icon） */
    icon: Component
    /** 图标渐变 tone */
    tone?: Tone
  }>(),
  { tone: 'leaf' }
)
</script>

<template>
  <div class="stat-card tech-topline">
    <div class="stat-body">
      <div class="stat-icon" :class="`icon-${tone}`">
        <el-icon :size="26"><component :is="icon" /></el-icon>
      </div>
      <div class="stat-main">
        <div class="card-label">
          {{ label }}
          <slot name="tag" />
        </div>
        <div class="card-value num">
          {{ value ?? '—' }}
          <template v-if="sub !== undefined"><span class="slash">/</span>{{ sub ?? '—' }}</template>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.stat-card {
  height: 100%;
  background: var(--zhenxinjian-white);
  border: 1px solid var(--zhenxinjian-border);
  border-radius: var(--zhenxinjian-radius-lg);
  padding: 18px 18px 16px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--zhenxinjian-shadow-card);
}

.stat-body {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: var(--zhenxinjian-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
}

.icon-leaf {
  background: linear-gradient(135deg, #00AC7C, #33BD96);
}

.icon-amber {
  background: linear-gradient(135deg, #FFB020, #FFC24D);
}

.icon-cyan {
  background: linear-gradient(135deg, #0891B2, #22D3EE);
}

.icon-mix {
  background: linear-gradient(135deg, #00AC7C, #FFB020);
}

.stat-main {
  flex: 1;
  min-width: 0;
}

.card-label {
  font-size: 13px;
  color: var(--zhenxinjian-text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}

.card-value {
  margin-top: 4px;
  font-size: 30px;
  font-weight: 700;
  color: var(--zhenxinjian-text);
  line-height: 1.1;
}

.slash {
  color: var(--zhenxinjian-text-placeholder);
  margin: 0 6px;
  font-weight: 400;
}
</style>