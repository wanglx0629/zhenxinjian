<script setup lang="ts">
/**
 * 拍照识别食物页：选图（相册/相机）→ 上传识别 → 候选卡片（每 100g 四值 + 克数）
 * → 勾选后逐条走 source=3 手动记录链路加入今日记录（敏感词/守恒校验复用后端既有体系）
 * 作者: wanglx
 */
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { recognizeFood, type FoodRecognizeVO } from '@/api/ai'
import { useDietStore } from '@/store/diet'
import { MEAL_TYPES } from '@/config/constants'
import { track, trackPage } from '@/utils/track'
import { TRACK_EVENT } from '@/config/track-events'
import EmptyState from '@/components/EmptyState.vue'
import { iconSrc } from '@/utils/icons'

const cameraIcon = iconSrc('camera', '#008A63')
const checkIcon = iconSrc('check', '#ffffff')

const dietStore = useDietStore()

/** 克数区间（与添加饮食页一致） */
const GRAMS_MIN = 1
const GRAMS_MAX = 5000

/** 识别阶段：idle 未选图 / loading 识别中 / done 有结果 / empty 未识别到食物 */
type Phase = 'idle' | 'loading' | 'done' | 'empty'
const phase = ref<Phase>('idle')

/** 本地待上传图片路径 */
const imagePath = ref('')

/** 候选项（附带勾选态与克数输入） */
interface Candidate extends FoodRecognizeVO {
  checked: boolean
  gramsInput: string
  invalid: boolean
}
const candidates = ref<Candidate[]>([])

/** 餐别（默认按时段选中可改，与添加饮食页同口径） */
function defaultMealType(): number {
  const h = new Date().getHours() + new Date().getMinutes() / 60
  if (h >= 5 && h < 10) return 1
  if (h >= 10 && h < 15) return 2
  if (h >= 15 && h < 20.5) return 3
  return 4
}
const mealType = ref(defaultMealType())

/** 选中且克数合法的候选项 */
const selectedItems = computed(() => candidates.value.filter((c) => c.checked && !c.invalid && Number(c.gramsInput) > 0))

onShow(() => {
  trackPage('pages/record/recognize')
})

/** 选图并识别（相册/相机二选一由系统 actionSheet 决定） */
function chooseImage() {
  if (phase.value === 'loading') return
  uni.chooseImage({
    count: 1,
    sourceType: ['album', 'camera'],
    success: (res) => {
      const path = res.tempFilePaths?.[0]
      if (!path) return
      imagePath.value = path
      void doRecognize(path)
    }
  })
}

/** 调识别接口 */
async function doRecognize(path: string) {
  phase.value = 'loading'
  try {
    const items = await recognizeFood(path)
    candidates.value = items.map((item) => ({
      ...item,
      checked: true,
      gramsInput: '100',
      invalid: false
    }))
    phase.value = items.length > 0 ? 'done' : 'empty'
  } catch {
    // 错误提示已由 api 层 toast；回到选图态允许重试
    phase.value = candidates.value.length > 0 ? 'done' : 'idle'
  }
}

/** 克数输入校验 */
function handleGramsInput(item: Candidate) {
  const n = Number(item.gramsInput)
  item.invalid = item.gramsInput !== '' && (Number.isNaN(n) || n < GRAMS_MIN || n > GRAMS_MAX)
}

/** 1 位小数四舍五入 */
function round1(v: number): number {
  return Math.round(v * 10) / 10
}

/** 按克数换算实际摄入（每 100g 值 × 克数 ÷ 100） */
function scale(item: Candidate) {
  const r = Number(item.gramsInput) / 100
  return {
    carb: round1(item.carb * r),
    protein: round1(item.protein * r),
    fat: round1(item.fat * r),
    kcal: round1(item.kcal * r)
  }
}

/** 勾选项逐条加入今日记录（source=3 手动链路；任一失败则中止，已成功的不回滚） */
async function handleSave() {
  if (dietStore.submitting) return
  const items = selectedItems.value
  if (items.length === 0) {
    uni.showToast({ title: '请勾选食物并确认克数', icon: 'none' })
    return
  }
  let saved = 0
  for (const item of items) {
    const scaled = scale(item)
    try {
      await dietStore.addRecord({
        mealType: mealType.value,
        source: 3,
        name: item.name,
        carb: scaled.carb,
        protein: scaled.protein,
        fat: scaled.fat,
        kcal: scaled.kcal
      })
      track(TRACK_EVENT.RECORD_ADD, { mealType: mealType.value, source: 3, from: 'ocr' })
      saved += 1
    } catch {
      // 错误已由 request.ts toast（如敏感词拦截）；中止后续，保留页面与已成功记录
      break
    }
  }
  if (saved > 0) {
    uni.showToast({ title: saved === items.length ? '记录成功' : `已加入 ${saved} 条`, icon: 'none' })
    setTimeout(() => uni.switchTab({ url: '/pages/record/index' }), 800)
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

    <!-- 选图 / 识别中 -->
    <view class="panel picker-panel" @click="chooseImage">
      <image v-if="imagePath && phase !== 'idle'" :src="imagePath" mode="aspectFill" class="preview-img" />
      <view v-else class="picker-placeholder">
        <image class="picker-icon" :src="cameraIcon" />
        <text class="picker-text">拍照 / 从相册选一张食物图</text>
        <text class="picker-hint">支持 jpg、png、webp，≤10MB</text>
      </view>
      <view v-if="phase === 'loading'" class="picker-mask">
        <text class="picker-loading">AI 识别中…</text>
      </view>
      <view v-if="phase === 'done' || phase === 'empty'" class="repick-btn">重新选图</view>
    </view>

    <!-- 未识别到食物 -->
    <EmptyState
      v-if="phase === 'empty'"
      type="search"
      title="未识别到食物"
      desc="换张清晰、光线充足的食物照片再试一次吧"
      btn-text="重新拍照"
      @action="chooseImage"
    />

    <!-- 初始引导 -->
    <EmptyState
      v-if="phase === 'idle'"
      type="bowl"
      title="拍一拍，自动识别"
      desc="AI 识别图中食物并给出每 100g 碳水/蛋白/脂肪/热量，勾选即可记录"
    />

    <!-- 候选列表 -->
    <template v-if="phase === 'done'">
      <view v-for="(item, idx) in candidates" :key="idx" class="panel candidate-card">
        <view class="candidate-head" @click="item.checked = !item.checked">
          <view class="checkbox" :class="{ checked: item.checked }">
            <image v-if="item.checked" class="checkbox-tick" :src="checkIcon" />
          </view>
          <text class="candidate-name">{{ item.name }}</text>
        </view>
        <view class="macro-grid">
          <view class="cell"><text class="num">{{ item.carb }}</text><text class="lbl">碳水</text></view>
          <view class="cell"><text class="num">{{ item.protein }}</text><text class="lbl">蛋白</text></view>
          <view class="cell"><text class="num">{{ item.fat }}</text><text class="lbl">脂肪</text></view>
          <view class="cell"><text class="num">{{ item.kcal }}</text><text class="lbl">kcal</text></view>
        </view>
        <text class="macro-hint">以上为每 100g 含量</text>

        <view class="grams-row">
          <text class="grams-label">吃了</text>
          <input
            v-model="item.gramsInput"
            class="grams-input"
            type="digit"
            :disabled="!item.checked"
            @input="handleGramsInput(item)"
          />
          <text class="grams-unit">克</text>
        </view>
        <text v-if="item.invalid" class="err">克数须在 {{ GRAMS_MIN }}-{{ GRAMS_MAX }} 之间</text>

        <view v-if="item.checked && !item.invalid && Number(item.gramsInput) > 0" class="intake">
          <text class="intake-title">摄入预览</text>
          <view class="macro-grid">
            <view class="cell"><text class="num hl">{{ scale(item).carb }}</text><text class="lbl">碳水 g</text></view>
            <view class="cell"><text class="num hl">{{ scale(item).protein }}</text><text class="lbl">蛋白 g</text></view>
            <view class="cell"><text class="num hl">{{ scale(item).fat }}</text><text class="lbl">脂肪 g</text></view>
            <view class="cell"><text class="num hl">{{ scale(item).kcal }}</text><text class="lbl">kcal</text></view>
          </view>
        </view>
      </view>

      <view class="submit-bar">
        <text class="submit-count">已选 {{ selectedItems.length }} 项</text>
        <view class="submit-btn" hover-class="submit-btn-hover" :class="{ disabled: dietStore.submitting || selectedItems.length === 0 }" @click="handleSave">
          {{ dietStore.submitting ? '保存中...' : '加入今日记录' }}
        </view>
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
  border-radius: $zhenxinjian-radius-lg;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: $zhenxinjian-shadow-card;
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
  border: 1rpx solid $zhenxinjian-border-input;
  border-radius: $zhenxinjian-radius-md;
  font-size: 26rpx;
  color: $zhenxinjian-text;
}

.meal-tab.active {
  background: $zhenxinjian-gradient-brand;
  color: $zhenxinjian-white;
  border-color: $zhenxinjian-primary;
}

/* 选图区 */
.picker-panel {
  position: relative;
  padding: 0;
  overflow: hidden;
}

.preview-img {
  display: block;
  width: 100%;
  height: 420rpx;
}

.picker-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 64rpx 32rpx;
  border: 2rpx dashed $zhenxinjian-primary-lighter;
  border-radius: $zhenxinjian-radius-md;
  background: $zhenxinjian-primary-bg;
}

.picker-icon {
  width: 72rpx;
  height: 72rpx;
  margin-bottom: 16rpx;
}

.picker-text {
  font-size: 28rpx;
  font-weight: 600;
  color: $zhenxinjian-primary-dark;
}

.picker-hint {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  margin-top: 8rpx;
}

.picker-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.72);
}

.picker-loading {
  font-size: 30rpx;
  font-weight: 600;
  color: $zhenxinjian-primary;
}

.repick-btn {
  position: absolute;
  right: 20rpx;
  bottom: 20rpx;
  padding: 10rpx 28rpx;
  border-radius: $zhenxinjian-radius-pill;
  background: rgba(0, 172, 124, 0.88);
  color: $zhenxinjian-white;
  font-size: 24rpx;
}

/* 候选卡片 */
.candidate-head {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.checkbox {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  border: 2rpx solid $zhenxinjian-primary-lighter;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.checkbox.checked {
  background: $zhenxinjian-gradient-cta;
  border-color: $zhenxinjian-cta;
}

.checkbox-tick {
  width: 24rpx;
  height: 24rpx;
}

.candidate-name {
  font-size: 32rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.macro-grid {
  display: flex;
  flex-wrap: wrap;
  margin-top: 20rpx;
}

.cell {
  width: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 16rpx;
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

.macro-hint {
  display: block;
  text-align: center;
  font-size: 20rpx;
  color: $zhenxinjian-text-placeholder;
}

.grams-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid $zhenxinjian-border;
}

.grams-label {
  font-size: 26rpx;
  color: $zhenxinjian-text;
}

.grams-input {
  flex: 1;
  height: 72rpx;
  border: 1rpx solid $zhenxinjian-border-input;
  border-radius: $zhenxinjian-radius-md;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: $zhenxinjian-text;
  box-sizing: border-box;
}

.grams-unit {
  font-size: 26rpx;
  color: $zhenxinjian-text-secondary;
}

.err {
  display: block;
  font-size: 22rpx;
  color: $zhenxinjian-danger;
  margin-top: 12rpx;
}

.intake {
  margin-top: 8rpx;
}

.intake-title {
  display: block;
  font-size: 24rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 4rpx;
}

/* 底部提交条 */
.submit-bar {
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-top: 8rpx;
}

.submit-count {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
  flex-shrink: 0;
}

.submit-btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  background: $zhenxinjian-gradient-cta;
  color: $zhenxinjian-white;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: $zhenxinjian-radius-pill;
  box-shadow: $zhenxinjian-shadow-fab;
}

.submit-btn-hover {
  background: $zhenxinjian-cta-active;
}

.submit-btn.disabled {
  opacity: 0.55;
  box-shadow: none;
}
</style>
