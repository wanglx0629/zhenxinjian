<script setup lang="ts">
/**
 * 能量环 Web 版（设计系统 §8.6）：内联 SVG 环形仪表——进度弧走能量渐变（绿→琥珀），
 * 未走完的缺口段露琥珀 track（「缺口即预算」），超标切 danger 满环。
 * 与小程序版同源同语义，仅渲染层不同（Web 无 mp-weixin 限制，直接内联 SVG）。
 * 动效：stroke-dasharray CSS transition 300ms ease-out，首屏直接到位不做加载动画；
 * prefers-reduced-motion 下禁用补间（§8.7）。
 * 作者: wanglx
 */
import { computed, onMounted, ref, useId } from 'vue'

const props = withDefaults(
  defineProps<{
    /** 达成率 0–100+（>100 触发超标态：红色满环） */
    percent: number
    /** 环直径（px，dashboard 默认 120） */
    size?: number
    /** 描边宽度（px，默认 7） */
    stroke?: number
    /** 缺口 track 色（默认琥珀半透明） */
    trackColor?: string
    /** 超标满环色 */
    overColor?: string
  }>(),
  {
    size: 120,
    stroke: 7,
    trackColor: 'rgba(255,176,32,0.45)',
    overColor: 'var(--zhenxinjian-danger)'
  }
)

/** 环几何单一真源（设计系统 §8.6）：viewBox=120/r=52 与小程序版 components/EnergyRing.vue 同源同值，改口径须双端同步 */
const RING_VIEWBOX = 120
const RING_RADIUS = 52
const C = 2 * Math.PI * RING_RADIUS

/** 渐变 id 逐实例唯一（同页多环不撞 id） */
const gradientId = `eg-grad-${useId()}`

/** mount 后下一帧才启用补间，避免 0→值的首屏加载动画（D2） */
const ready = ref(false)
onMounted(() => {
  requestAnimationFrame(() => {
    ready.value = true
  })
})

const pct = computed(() => Math.max(0, Math.min(100, props.percent)))
const over = computed(() => props.percent > 100)

const len = computed(() => C * (pct.value / 100))
const arcStyle = computed(() => ({
  strokeDasharray: `${len.value} ${C - len.value}`
}))

const boxStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`
}))
</script>

<template>
  <div class="energy-ring" :class="{ ready }" :style="boxStyle">
    <svg :viewBox="`0 0 ${RING_VIEWBOX} ${RING_VIEWBOX}`" class="ring-svg">
      <defs>
        <linearGradient
          :id="gradientId"
          gradientUnits="userSpaceOnUse"
          x1="8"
          y1="8"
          x2="112"
          y2="112"
        >
          <stop offset="0" stop-color="var(--zhenxinjian-primary)" />
          <stop offset="1" stop-color="var(--zhenxinjian-cta)" />
        </linearGradient>
      </defs>
      <circle
        cx="60"
        cy="60"
        :r="RING_RADIUS"
        fill="none"
        :stroke="trackColor"
        :stroke-width="stroke"
      />
      <circle
        class="arc"
        cx="60"
        cy="60"
        :r="RING_RADIUS"
        fill="none"
        :stroke="over ? overColor : `url(#${gradientId})`"
        :stroke-width="stroke"
        stroke-linecap="round"
        transform="rotate(-90 60 60)"
        :style="arcStyle"
      />
    </svg>
    <div class="ring-center">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.energy-ring {
  position: relative;
}

.ring-svg {
  width: 100%;
  height: 100%;
}

.ready .arc {
  transition: stroke-dasharray 0.3s ease-out;
}

.ring-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

@media (prefers-reduced-motion: reduce) {
  .ready .arc {
    transition: none;
  }
}
</style>
