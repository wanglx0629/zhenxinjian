<script setup lang="ts">
/**
 * 自定义食物录入/编辑页：名称 + 每 100g 三宏 + 能量
 * 前端先做区间与 ±10% 守恒校验（与后端同规则），提交以后端为准
 * 作者: wanglx
 */
import { computed, reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useFoodStore } from '@/store/food'
import type { FoodCategoryVO } from '@/api/food'
import { canSubmit } from '@/utils/throttle'
import { track, trackPage } from '@/utils/track'
import { TRACK_EVENT } from '@/config/track-events'
import { checkKcalConsistency, kcalFromMacros } from '@/utils/validate'

const foodStore = useFoodStore()

/** 宏量区间：每 100g 值非负且 ≤100 */
const MACRO_MAX = 100
/** 能量区间（与后端 MacroConsistencyValidator.PER_100G_KCAL_MAX 一致） */
const KCAL_MAX = 900

/** 编辑模式：路由带 id 参数 */
const editId = ref<number | null>(null)
const categories = ref<FoodCategoryVO[]>([])
const submitting = ref(false)
const errors = reactive<Record<string, string>>({})

const form = reactive({
  name: '',
  alias: '',
  categoryCode: '10',
  carb: '',
  protein: '',
  fat: '',
  kcal: '',
  serving: ''
})

const title = computed(() => (editId.value ? '编辑自定义食物' : '新增自定义食物'))

/** 当前分类在列表中的下标（picker 选中态） */
const categoryIndex = computed(() =>
  Math.max(0, categories.value.findIndex((c) => c.code === form.categoryCode))
)

function handleCategoryChange(e: any) {
  const idx = Number(e.detail.value)
  if (categories.value[idx]) {
    form.categoryCode = categories.value[idx].code
  }
}

onLoad(async (options) => {
  categories.value = await foodStore.fetchCategories()
  const id = Number(options?.id)
  if (!id) return
  editId.value = id
  uni.setNavigationBarTitle({ title: '编辑自定义食物' })
  try {
    const food = await foodStore.detail(id)
    form.name = food.name
    form.alias = food.alias || ''
    form.categoryCode = food.categoryCode || '10'
    form.carb = String(food.carb)
    form.protein = String(food.protein)
    form.fat = String(food.fat)
    form.kcal = String(food.kcal)
    form.serving = String(Math.round(food.serving))
  } catch {
    uni.showToast({ title: '食物加载失败', icon: 'none' })
  }
})

onShow(() => {
  trackPage('pages/food/custom-edit')
})

/** 数值解析：空/非法返回 null */
function num(v: string): number | null {
  if (!v.trim()) return null
  const n = Number(v)
  return Number.isNaN(n) ? null : n
}

/** 前端校验：区间 + 能量守恒 ±10%（与后端 CustomFoodService 同规则） */
function validate(): boolean {
  Object.keys(errors).forEach((k) => delete errors[k])
  if (!form.name.trim()) {
    errors.name = '请输入食物名称'
  } else if (form.name.trim().length > 100) {
    errors.name = '名称过长（≤100字）'
  }
  const macroFields: Array<[string, string, string]> = [
    ['carb', '碳水', '碳水化合物'],
    ['protein', '蛋白', '蛋白质'],
    ['fat', '脂肪', '脂肪']
  ]
  for (const [key, label, fullName] of macroFields) {
    const v = num((form as Record<string, string>)[key])
    if (v === null || v < 0 || v > MACRO_MAX) {
      errors[key] = `${fullName}须在 0-${MACRO_MAX}g 之间`
      continue
    }
    if (Math.round(v * 10) / 10 !== v) {
      errors[key] = `${label}最多 1 位小数`
    }
  }
  const kcal = num(form.kcal)
  if (kcal === null || kcal < 0 || kcal > KCAL_MAX || !Number.isInteger(kcal)) {
    errors.kcal = `能量须在 0-${KCAL_MAX} kcal 整数之间`
  }
  const serving = form.serving.trim() ? num(form.serving) : 100
  if (serving === null || serving < 5 || serving > 1000) {
    errors.serving = '单份克数须在 5-1000g 之间'
  }
  // 守恒校验：|碳水×4 + 蛋白×4 + 脂肪×9 − 能量| ÷ max(能量,1) ≤ 10%（统一走 utils/validate）
  if (!errors.carb && !errors.protein && !errors.fat && !errors.kcal) {
    const carb = num(form.carb)!
    const protein = num(form.protein)!
    const fat = num(form.fat)!
    if (!checkKcalConsistency(carb, protein, fat, kcal!).ok) {
      errors.kcal = `能量与宏量不匹配：${carb}g碳水+${protein}g蛋白+${fat}g脂肪约产 ${Math.round(kcalFromMacros(carb, protein, fat))} kcal`
    }
  }
  return Object.keys(errors).length === 0
}

/** 提交：校验 → 保存 → 返回 */
async function handleSubmit() {
  if (!validate()) {
    uni.showToast({ title: '请检查填写内容', icon: 'none' })
    return
  }
  if (submitting.value || !canSubmit()) return
  submitting.value = true
  try {
    await foodStore.saveCustom({
      id: editId.value ?? undefined,
      name: form.name.trim(),
      alias: form.alias.trim() || undefined,
      categoryCode: form.categoryCode,
      carb: num(form.carb)!,
      protein: num(form.protein)!,
      fat: num(form.fat)!,
      kcal: num(form.kcal)!,
      serving: form.serving.trim() ? num(form.serving)! : undefined
    })
    if (!editId.value) track(TRACK_EVENT.FOOD_CUSTOM_ADD)
    uni.showToast({ title: editId.value ? '保存成功' : '新增成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch {
    // request.ts 已统一 toast
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <view class="page">
    <view class="panel">
      <text class="panel-title">{{ title }}</text>

      <view class="field">
        <text class="label">食物名称</text>
        <input v-model="form.name" class="input" placeholder="如：自制燕麦碗" />
        <text v-if="errors.name" class="err">{{ errors.name }}</text>
      </view>

      <view class="field">
        <text class="label">别名/俗称（选填）</text>
        <input v-model="form.alias" class="input" placeholder="便于搜索到的俗称" />
      </view>

      <view class="field">
        <text class="label">分类</text>
        <picker :value="categoryIndex" :range="categories" range-key="name" @change="handleCategoryChange">
          <view class="picker">
            {{ categories.find((c) => c.code === form.categoryCode)?.name || '请选择分类' }}
          </view>
        </picker>
      </view>

      <view class="field">
        <text class="label">碳水（g/100g，0-100）</text>
        <input v-model="form.carb" class="input" type="digit" placeholder="每 100g 碳水克数" />
        <text v-if="errors.carb" class="err">{{ errors.carb }}</text>
      </view>

      <view class="field">
        <text class="label">蛋白（g/100g，0-100）</text>
        <input v-model="form.protein" class="input" type="digit" placeholder="每 100g 蛋白克数" />
        <text v-if="errors.protein" class="err">{{ errors.protein }}</text>
      </view>

      <view class="field">
        <text class="label">脂肪（g/100g，0-100）</text>
        <input v-model="form.fat" class="input" type="digit" placeholder="每 100g 脂肪克数" />
        <text v-if="errors.fat" class="err">{{ errors.fat }}</text>
      </view>

      <view class="field">
        <text class="label">能量（kcal/100g，0-900）</text>
        <input v-model="form.kcal" class="input" type="number" placeholder="每 100g 能量" />
        <text v-if="errors.kcal" class="err">{{ errors.kcal }}</text>
      </view>

      <view class="field">
        <text class="label">常用单份克数（选填，5-1000，默认 100）</text>
        <input v-model="form.serving" class="input" type="digit" placeholder="默认 100" />
        <text v-if="errors.serving" class="err">{{ errors.serving }}</text>
      </view>
    </view>

    <view class="tip">
      <text>能量须与宏量守恒：碳水×4 + 蛋白×4 + 脂肪×9 ≈ 能量（偏差 ≤10%）</text>
    </view>

    <button class="btn-primary" :loading="submitting" @click="handleSubmit">
      {{ editId ? '保存修改' : '创建食物' }}
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

.field {
  margin-bottom: 28rpx;
}

.label {
  font-size: 26rpx;
  color: $zhenxinjian-text-secondary;
  margin-bottom: 12rpx;
  display: block;
}

.input {
  height: 80rpx;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: $zhenxinjian-radius-md;
  padding: 0 24rpx;
  font-size: 30rpx;
  color: $zhenxinjian-text;
  box-sizing: border-box;
}

.picker {
  height: 80rpx;
  line-height: 80rpx;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: $zhenxinjian-radius-md;
  padding: 0 24rpx;
  font-size: 30rpx;
  color: $zhenxinjian-text;
  box-sizing: border-box;
}

.err {
  font-size: 22rpx;
  color: $zhenxinjian-danger;
  margin-top: 8rpx;
  display: block;
}

.tip {
  padding: 0 8rpx 24rpx;
}

.tip text {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
}


</style>
