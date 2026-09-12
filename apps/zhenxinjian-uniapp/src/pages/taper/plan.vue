<script setup lang="ts">
/**
 * P08 532 四阶段计划卡：四阶段说明 + 今日目标（基线 + 平台下调 + 经期上浮）
 * 作者: wanglx
 */
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useTaperStore } from '@/store/taper'
import { track, trackPage } from '@/utils/track'
import { TRACK_EVENT } from '@/config/track-events'
import { getToken } from '@/utils/storage'

const taperStore = useTaperStore()

const loading = ref(true)
const plan = computed(() => taperStore.plan)

const stages = computed(() => plan.value?.stages ?? [])
const today = computed(() => plan.value?.today ?? null)
const isEmpty = computed(() => !loading.value && (!plan.value || !plan.value.stages.length))

onShow(async () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/auth/guide' })
    return
  }
  trackPage('pages/taper/plan')
  loading.value = true
  try {
    await taperStore.fetch()
    track(TRACK_EVENT.PLAN_VIEW, { mode: 1 })
  } catch {
    // request.ts 已统一 toast
  } finally {
    loading.value = false
  }
})

/** 调整值展示（+/- 前缀） */
function deltaText(n: number): string {
  if (n === 0) return '0'
  return n > 0 ? `+${n}` : String(n)
}

/** 去录入身体数据（未建档空态） */
function goProfile() {
  uni.redirectTo({ url: '/pages/body/profile' })
}
</script>

<template>
  <view class="page">
    <!-- 空态：未建档 -->
    <view v-if="isEmpty" class="panel empty">
      <text class="empty-title">还没有身体数据</text>
      <text class="empty-desc">录入身体数据后，这里会展示 532 四阶段计划与今日目标。</text>
      <button class="btn-primary" @click="goProfile">去录入</button>
    </view>

    <template v-else-if="!loading">
      <!-- 今日目标头卡 -->
      <view v-if="today" class="panel today-card">
        <view class="today-head">
          <text class="today-title">今日 532 目标</text>
          <text v-if="today.adjusted" class="tag adjusted">平台下调中</text>
          <text v-if="today.phaseName" class="tag phase">{{ today.phaseName }}</text>
        </view>
        <view class="today-grid">
          <view class="cell"><text class="num">{{ today.carb }}</text><text class="label">碳水 g</text></view>
          <view class="cell"><text class="num">{{ today.protein }}</text><text class="label">蛋白 g</text></view>
          <view class="cell"><text class="num">{{ today.fat }}</text><text class="label">脂肪 g</text></view>
          <view class="cell"><text class="num highlight">{{ today.kcal }}</text><text class="label">kcal</text></view>
        </view>
        <text class="today-tip">目标 = 档案基线 +（平台下调 −20g/−80kcal 若触发）+（经期上浮 若叠加）；蛋白脂肪不变。</text>
      </view>

      <!-- 四阶段计划卡 -->
      <view v-for="stage in stages" :key="stage.n" class="panel stage-card">
        <view class="stage-head">
          <view class="stage-badge" :class="{ trigger: stage.plateauTriggered }">{{ stage.n }}</view>
          <view class="stage-title-wrap">
            <text class="stage-name">{{ stage.name }}</text>
            <view class="stage-deltas">
              <text class="delta" :class="{ neg: stage.carbDelta < 0 }">碳水 {{ deltaText(stage.carbDelta) }}g</text>
              <text class="delta" :class="{ neg: stage.kcalDelta < 0 }">热量 {{ deltaText(stage.kcalDelta) }}kcal</text>
              <text v-if="stage.plateauTriggered" class="trigger-tag">平台触发式</text>
            </view>
          </view>
        </view>
        <text class="stage-desc">{{ stage.desc }}</text>
      </view>
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

.panel {
  background: $zhenxinjian-white;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}

/* 今日目标头卡 */
.today-card {
  background: linear-gradient(160deg, #0d9488 0%, #14b8a6 100%);
  border: none;
  color: #fff;
}

.today-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 24rpx;
}

.today-title {
  font-size: 32rpx;
  font-weight: 700;
}

.tag {
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  background: rgba(255, 255, 255, 0.25);
}

.tag.adjusted {
  background: #fdf6ec;
  color: #b88230;
}

.tag.phase {
  background: rgba(255, 255, 255, 0.25);
}

.today-grid {
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
  font-weight: 700;
}

.num.highlight {
  color: #fff8e1;
}

.label {
  font-size: 22rpx;
  opacity: 0.85;
  margin-top: 6rpx;
}

.today-tip {
  display: block;
  margin-top: 20rpx;
  font-size: 20rpx;
  opacity: 0.85;
  line-height: 1.5;
}

/* 阶段卡 */
.stage-head {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 16rpx;
}

.stage-badge {
  width: 64rpx;
  height: 64rpx;
  line-height: 64rpx;
  text-align: center;
  border-radius: 12rpx;
  background: $zhenxinjian-primary-light;
  color: $zhenxinjian-primary;
  font-size: 32rpx;
  font-weight: 700;
  flex-shrink: 0;
}

.stage-badge.trigger {
  background: #fdf6ec;
  color: #b88230;
}

.stage-title-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.stage-name {
  font-size: 30rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.stage-deltas {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  align-items: center;
}

.delta {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
}

.delta.neg {
  color: #b88230;
}

.trigger-tag {
  padding: 2rpx 12rpx;
  border-radius: 8rpx;
  background: #fdf6ec;
  color: #b88230;
  font-size: 20rpx;
}

.stage-desc {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.7;
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