<script setup lang="ts">
/**
 * P11 添加饮食页：食物模式（路由带入 foodId + 克数，实时换算预览）/ 手动模式（名称 + 三宏 + 热量，前端校验）
 * 作者: wanglx
 */
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useDietStore } from '@/store/diet'
import { useFoodStore } from '@/store/food'
import { MEAL_TYPES } from '@/config/constants'
import type { FoodVO } from '@/api/food'
import { ymd } from '@/utils/format'
import { checkKcalConsistency } from '@/utils/validate'
import { track, trackPage } from '@/utils/track'
import { TRACK_EVENT } from '@/config/track-events'

const dietStore = useDietStore()
const foodStore = useFoodStore()

/** 份量克数区间 */
const GRAMS_MIN = 1
const GRAMS_MAX = 5000

/** 手动输入宏量上限 g */
const MACRO_MAX = 2000
/** 手动输入能量上限 kcal */
const KCAL_MAX = 20000

/**
 * 按当前时段取默认餐别（05–10 早 / 10–15 午 / 15–20:30 晚 / 其余加餐）
 */
function defaultMealType(): number {
  const h = new Date().getHours() + new Date().getMinutes() / 60
  if (h >= 5 && h < 10) return 1
  if (h >= 10 && h < 15) return 2
  if (h >= 15 && h < 20.5) return 3
  return 4
}

/** 模式：food 食物来源 / manual 手动输入 */
const mode = ref<'food' | 'manual'>('food')

/** 餐别（默认按时段选中可改） */
const mealType = ref(defaultMealType())

/** 记录日期（默认当日） */
const recordDate = ref(ymd(new Date()))

/** 备注 */
const remark = ref('')

/* ---------- 食物模式 ---------- */
const food = ref<FoodVO | null>(null)
const gramsInput = ref('')
const foodErr = ref('')

/** 当前试算克数 */
const grams = computed(() => {
  const n = Number(gramsInput.value)
  if (!gramsInput.value || Number.isNaN(n)) return null
  if (n < GRAMS_MIN || n > GRAMS_MAX) return null
  return Math.round(n)
})

/** 食物模式实时换算预览 */
const foodPreview = computed(() => {
  if (!food.value || grams.value === null) return null
  const r = grams.value / 100
  return {
    carb: Math.round(food.value.carb * r * 10) / 10,
    protein: Math.round(food.value.protein * r * 10) / 10,
    fat: Math.round(food.value.fat * r * 10) / 10,
    kcal: Math.round(food.value.kcal * r)
  }
})

/* ---------- 手动模式 ---------- */
const manualName = ref('')
const manualCarb = ref('')
const manualProtein = ref('')
const manualFat = ref('')
const manualKcal = ref('')
const manualErr = ref('')

/** 手动模式能量守恒校验（±10%） */
const manualValid = computed(() => {
  manualErr.value = ''
  const name = manualName.value.trim()
  const carb = Number(manualCarb.value)
  const protein = Number(manualProtein.value)
  const fat = Number(manualFat.value)
  const kcal = Number(manualKcal.value)

  if (!name) { manualErr.value = '请填写食物名称'; return false }
  if (name.length > 50) { manualErr.value = '食物名称过长（≤50字）'; return false }
  if (Number.isNaN(carb) || carb < 0 || carb > MACRO_MAX) { manualErr.value = '碳水须在 0-2000g 之间'; return false }
  if (Number.isNaN(protein) || protein < 0 || protein > MACRO_MAX) { manualErr.value = '蛋白须在 0-2000g 之间'; return false }
  if (Number.isNaN(fat) || fat < 0 || fat > MACRO_MAX) { manualErr.value = '脂肪须在 0-2000g 之间'; return false }
  if (Number.isNaN(kcal) || kcal < 0 || kcal > KCAL_MAX) { manualErr.value = '能量须在 0-20000kcal 之间'; return false }

  // 能量守恒：|碳水×4 + 蛋白×4 + 脂肪×9 − 能量| ÷ max(能量,1) ≤ 10%（统一走 utils/validate）
  const cc = checkKcalConsistency(carb, protein, fat, kcal)
  if (!cc.ok) {
    manualErr.value = `能量须约等于 碳水×4+蛋白×4+脂肪×9（当前差 ${Math.round(cc.diff)}kcal，偏差 ${(cc.ratio * 100).toFixed(1)}%）`
    return false
  }
  return true
})

/** 编辑模式ID（非空则为编辑） */
const editId = ref<number | null>(null)

/** 加载：路由带入 foodId + 克数进入食物模式；editId 编辑态从 diet store 当日分组按 id 回显，否则手动模式 */
onLoad(async (options) => {
  // 编辑模式：URL 仅带记录 id，回显数据从 diet store 当日分组查找（含中文备注不经 URL 编码）
  const eid = Number(options?.editId)
  if (eid) {
    editId.value = eid
    const record = dietStore.dayData?.meals
      .flatMap((m) => m.records)
      .find((r) => r.id === eid)
    if (!record) {
      // 深链/日期切换/记录已删：数据不在当前分组，引导返回重进
      uni.showToast({ title: '记录不存在或已过期，请返回重试', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 800)
      return
    }
    mealType.value = record.mealType
    remark.value = record.remark || ''

    if (record.source !== 3 && record.foodId) {
      // 食物来源编辑：回显食物 + 克数
      mode.value = 'food'
      try {
        food.value = await foodStore.detail(record.foodId)
        gramsInput.value = String(Math.round(record.amountG) || food.value.serving)
      } catch {
        foodErr.value = '食物加载失败，请返回重试'
      }
    } else {
      // 手动输入编辑：回显名称 + 三宏 + 热量
      mode.value = 'manual'
      manualName.value = record.foodName
      manualCarb.value = String(record.carbG)
      manualProtein.value = String(record.proteinG)
      manualFat.value = String(record.fatG)
      manualKcal.value = String(record.kcal)
    }
    return
  }

  // 新增模式
  const foodId = Number(options?.foodId)
  if (foodId) {
    mode.value = 'food'
    try {
      food.value = await foodStore.detail(foodId)
      gramsInput.value = String(Math.round(Number(options?.grams) || food.value.serving))
    } catch {
      foodErr.value = '食物加载失败，请返回重试'
    }
  } else {
    mode.value = 'manual'
  }
})

onShow(() => {
  trackPage('pages/record/add')
})

/** 克数输入 */
function handleGramsInput() {
  foodErr.value = ''
  if (grams.value === null && gramsInput.value) {
    foodErr.value = `克数须在 ${GRAMS_MIN}-${GRAMS_MAX} 之间`
  }
}

/** 切换模式 */
function switchMode(m: 'food' | 'manual') {
  mode.value = m
}

/** 提交 */
async function handleSubmit() {
  if (dietStore.submitting) return

  if (mode.value === 'food') {
    if (!food.value || grams.value === null) {
      uni.showToast({ title: '请确认食物与克数', icon: 'none' })
      return
    }
    try {
      if (editId.value) {
        // 编辑：食物来源仅改餐别/份量/备注
        await dietStore.editRecord(editId.value, {
          mealType: mealType.value,
          amountG: grams.value,
          remark: remark.value || undefined
        })
        track(TRACK_EVENT.RECORD_EDIT, { mealType: mealType.value })
        uni.showToast({ title: '已更新', icon: 'success' })
      } else {
        await dietStore.addRecord({
          mealType: mealType.value,
          source: food.value.source === 2 ? 2 : 1,
          foodId: food.value.id,
          amountG: grams.value,
          recordDate: recordDate.value,
          remark: remark.value || undefined
        })
        track(TRACK_EVENT.RECORD_ADD, { mealType: mealType.value, source: food.value.source === 2 ? 2 : 1 })
        uni.showToast({ title: '记录成功', icon: 'success' })
      }
      setTimeout(() => uni.switchTab({ url: '/pages/record/index' }), 800)
    } catch {
      // 错误已由 request.ts toast
    }
  } else {
    if (!manualValid.value) return
    try {
      if (editId.value) {
        // 编辑：手动输入可改名称与三宏热量
        await dietStore.editRecord(editId.value, {
          mealType: mealType.value,
          name: manualName.value.trim(),
          carb: Number(manualCarb.value),
          protein: Number(manualProtein.value),
          fat: Number(manualFat.value),
          kcal: Number(manualKcal.value),
          remark: remark.value || undefined
        })
        track(TRACK_EVENT.RECORD_EDIT, { mealType: mealType.value })
        uni.showToast({ title: '已更新', icon: 'success' })
      } else {
        await dietStore.addRecord({
          mealType: mealType.value,
          source: 3,
          name: manualName.value.trim(),
          carb: Number(manualCarb.value),
          protein: Number(manualProtein.value),
          fat: Number(manualFat.value),
          kcal: Number(manualKcal.value),
          recordDate: recordDate.value,
          remark: remark.value || undefined
        })
        track(TRACK_EVENT.RECORD_ADD, { mealType: mealType.value, source: 3 })
        uni.showToast({ title: '记录成功', icon: 'success' })
      }
      setTimeout(() => uni.switchTab({ url: '/pages/record/index' }), 800)
    } catch {
      // 错误已由 request.ts toast
    }
  }
}
</script>

<template>
  <view class="page">
    <!-- 餐别选择 -->
    <view class="panel">
      <text class="panel-title">餐别</text>
      <view class="meal-tabs">
        <view
          v-for="m in MEAL_TYPES"
          :key="m.code"
          class="meal-tab"
          :class="{ active: mealType === m.code }"
          @click="mealType = m.code"
        >
          {{ m.name }}
        </view>
      </view>
    </view>

    <!-- 模式切换（仅无 foodId 时显示） -->
    <view v-if="!food" class="panel">
      <view class="mode-tabs">
        <view class="mode-tab" :class="{ active: mode === 'manual' }" @click="switchMode('manual')">手动输入</view>
      </view>
    </view>

    <!-- 食物模式 -->
    <template v-if="mode === 'food' && food">
      <view class="panel">
        <view class="food-head">
          <image v-if="food.image" :src="food.image" mode="aspectFill" class="food-thumb" />
          <view v-else class="food-thumb food-thumb-empty"><text class="food-thumb-emoji">🥗</text></view>
          <text class="food-name">{{ food.name }}</text>
          <text v-if="food.source === 2" class="tag">自定义</text>
        </view>
        <text class="food-cat">{{ food.categoryName }} · 每 100g</text>
        <view class="food-grid">
          <view class="cell"><text class="num">{{ food.carb }}</text><text class="lbl">碳水</text></view>
          <view class="cell"><text class="num">{{ food.protein }}</text><text class="lbl">蛋白</text></view>
          <view class="cell"><text class="num">{{ food.fat }}</text><text class="lbl">脂肪</text></view>
          <view class="cell"><text class="num">{{ food.kcal }}</text><text class="lbl">kcal</text></view>
        </view>
      </view>

      <view class="panel">
        <text class="panel-title">份量（克）</text>
        <view class="grams-row">
          <input v-model="gramsInput" class="grams-input" type="number" placeholder="输入克数" @input="handleGramsInput" />
          <text class="grams-unit">g</text>
        </view>
        <text v-if="foodErr" class="err">{{ foodErr }}</text>

        <view v-if="foodPreview" class="preview">
          <text class="preview-title">摄入预览</text>
          <view class="preview-grid">
            <view class="cell"><text class="num hl">{{ foodPreview.carb }}</text><text class="lbl">碳水 g</text></view>
            <view class="cell"><text class="num hl">{{ foodPreview.protein }}</text><text class="lbl">蛋白 g</text></view>
            <view class="cell"><text class="num hl">{{ foodPreview.fat }}</text><text class="lbl">脂肪 g</text></view>
            <view class="cell"><text class="num hl">{{ foodPreview.kcal }}</text><text class="lbl">kcal</text></view>
          </view>
        </view>
      </view>
    </template>

    <!-- 手动模式 -->
    <template v-if="mode === 'manual'">
      <view class="panel">
        <text class="panel-title">食物信息</text>
        <view class="field">
          <text class="field-label">名称</text>
          <input v-model="manualName" class="field-input" placeholder="吃了什么？（≤50字）" maxlength="50" />
        </view>
        <view class="field">
          <text class="field-label">碳水 g</text>
          <input v-model="manualCarb" class="field-input" type="digit" placeholder="实际吃下的碳水总量" />
        </view>
        <view class="field">
          <text class="field-label">蛋白 g</text>
          <input v-model="manualProtein" class="field-input" type="digit" placeholder="实际吃下的蛋白总量" />
        </view>
        <view class="field">
          <text class="field-label">脂肪 g</text>
          <input v-model="manualFat" class="field-input" type="digit" placeholder="实际吃下的脂肪总量" />
        </view>
        <view class="field">
          <text class="field-label">能量 kcal</text>
          <input v-model="manualKcal" class="field-input" type="number" placeholder="实际吃下的总热量" />
        </view>
        <text v-if="manualErr" class="err">{{ manualErr }}</text>
        <text class="hint">能量须约等于 碳水×4 + 蛋白×4 + 脂肪×9（偏差 ≤10%）</text>
      </view>
    </template>

    <!-- 备注 -->
    <view class="panel">
      <view class="field">
        <text class="field-label">备注</text>
        <input v-model="remark" class="field-input" placeholder="可空，≤100字" maxlength="100" />
      </view>
    </view>

    <!-- 提交 -->
    <view class="submit-area">
      <view class="submit-btn" :class="{ disabled: dietStore.submitting }" @click="handleSubmit">
        {{ dietStore.submitting ? '提交中...' : (editId ? '确认修改' : '确认记录') }}
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

/* 餐别段选 */
.meal-tabs {
  display: flex;
  gap: 12rpx;
}

.meal-tab {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: $zhenxinjian-text;
}

.meal-tab.active {
  background: $zhenxinjian-primary;
  color: #fff;
  border-color: $zhenxinjian-primary;
}

/* 模式切换 */
.mode-tabs {
  display: flex;
  gap: 12rpx;
}

.mode-tab {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: $zhenxinjian-text;
}

.mode-tab.active {
  background: $zhenxinjian-primary;
  color: #fff;
  border-color: $zhenxinjian-primary;
}

/* 食物信息 */
.food-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.food-thumb {
  width: 88rpx;
  height: 88rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
  background: $zhenxinjian-bg;
}

.food-thumb-empty {
  display: flex;
  align-items: center;
  justify-content: center;
}

.food-thumb-emoji {
  font-size: 44rpx;
}

.food-name {
  font-size: 32rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.tag {
  font-size: 20rpx;
  color: $zhenxinjian-primary;
  background: $zhenxinjian-primary-light;
  border-radius: 8rpx;
  padding: 2rpx 10rpx;
}

.food-cat {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  display: block;
  margin-top: 8rpx;
}

.food-grid, .preview-grid {
  display: flex;
  flex-wrap: wrap;
  margin-top: 20rpx;
}

.cell {
  width: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20rpx;
}

.num {
  font-size: 32rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.num.hl {
  color: $zhenxinjian-primary;
}

.lbl {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  margin-top: 4rpx;
}

/* 克数输入 */
.grams-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.grams-input {
  flex: 1;
  height: 80rpx;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 30rpx;
  color: $zhenxinjian-text;
  box-sizing: border-box;
}

.grams-unit {
  font-size: 26rpx;
  color: $zhenxinjian-text-secondary;
}

/* 摄入预览 */
.preview {
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid $zhenxinjian-border;
}

.preview-title {
  font-size: 26rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  display: block;
  margin-bottom: 12rpx;
}

/* 手动输入表单 */
.field {
  margin-bottom: 24rpx;
}

.field-label {
  font-size: 26rpx;
  color: $zhenxinjian-text;
  display: block;
  margin-bottom: 12rpx;
}

.field-input {
  height: 80rpx;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: $zhenxinjian-text;
  box-sizing: border-box;
  width: 100%;
}

.err {
  font-size: 22rpx;
  color: #f56c6c;
  margin-top: 12rpx;
  display: block;
}

.hint {
  font-size: 20rpx;
  color: $zhenxinjian-text-secondary;
  margin-top: 12rpx;
  display: block;
}

/* 提交 */
.submit-area {
  margin-top: 32rpx;
}

.submit-btn {
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  background: $zhenxinjian-primary;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 12rpx;
}

.submit-btn.disabled {
  opacity: 0.6;
}
</style>
