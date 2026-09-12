<script setup lang="ts">
/**
 * P13 提醒设置：总开关 + 早/午/晚三行（switch + time picker）
 * 保存统一 PUT；每次保存成功后重新发起订阅补额度（规格 MUST），
 * 拒绝授权不阻塞保存并 toast 降级提示
 * 作者: wanglx
 */
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/user'
import { useReminderStore } from '@/store/reminder'
import { getToken } from '@/utils/storage'
import { canSubmit } from '@/utils/throttle'
import { track, trackPage } from '@/utils/track'
import { TRACK_EVENT } from '@/config/track-events'

const userStore = useUserStore()
const reminderStore = useReminderStore()

/** 表单状态 */
const masterSwitch = ref(0)
const breakfastSwitch = ref(0)
const breakfastTime = ref('08:30')
const lunchSwitch = ref(0)
const lunchTime = ref('12:00')
const dinnerSwitch = ref(0)
const dinnerTime = ref('18:30')
const subscribeCredit = ref(0)
const templateId = ref('')

/** 加载态/保存态 */
const loading = ref(true)
const saving = ref(false)

/** 当前是否存在任一开启项（总开关开 + 任一餐别开） */
const anyEnabled = computed(() =>
  masterSwitch.value === 1 && (breakfastSwitch.value === 1 || lunchSwitch.value === 1 || dinnerSwitch.value === 1)
)

onShow(async () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/auth/guide' })
    return
  }
  trackPage('pages/reminder/index')
  await load()
})

/** 拉取设置（无记录后端默认 08:30/12:00/18:30 全开落库） */
async function load() {
  loading.value = true
  try {
    const vo = await reminderStore.fetch()
    masterSwitch.value = vo.masterSwitch
    breakfastSwitch.value = vo.breakfastSwitch
    breakfastTime.value = vo.breakfastTime
    lunchSwitch.value = vo.lunchSwitch
    lunchTime.value = vo.lunchTime
    dinnerSwitch.value = vo.dinnerSwitch
    dinnerTime.value = vo.dinnerTime
    subscribeCredit.value = vo.subscribeCredit
    templateId.value = vo.templateId || ''
  } catch {
    // request.ts 已统一 toast
  } finally {
    loading.value = false
  }
}

/** 时间选择器变更（统一 HH:mm 字符串） */
function onTimeChange(meal: 'breakfast' | 'lunch' | 'dinner', e: { detail: { value: string } }) {
  const v = e.detail.value
  if (meal === 'breakfast') breakfastTime.value = v
  else if (meal === 'lunch') lunchTime.value = v
  else dinnerTime.value = v
}

/** 开关变更统一处理（switch 布尔 → 0/1 落库口径） */
function onSwitchChange(meal: 'master' | 'breakfast' | 'lunch' | 'dinner', e: { detail: { value: boolean } }) {
  const v = e.detail.value ? 1 : 0
  if (meal === 'master') masterSwitch.value = v
  else if (meal === 'breakfast') breakfastSwitch.value = v
  else if (meal === 'lunch') lunchSwitch.value = v
  else dinnerSwitch.value = v
}

/** switch 的 change 事件：uni 类型声明为 Event，运行时 detail 需强转 */
function switchEvent(e: Event): { detail: { value: boolean } } {
  return e as unknown as { detail: { value: boolean } }
}

/** 模板绑定（模板内不支持类型标注的箭头函数，统一收敛到具名函数） */
function onMasterChange(e: Event) {
  onSwitchChange('master', switchEvent(e))
}

function onBreakfastSwitch(e: Event) {
  onSwitchChange('breakfast', switchEvent(e))
}

function onLunchSwitch(e: Event) {
  onSwitchChange('lunch', switchEvent(e))
}

function onDinnerSwitch(e: Event) {
  onSwitchChange('dinner', switchEvent(e))
}

function onBreakfastTime(e: { detail: { value: string } }) {
  onTimeChange('breakfast', e)
}

function onLunchTime(e: { detail: { value: string } }) {
  onTimeChange('lunch', e)
}

function onDinnerTime(e: { detail: { value: string } }) {
  onTimeChange('dinner', e)
}

/** 引导订阅授权（仅在微信小程序环境且有模板 ID 时有效）；返回是否成功获得授权 */
function requestSubscribe(): Promise<boolean> {
  return new Promise((resolve) => {
    // #ifdef MP-WEIXIN
    if (!templateId.value) {
      resolve(false)
      return
    }
    uni.requestSubscribeMessage({
      tmplIds: [templateId.value],
      success: (res) => {
        // 回调结果按模板 ID 索引（accept/reject/ban），类型定义未含下标签名需强转
        const v = (res as unknown as Record<string, string>)[templateId.value]
        resolve(v === 'accept')
      },
      fail: () => resolve(false)
    })
    // #endif
    // #ifndef MP-WEIXIN
    resolve(false)
    // #endif
  })
}

/** 保存设置：保存成功后重新发起订阅补额度（规格 MUST）；拒绝授权不阻塞保存 */
async function handleSave() {
  if (saving.value || !canSubmit()) return
  saving.value = true
  try {
    await reminderStore.save({
      masterSwitch: masterSwitch.value,
      breakfastSwitch: breakfastSwitch.value,
      breakfastTime: breakfastTime.value,
      lunchSwitch: lunchSwitch.value,
      lunchTime: lunchTime.value,
      dinnerSwitch: dinnerSwitch.value,
      dinnerTime: dinnerTime.value
    })
    track(TRACK_EVENT.REMINDER_SAVE)
    uni.showToast({ title: '已保存', icon: 'success' })
    // 保存成功后补订阅额度（游客不引导：无 openid 收不到推送）
    if (!userStore.isGuest && anyEnabled.value && templateId.value) {
      const accepted = await requestSubscribe()
      if (accepted) {
        try {
          await reminderStore.report()
          track(TRACK_EVENT.REMINDER_SUBSCRIBE)
        } catch {
          // 上报失败不阻塞
        }
      } else {
        uni.showToast({ title: '未授权将无法接收推送', icon: 'none' })
      }
    }
    // 重新加载以同步额度/快照
    await load()
  } catch {
    // request.ts 已统一 toast
  } finally {
    saving.value = false
  }
}

/** 额度为 0 且存在开启项：重新授权引导 */
async function handleReAuth() {
  if (!templateId.value) {
    uni.showToast({ title: '订阅模板未配置，请联系管理员', icon: 'none' })
    return
  }
  const accepted = await requestSubscribe()
  if (accepted) {
    try {
      await reminderStore.report()
      track(TRACK_EVENT.REMINDER_SUBSCRIBE)
      uni.showToast({ title: '已恢复推送额度', icon: 'success' })
      await load()
    } catch {
      // request.ts 已统一 toast
    }
  } else {
    uni.showToast({ title: '未授权将无法接收推送', icon: 'none' })
  }
}
</script>

<template>
  <view class="page">
    <!-- 游客提示（D5：可保存不可推送） -->
    <view v-if="userStore.isGuest" class="tip-card">
      <text class="tip-icon">🔔</text>
      <view class="tip-main">
        <text class="tip-title">授权登录后才能接收微信推送</text>
        <text class="tip-desc">当前可保存提醒设置，授权登录后自动迁移并开启推送</text>
      </view>
    </view>

    <!-- 额度耗尽提示（非游客 + 存在开启项 + 额度 0） -->
    <view v-else-if="anyEnabled && subscribeCredit === 0" class="tip-card warning">
      <text class="tip-icon">⚠️</text>
      <view class="tip-main">
        <text class="tip-title">推送额度已用完</text>
        <text class="tip-desc">点击右侧按钮重新授权即可恢复推送</text>
      </view>
      <view class="tip-action" @click="handleReAuth">重新授权</view>
    </view>

    <!-- 总开关 -->
    <view class="panel">
      <view class="row">
        <view class="row-main">
          <text class="row-title">饮食提醒总开关</text>
          <text class="row-sub">关闭后不再接收任何三餐提醒</text>
        </view>
        <switch :checked="masterSwitch === 1" color="#0d9488" @change="onMasterChange" />
      </view>
    </view>

    <!-- 三餐设置 -->
    <view class="panel">
      <text class="panel-title">三餐提醒</text>

      <view class="meal-row">
        <view class="row-main">
          <text class="row-title">早餐</text>
          <picker
            mode="time"
            :value="breakfastTime"
            :disabled="masterSwitch === 0 || breakfastSwitch === 0"
            @change="onBreakfastTime"
          >
            <view class="time-picker" :class="{ disabled: masterSwitch === 0 || breakfastSwitch === 0 }">
              {{ breakfastTime }}
            </view>
          </picker>
        </view>
        <switch
          :checked="breakfastSwitch === 1"
          :disabled="masterSwitch === 0"
          color="#0d9488"
          @change="onBreakfastSwitch"
        />
      </view>

      <view class="meal-row">
        <view class="row-main">
          <text class="row-title">午餐</text>
          <picker
            mode="time"
            :value="lunchTime"
            :disabled="masterSwitch === 0 || lunchSwitch === 0"
            @change="onLunchTime"
          >
            <view class="time-picker" :class="{ disabled: masterSwitch === 0 || lunchSwitch === 0 }">
              {{ lunchTime }}
            </view>
          </picker>
        </view>
        <switch
          :checked="lunchSwitch === 1"
          :disabled="masterSwitch === 0"
          color="#0d9488"
          @change="onLunchSwitch"
        />
      </view>

      <view class="meal-row">
        <view class="row-main">
          <text class="row-title">晚餐</text>
          <picker
            mode="time"
            :value="dinnerTime"
            :disabled="masterSwitch === 0 || dinnerSwitch === 0"
            @change="onDinnerTime"
          >
            <view class="time-picker" :class="{ disabled: masterSwitch === 0 || dinnerSwitch === 0 }">
              {{ dinnerTime }}
            </view>
          </picker>
        </view>
        <switch
          :checked="dinnerSwitch === 1"
          :disabled="masterSwitch === 0"
          color="#0d9488"
          @change="onDinnerSwitch"
        />
      </view>

      <text class="field-tip">· 到点未记录该餐时才会推送；已记录自动跳过</text>
      <text class="field-tip">· 一次授权仅支持一次推送，额度随授权增加</text>
    </view>

    <button class="btn-primary" :loading="saving" :disabled="loading" @click="handleSave">
      保存设置
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

/* 提示卡 */
.tip-card {
  background: #fffbf0;
  border: 1rpx solid #f5e6c0;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.tip-card.warning {
  background: #fff5f5;
  border-color: #fbc4c4;
}

.tip-icon {
  font-size: 40rpx;
}

.tip-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.tip-title {
  font-size: 26rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.tip-desc {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.5;
}

.tip-action {
  padding: 10rpx 24rpx;
  border-radius: 24rpx;
  background: $zhenxinjian-primary;
  color: $zhenxinjian-white;
  font-size: 24rpx;
  font-weight: 600;
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
  margin-bottom: 16rpx;
  display: block;
}

/* 行 */
.row,
.meal-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.meal-row {
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.meal-row:last-of-type {
  border-bottom: none;
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

/* 时间选择器 */
.time-picker {
  display: inline-block;
  padding: 8rpx 24rpx;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: $zhenxinjian-primary;
  font-weight: 600;
  align-self: flex-start;
}

.time-picker.disabled {
  color: $zhenxinjian-text-secondary;
  opacity: 0.5;
}

.field-tip {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  margin-top: 12rpx;
  display: block;
  line-height: 1.6;
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
