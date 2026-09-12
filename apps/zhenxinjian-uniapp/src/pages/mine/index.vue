<script setup lang="ts">
/**
 * P14 我的：用户信息卡（游客/正式双态）+ 迁移提示 + 身体数据/减脂模式入口 + 留存说明
 * 作者: wanglx
 */
import { onShow } from '@dcloudio/uni-app'
import { computed } from 'vue'
import TeLogo from '@/components/TeLogo.vue'
import { useUserStore } from '@/store/user'
import { useDietStore } from '@/store/diet'
import { useBodyStore } from '@/store/body'
import { useCycleStore } from '@/store/cycle'
import { useWeightStore } from '@/store/weight'
import { useMenstrualStore } from '@/store/menstrual'
import { useReminderStore } from '@/store/reminder'
import { DIET_MODES } from '@/config/constants'
import { guestLeftText } from '@/utils/format'
import { getToken } from '@/utils/storage'
import { trackPage } from '@/utils/track'

const userStore = useUserStore()
const dietStore = useDietStore()
const bodyStore = useBodyStore()
const cycleStore = useCycleStore()
const weightStore = useWeightStore()
const menstrualStore = useMenstrualStore()
const reminderStore = useReminderStore()

/** 提醒设置状态（onShow 同步；以总开关为准） */
const reminderEnabled = computed(() => reminderStore.masterEnabled)

const displayName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || '用户'
)

/** 游客剩余时长文案（与首页同一算法） */
const guestLeft = computed(() =>
  userStore.isGuest ? guestLeftText(userStore.userInfo?.guestExpireAt) : ''
)

/** 身体数据档案摘要（未录入展示「未录入」） */
const bodyBrief = computed(() => {
  const p = bodyStore.profile
  if (!p?.recorded) return '未录入'
  const gender = p.gender === 1 ? '男' : '女'
  return `${gender} · ${p.age} 岁 · ${p.height}cm · ${p.weight}kg`
})

/** 当前减脂模式名（以档案 mode 为准，默认 532） */
const modeName = computed(() => {
  const mode = bodyStore.profile?.mode ?? 1
  return DIET_MODES.find(m => m.code === mode)?.name ?? '532'
})

/** 体重记录副标题（最近体重 / 未记录） */
const weightBrief = computed(() => {
  const w = weightStore.latestWeight
  return w != null ? `${w}kg` : '未记录'
})

/** 月经周期副标题（当前阶段 / 未设置） */
const menstrualBrief = computed(() => {
  if (!menstrualStore.applicable) return ''
  return menstrualStore.phaseName || '未设置'
})

/** 是否展示月经周期入口（仅女性） */
const showMenstrual = computed(() => menstrualStore.applicable)

onShow(() => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/auth/guide' })
    return
  }
  trackPage('pages/mine/index')
  userStore.fetchUserInfo().catch(() => undefined)
  if (!bodyStore.loaded) {
    bodyStore.fetchProfile().catch(() => undefined)
  }
  syncReminderStatus()
  syncWeightAndMenstrual()
})

/** 同步体重与经期入口副标题（失败静默不阻塞页面） */
function syncWeightAndMenstrual() {
  weightStore.fetchRecords().catch(() => undefined)
  menstrualStore.fetch().catch(() => undefined)
}

/** 同步提醒设置副标题（以总开关为准；失败静默不阻塞页面） */
function syncReminderStatus() {
  reminderStore.fetch().catch(() => undefined)
}

/** 授权登录（游客 → 复用引导页完整授权流程） */
function goAuth() {
  uni.navigateTo({ url: '/pages/auth/guide' })
}

/** 身体数据（P03） */
function goBody() {
  uni.navigateTo({ url: '/pages/body/profile' })
}

/** 减脂模式（P05） */
function goMode() {
  uni.navigateTo({ url: '/pages/mode/select' })
}

/** 提醒设置（P13） */
function goReminder() {
  uni.navigateTo({ url: '/pages/reminder/index' })
}

/** 体重记录（P09） */
function goWeight() {
  uni.navigateTo({ url: '/pages/weight/index' })
}

/** 月经周期（经期设置） */
function goMenstrual() {
  uni.navigateTo({ url: '/pages/menstrual/index' })
}

/** 隐私与安全（静态说明） */
function showPrivacy() {
  uni.showToast({ title: '数据已加密存储，禁止明文传输', icon: 'none' })
}

/** 退出登录（二次确认；清理全部业务 store 后登出回引导页） */
function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确认退出登录？',
    success: async (res) => {
      if (!res.confirm) return
      dietStore.reset()
      bodyStore.reset()
      cycleStore.reset()
      weightStore.reset()
      menstrualStore.reset()
      await userStore.logout()
    }
  })
}
</script>

<template>
  <view class="page">
    <!-- 头部用户卡 -->
    <view class="hero">
      <view class="avatar">
        <image v-if="userStore.userInfo?.avatar" class="avatar-img" :src="userStore.userInfo.avatar" mode="aspectFill" />
        <TeLogo v-else :size="72" />
      </view>
      <view class="hero-info">
        <view class="name-row">
          <text class="name">{{ displayName }}</text>
          <text v-if="userStore.isGuest" class="badge">游客</text>
        </view>
        <text v-if="userStore.isGuest" class="meta">🎁 游客体验中 · 剩 {{ guestLeft }}</text>
        <text v-else class="meta">@{{ userStore.userInfo?.username || '-' }}</text>
      </view>
      <view v-if="userStore.isGuest" class="auth-btn" @click="goAuth">授权登录</view>
    </view>

    <!-- 游客数据迁移提示（F03，仅游客） -->
    <view v-if="userStore.isGuest" class="card migrate-card">
      <text class="migrate-icon">📦</text>
      <view class="migrate-main">
        <text class="migrate-title">体验期数据将自动迁移</text>
        <text class="migrate-desc">
          当前所有计算数据、饮食记录、周期配置均为临时缓存，授权登录后自动迁移至个人账号并永久留存。过期未登录，临时数据保留 7 天后清空。
        </text>
      </view>
    </view>

    <!-- 功能列表 -->
    <view class="card list-card">
      <view class="list-item" @click="goBody">
        <text class="list-icon">👤</text>
        <view class="list-main">
          <text class="list-title">身体数据</text>
          <text class="list-sub">{{ bodyBrief }}</text>
        </view>
        <text class="list-arrow">›</text>
      </view>
      <view class="list-item" @click="goMode">
        <text class="list-icon">🔄</text>
        <view class="list-main">
          <text class="list-title">减脂模式</text>
          <text class="list-sub">当前：{{ modeName }}</text>
        </view>
        <text class="list-tag">切换</text>
        <text class="list-arrow">›</text>
      </view>
      <view class="list-item" @click="goReminder">
        <text class="list-icon">🔔</text>
        <view class="list-main">
          <text class="list-title">提醒设置</text>
          <text class="list-sub">{{ reminderEnabled ? '已开启' : '已关闭' }}</text>
        </view>
        <text class="list-arrow">›</text>
      </view>
      <view class="list-item" @click="goWeight">
        <text class="list-icon">⚖️</text>
        <view class="list-main">
          <text class="list-title">体重记录</text>
          <text class="list-sub">{{ weightBrief }}</text>
        </view>
        <text class="list-arrow">›</text>
      </view>
      <view v-if="showMenstrual" class="list-item" @click="goMenstrual">
        <text class="list-icon">🩸</text>
        <view class="list-main">
          <text class="list-title">月经周期</text>
          <text class="list-sub">{{ menstrualBrief }}</text>
        </view>
        <text class="list-arrow">›</text>
      </view>
      <view class="list-item" @click="showPrivacy">
        <text class="list-icon">🔒</text>
        <view class="list-main">
          <text class="list-title">隐私与安全</text>
          <text class="list-sub">隐私数据加密存储 · 禁止明文传输</text>
        </view>
        <text class="list-arrow">›</text>
      </view>
    </view>

    <!-- 数据留存说明（F04/F28） -->
    <view class="card">
      <text class="card-title">☁️ 数据留存说明</text>
      <view class="note-list">
        <text class="note-item">· 个人资料、减脂模式、周期数据、饮食记录 永久云端留存，不自动清空</text>
        <text class="note-item">· 登录状态持久化，下次打开自动加载个人数据</text>
        <text class="note-item">· 游客数据临时留存，登录后自动迁移合并</text>
        <text class="note-item">· 游客体验过期未登录，临时数据保留 7 天后自动清空</text>
      </view>
    </view>

    <!-- 关于 -->
    <view class="card">
      <text class="card-title">📌 关于</text>
      <text class="about-text">臻心减 V1.1 · 生活化减脂计算器</text>
      <text class="about-text">计算核心后置，前端参数不可篡改；核心计算响应速度与页面加载满足一期性能标准。</text>
    </view>

    <!-- 退出登录 -->
    <button class="logout" @click="handleLogout">退出登录</button>

    <text class="disclaimer">📌 本工具所有健康计算结果内置免责声明，仅作生活化减脂参考，非医疗建议。</text>
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

.card-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 16rpx;
}

/* 头部用户卡 */
.hero {
  background: linear-gradient(160deg, #0d9488 0%, #14b8a6 100%);
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
  color: #fff;
}

.avatar {
  width: 104rpx;
  height: 104rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.avatar-img {
  width: 104rpx;
  height: 104rpx;
}

.hero-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.name {
  font-size: 34rpx;
  font-weight: 700;
}

.badge {
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  background: rgba(255, 255, 255, 0.25);
  font-size: 22rpx;
}

.meta {
  font-size: 24rpx;
  opacity: 0.9;
}

.auth-btn {
  padding: 12rpx 24rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.22);
  font-size: 24rpx;
  font-weight: 600;
}

/* 游客迁移提示卡 */
.migrate-card {
  display: flex;
  gap: 20rpx;
  background: #fffbf0;
  border-color: #f5e6c0;
}

.migrate-icon {
  font-size: 40rpx;
}

.migrate-main {
  flex: 1;
}

.migrate-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 8rpx;
}

.migrate-desc {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.7;
}

/* 功能列表 */
.list-card {
  padding: 0 28rpx;
}

.list-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 28rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.list-item:last-child {
  border-bottom: none;
}

.list-icon {
  font-size: 36rpx;
}

.list-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.list-title {
  font-size: 28rpx;
  color: $zhenxinjian-text;
}

.list-sub {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
}

.list-tag {
  padding: 4rpx 16rpx;
  border-radius: 16rpx;
  background: $zhenxinjian-primary-light;
  color: $zhenxinjian-primary;
  font-size: 22rpx;
  font-weight: 600;
  margin-right: 8rpx;
}

.list-arrow {
  font-size: 32rpx;
  color: $zhenxinjian-text-secondary;
}

/* 留存说明 */
.note-list {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.note-item {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.7;
}

/* 关于 */
.about-text {
  display: block;
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.7;
}

/* 退出登录 */
.logout {
  margin-top: 16rpx;
  height: 88rpx;
  line-height: 88rpx;
  background: $zhenxinjian-white;
  color: #f56c6c;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  font-size: 30rpx;
}

.disclaimer {
  display: block;
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.6;
  padding: 24rpx 16rpx 0;
}
</style>
