<script setup lang="ts">
/**
 * P09 体重记录页：按日记录（kg/斤切换，同日覆盖）+ 平台期提示 + 调碳日志
 * 超过 7 条显示删除按钮；数据永久留存
 * 作者: wanglx
 */
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useWeightStore } from '@/store/weight'
import { ymd } from '@/utils/format'
import { canSubmit } from '@/utils/throttle'
import { track, trackPage } from '@/utils/track'
import { TRACK_EVENT } from '@/config/track-events'

const weightStore = useWeightStore()

/** 单位：0 kg / 1 斤 */
const unit = ref(0)
/** 录入日期（默认今日） */
const recordDate = ref(ymd(new Date()))
/** 录入体重（按当前单位输入） */
const weight = ref('')
/** 删除确认弹窗 */
const showDeleteConfirm = ref(false)
const deleteTargetId = ref<number | null>(null)

const records = computed(() => weightStore.records)
const isEmpty = computed(() => weightStore.loaded && !records.value.length)

/** kg → 当前单位展示 */
function displayWeight(kg: number): string {
  return unit.value === 0 ? String(kg) : String(Math.round(kg * 2 * 10) / 10)
}

onShow(async () => {
  trackPage('pages/weight/index')
  try {
    await weightStore.fetchRecords()
    if (weightStore.loaded) {
      await weightStore.fetchLogs().catch(() => undefined)
    }
  } catch {
    // request.ts 已统一 toast
  }
})

/** 切换单位 */
function switchUnit(v: number) {
  unit.value = v
}

/** 日期选择 */
function onDateChange(e: { detail: { value: string } }) {
  recordDate.value = e.detail.value
}

/** 保存：kg/斤按当前单位换算为 kg 提交；同日覆盖 */
async function handleSave() {
  const v = Number(weight.value)
  if (!weight.value || Number.isNaN(v)) {
    uni.showToast({ title: '请输入体重', icon: 'none' })
    return
  }
  const kg = unit.value === 0 ? v : v / 2
  if (kg < 25 || kg > 200) {
    uni.showToast({ title: '体重需在 25-200kg 之间', icon: 'none' })
    return
  }
  if (weightStore.submitting || !canSubmit()) return
  try {
    await weightStore.save({ recordDate: recordDate.value, weight: Math.round(kg * 10) / 10 })
    track(TRACK_EVENT.WEIGHT_ADD)
    weight.value = ''
    uni.showToast({ title: '已记录', icon: 'success' })
  } catch {
    // request.ts 已统一 toast
  }
}

/** 删除确认 */
function handleDelete(id: number) {
  deleteTargetId.value = id
  showDeleteConfirm.value = true
}

async function confirmDelete() {
  if (deleteTargetId.value === null) return
  showDeleteConfirm.value = false
  try {
    await weightStore.remove(deleteTargetId.value)
    uni.showToast({ title: '已删除', icon: 'success' })
  } catch {
    // request.ts 已统一 toast
  }
  deleteTargetId.value = null
}

function cancelDelete() {
  showDeleteConfirm.value = false
  deleteTargetId.value = null
}
</script>

<template>
  <view class="page">
    <!-- 平台期提示 -->
    <view v-if="weightStore.isPlateau" class="panel plateau-card">
      <text class="plateau-icon">⚠️</text>
      <view class="plateau-main">
        <text class="plateau-title">已进入平台期</text>
        <text class="plateau-desc">近 7 天体重波动小于 0.3kg，已触发碳水 −20g / 热量 −80kcal 微调。</text>
      </view>
    </view>

    <!-- 录入卡 -->
    <view class="panel">
      <text class="panel-title">记录体重</text>
      <view class="unit-seg">
        <view class="unit-item" :class="{ active: unit === 0 }" @click="switchUnit(0)">kg</view>
        <view class="unit-item" :class="{ active: unit === 1 }" @click="switchUnit(1)">斤</view>
      </view>
      <view class="date-row">
        <text class="label">日期</text>
        <picker mode="date" :value="recordDate" :end="ymd(new Date())" @change="onDateChange">
          <view class="date-picker">{{ recordDate }}</view>
        </picker>
      </view>
      <view class="input-row">
        <text class="label">体重（{{ unit === 0 ? 'kg' : '斤' }}）</text>
        <input
          v-model="weight"
          class="input"
          type="digit"
          :placeholder="unit === 0 ? '请输入体重 kg（25-200）' : '请输入体重斤（50-400）'"
        />
      </view>
      <text class="field-tip">同日重复记录会覆盖；建议每周一空腹称重。</text>
      <button class="btn-primary" :loading="weightStore.submitting" @click="handleSave">保存</button>
    </view>

    <!-- 记录列表 -->
    <view class="panel">
      <text class="panel-title">历史记录</text>
      <view v-if="isEmpty" class="empty">
        <text class="empty-text">暂无记录</text>
      </view>
      <view v-for="r in records" :key="r.id" class="record-row">
        <text class="record-date">{{ r.recordDate }}</text>
        <text class="record-weight">{{ displayWeight(r.weight) }} {{ unit === 0 ? 'kg' : '斤' }}</text>
        <text v-if="r.plateau" class="record-flag">平台</text>
        <text v-if="records.length > 7" class="record-del" @click="handleDelete(r.id)">删除</text>
      </view>
    </view>

    <!-- 调碳日志 -->
    <view v-if="weightStore.logs.length" class="panel">
      <text class="panel-title">调碳日志</text>
      <view v-for="log in weightStore.logs" :key="log.id" class="log-row">
        <text class="log-action">{{ log.actionName }}</text>
        <text class="log-meta">{{ log.triggerWeight }}kg · {{ log.createTime }}</text>
      </view>
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
  padding: 24rpx 32rpx 64rpx;
  box-sizing: border-box;
  background: $zhenxinjian-bg;
}

/* 平台提示 */
.plateau-card {
  display: flex;
  gap: 20rpx;
  background: #fff5f5;
  border-color: #fbc4c4;
}

.plateau-icon {
  font-size: 40rpx;
}

.plateau-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.plateau-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.plateau-desc {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.6;
}

/* 单位切换 */
.unit-seg {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.unit-item {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: $zhenxinjian-text;
}

.unit-item.active {
  background: $zhenxinjian-primary-light;
  border-color: $zhenxinjian-primary;
  color: $zhenxinjian-primary;
}

.date-row,
.input-row {
  margin-bottom: 24rpx;
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
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 30rpx;
  color: $zhenxinjian-primary;
  font-weight: 600;
  box-sizing: border-box;
}

.input {
  height: 80rpx;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 30rpx;
  color: $zhenxinjian-text;
  box-sizing: border-box;
}

.field-tip {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  display: block;
  margin-bottom: 24rpx;
  line-height: 1.6;
}

/* 记录列表 */
.empty {
  padding: 24rpx 0;
}

.empty-text {
  font-size: 26rpx;
  color: $zhenxinjian-text-secondary;
}

.record-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.record-row:last-child {
  border-bottom: none;
}

.record-date {
  flex: 1;
  font-size: 26rpx;
  color: $zhenxinjian-text;
}

.record-weight {
  font-size: 28rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.record-flag {
  padding: 2rpx 12rpx;
  border-radius: 8rpx;
  background: #fdf6ec;
  color: #b88230;
  font-size: 20rpx;
}

.record-del {
  font-size: 24rpx;
  color: #f56c6c;
}

/* 调碳日志 */
.log-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.log-row:last-child {
  border-bottom: none;
}

.log-action {
  font-size: 26rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.log-meta {
  flex: 1;
  text-align: right;
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
}

/* 弹窗 */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal {
  width: 560rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 40rpx 32rpx;
  display: flex;
  flex-direction: column;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 12rpx;
  text-align: center;
}

.modal-desc {
  font-size: 26rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.6;
  text-align: center;
  margin-bottom: 32rpx;
}

.modal-actions {
  display: flex;
  gap: 16rpx;
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
  background: #f5f7fa;
  color: $zhenxinjian-text;
}

.modal-btn.confirm {
  background: #f56c6c;
  color: #fff;
}
</style>