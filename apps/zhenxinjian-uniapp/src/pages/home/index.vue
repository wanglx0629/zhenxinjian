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
import EmptyState from '@/components/EmptyState.vue'
import { CYCLE_DAY_TYPES, MEAL_TYPES } from '@/config/constants'
import { greeting, guestLeftText, mdWeek, ymd } from '@/utils/format'
import { getToken } from '@/utils/storage'
import { track, trackPage } from '@/utils/track'
import { TRACK_EVENT } from '@/config/track-events'
import { iconSrc, ringSrc, type IconName } from '@/utils/icons'

/** 餐别线性图标（替换原 emoji，H5/小程序一致） */
const MEAL_ICONS: Record<number, IconName> = { 1: 'breakfast', 2: 'bowl', 3: 'moon', 4: 'apple' }
function mealIcon(code: number) {
  return iconSrc(MEAL_ICONS[code] || 'bowl', '#10312B')
}

const userStore = useUserStore()
const dietStore = useDietStore()
const bodyStore = useBodyStore()
const cycleStore = useCycleStore()
const menstrualStore = useMenstrualStore()

/** 页面加载态 / 错误态 */
const loading = ref(true)
const loadError = ref(false)
/** 是否已完成首次加载（tab 切回时静默刷新，不再整页骨架阻塞） */
const firstLoaded = ref(false)

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
/** 当前模式（bodyStore 档案单一真源，B-T22 收敛） */
const isCycle = computed(() => bodyStore.isCycleMode)

/** 今日总热量目标 / 已摄入 / 剩余（可为负） */
const kcalTarget = computed(() => summary.value?.kcalTarget ?? 0)
const kcalActual = computed(() => summary.value?.kcalActual ?? 0)
const kcalRemain = computed(() => kcalTarget.value - kcalActual.value)
/** 头卡进度条颜色（三色语义：80–100 绿 / <80 黄 / >100 红） */
const kcalBarColor = computed(() => {
  const rate = summary.value?.kcalRate ?? 0
  if (rate > 100) return '#FF4747'
  if (rate >= 80) return '#00AC7C'
  return '#FFB020'
})

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

/** 空态三分支：未建档 / 碳循环无周期 / 已建档当日无记录（前二经 dietStore 单一真源下发，B-T22） */
const showBodyEmpty = computed(() => dietStore.showBodyEmpty)
const showCycleEmpty = computed(() => dietStore.showCycleEmpty)
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

/** 快捷入口副文案 */
const recordSub = computed(() => {
  const n = mealRows.value.filter(m => m.count > 0).length
  return n > 0 ? `今天已记 ${n}/4 餐` : '今天还没记录哦'
})
const planSub = computed(() => modeTag.value.replace('当前模式：', ''))

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
  // 仅首次进入展示骨架屏；tab 切回静默刷新（旧数据先渲染，新数据返回即更新）
  loading.value = !firstLoaded.value
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
    loadError.value = !firstLoaded.value
  } finally {
    loading.value = false
    firstLoaded.value = true
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
  track(TRACK_EVENT.HOME_QUICK_ENTRY, { target: 'record' })
  uni.switchTab({ url: '/pages/record/index' })
}

/** 看计划：碳循环 → P07；532 → P08 四阶段计划卡 */
function goPlan() {
  track(TRACK_EVENT.HOME_QUICK_ENTRY, { target: 'plan' })
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
        <!-- 漂浮装饰图标 -->
        <image class="hero-deco deco-avocado" :src="iconSrc('avocado', '#ffffff')" />
        <image class="hero-deco deco-sparkle" :src="iconSrc('sparkles', '#ffffff')" />
        <image class="hero-deco deco-clover" :src="iconSrc('leaf', '#ffffff')" />
        <view class="hero-top">
          <view class="hero-title">
            <text class="hero-date">{{ mdWeek(new Date()) }} · {{ greeting() }}</text>
            <text class="hero-hello">{{ displayName }}，今天也要好好吃饭</text>
          </view>
          <view v-if="userStore.isGuest" class="guest-badge" @click="goAuth">
            <text class="guest-badge-label">游客体验</text>
            <text class="guest-badge-value">剩 {{ guestLeft }}</text>
          </view>
        </view>

        <template v-if="recorded">
          <!-- 三色环形进度 -->
          <view class="ring-wrap">
            <view class="ring-box">
              <image class="ring-svg" :src="ringSrc(summary?.kcalRate ?? 0, kcalBarColor)" />
              <view class="ring-center">
                <text class="ring-label">已摄入</text>
                <text class="ring-num">{{ kcalActual }}</text>
                <text class="ring-target">/ {{ kcalTarget }} kcal</text>
              </view>
            </view>
            <view class="hero-remain" :class="{ over: kcalRemain < 0 }">
              {{ kcalRemain >= 0 ? `还可吃 ${kcalRemain} kcal` : `已超标 ${-kcalRemain} kcal` }}
            </view>
          </view>
          <text class="hero-mode">{{ modeTag }}</text>
          <text v-if="periodPhase" class="hero-period">
            <image class="period-icon" :src="iconSrc('droplet', '#ffffff')" />
            {{ periodPhase }}
          </text>
        </template>
      </view>

      <!-- 空态：未建档 -->
      <view v-if="showBodyEmpty" class="card empty-card">
        <EmptyState
          type="body"
          title="先录入身体数据"
          desc="录入身高体重后，这里会显示今日目标与摄入进度"
          btn-text="去录入"
          @action="goBody"
        />
      </view>

      <!-- 空态：碳循环无进行中周期 -->
      <view v-else-if="showCycleEmpty" class="card empty-card">
        <EmptyState
          type="calendar"
          title="还没有进行中的碳循环"
          desc="创建周期后，这里会按高/中/低碳日显示今日目标与进度"
          btn-text="去创建周期"
          @action="goCycleSetting"
        />
      </view>

      <template v-if="recorded">
        <!-- 今日宏量进度（F09/F26） -->
        <view class="card">
          <view class="card-title-row">
            <view class="card-title-withicon">
              <image class="title-icon" :src="iconSrc('chart', '#00AC7C')" />
              <text class="card-title">今日宏量进度</text>
            </view>
            <text class="mode-switch" @click="goMode">换模式</text>
          </view>
          <MacroProgress :summary="summary" />
          <view class="body-row" hover-class="body-row-hover" @click="goBody">
            <image class="body-icon" :src="iconSrc('body', '#475569')" />
            <text class="body-brief">身体数据：{{ bodyBrief }}</text>
            <view class="body-edit">
              <text class="body-edit-text">修改</text>
              <image class="body-edit-icon" :src="iconSrc('chevronRight', '#00AC7C')" />
            </view>
          </view>
        </view>

        <!-- 超标预警（F26） -->
        <OverLimitCard :summary="summary" />

        <!-- 已建档当日无记录提示 -->
        <view v-if="showMealEmpty" class="card empty-card">
          <EmptyState
            type="bowl"
            title="今天还没有任何饮食记录"
            desc="记下第一餐，就能看到碳蛋脂进度和超标提醒"
            btn-text="去记录"
            @action="goRecord"
          />
        </view>
      </template>

      <!-- 快捷入口 -->
      <view class="quick-grid">
        <view class="quick-item record" hover-class="quick-hover" @click="goRecord">
          <image class="quick-icon" :src="iconSrc('bowl', '#ffffff')" />
          <view class="quick-text">
            <text class="quick-label">记饮食</text>
            <text class="quick-sub">{{ recordSub }}</text>
          </view>
          <image class="quick-arrow" :src="iconSrc('chevronRight', '#ffffff')" />
        </view>
        <view class="quick-item plan" hover-class="quick-hover" @click="goPlan">
          <image class="quick-icon" :src="iconSrc('calendar', '#ffffff')" />
          <view class="quick-text">
            <text class="quick-label">看计划</text>
            <text class="quick-sub">{{ planSub }}</text>
          </view>
          <image class="quick-arrow" :src="iconSrc('chevronRight', '#ffffff')" />
        </view>
      </view>

      <!-- 今日餐次 -->
      <view class="card meals-card" hover-class="card-hover" @click="goRecord">
        <view class="card-title-withicon">
          <image class="title-icon" :src="iconSrc('utensils', '#00AC7C')" />
          <text class="card-title">今日餐次</text>
        </view>
        <view v-for="meal in mealRows" :key="meal.code" class="meal-row">
          <view class="meal-left">
            <view class="meal-chip" :class="'mc' + meal.code">
              <image class="meal-chip-icon" :src="mealIcon(meal.code)" />
            </view>
            <text class="meal-name">{{ meal.name }}</text>
          </view>
          <text v-if="meal.count > 0" class="meal-val">{{ meal.kcal }} kcal · {{ meal.count }} 条</text>
          <text v-else class="meal-val empty">未记录</text>
        </view>
      </view>
    </template>

    <!-- 健康免责声明（常驻所有状态，不可移除） -->
    <view class="disclaimer-row">
      <image class="disclaimer-icon" :src="iconSrc('alert', '#475569')" />
      <text class="disclaimer">{{ disclaimerText }}</text>
    </view>
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
  border-radius: $zhenxinjian-radius-lg;
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

/* 标题前线性图标 */
.card-title-withicon {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.title-icon {
  width: 34rpx;
  height: 34rpx;
}

.mode-switch {
  padding: 6rpx 20rpx;
  border-radius: $zhenxinjian-radius-pill;
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
  background: $zhenxinjian-track;
  border-radius: $zhenxinjian-radius-md;
  margin-bottom: 20rpx;
}

.skeleton-hero {
  height: 260rpx;
  border-radius: $zhenxinjian-radius-lg;
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
  color: $zhenxinjian-white;
  font-size: 26rpx;
  border-radius: $zhenxinjian-radius-md;
}

/* 顶部渐变头卡 */
.hero {
  position: relative;
  overflow: hidden;
  background: $zhenxinjian-gradient-brand;
  border-radius: $zhenxinjian-radius-xl;
  padding: 32rpx;
  margin-bottom: 24rpx;
  color: $zhenxinjian-white;
  box-shadow: $zhenxinjian-shadow-hero;
}

/* 漂浮装饰图标 */
.hero-deco {
  position: absolute;
  opacity: 0.4;
  pointer-events: none;
}

.deco-avocado {
  top: 24rpx;
  right: 32rpx;
  width: 60rpx;
  height: 60rpx;
}

.deco-sparkle {
  top: 110rpx;
  right: 140rpx;
  width: 32rpx;
  height: 32rpx;
  opacity: 0.5;
}

.deco-clover {
  bottom: 24rpx;
  left: 24rpx;
  width: 44rpx;
  height: 44rpx;
  opacity: 0.3;
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
  border-radius: $zhenxinjian-radius-lg;
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

/* 三色环形进度 */
.ring-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.ring-box {
  width: 260rpx;
  height: 260rpx;
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

.ring-label {
  font-size: 22rpx;
  opacity: 0.85;
}

.ring-num {
  font-size: 64rpx;
  font-weight: 800;
  line-height: 1.1;
}

.ring-target {
  font-size: 22rpx;
  opacity: 0.85;
}

.hero-remain {
  margin-top: 16rpx;
  padding: 8rpx 28rpx;
  border-radius: $zhenxinjian-radius-pill;
  background: rgba(255, 255, 255, 0.22);
  font-size: 24rpx;
  font-weight: 600;
}

.hero-remain.over {
  background: rgba(255, 71, 71, 0.4);
}

.hero-mode {
  display: block;
  font-size: 22rpx;
  opacity: 0.9;
  margin-top: 16rpx;
  text-align: center;
}

.hero-period {
  display: flex;
  align-items: center;
  gap: 6rpx;
  margin: 12rpx auto 0;
  width: fit-content;
  padding: 4rpx 16rpx;
  border-radius: $zhenxinjian-radius-pill;
  background: rgba(255, 255, 255, 0.22);
  font-size: 22rpx;
  font-weight: 600;
}

.period-icon {
  width: 20rpx;
  height: 20rpx;
}

/* 身体数据摘要行 */
.body-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 24rpx;
  padding: 12rpx 8rpx;
  border-top: 1rpx solid $zhenxinjian-divider;
  border-radius: $zhenxinjian-radius-sm;
}

.body-row-hover {
  background: $zhenxinjian-primary-bg;
}

.body-brief {
  flex: 1;
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
}

.body-icon {
  width: 26rpx;
  height: 26rpx;
  flex-shrink: 0;
}

.body-edit {
  display: flex;
  align-items: center;
  gap: 4rpx;
}

.body-edit-text {
  font-size: 22rpx;
  color: $zhenxinjian-primary;
  font-weight: 600;
}

.body-edit-icon {
  width: 22rpx;
  height: 22rpx;
}

/* 空态（插画与按钮收敛至 EmptyState 组件） */
.empty-card {
  padding: 8rpx 0;
}

/* 快捷入口（渐变彩砖） */
.quick-grid {
  display: flex;
  gap: 24rpx;
  margin-bottom: 24rpx;
}

.quick-item {
  flex: 1;
  border-radius: $zhenxinjian-radius-xl;
  padding: 28rpx 24rpx;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16rpx;
  color: $zhenxinjian-white;
  transition: transform 0.15s ease;
}

.quick-hover {
  transform: scale(0.96);
}

.card-hover {
  background: $zhenxinjian-primary-bg;
}

.quick-item.record {
  background: $zhenxinjian-gradient-cta;
  box-shadow: 0 8rpx 20rpx rgba(255, 176, 32, 0.32);
}

.quick-item.plan {
  background: $zhenxinjian-gradient-brand;
  box-shadow: 0 8rpx 20rpx rgba(0, 172, 124, 0.32);
}

.quick-icon {
  width: 48rpx;
  height: 48rpx;
  flex-shrink: 0;
}

.quick-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.quick-label {
  font-size: 30rpx;
  font-weight: 700;
  color: $zhenxinjian-white;
}

.quick-sub {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.85);
}

.quick-arrow {
  width: 32rpx;
  height: 32rpx;
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
  min-height: 88rpx;
  padding: 12rpx 0;
  border-bottom: 1rpx solid $zhenxinjian-divider;
}

.meal-row:last-child {
  border-bottom: none;
}

.meal-name {
  font-size: 26rpx;
  color: $zhenxinjian-text;
}

.meal-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.meal-chip {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $zhenxinjian-primary-bg;
}

.meal-chip-icon {
  width: 36rpx;
  height: 36rpx;
}

.meal-chip.mc1 { background: $zhenxinjian-warning-bg; }
.meal-chip.mc2 { background: $zhenxinjian-primary-border; }
.meal-chip.mc3 { background: $zhenxinjian-primary-bg; }
.meal-chip.mc4 { background: $zhenxinjian-warning-border; }

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
.disclaimer-row {
  display: flex;
  align-items: flex-start;
  gap: 8rpx;
  padding: 8rpx 16rpx 0;
}

.disclaimer-icon {
  width: 28rpx;
  height: 28rpx;
  margin-top: 4rpx;
  flex-shrink: 0;
}

.disclaimer {
  flex: 1;
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.6;
}
</style>
