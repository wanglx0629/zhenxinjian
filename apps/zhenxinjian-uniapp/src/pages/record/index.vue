<script setup lang="ts">
/**
 * P12 当日记录页：日期导航 + 餐别分组列表 + 三色进度 + 超标建议
 * 作者: wanglx
 */
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useDietStore } from '@/store/diet'
import { useBodyStore } from '@/store/body'
import { useCycleStore } from '@/store/cycle'
import { MEAL_TYPES, OVER_ADVICE, CYCLE_DAY_TYPES } from '@/config/constants'
import { ymd, md, week } from '@/utils/format'
import { buildProgressItems, buildAdviceList } from '@/utils/macro'
import type { DietRecordVO } from '@/api/diet'
import { track, trackPage } from '@/utils/track'
import { TRACK_EVENT } from '@/config/track-events'

const dietStore = useDietStore()
const bodyStore = useBodyStore()
const cycleStore = useCycleStore()

/** 是否显示删除确认弹窗 */
const showDeleteConfirm = ref(false)
/** 待删除记录ID */
const deleteTargetId = ref<number | null>(null)

/** 当前日期是否今日 */
const isToday = computed(() => dietStore.currentDate === ymd(new Date()))

/** 日期显示文本 */
const dateLabel = computed(() => {
  const d = dietStore.currentDate
  if (isToday.value) return '今天'
  return md(d) + ' ' + week(d)
})

/** 进度条数据（三宏 + 总热量，口径单一来源 utils/macro.ts） */
const progressItems = computed(() => buildProgressItems(dietStore.summary))

/** 超标项列表 */
const overItems = computed(() => progressItems.value.filter(item => item.overAmount > 0))

/** 超标建议列表（口径单一来源 utils/macro.ts） */
const adviceList = computed(() => buildAdviceList(overItems.value))

/** 是否碳循环模式（bodyStore 档案单一真源，B-T22 收敛；目标口径按日型分发） */
const isCycle = computed(() => bodyStore.isCycleMode)

/** 目标口径标签：532「今日目标」/ 碳循环「今日高/中/低碳日」 */
const targetLabel = computed(() => {
  if (!isCycle.value) return '今日目标'
  const dayType = cycleStore.todayDay?.dayType
  return dayType ? `今日${CYCLE_DAY_TYPES[dayType]?.name ?? '目标'}` : '今日目标'
})

/** 空态分流（dietStore 单一真源，B-T22 收敛）：未建档 → 引导录入身体数据；已建档但碳循环无周期 → 引导创建周期 */
const showBodyEmpty = computed(() => dietStore.showBodyEmpty)
const showCycleEmpty = computed(() => dietStore.showCycleEmpty)

/** 页面显示时拉取数据 */
onShow(async () => {
  trackPage('pages/record/index')
  try {
    await dietStore.fetchDay()
  } catch {
    // 请求失败已由 request.ts toast
  }
  // 同步拉取身体档案（判断空态）
  if (!bodyStore.loaded) {
    try { await bodyStore.fetchProfile() } catch { /* ignore */ }
  }
  // 碳循环模式拉取当前周期（今日日型标签）
  if (isCycle.value) {
    try { await cycleStore.fetchCurrent() } catch { /* ignore */ }
  }
})

/** 日期导航：前一天 */
function prevDay() {
  const d = new Date(dietStore.currentDate)
  d.setDate(d.getDate() - 1)
  dietStore.changeDate(ymd(d))
}

/** 日期导航：后一天（超今日禁用） */
function nextDay() {
  if (isToday.value) return
  const d = new Date(dietStore.currentDate)
  d.setDate(d.getDate() + 1)
  dietStore.changeDate(ymd(d))
}

/** 回到今天 */
function goToday() {
  if (isToday.value) return
  dietStore.changeDate(ymd(new Date()))
}

/** 跳添加页（手动输入） */
function goAddManual() {
  uni.navigateTo({ url: '/pages/record/add' })
}

/** 编辑记录：URL 仅带记录 id，回显数据由 add 页从 diet store 当日分组按 id 查找（不再全量编码传参） */
function handleEdit(record: DietRecordVO) {
  uni.navigateTo({ url: `/pages/record/add?editId=${record.id}` })
}

/** 删除记录（弹确认） */
function handleDelete(id: number) {
  deleteTargetId.value = id
  showDeleteConfirm.value = true
}

/** 确认删除 */
async function confirmDelete() {
  if (deleteTargetId.value === null) return
  showDeleteConfirm.value = false
  try {
    await dietStore.removeRecord(deleteTargetId.value)
    track(TRACK_EVENT.RECORD_DELETE)
    uni.showToast({ title: '已删除', icon: 'success' })
  } catch {
    // 错误已由 request.ts toast
  }
  deleteTargetId.value = null
}

/** 取消删除 */
function cancelDelete() {
  showDeleteConfirm.value = false
  deleteTargetId.value = null
}

/** 去录入身体数据 */
function goBodyProfile() {
  uni.navigateTo({ url: '/pages/body/profile' })
}

/** 去创建碳循环周期（P06） */
function goCycleSetting() {
  uni.navigateTo({ url: '/pages/cycle/setting' })
}
</script>

<template>
  <view class="page">
    <!-- 日期导航 -->
    <view class="date-nav">
      <view class="nav-arrow" @click="prevDay">
        <text class="arrow-icon">&lt;</text>
      </view>
      <view class="date-center" @click="goToday">
        <text class="date-label">{{ dateLabel }}</text>
        <text class="date-sub">{{ dietStore.currentDate }}</text>
      </view>
      <view class="nav-arrow" :class="{ disabled: isToday }" @click="nextDay">
        <text class="arrow-icon">&gt;</text>
      </view>
    </view>

    <!-- 未建档空态 -->
    <view v-if="showBodyEmpty" class="panel empty-profile">
      <text class="empty-title">先录入身体数据</text>
      <text class="empty-desc">录入身高体重后，这里会显示目标与进度</text>
      <view class="empty-btn" @click="goBodyProfile">去录入</view>
    </view>

    <!-- 碳循环无周期空态 -->
    <view v-else-if="showCycleEmpty" class="panel empty-profile">
      <text class="empty-title">还没有进行中的碳循环</text>
      <text class="empty-desc">创建周期后，这里会按高/中/低碳日显示目标与进度</text>
      <view class="empty-btn" @click="goCycleSetting">去创建周期</view>
    </view>

    <!-- 三色进度区 -->
    <view v-if="!dietStore.noProfile && progressItems.length" class="panel">
      <text class="panel-title">当日进度 · {{ targetLabel }}</text>
      <view v-for="item in progressItems" :key="item.key" class="progress-row">
        <view class="progress-info">
          <text class="progress-label">{{ item.label }}</text>
          <text class="progress-value">{{ item.actual }}/{{ item.target }}{{ item.unit }}</text>
        </view>
        <view class="progress-bar">
          <view class="progress-fill" :style="{ width: item.displayRate + '%', background: item.color }"></view>
        </view>
        <view class="progress-status">
          <text v-if="item.overAmount > 0" class="over-text" :style="{ color: item.color }">已超标 {{ item.overAmount }}{{ item.unit }}</text>
          <text v-else class="rate-text" :style="{ color: item.color }">{{ Math.round(item.rate ?? 0) }}%</text>
        </view>
      </view>
    </view>

    <!-- 超标建议卡 -->
    <view v-if="adviceList.length" class="panel advice-card">
      <text class="panel-title">微调建议</text>
      <text v-for="(advice, i) in adviceList" :key="i" class="advice-text">{{ advice }}</text>
      <text class="advice-disclaimer">{{ OVER_ADVICE.disclaimer }}</text>
    </view>

    <!-- 餐别分组列表 -->
    <view v-for="group in dietStore.dayData?.meals" :key="group.mealType" class="panel">
      <view class="meal-header">
        <text class="meal-name">{{ group.mealName }}</text>
        <text v-if="group.records.length" class="meal-sub">{{ group.kcal }}kcal</text>
      </view>

      <view v-if="!group.records.length" class="meal-empty">
        <text class="meal-empty-text">暂无记录</text>
      </view>

      <view v-for="record in group.records" :key="record.id" class="record-item">
        <view class="record-info">
          <text class="record-name">{{ record.foodName }}</text>
          <text class="record-detail">
            {{ record.source === 3 ? '手动输入' : record.amountG + 'g' }}
            · {{ record.carbG }}碳水 {{ record.proteinG }}蛋白 {{ record.fatG }}脂肪
          </text>
          <text v-if="record.remark" class="record-remark">{{ record.remark }}</text>
        </view>
        <view class="record-right">
          <text class="record-kcal">{{ record.kcal }}kcal</text>
          <view class="record-actions">
            <text class="action-btn" @click="handleEdit(record)">编辑</text>
            <text class="action-btn delete" @click="handleDelete(record.id)">删除</text>
          </view>
        </view>
      </view>

      <view v-if="group.records.length" class="meal-total">
        <text class="meal-total-text">小计：{{ group.carb }}碳水 {{ group.protein }}蛋白 {{ group.fat }}脂肪</text>
      </view>
    </view>

    <!-- 当日合计 -->
    <view v-if="dietStore.dayData && dietStore.dayData.totalKcal > 0" class="panel">
      <view class="day-total">
        <text class="day-total-label">当日合计</text>
        <text class="day-total-value">
          {{ dietStore.dayData.totalCarb }}碳水
          {{ dietStore.dayData.totalProtein }}蛋白
          {{ dietStore.dayData.totalFat }}脂肪
          {{ dietStore.dayData.totalKcal }}kcal
        </text>
      </view>
    </view>

    <!-- 添加按钮 -->
    <view class="fab" @click="goAddManual">
      <text class="fab-icon">+</text>
    </view>

    <!-- 删除确认弹窗 -->
    <view v-if="showDeleteConfirm" class="modal-mask" @click="cancelDelete">
      <view class="modal" @click.stop>
        <text class="modal-title">确认删除</text>
        <text class="modal-desc">删除后不可恢复，确定要删除这条记录吗？</text>
        <view class="modal-actions">
          <view class="modal-btn cancel" @click="cancelDelete">取消</view>
          <view class="modal-btn confirm" @click="confirmDelete">删除</view>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx 32rpx 120rpx;
  box-sizing: border-box;
  background: $zhenxinjian-bg;
}

/* 日期导航 */
.date-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
  padding: 0 16rpx;
}

.nav-arrow {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-arrow.disabled {
  opacity: 0.3;
}

.arrow-icon {
  font-size: 36rpx;
  color: $zhenxinjian-text;
}

.date-center {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.date-label {
  font-size: 32rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.date-sub {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  margin-top: 4rpx;
}

/* 面板 */
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
  margin-bottom: 20rpx;
  display: block;
}

/* 未建档空态 */
.empty-profile {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 32rpx;
}

.empty-title {
  font-size: 28rpx;
  color: $zhenxinjian-text;
  margin-bottom: 12rpx;
}

.empty-desc {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
  margin-bottom: 24rpx;
}

.empty-btn {
  padding: 16rpx 48rpx;
  background: $zhenxinjian-primary;
  color: #fff;
  font-size: 26rpx;
  border-radius: 12rpx;
}

/* 进度条 */
.progress-row {
  margin-bottom: 24rpx;
}

.progress-row:last-child {
  margin-bottom: 0;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8rpx;
}

.progress-label {
  font-size: 26rpx;
  color: $zhenxinjian-text;
}

.progress-value {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
}

.progress-bar {
  height: 16rpx;
  background: #f0f0f0;
  border-radius: 8rpx;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 8rpx;
  transition: width 0.3s;
}

.progress-status {
  margin-top: 8rpx;
  display: flex;
  justify-content: flex-end;
}

.rate-text {
  font-size: 22rpx;
}

.over-text {
  font-size: 22rpx;
  font-weight: 600;
}

/* 建议卡 */
.advice-card {
  background: #fffbe6;
  border-color: #ffe58f;
}

.advice-text {
  font-size: 24rpx;
  color: $zhenxinjian-text;
  display: block;
  margin-bottom: 12rpx;
  line-height: 1.6;
}

.advice-disclaimer {
  font-size: 20rpx;
  color: $zhenxinjian-text-secondary;
  display: block;
  margin-top: 16rpx;
}

/* 餐别分组 */
.meal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.meal-name {
  font-size: 28rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.meal-sub {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
}

.meal-empty {
  padding: 32rpx 0;
  display: flex;
  justify-content: center;
}

.meal-empty-text {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
}

/* 记录项 */
.record-item {
  display: flex;
  justify-content: space-between;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.record-item:last-child {
  border-bottom: none;
}

.record-info {
  flex: 1;
  min-width: 0;
}

.record-name {
  font-size: 26rpx;
  color: $zhenxinjian-text;
  display: block;
}

.record-detail {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  display: block;
  margin-top: 6rpx;
}

.record-remark {
  font-size: 20rpx;
  color: $zhenxinjian-text-secondary;
  display: block;
  margin-top: 4rpx;
  font-style: italic;
}

.record-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  margin-left: 20rpx;
}

.record-kcal {
  font-size: 26rpx;
  font-weight: 600;
  color: $zhenxinjian-primary;
}

.record-actions {
  display: flex;
  gap: 16rpx;
}

.action-btn {
  font-size: 22rpx;
  color: $zhenxinjian-primary;
}

.action-btn.delete {
  color: #f56c6c;
}

/* 小计 */
.meal-total {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #f0f0f0;
}

.meal-total-text {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
}

/* 当日合计 */
.day-total {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.day-total-label {
  font-size: 26rpx;
  color: $zhenxinjian-text;
  margin-bottom: 8rpx;
}

.day-total-value {
  font-size: 24rpx;
  color: $zhenxinjian-primary;
  font-weight: 600;
}

/* 浮动添加按钮 */
.fab {
  position: fixed;
  right: 32rpx;
  bottom: 120rpx;
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: $zhenxinjian-primary;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(64, 158, 255, 0.4);
  z-index: 10;
}

.fab-icon {
  font-size: 48rpx;
  color: #fff;
  font-weight: 300;
}

/* 弹窗 */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal {
  width: 560rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 48rpx 32rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.modal-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 16rpx;
}

.modal-desc {
  font-size: 26rpx;
  color: $zhenxinjian-text-secondary;
  margin-bottom: 32rpx;
}

.modal-actions {
  display: flex;
  gap: 24rpx;
  width: 100%;
}

.modal-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border-radius: 12rpx;
  font-size: 28rpx;
}

.modal-btn.cancel {
  border: 1rpx solid $zhenxinjian-border;
  color: $zhenxinjian-text;
}

.modal-btn.confirm {
  background: #f56c6c;
  color: #fff;
}
</style>
