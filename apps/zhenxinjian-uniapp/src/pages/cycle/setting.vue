<script setup lang="ts">
/**
 * P06 碳循环周期设置页：天数步进 7–14 + cfc 段选（回填上次）+ 运动日多选（≤ 天数，随天数裁剪）
 * 提交创建周期 → 跳 P07 计划页
 * 作者: wanglx
 */
import { computed, ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useCycleStore } from '@/store/cycle'
import { useBodyStore } from '@/store/body'
import { CYCLE, CYCLE_DAYS_RANGE } from '@/config/constants'
import { md, week } from '@/utils/format'
import { canSubmit } from '@/utils/throttle'
import { trackPage } from '@/utils/track'

const cycleStore = useCycleStore()
const bodyStore = useBodyStore()

/** 周期天数（默认 7） */
const cycleDays = ref<number>(CYCLE_DAYS_RANGE.default)
/** 脂肪系数（回填上次选择） */
const cfc = ref<number>(cycleStore.lastCfc)
/** 运动日日序集合（1..N） */
const sportDays = ref<number[]>([])

/** 天数减（下限 7） */
function decDays() {
  if (cycleDays.value > CYCLE_DAYS_RANGE.min) cycleDays.value -= 1
}

/** 天数加（上限 14） */
function incDays() {
  if (cycleDays.value < CYCLE_DAYS_RANGE.max) cycleDays.value += 1
}

/** 天数变化时裁剪越界运动日 */
watch(cycleDays, (n) => {
  sportDays.value = sportDays.value.filter(d => d <= n)
})

/** 运动日选项（日序 1..N + 对应日历日/星期，起始日为创建当日） */
const dayOptions = computed(() => {
  const today = new Date()
  return Array.from({ length: cycleDays.value }, (_, i) => {
    const d = new Date(today)
    d.setDate(d.getDate() + i)
    return { dayIndex: i + 1, dateLabel: md(d), weekLabel: week(d) }
  })
})

/** 切换脂肪系数（切换即持久化，创建失败不丢失选择） */
function selectCfc(opt: number) {
  cfc.value = opt
  cycleStore.setCfc(opt)
}

/** 切换运动日选中（≤ 天数） */
function toggleSport(dayIndex: number) {
  const i = sportDays.value.indexOf(dayIndex)
  if (i >= 0) {
    sportDays.value.splice(i, 1)
  } else if (sportDays.value.length < cycleDays.value) {
    sportDays.value.push(dayIndex)
  }
}

onShow(async () => {
  trackPage('pages/cycle/setting')
  if (!bodyStore.loaded) {
    try { await bodyStore.fetchProfile() } catch { /* request.ts 已 toast */ }
  }
})

/** 提交创建周期 → 跳 P07 */
async function handleSubmit() {
  if (cycleStore.creating || !canSubmit()) return
  try {
    await cycleStore.create({
      cycleDays: cycleDays.value,
      cfc: cfc.value,
      sportDays: sportDays.value.length ? [...sportDays.value].sort((a, b) => a - b) : undefined
    })
    uni.redirectTo({ url: '/pages/cycle/plan' })
  } catch {
    // request.ts 已统一 toast；此处仅保持表单可重试
  }
}
</script>

<template>
  <view class="page">
    <view class="panel">
      <text class="panel-title">周期天数</text>
      <view class="stepper">
        <view class="step-btn" :class="{ disabled: cycleDays <= CYCLE_DAYS_RANGE.min }" @click="decDays">−</view>
        <text class="step-value">{{ cycleDays }} 天</text>
        <view class="step-btn" :class="{ disabled: cycleDays >= CYCLE_DAYS_RANGE.max }" @click="incDays">＋</view>
      </view>
      <text class="field-tip">{{ CYCLE_DAYS_RANGE.min }}-{{ CYCLE_DAYS_RANGE.max }} 天，日型按「高·中·低·低·中·高·低」循环轮播</text>
    </view>

    <view class="panel">
      <text class="panel-title">脂肪系数</text>
      <view class="seg">
        <view
          v-for="opt in CYCLE.fatCoefOptions"
          :key="opt"
          class="seg-item"
          :class="{ active: cfc === opt }"
          @click="selectCfc(opt)"
        >{{ opt }}</view>
      </view>
      <text class="field-tip">0.8 标准档；1.0 脂肪略高更易执行，可按口味选择</text>
    </view>

    <view class="panel">
      <text class="panel-title">运动日（可空）</text>
      <text class="field-tip">运动日将优先排为高碳日；已选 {{ sportDays.length }}/{{ cycleDays }}</text>
      <view class="day-grid">
        <view
          v-for="d in dayOptions"
          :key="d.dayIndex"
          class="day-item"
          :class="{ active: sportDays.includes(d.dayIndex) }"
          @click="toggleSport(d.dayIndex)"
        >
          <text class="day-idx">第{{ d.dayIndex }}天</text>
          <text class="day-meta">{{ d.weekLabel }} {{ d.dateLabel }}</text>
        </view>
      </view>
    </view>

    <button class="btn-primary" :loading="cycleStore.creating" @click="handleSubmit">
      生成周期计划
    </button>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx 32rpx 64rpx;
  box-sizing: border-box;
  background: $zhenxinjian-bg;
}

.panel {
  background: $zhenxinjian-white;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}

.panel-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 24rpx;
  display: block;
}

.field-tip {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  margin-top: 16rpx;
  display: block;
  line-height: 1.6;
}

.stepper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 48rpx;
}

.step-btn {
  width: 72rpx;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  font-size: 40rpx;
  color: $zhenxinjian-text;
}

.step-btn.disabled {
  opacity: 0.3;
}

.step-value {
  font-size: 36rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.seg {
  display: flex;
  gap: 16rpx;
}

.seg-item {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: $zhenxinjian-text;
}

.seg-item.active {
  background: $zhenxinjian-primary-light;
  border-color: $zhenxinjian-primary;
  color: $zhenxinjian-primary;
}

.day-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 24rpx;
}

.day-item {
  width: 160rpx;
  padding: 16rpx 0;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.day-item.active {
  background: $zhenxinjian-primary-light;
  border-color: $zhenxinjian-primary;
}

.day-idx {
  font-size: 26rpx;
  color: $zhenxinjian-text;
}

.day-item.active .day-idx {
  color: $zhenxinjian-primary;
}

.day-meta {
  font-size: 20rpx;
  color: $zhenxinjian-text-secondary;
  margin-top: 4rpx;
}

.btn-primary {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: $zhenxinjian-primary;
  color: $zhenxinjian-white;
  border-radius: 12rpx;
  font-size: 30rpx;
}
</style>
