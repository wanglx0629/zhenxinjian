<script setup lang="ts">
/**
 * P07 碳循环周期计划页：池总量头卡 + 逐日日型卡片（色块/三宏/kcal/运动标）+ 今日高亮
 * 空态引导 P06；「提前结束/切换模式」经 F15 弹窗确认后调既有切换接口
 * 作者: wanglx
 */
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useBodyStore } from '@/store/body'
import { useCycleStore } from '@/store/cycle'
import { CYCLE_DAY_TYPES } from '@/config/constants'
import { md, week } from '@/utils/format'
import ModeSwitchConfirm from '@/components/ModeSwitchConfirm.vue'
import { track, trackPage } from '@/utils/track'
import { TRACK_EVENT } from '@/config/track-events'
import type { CycleDayVO } from '@/api/cycle'

const cycleStore = useCycleStore()
const bodyStore = useBodyStore()
const loading = ref(true)

const plan = computed(() => cycleStore.currentPlan)
const days = computed(() => cycleStore.days)
const todayIndex = computed(() => cycleStore.todayIndex)

/** F15 切换确认弹窗显隐 */
const showConfirm = ref(false)
/** 切换提交中（防重复点击） */
const switching = ref(false)

onShow(async () => {
  trackPage('pages/cycle/plan')
  loading.value = true
  try {
    await cycleStore.fetchCurrent()
    track(TRACK_EVENT.PLAN_VIEW, { mode: 2 })
  } catch {
    // request.ts 已统一 toast
  } finally {
    loading.value = false
  }
})

/** 日型字典取值（兜底中碳样式） */
function typeCfg(day: CycleDayVO) {
  return CYCLE_DAY_TYPES[day.dayType] ?? CYCLE_DAY_TYPES[2]
}

/** 自定义返回（页面栈空时兜底回首页 tab） */
function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
  } else {
    uni.switchTab({ url: '/pages/home/index' })
  }
}

/** 空态引导：去 P06 创建周期 */
function goSetting() {
  uni.redirectTo({ url: '/pages/cycle/setting' })
}

/** 打开「提前结束/切换模式」F15 确认弹窗（展示周期摘要，确认后才执行） */
function openSwitchConfirm() {
  showConfirm.value = true
}

/** F15 确认：调既有切换接口（切 532 自动终止进行中周期并清空进度） */
async function onConfirmSwitch() {
  showConfirm.value = false
  if (switching.value) return
  switching.value = true
  try {
    await cycleStore.switchMode(1)
    track(TRACK_EVENT.MODE_SWITCH, { mode: 1 })
    await bodyStore.fetchProfile()
    uni.showToast({ title: '已切换为 532 模式', icon: 'none' })
    uni.redirectTo({ url: '/pages/taper/plan' })
  } catch {
    // request.ts 已统一 toast
  } finally {
    switching.value = false
  }
}

/** F15 取消：关闭弹窗，周期不变 */
function onCancelSwitch() {
  showConfirm.value = false
}
</script>

<template>
  <view class="page">
    <!-- 自定义返回入口 -->
    <view class="back-row" hover-class="back-row-hover" @click="goBack">
      <text class="back-icon">‹</text>
      <text class="back-text">返回</text>
    </view>

    <!-- 空态：无进行中周期 -->
    <view v-if="!loading && cycleStore.noPlan" class="panel empty">
      <text class="empty-title">还没有进行中的碳循环</text>
      <text class="empty-desc">创建一个 7-14 天周期，系统将按公式为你分配高/中/低碳日目标。</text>
      <button class="btn-primary" @click="goSetting">去创建</button>
    </view>

    <template v-else-if="!loading && plan?.id">
      <!-- 池总量头卡 -->
      <view class="panel">
        <text class="panel-title">本周期总量</text>
        <view class="grid">
          <view class="cell"><text class="num">{{ plan.carbPool }}</text><text class="label">碳水池 g</text></view>
          <view class="cell"><text class="num">{{ plan.fatPool }}</text><text class="label">脂肪池 g</text></view>
          <view class="cell"><text class="num">{{ plan.dailyProtein }}</text><text class="label">每日蛋白 g</text></view>
        </view>
        <text class="meta">
          {{ plan.cycleDays }} 天 · 脂肪系数 {{ plan.cfc }} · {{ plan.startDate }} 至 {{ plan.endDate }}
        </text>
      </view>

      <!-- 逐日日型卡片 -->
      <view
        v-for="day in days"
        :key="day.dayIndex"
        class="panel day-card"
        :class="{ today: day.dayIndex === todayIndex }"
      >
        <view class="day-head">
          <view class="day-left">
            <text class="day-badge" :style="{ background: typeCfg(day).bg, color: typeCfg(day).color }">
              {{ typeCfg(day).short }}
            </text>
            <view class="day-title">
              <text class="day-name">{{ day.dayTypeName }}</text>
              <text class="day-date">第{{ day.dayIndex }}天 · {{ week(day.dayDate) }} {{ md(day.dayDate) }}</text>
            </view>
          </view>
          <view class="day-tags">
            <text v-if="day.isSport === 1" class="tag sport">运动日</text>
            <text v-if="day.dayIndex === todayIndex" class="tag today-tag">今天</text>
          </view>
        </view>
        <view class="day-body">
          <view class="macro"><text class="macro-num">{{ day.carbG }}</text><text class="macro-label">碳水 g</text></view>
          <view class="macro"><text class="macro-num">{{ day.proteinG }}</text><text class="macro-label">蛋白 g</text></view>
          <view class="macro"><text class="macro-num">{{ day.fatG }}</text><text class="macro-label">脂肪 g</text></view>
          <view class="macro"><text class="macro-num highlight">{{ day.kcal }}</text><text class="macro-label">kcal</text></view>
        </view>
      </view>

      <button class="btn-ghost" :disabled="switching" @click="openSwitchConfirm">提前结束/切换模式</button>
    </template>

    <!-- F15 切换确认弹窗（周期摘要 + 不可恢复警示） -->
    <ModeSwitchConfirm
      :visible="showConfirm"
      :plan="plan"
      @confirm="onConfirmSwitch"
      @cancel="onCancelSwitch"
    />
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx 32rpx 64rpx;
  box-sizing: border-box;
  background: $zhenxinjian-bg;
}

/* 自定义返回入口 */
.back-row {
  display: inline-flex;
  align-items: center;
  gap: 4rpx;
  padding: 8rpx 20rpx 8rpx 12rpx;
  margin-bottom: 20rpx;
  border-radius: $zhenxinjian-radius-pill;
}

.back-row-hover {
  background: $zhenxinjian-primary-bg;
}

.back-icon {
  font-size: 36rpx;
  line-height: 1;
  color: $zhenxinjian-text;
}

.back-text {
  font-size: 26rpx;
  color: $zhenxinjian-text;
}

.grid {
  display: flex;
}

.cell {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.num {
  font-size: 40rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.label {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  margin-top: 8rpx;
}

.meta {
  display: block;
  text-align: center;
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  margin-top: 16rpx;
}

/* 逐日卡片 */
.day-card.today {
  border: 2rpx solid $zhenxinjian-primary;
}

.day-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.day-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.day-badge {
  width: 64rpx;
  height: 64rpx;
  line-height: 64rpx;
  text-align: center;
  border-radius: $zhenxinjian-radius-md;
  font-size: 30rpx;
  font-weight: 600;
}

.day-title {
  display: flex;
  flex-direction: column;
}

.day-name {
  font-size: 30rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.day-date {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  margin-top: 4rpx;
}

.day-tags {
  display: flex;
  gap: 12rpx;
}

.tag {
  padding: 4rpx 16rpx;
  border-radius: $zhenxinjian-radius-sm;
  font-size: 22rpx;
}

.tag.sport {
  background: $zhenxinjian-warning-bg;
  color: $zhenxinjian-warning-deep;
}

.tag.today-tag {
  background: $zhenxinjian-primary-light;
  color: $zhenxinjian-primary;
}

.day-body {
  display: flex;
}

.macro {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.macro-num {
  font-size: 34rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.macro-num.highlight {
  color: $zhenxinjian-primary;
}

.macro-label {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  margin-top: 4rpx;
}

/* 空态 */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 64rpx 48rpx;
}

.empty-title {
  font-size: 32rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 16rpx;
}

.empty-desc {
  font-size: 26rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.6;
  margin-bottom: 40rpx;
  text-align: center;
}

.btn-ghost {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: $zhenxinjian-white;
  color: $zhenxinjian-danger;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: $zhenxinjian-radius-md;
  font-size: 30rpx;
}
</style>
