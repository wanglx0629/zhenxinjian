<script setup lang="ts">
/**
 * P05 模式选择页：532 / 碳循环双卡片 + 当前模式标记
 * 选碳循环：无周期 → 切模式后引导 P06；有进行中周期直达 P07
 * 碳循环切回 532：P16 弹窗二次确认「终止周期并清空进度」
 * 作者: wanglx
 */
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useBodyStore } from '@/store/body'
import { useCycleStore } from '@/store/cycle'
import { switchDietMode } from '@/api/cycle'
import { DIET_MODES } from '@/config/constants'
import ModeSwitchConfirm from '@/components/ModeSwitchConfirm.vue'
import { track, trackPage } from '@/utils/track'

const bodyStore = useBodyStore()
const cycleStore = useCycleStore()

/** 切换提交中（防重复点击） */
const switching = ref(false)
/** P16 切换确认弹窗显隐 */
const showConfirm = ref(false)

/** 当前模式（默认 532） */
const currentMode = computed(() => bodyStore.profile?.mode ?? 1)

onShow(async () => {
  trackPage('pages/mode/select')
  try {
    if (!bodyStore.loaded) {
      await bodyStore.fetchProfile()
    }
    await cycleStore.fetchCurrent()
  } catch {
    // request.ts 已统一 toast
  }
})

/** 点击模式卡片 */
async function handleSelect(mode: number) {
  if (switching.value || mode === currentMode.value) return
  if (mode === 2) {
    // 切碳循环：有进行中周期直达 P07，否则切模式后引导 P06
    switching.value = true
    try {
      await switchDietMode(2)
      track('mode_switch', { mode: 2 })
      await bodyStore.fetchProfile()
      if (cycleStore.currentPlan?.id) {
        uni.redirectTo({ url: '/pages/cycle/plan' })
      } else {
        uni.redirectTo({ url: '/pages/cycle/setting' })
      }
    } catch {
      // request.ts 已统一 toast
    } finally {
      switching.value = false
    }
    return
  }
  // 切回 532：有进行中周期需 P16 二次确认；无周期直接切
  if (cycleStore.currentPlan?.id) {
    showConfirm.value = true
  } else {
    await doSwitch532()
  }
}

/** 执行切回 532（后端自动终止进行中周期） */
async function doSwitch532() {
  if (switching.value) return
  switching.value = true
  try {
    await switchDietMode(1)
    track('mode_switch', { mode: 1 })
    cycleStore.reset()
    await bodyStore.fetchProfile()
    uni.showToast({ title: '已切换为 532 模式', icon: 'none' })
  } catch {
    // request.ts 已统一 toast
  } finally {
    switching.value = false
  }
}

/** P16 确认切换 */
async function onConfirmSwitch() {
  showConfirm.value = false
  await doSwitch532()
}

/** P16 取消 */
function onCancelSwitch() {
  showConfirm.value = false
}
</script>

<template>
  <view class="page">
    <text class="page-title">选择减脂模式</text>
    <text class="page-desc">模式决定每日营养目标的计算口径，切换后立即生效。</text>

    <view
      v-for="m in DIET_MODES"
      :key="m.code"
      class="mode-card"
      :class="{ active: currentMode === m.code }"
      @click="handleSelect(m.code)"
    >
      <view class="mode-head">
        <text class="mode-name">{{ m.name }}</text>
        <text v-if="currentMode === m.code" class="mode-badge">当前模式</text>
      </view>
      <text class="mode-desc">{{ m.desc }}</text>
    </view>

    <ModeSwitchConfirm
      :visible="showConfirm"
      @confirm="onConfirmSwitch"
      @cancel="onCancelSwitch"
    />
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 32rpx 32rpx 64rpx;
  box-sizing: border-box;
  background: $zhenxinjian-bg;
}

.page-title {
  font-size: 36rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  display: block;
}

.page-desc {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
  margin: 12rpx 0 32rpx;
  display: block;
  line-height: 1.6;
}

.mode-card {
  background: $zhenxinjian-white;
  border: 2rpx solid $zhenxinjian-border;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}

.mode-card.active {
  border-color: $zhenxinjian-primary;
  background: $zhenxinjian-primary-light;
}

.mode-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.mode-name {
  font-size: 32rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.mode-badge {
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  background: $zhenxinjian-primary;
  color: $zhenxinjian-white;
  font-size: 22rpx;
}

.mode-desc {
  font-size: 26rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.6;
}
</style>
