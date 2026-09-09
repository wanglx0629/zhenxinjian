<script setup lang="ts">
/**
 * 食物详情页：每 100g 营养值展示 + 份量克数输入实时试算（默认常用单份克数）
 * 本期仅查看与试算，「写入饮食记录」入口留待 Change 5
 * 作者: wanglx
 */
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useFoodStore } from '@/store/food'
import type { FoodCalcVO, FoodVO } from '@/api/food'

const foodStore = useFoodStore()

/** 试算克数区间（与后端一致） */
const GRAMS_MIN = 1
const GRAMS_MAX = 10000

const food = ref<FoodVO | null>(null)
const gramsInput = ref('')
const calcResult = ref<FoodCalcVO | null>(null)
const loadFailed = ref(false)
const calcErr = ref('')

/** 当前试算克数（取输入值；非法时不参与换算） */
const grams = computed(() => {
  const n = Number(gramsInput.value)
  if (!gramsInput.value || Number.isNaN(n)) return null
  if (n < GRAMS_MIN || n > GRAMS_MAX) return null
  return Math.round(n)
})

/** 加载食物详情并默认以常用单份克数试算 */
onLoad(async (options) => {
  const id = Number(options?.id)
  if (!id) {
    loadFailed.value = true
    return
  }
  try {
    food.value = await foodStore.detail(id)
    gramsInput.value = String(Math.round(food.value.serving))
    await runCalc()
  } catch {
    loadFailed.value = true
  }
})

/** 执行试算：在线优先后端，失败/降级由 store 本地换算 */
async function runCalc() {
  calcErr.value = ''
  if (!food.value) return
  if (grams.value === null) {
    calcResult.value = null
    calcErr.value = `克数须在 ${GRAMS_MIN}-${GRAMS_MAX} 之间`
    return
  }
  calcResult.value = await foodStore.calc(food.value, grams.value)
}

/** 克数输入：300ms 防抖后实时换算 */
let calcTimer: ReturnType<typeof setTimeout> | null = null
function handleGramsInput() {
  if (calcTimer) clearTimeout(calcTimer)
  calcTimer = setTimeout(() => runCalc(), 300)
}

/** 快捷设置常用单份克数 */
function useServing() {
  if (!food.value) return
  gramsInput.value = String(Math.round(food.value.serving))
  runCalc()
}

/** 快捷档位（100g 基准） */
function setGrams(g: number) {
  gramsInput.value = String(g)
  runCalc()
}
</script>

<template>
  <view class="page">
    <view v-if="loadFailed" class="empty">
      <text class="empty-title">食物加载失败</text>
      <text class="empty-desc">请返回重试</text>
    </view>

    <template v-else-if="food">
      <view class="panel">
        <view class="head">
          <text class="name">{{ food.name }}</text>
          <text v-if="food.source === 2" class="tag">自定义</text>
        </view>
        <text v-if="food.alias" class="alias">{{ food.alias }}</text>
        <text class="category">{{ food.categoryName }} · 每 100g 可食部</text>

        <view class="grid">
          <view class="cell">
            <text class="num">{{ food.carb }}</text>
            <text class="lbl">碳水 g</text>
          </view>
          <view class="cell">
            <text class="num">{{ food.protein }}</text>
            <text class="lbl">蛋白 g</text>
          </view>
          <view class="cell">
            <text class="num">{{ food.fat }}</text>
            <text class="lbl">脂肪 g</text>
          </view>
          <view class="cell">
            <text class="num">{{ food.kcal }}</text>
            <text class="lbl">能量 kcal</text>
          </view>
        </view>
      </view>

      <view class="panel">
        <text class="panel-title">份量试算</text>
        <view class="grams-row">
          <input
            v-model="gramsInput"
            class="grams-input"
            type="number"
            placeholder="输入克数"
            @input="handleGramsInput"
          />
          <text class="grams-unit">g</text>
          <view class="serving-btn" @click="useServing">常用单份 {{ food.serving }}g</view>
        </view>
        <view class="quick">
          <view class="quick-item" @click="setGrams(50)">50g</view>
          <view class="quick-item" @click="setGrams(100)">100g</view>
          <view class="quick-item" @click="setGrams(150)">150g</view>
          <view class="quick-item" @click="setGrams(200)">200g</view>
        </view>
        <text v-if="calcErr" class="err">{{ calcErr }}</text>

        <view v-if="calcResult" class="calc-grid">
          <view class="cell">
            <text class="num hl">{{ calcResult.carb }}</text>
            <text class="lbl">碳水 g</text>
          </view>
          <view class="cell">
            <text class="num hl">{{ calcResult.protein }}</text>
            <text class="lbl">蛋白 g</text>
          </view>
          <view class="cell">
            <text class="num hl">{{ calcResult.fat }}</text>
            <text class="lbl">脂肪 g</text>
          </view>
          <view class="cell">
            <text class="num hl">{{ calcResult.kcal }}</text>
            <text class="lbl">能量 kcal</text>
          </view>
        </view>
      </view>

      <view class="tip">
        <text>试算结果仅供记录参考，写入饮食记录功能即将上线</text>
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

.head {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.name {
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

.alias {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
  display: block;
  margin-top: 8rpx;
}

.category {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  display: block;
  margin-top: 8rpx;
}

.grid,
.calc-grid {
  display: flex;
  flex-wrap: wrap;
  margin-top: 24rpx;
}

.cell {
  width: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24rpx;
}

.num {
  font-size: 36rpx;
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

.panel-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 20rpx;
  display: block;
}

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

.serving-btn {
  padding: 14rpx 20rpx;
  border-radius: 12rpx;
  background: $zhenxinjian-primary-light;
  color: $zhenxinjian-primary;
  font-size: 24rpx;
}

.quick {
  display: flex;
  gap: 16rpx;
  margin-top: 20rpx;
}

.quick-item {
  flex: 1;
  height: 64rpx;
  line-height: 64rpx;
  text-align: center;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  font-size: 24rpx;
  color: $zhenxinjian-text;
}

.err {
  font-size: 22rpx;
  color: #f56c6c;
  margin-top: 12rpx;
  display: block;
}

.tip {
  padding: 0 8rpx;
}

.tip text {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
}

.empty {
  padding: 80rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.empty-title {
  font-size: 28rpx;
  color: $zhenxinjian-text;
  margin-bottom: 12rpx;
}

.empty-desc {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
}
</style>
