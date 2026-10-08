<script setup lang="ts">
/**
 * 能量环（设计系统 §8.6）：conic 语义的环形仪表——进度弧走能量渐变（绿→琥珀），
 * 未走完的缺口段露琥珀 track（「缺口即预算」），超标切 #FF4747 满环。
 * 实现：SVG data-uri 经 <image> 渲染（mp-weixin 不支持内联 <svg>，先例 icons.ts）
 * 动效：percent 上升时 300ms ease-out 补间（记录成功→环闭合，§8.7 动效只回应操作）
 * 作者: wanglx
 */
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { PROGRESS_COLORS } from '@/config/constants'

const props = withDefaults(
  defineProps<{
    /** 达成率 0–100+（>100 触发超标态：红色满环） */
    percent: number
    /** 环直径（rpx，§8.6 首页约 360） */
    size?: number
    /** 描边宽度（rpx，§8.6 档位 16–20） */
    stroke?: number
    /** 缺口 track 色（默认琥珀半透明；hero 渐变底上可读） */
    trackColor?: string
    /** 超标满环色 */
    overColor?: string
  }>(),
  { size: 360, stroke: 20, trackColor: 'rgba(255,176,32,0.45)', overColor: PROGRESS_COLORS.red }
)

/** 环几何与令牌单一真源（设计系统 §8.6）：色值取 constants.PROGRESS_COLORS；viewBox=120/r=52 与 web 版 EnergyRing.vue 同源同值，改口径须双端同步 */
const RING_VIEWBOX = 120
const RING_RADIUS = 52
const C = 2 * Math.PI * RING_RADIUS

/** 当前渲染的达成率（补间目标） */
const display = ref(props.percent)
let timer: ReturnType<typeof setInterval> | null = null

watch(
  () => props.percent,
  (next, prev) => {
    // 首次渲染 / 下降 / 无变化：直接跳变，不做加载动效（§8.7）
    if (prev === undefined || next <= display.value) {
      stopTween()
      display.value = next
      return
    }
    tween(next)
  }
)

/** 300ms ease-out 补间：约 10 步重生成 data-uri */
function tween(target: number) {
  stopTween()
  const from = display.value
  const diff = target - from
  if (diff <= 0) return
  const steps = 10
  let i = 0
  timer = setInterval(() => {
    i++
    if (i >= steps) {
      display.value = target
      stopTween()
      return
    }
    const t = i / steps
    const eased = 1 - Math.pow(1 - t, 3) // easeOutCubic
    display.value = from + diff * eased
  }, 30)
}

function stopTween() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onBeforeUnmount(stopTween)

const cache = new Map<string, string>()

/** 生成能量环 SVG data-uri（Map 缓存） */
const ringSrc = computed(() => {
  const pct = Math.max(0, Math.min(100, display.value))
  const over = display.value > 100
  const strokeW = (RING_VIEWBOX / props.size) * props.stroke // rpx → viewBox 坐标
  const key = `${pct.toFixed(1)}|${over}|${strokeW.toFixed(2)}|${props.trackColor}|${props.overColor}`
  const hit = cache.get(key)
  if (hit) return hit
  const len = C * (pct / 100)
  const offset = C - len
  const arc =
    `<circle cx="60" cy="60" r="${RING_RADIUS}" fill="none" stroke="${over ? props.overColor : 'url(#eg)'}" ` +
    `stroke-width="${strokeW}" stroke-linecap="round" ` +
    `stroke-dasharray="${len} ${offset}" transform="rotate(-90 60 60)"/>`
  const svg =
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${RING_VIEWBOX} ${RING_VIEWBOX}">` +
    `<defs><linearGradient id="eg" gradientUnits="userSpaceOnUse" x1="8" y1="8" x2="112" y2="112">` +
    `<stop offset="0" stop-color="${PROGRESS_COLORS.green}"/><stop offset="1" stop-color="${PROGRESS_COLORS.yellow}"/></linearGradient></defs>` +
    `<circle cx="60" cy="60" r="${RING_RADIUS}" fill="none" stroke="${props.trackColor}" stroke-width="${strokeW}"/>` +
    arc +
    `</svg>`
  const uri = `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`
  cache.set(key, uri)
  return uri
})

const boxStyle = computed(() => ({ width: `${props.size}rpx`, height: `${props.size}rpx` }))
</script>

<template>
  <view class="energy-ring" :style="boxStyle">
    <image class="ring-svg" :src="ringSrc" mode="aspectFit" />
    <view class="ring-center">
      <slot />
    </view>
  </view>
</template>

<style scoped lang="scss">
.energy-ring {
  position: relative;
}

.ring-svg {
  width: 100%;
  height: 100%;
}

.ring-center {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
</style>
