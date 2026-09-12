<script setup lang="ts">
/**
 * P02 首页·今日总览：热量头卡 + 三宏进度（F09）+ 超标预警（F26）+ 快捷入口 + 今日餐次
 * 作者: wanglx
 */
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/user'
import { useDietStore } from '@/store/diet'
import { useBodyStore } from '@/store/body'
import { useCycleStore } from '@/store/cycle'
import { useMenstrualStore } from '@/store/menstrual'
import MacroProgress from '@/components/MacroProgress.vue'
import OverLimitCard from '@/components/OverLimitCard.vue'
import { CYCLE_DAY_TYPES, MEAL_TYPES } from '@/config/constants'
import { greeting, guestLeftText, mdWeek, ymd } from '@/utils/format'
import { getToken } from '@/utils/storage'
import { track, trackPage } from '@/utils/track'

const userStore = useUserStore()
const dietStore = useDietStore()
const bodyStore = useBodyStore()
const cycleStore = useCycleStore()
const menstrualStore = useMenstrualStore()

/** 页面加载态 / 错误态 */
const loading = ref(true)
const loadError = ref(false)

/** 经期阶段徽标（仅女性且已开启时非空，经 menstrualStore 收敛） */
const periodPhase = computed(() => menstrualStore.phaseName)

const displayName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || '用户'
)
/** 游客剩余时长文案（仅游客展示） */
const guestLeft = computed(() =>
  userStore.isGuest ? guestLeftText(userStore.userInfo?.guestExpireAt) : ''
)

const summary = computed(() => dietStore.summary)
/** 已建档且有目标（碳循环有周期同理经 summary.recorded 下发） */
const recorded = computed(() => !!summary.value?.recorded)
const isCycle = computed(() => summary.value?.mode === 2)

/** 今日总热量目标 / 已摄入 / 剩余（可为负） */
const kcalTarget = computed(() => summary.value?.kcalTarget ?? 0)
const kcalActual = computed(() => summary.value?.kcalActual ?? 0)
const kcalRemain = computed(() => kcalTarget.value - kcalActual.value)
/** 头卡进度条宽度（超标按 >100% 口径呈现，上限 150%，与三宏 displayRate 同口径） */
const kcalBarRate = computed(() =>
  Math.max(0, Math.min(summary.value?.kcalRate ?? 0, 150))
)

/** 模式标签：532 碳水渐降 / 碳循环 · 今日X碳日 */
const modeTag = computed(() => {
  if (!isCycle.value) return '当前模式：532 碳水渐降'
  const dayType = cycleStore.todayDay?.dayType
  const name = dayType ? CYCLE_DAY_TYPES[dayType]?.name : ''
  return name ? `当前模式：碳循环 · 今日${name}` : '当前模式：碳循环'
})

/** 身体数据摘要（性别/年龄/身高/体重/活动档位） */
const bodyBrief = computed(() => {
  const p = bodyStore.profile
  if (!p?.recorded) return ''
  const gender = p.gender === 1 ? '男' : '女'
  const act = ['久坐', '轻度活动', '中度活动', '高度活动'][(p.activityLevel ?? 1) - 1]
  return `${gender} · ${p.age}岁 · ${p.height}cm · ${p.weight}kg · ${act}`
})

/** 空态三分支：未建档 / 碳循环无周期 / 已建档当日无记录 */
const showBodyEmpty = computed(
  () => dietStore.loaded && dietStore.noProfile && !bodyStore.profile?.recorded
)
const showCycleEmpty = computed(
  () => dietStore.loaded && dietStore.noProfile && !!bodyStore.profile?.recorded && isCycle.value
)
const showMealEmpty = computed(
  () => recorded.value && !!dietStore.dayData && dietStore.dayData.totalKcal === 0
)

/** 今日餐次（固定四餐顺序，小计 kcal 与记录条数） */
const mealRows = computed(() => {
  const groups = dietStore.dayData?.meals ?? []
  return MEAL_TYPES.map(m => {
    const g = groups.find(x => x.mealType === m.code)
    return { code: m.code, name: m.name, count: g?.records.length ?? 0, kcal: g?.kcal ?? 0 }
  })
})

/** 页底免责声明（优先取后端下发，未建档用常驻兜底文案；不可移除） */
const disclaimerText = computed(
  () =>
    bodyStore.disclaimer ||
    '计算结果仅作生活化减脂参考，非医疗建议；数据已加密存储、永久云端留存。'
)

onShow(() => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/auth/guide' })
    return
  }
  trackPage('pages/home/index')
  loadAll()
})

/** 并行拉取：当日记录+累计（peekDay 不改记录页查看日期）/ 身体档案 / 用户信息；碳循环再拉当前周期（失败不阻塞） */
async function loadAll() {
  loading.value = true
  loadError.value = false
  try {
    await Promise.all([
      dietStore.peekDay(ymd(new Date())),
      bodyStore.fetchProfile(),
      userStore.fetchUserInfo()
    ])
    if (isCycle.value) {
      try {
        await cycleStore.fetchCurrent()
      } catch {
        // 周期拉取失败不阻塞首页主数据
      }
    }
    // 经期阶段徽标（仅女性且已开启时返回阶段；失败静默不阻塞首页）
    menstrualStore.fetch().catch(() => undefined)
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

/** 去授权登录（游客） */
function goAuth() {
  uni.navigateTo({ url: '/pages/auth/guide' })
}

/** 换模式（P05，carb-cycle 交付） */
function goMode() {
  uni.navigateTo({ url: '/pages/mode/select' })
}

/** 身体数据（P03） */
function goBody() {
  uni.navigateTo({ url: '/pages/body/profile' })
}

/** 创建碳循环周期（P06，carb-cycle 交付） */
function goCycleSetting() {
  uni.navigateTo({ url: '/pages/cycle/setting' })
}

/** 记饮食（记录 tab） */
function goRecord() {
  track('home_quick_entry', { target: 'record' })
  uni.switchTab({ url: '/pages/record/index' })
}

/** 看计划：碳循环 → P07；532 → P08 四阶段计划卡 */
function goPlan() {
  track('home_quick_entry', { target: 'plan' })
  if (isCycle.value) {
    uni.navigateTo({ url: '/pages/cycle/plan' })
  } else {
    uni.navigateTo({ url: '/pages/taper/plan' })
  }
}
</script>

<template>
  <view class="page">
    <!-- 加载骨架 -->
    <view v-if="loading" class="skeleton-wrap">
      <view class="skeleton skeleton-hero"></view>
      <view class="skeleton skeleton-block"></view>
      <view class="skeleton skeleton-line" style="width: 76%"></view>
      <view class="skeleton skeleton-line" style="width: 60%"></view>
      <view class="skeleton skeleton-block"></view>
    </view>

    <!-- 错误态 -->
    <view v-else-if="loadError" class="card error-card">
      <text class="error-title">数据加载失败</text>
      <text class="error-desc">请检查网络后重试</text>
      <view class="error-btn" @click="loadAll">重试</view>
    </view>

    <template v-else>
      <!-- 顶部渐变头卡 -->
      <view class="hero">
        <view class="hero-top">
          <view class="hero-title">
            <text class="hero-date">{{ mdWeek(new Date()) }} · {{ greeting() }}</text>
            <text class="hero-hello">{{ displayName }}，今天也要好好吃饭</text>
          </view>
          <view v-if="userStore.isGuest" class="guest-badge" @click="goAuth">
            <text class="guest-badge-label">🎁 游客体验</text>
            <text class="guest-badge-value">剩 {{ guestLeft }}</text>
          </view>
        </view>

        <template v-if="recorded">
          <view class="hero-kcal">
            <view class="hero-target">
              <text class="hero-label">今日总热量目标</text>
              <text class="hero-target-value">
                {{ kcalTarget }} <text class="hero-target-unit">kcal</text>
              </text>
            </view>
            <view class="hero-eaten">
              <text class="hero-label">已摄入 / 剩余</text>
              <text class="hero-eaten-value">{{ kcalActual }} / {{ kcalRemain }}</text>
            </view>
          </view>
          <view class="hero-bar">
            <view class="hero-bar-fill" :style="{ width: kcalBarRate + '%' }"></view>
          </view>
          <text class="hero-mode">{{ modeTag }}</text>
          <text v-if="periodPhase" class="hero-period">🩸 {{ periodPhase }}</text>
        </template>
      </view>

      <!-- 空态：未建档 -->
      <view v-if="showBodyEmpty" class="card empty-card">
        <text class="empty-icon">🥗</text>
        <text class="empty-title">先录入身体数据</text>
        <text class="empty-desc">录入身高体重后，这里会显示今日目标与摄入进度</text>
        <view class="empty-btn" @click="goBody">去录入</view>
      </view>

      <!-- 空态：碳循环无进行中周期 -->
      <view v-else-if="showCycleEmpty" class="card empty-card">
        <text class="empty-icon">📅</text>
        <text class="empty-title">还没有进行中的碳循环</text>
        <text class="empty-desc">创建周期后，这里会按高/中/低碳日显示今日目标与进度</text>
        <view class="empty-btn" @click="goCycleSetting">去创建周期</view>
      </view>

      <template v-if="recorded">
        <!-- 今日宏量进度（F09/F26） -->
        <view class="card">
          <view class="card-title-row">
            <text class="card-title">📊 今日宏量进度</text>
            <text class="mode-switch" @click="goMode">换模式</text>
          </view>
          <MacroProgress :summary="summary" />
          <view class="body-row" @click="goBody">
            <text class="body-brief">👤 身体数据：{{ bodyBrief }}</text>
            <text class="body-edit">修改 ›</text>
          </view>
        </view>

        <!-- 超标预警（F26） -->
        <OverLimitCard :summary="summary" />

        <!-- 已建档当日无记录提示 -->
        <view v-if="showMealEmpty" class="card empty-card">
          <text class="empty-icon">🍚</text>
          <text class="empty-title">今天还没有任何饮食记录</text>
          <text class="empty-desc">记下第一餐，就能看到碳蛋脂进度和超标提醒</text>
          <view class="empty-btn" @click="goRecord">去记录</view>
        </view>
      </template>

      <!-- 快捷入口 -->
      <view class="quick-grid">
        <view class="quick-item" @click="goRecord">
          <text class="quick-icon">🍚</text>
          <text class="quick-label">记饮食</text>
        </view>
        <view class="quick-item" @click="goPlan">
          <text class="quick-icon">📅</text>
          <text class="quick-label">看计划</text>
        </view>
      </view>

      <!-- 今日餐次 -->
      <view class="card meals-card" @click="goRecord">
        <text class="card-title">🍽️ 今日餐次</text>
        <view v-for="meal in mealRows" :key="meal.code" class="meal-row">
          <text class="meal-name">{{ meal.name }}</text>
          <text v-if="meal.count > 0" class="meal-val">{{ meal.kcal }} kcal · {{ meal.count }} 条</text>
          <text v-else class="meal-val empty">未记录</text>
        </view>
      </view>
    </template>

    <!-- 健康免责声明（常驻所有状态，不可移除） -->
    <text class="disclaimer">📌 {{ disclaimerText }}</text>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx 24rpx 48rpx;
  box-sizing: border-box;
  background: $zhenxinjian-bg;
}

.card {
  background: $zhenxinjian-white;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.mode-switch {
  padding: 6rpx 20rpx;
  border-radius: 20rpx;
  background: $zhenxinjian-primary-light;
  color: $zhenxinjian-primary;
  font-size: 22rpx;
  font-weight: 600;
}

/* 骨架屏 */
.skeleton-wrap {
  padding-top: 8rpx;
}

.skeleton {
  background: #e8ecef;
  border-radius: 12rpx;
  margin-bottom: 20rpx;
}

.skeleton-hero {
  height: 260rpx;
  border-radius: 16rpx;
}

.skeleton-block {
  height: 180rpx;
}

.skeleton-line {
  height: 28rpx;
}

/* 错误态 */
.error-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 64rpx 32rpx;
}

.error-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 12rpx;
}

.error-desc {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
  margin-bottom: 24rpx;
}

.error-btn {
  padding: 16rpx 48rpx;
  background: $zhenxinjian-primary;
  color: #fff;
  font-size: 26rpx;
  border-radius: 12rpx;
}

/* 顶部渐变头卡 */
.hero {
  background: linear-gradient(160deg, #0d9488 0%, #14b8a6 100%);
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  color: #fff;
}

.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 28rpx;
}

.hero-title {
  display: flex;
  flex-direction: column;
}

.hero-date {
  font-size: 24rpx;
  opacity: 0.9;
}

.hero-hello {
  font-size: 34rpx;
  font-weight: 700;
  margin-top: 8rpx;
}

.guest-badge {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 16rpx;
  padding: 10rpx 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.guest-badge-label {
  font-size: 20rpx;
}

.guest-badge-value {
  font-size: 24rpx;
  font-weight: 700;
  margin-top: 4rpx;
}

.hero-kcal {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}

.hero-label {
  font-size: 22rpx;
  opacity: 0.85;
  display: block;
}

.hero-target-value {
  font-size: 56rpx;
  font-weight: 800;
  line-height: 1.1;
}

.hero-target-unit {
  font-size: 26rpx;
  font-weight: 500;
}

.hero-eaten {
  text-align: right;
}

.hero-eaten-value {
  font-size: 30rpx;
  font-weight: 700;
}

.hero-bar {
  height: 12rpx;
  border-radius: 6rpx;
  background: rgba(255, 255, 255, 0.28);
  margin-top: 20rpx;
  overflow: hidden;
}

.hero-bar-fill {
  height: 100%;
  background: #fff;
  border-radius: 6rpx;
  transition: width 0.45s;
}

.hero-mode {
  display: block;
  font-size: 22rpx;
  opacity: 0.9;
  margin-top: 12rpx;
}

.hero-period {
  display: inline-block;
  margin-top: 12rpx;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.22);
  font-size: 22rpx;
  font-weight: 600;
}

/* 身体数据摘要行 */
.body-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #eef2f1;
}

.body-brief {
  flex: 1;
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
}

.body-edit {
  font-size: 22rpx;
  color: $zhenxinjian-primary;
  font-weight: 600;
}

/* 空态 */
.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 32rpx;
}

.empty-icon {
  font-size: 56rpx;
  margin-bottom: 16rpx;
}

.empty-title {
  font-size: 28rpx;
  color: $zhenxinjian-text;
  margin-bottom: 12rpx;
}

.empty-desc {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
  text-align: center;
  line-height: 1.6;
  margin-bottom: 24rpx;
}

.empty-btn {
  padding: 16rpx 48rpx;
  background: $zhenxinjian-primary;
  color: #fff;
  font-size: 26rpx;
  border-radius: 12rpx;
}

/* 快捷入口 */
.quick-grid {
  display: flex;
  gap: 24rpx;
  margin-bottom: 24rpx;
}

.quick-item {
  flex: 1;
  background: $zhenxinjian-white;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 16rpx;
  padding: 28rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}

.quick-icon {
  font-size: 40rpx;
}

.quick-label {
  font-size: 26rpx;
  color: $zhenxinjian-text;
}

/* 今日餐次 */
.meals-card {
  padding-bottom: 12rpx;
}

.meals-card .card-title {
  display: block;
  margin-bottom: 8rpx;
}

.meal-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.meal-row:last-child {
  border-bottom: none;
}

.meal-name {
  font-size: 26rpx;
  color: $zhenxinjian-text;
}

.meal-val {
  font-size: 24rpx;
  color: $zhenxinjian-primary;
  font-weight: 600;
}

.meal-val.empty {
  color: $zhenxinjian-text-secondary;
  font-weight: 400;
}

/* 免责声明 */
.disclaimer {
  display: block;
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.6;
  padding: 8rpx 16rpx 0;
}
</style>
