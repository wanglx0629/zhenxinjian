<script setup lang="ts">
/**
 * 经期设置页：女性专属；开启/关闭 + 末次月经起始日 + 周期长度 L + 经期天数 D
 * 男性适用标记 false（applicable=false）时展示不适用空态
 * 作者: wanglx
 */
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useMenstrualStore } from '@/store/menstrual'
import { RANGES } from '@/config/constants'
import { ymd } from '@/utils/format'
import { canSubmit } from '@/utils/throttle'
import { getToken } from '@/utils/storage'
import { track, trackPage } from '@/utils/track'
import { TRACK_EVENT } from '@/config/track-events'

const menstrualStore = useMenstrualStore()

const loading = ref(true)
const saving = ref(false)
/** 经期设置视图（applicable=false 男性不适用，经 menstrualStore 收敛） */
const vo = computed(() => menstrualStore.vo)

/** 表单状态 */
const enabled = ref(0)
const periodStartDate = ref('')
const cycleLen = ref<number>(RANGES.cycleLen.default ?? 28)
const periodDays = ref<number>(RANGES.periodDays.default ?? 5)

const applicable = computed(() => !!vo.value?.applicable)
const currentPhase = computed(() =>
  vo.value?.applicable && vo.value?.enabled === 1 ? vo.value?.phaseName || '' : ''
)

onShow(async () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/auth/guide' })
    return
  }
  trackPage('pages/menstrual/index')
  await load()
})

async function load() {
  loading.value = true
  try {
    const v = await menstrualStore.fetch()
    if (v?.applicable) {
      enabled.value = v.enabled ?? 0
      periodStartDate.value = v.periodStartDate || ''
      cycleLen.value = v.cycleLen ?? RANGES.cycleLen.default ?? 28
      periodDays.value = v.periodDays ?? RANGES.periodDays.default ?? 5
    }
  } catch {
    // request.ts 已统一 toast
  } finally {
    loading.value = false
  }
}

/** 周期长度加减 */
function decCycleLen() {
  if (cycleLen.value > RANGES.cycleLen.min) cycleLen.value -= 1
}
function incCycleLen() {
  if (cycleLen.value < RANGES.cycleLen.max) cycleLen.value += 1
}

/** 经期天数加减 */
function decPeriodDays() {
  if (periodDays.value > RANGES.periodDays.min) periodDays.value -= 1
}
function incPeriodDays() {
  if (periodDays.value < RANGES.periodDays.max) periodDays.value += 1
}

/** 日期选择 */
function onDateChange(e: { detail: { value: string } }) {
  periodStartDate.value = e.detail.value
}

/** 开关切换（switch 的 change 事件 uni 类型声明为 Event，运行时 detail 需强转） */
function onEnabledChange(e: Event) {
  const detail = (e as unknown as { detail: { value: boolean } }).detail
  enabled.value = detail.value ? 1 : 0
}

/** 保存：开启时校验起始日 */
async function handleSave() {
  if (enabled.value === 1 && !periodStartDate.value) {
    uni.showToast({ title: '开启需填写末次月经起始日', icon: 'none' })
    return
  }
  if (saving.value || !canSubmit()) return
  saving.value = true
  try {
    await menstrualStore.save({
      enabled: enabled.value,
      periodStartDate: enabled.value === 1 ? periodStartDate.value : null,
      cycleLen: cycleLen.value,
      periodDays: periodDays.value
    })
    track(TRACK_EVENT.MENSTRUAL_SAVE)
    uni.showToast({ title: '已保存', icon: 'success' })
  } catch {
    // request.ts 已统一 toast
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <view class="page">
    <!-- 加载中 -->
    <view v-if="loading" class="panel"><text class="empty-text">加载中…</text></view>

    <!-- 男性不适用空态 -->
    <view v-else-if="!applicable" class="panel empty">
      <text class="empty-title">经期管理不适用</text>
      <text class="empty-desc">经期管理仅面向女性用户开放。</text>
    </view>

    <template v-else>
      <!-- 当前阶段提示 -->
      <view v-if="currentPhase" class="panel phase-card">
        <text class="phase-title">当前阶段：{{ currentPhase }}</text>
        <text v-if="vo?.carbUplift" class="phase-desc">本期碳水上浮 +{{ vo.carbUplift }}g / 热量上浮 +{{ vo.kcalUplift }}kcal，蛋白脂肪不变。</text>
      </view>

      <view class="panel">
        <view class="row">
          <view class="row-main">
            <text class="row-title">开启经期管理</text>
            <text class="row-sub">开启后按阶段自动上浮碳水与热量</text>
          </view>
          <switch :checked="enabled === 1" color="#00AC7C" @change="onEnabledChange" />
        </view>
      </view>

      <view class="panel">
        <text class="panel-title">周期设置</text>

        <view class="field">
          <text class="label">末次月经起始日</text>
          <picker mode="date" :value="periodStartDate" :end="ymd(new Date())" :disabled="enabled === 0" @change="onDateChange">
            <view class="date-picker" :class="{ disabled: enabled === 0 }">
              {{ periodStartDate || '请选择日期' }}
            </view>
          </picker>
        </view>

        <view class="field">
          <text class="label">周期长度 L（{{ RANGES.cycleLen.min }}-{{ RANGES.cycleLen.max }} 天）</text>
          <view class="stepper">
            <view class="step-btn" :class="{ disabled: cycleLen <= RANGES.cycleLen.min }" @click="decCycleLen">−</view>
            <text class="step-value">{{ cycleLen }} 天</text>
            <view class="step-btn" :class="{ disabled: cycleLen >= RANGES.cycleLen.max }" @click="incCycleLen">＋</view>
          </view>
        </view>

        <view class="field">
          <text class="label">经期天数 D（{{ RANGES.periodDays.min }}-{{ RANGES.periodDays.max }} 天）</text>
          <view class="stepper">
            <view class="step-btn" :class="{ disabled: periodDays <= RANGES.periodDays.min }" @click="decPeriodDays">−</view>
            <text class="step-value">{{ periodDays }} 天</text>
            <view class="step-btn" :class="{ disabled: periodDays >= RANGES.periodDays.max }" @click="incPeriodDays">＋</view>
          </view>
        </view>
      </view>

      <button class="btn-primary" :loading="saving" @click="handleSave">保存设置</button>
    </template>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx 32rpx 64rpx;
  box-sizing: border-box;
  background: $zhenxinjian-bg;
}

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
  text-align: center;
}

.empty-text {
  font-size: 26rpx;
  color: $zhenxinjian-text-secondary;
}

/* 当前阶段 */
.phase-card {
  background: $zhenxinjian-primary-bg;
  border-color: $zhenxinjian-primary-border;
}

.phase-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 8rpx;
}

.phase-desc {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.6;
}

/* 开关行 */
.row {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.row-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.row-title {
  font-size: 28rpx;
  color: $zhenxinjian-text;
}

.row-sub {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
}

/* 表单 */
.field {
  margin-bottom: 28rpx;
}

.field:last-child {
  margin-bottom: 0;
}

.label {
  font-size: 26rpx;
  color: $zhenxinjian-text-secondary;
  margin-bottom: 12rpx;
  display: block;
}

.date-picker {
  height: 80rpx;
  line-height: 80rpx;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: $zhenxinjian-radius-md;
  padding: 0 24rpx;
  font-size: 30rpx;
  color: $zhenxinjian-primary;
  font-weight: 600;
  box-sizing: border-box;
}

.date-picker.disabled {
  color: $zhenxinjian-text-secondary;
  opacity: 0.5;
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
  border-radius: $zhenxinjian-radius-md;
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


</style>