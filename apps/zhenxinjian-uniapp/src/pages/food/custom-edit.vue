<script setup lang="ts">
/**
 * 自定义食物录入/编辑页（共建投稿）：图片 + 每 100g 三宏 + 能量（kcal/kJ 二选一联动）
 * 前端先做区间与 ±10% 守恒校验（与后端同规则），提交以后端为准
 * 提交后进入待审核（本人立即可用，审核通过后他人可见）
 * 作者: wanglx
 */
import { computed, reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useFoodStore, resolveImage } from '@/store/food'
import type { FoodCategoryVO } from '@/api/food'
import { uploadFoodImage } from '@/api/food'
import { canSubmit } from '@/utils/throttle'
import { track, trackPage } from '@/utils/track'
import { TRACK_EVENT } from '@/config/track-events'
import { checkKcalConsistency, kcalFromMacros } from '@/utils/validate'
import { iconSrc } from '@/utils/icons'

const cameraIcon = iconSrc('camera', '#94A3B8')

const foodStore = useFoodStore()

/** 宏量区间：每 100g 值非负且 ≤100 */
const MACRO_MAX = 100
/** 能量区间（与后端 MacroConsistencyValidator.PER_100G_KCAL_MAX 一致） */
const KCAL_MAX = 900
/** 能量千焦上限（与后端 CustomFoodService.KJ_MAX 一致） */
const KJ_MAX = 3800
/** 千焦→千卡换算系数（与后端 KJ_PER_KCAL 一致） */
const KJ_PER_KCAL = 4.184
/** 图片大小上限（与后端 spring.servlet.multipart max-file-size=10MB 对齐） */
const IMAGE_MAX_BYTES = 10 * 1024 * 1024

/** 编辑模式：路由带 id 参数 */
const editId = ref<number | null>(null)
const categories = ref<FoodCategoryVO[]>([])
const submitting = ref(false)
const errors = reactive<Record<string, string>>({})

/** 图片状态：image 为提交值（新传 objectKey / 编辑态沿用原值），preview 为展示地址 */
const image = ref<string | null>(null)
const imagePreview = ref<string | null>(null)
const uploading = ref(false)

/** 能量录入单位：KCAL（默认）/ KJ（后端以 kcal 为入库基准，前端按 4.184 联动换算） */
const energyUnit = ref<'KCAL' | 'KJ'>('KCAL')
const ENERGY_UNIT_OPTIONS = ['kcal（千卡）', 'kJ（千焦）']

const form = reactive({
  name: '',
  alias: '',
  categoryCode: '18',
  carb: '',
  protein: '',
  fat: '',
  energy: '',
  unit: '',
  serving: ''
})

const title = computed(() => (editId.value ? '编辑自定义食物' : '新增自定义食物'))

/** 当前分类在列表中的下标（picker 选中态） */
const categoryIndex = computed(() =>
  Math.max(0, categories.value.findIndex((c) => c.code === form.categoryCode))
)

const energyUnitIndex = computed(() => (energyUnit.value === 'KJ' ? 1 : 0))

/** 联动换算：按录入单位显示另一侧换算值（≈ 120 kcal / ≈ 502 kJ） */
const counterpart = computed(() => {
  const v = num(form.energy)
  if (v === null || v < 0) return ''
  return energyUnit.value === 'KJ'
    ? `≈ ${Math.round(v / KJ_PER_KCAL)} kcal/100g`
    : `≈ ${Math.round(v * KJ_PER_KCAL)} kJ/100g`
})

/** 换算单位：已录入数值随单位切换（kcal→kJ 乘 4.184，反向除），空值仅切换 */
function handleEnergyUnitChange(e: any) {
  const next: 'KCAL' | 'KJ' = Number(e.detail.value) === 1 ? 'KJ' : 'KCAL'
  if (next === energyUnit.value) return
  const v = num(form.energy)
  if (v !== null && v >= 0 && Number.isInteger(v)) {
    form.energy = String(next === 'KJ' ? Math.round(v * KJ_PER_KCAL) : Math.round(v / KJ_PER_KCAL))
  }
  energyUnit.value = next
}

/** 最终入库 kcal：KJ 录入按 ÷4.184 四舍五入（与后端 resolveEnergy 同口径） */
function resolvedKcal(): number | null {
  const v = num(form.energy)
  if (v === null) return null
  return energyUnit.value === 'KJ' ? Math.round(v / KJ_PER_KCAL) : v
}

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
    form.categoryCode = food.categoryCode || '18'
    form.carb = String(food.carb)
    form.protein = String(food.protein)
    form.fat = String(food.fat)
    // 有千焦记录默认按 kJ 回显，避免编辑其他字段时千焦值丢失
    if (food.kj != null) {
      energyUnit.value = 'KJ'
      form.energy = String(food.kj)
    } else {
      form.energy = String(food.kcal)
    }
    form.unit = food.unit || ''
    form.serving = String(Math.round(food.serving))
    // 编辑未换图时沿用原值提交（后端对绝对 URL 原样透传，不重复解析）
    image.value = food.image || null
    imagePreview.value = food.image || null
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

/** 选图并上传：chooseImage（压缩）→ 统一文件上传 → 存相对 objectKey */
function chooseImage() {
  if (uploading.value) return
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const path = res.tempFilePaths?.[0]
      if (!path) return
      // uni 类型联合含 H5 File，count:1 实际返回单元素数组，这里按数组口径取 size
      const files = res.tempFiles as Array<{ size?: number }> | undefined
      const size = files?.[0]?.size ?? 0
      if (size > IMAGE_MAX_BYTES) {
        uni.showToast({ title: '图片不能超过 10MB', icon: 'none' })
        return
      }
      uploading.value = true
      try {
        const uploaded = await uploadFoodImage(path)
        image.value = uploaded.objectKey
        imagePreview.value = resolveImage(uploaded.url) || path
      } catch {
        // uploadFoodImage 已统一 toast
      } finally {
        uploading.value = false
      }
    }
  })
}

/** 移除图片：提交时不传 image（后端置空） */
function removeImage() {
  image.value = null
  imagePreview.value = null
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
  const energy = num(form.energy)
  if (energy === null || energy < 0 || !Number.isInteger(energy)) {
    errors.energy = '请输入整数能量值'
  } else if (energyUnit.value === 'KJ') {
    if (energy > KJ_MAX) {
      errors.energy = `能量须在 0-${KJ_MAX} kJ 之间`
    } else if (resolvedKcal()! > KCAL_MAX) {
      errors.energy = `千焦过大：换算约 ${resolvedKcal()} kcal，超过 ${KCAL_MAX} kcal 上限`
    }
  } else if (energy > KCAL_MAX) {
    errors.energy = `能量须在 0-${KCAL_MAX} kcal 之间`
  }
  const serving = form.serving.trim() ? num(form.serving) : 100
  if (serving === null || serving < 5 || serving > 1000) {
    errors.serving = '单份克数须在 5-1000g 之间'
  }
  // 守恒校验：|碳水×4 + 蛋白×4 + 脂肪×9 − 能量| ÷ max(能量,1) ≤ 10%（统一走 utils/validate）
  if (!errors.carb && !errors.protein && !errors.fat && !errors.energy) {
    const carb = num(form.carb)!
    const protein = num(form.protein)!
    const fat = num(form.fat)!
    const kcal = resolvedKcal()!
    if (!checkKcalConsistency(carb, protein, fat, kcal).ok) {
      errors.energy = `能量与宏量不匹配：${carb}g碳水+${protein}g蛋白+${fat}g脂肪约产 ${Math.round(kcalFromMacros(carb, protein, fat))} kcal`
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
      kcal: resolvedKcal()!,
      kj: energyUnit.value === 'KJ' ? num(form.energy)! : undefined,
      energyUnit: energyUnit.value,
      unit: form.unit.trim() || undefined,
      image: image.value || undefined,
      serving: form.serving.trim() ? num(form.serving)! : undefined
    })
    if (!editId.value) track(TRACK_EVENT.FOOD_CUSTOM_ADD)
    uni.showToast({
      title: editId.value ? '已保存并重新提交' : '创建成功，审核通过后他人可见',
      icon: 'none'
    })
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
        <text class="label">食物图片（选填）</text>
        <view v-if="imagePreview" class="img-box">
          <image class="img-preview" :src="imagePreview" mode="aspectFill" @click="chooseImage" />
          <view class="img-ops">
            <text class="img-op" hover-class="img-op-hover" @click="chooseImage">更换</text>
            <text class="img-op danger" hover-class="img-op-danger-hover" @click="removeImage">删除</text>
          </view>
        </view>
        <view v-else class="img-picker" hover-class="img-picker-hover" @click="chooseImage">
          <image class="img-picker-icon" :src="cameraIcon" />
          <text class="img-picker-text">{{ uploading ? '上传中…' : '拍一张或从相册选择' }}</text>
        </view>
      </view>

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
        <text class="label">能量（每 100g）</text>
        <view class="energy-row">
          <input
            v-model="form.energy"
            class="input energy-input"
            type="number"
            :placeholder="energyUnit === 'KJ' ? `0-${KJ_MAX}` : `0-${KCAL_MAX}`"
          />
          <picker
            class="energy-picker"
            :value="energyUnitIndex"
            :range="ENERGY_UNIT_OPTIONS"
            @change="handleEnergyUnitChange"
          >
            <view class="picker energy-unit">
              {{ ENERGY_UNIT_OPTIONS[energyUnitIndex] }}
            </view>
          </picker>
        </view>
        <text v-if="counterpart" class="energy-hint">{{ counterpart }}</text>
        <text v-if="errors.energy" class="err">{{ errors.energy }}</text>
      </view>

      <view class="field">
        <text class="label">常用单位（选填）</text>
        <input v-model="form.unit" class="input" placeholder="如：份、个、杯、包" maxlength="16" />
      </view>

      <view class="field">
        <text class="label">常用单份克数（选填，5-1000，默认 100）</text>
        <input v-model="form.serving" class="input" type="digit" placeholder="默认 100" />
        <text v-if="errors.serving" class="err">{{ errors.serving }}</text>
      </view>
    </view>

    <view class="tip">
      <text>能量须与宏量守恒：碳水×4 + 蛋白×4 + 脂肪×9 ≈ 能量（偏差 ≤10%）</text>
      <text>提交后本人立即可用，审核通过后其他用户可见</text>
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

.img-box {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12rpx;
}

.img-preview {
  width: 240rpx;
  height: 240rpx;
  border-radius: $zhenxinjian-radius-md;
  background: $zhenxinjian-bg;
}

.img-ops {
  display: flex;
  gap: 24rpx;
}

.img-op {
  font-size: 24rpx;
  color: $zhenxinjian-primary;
  padding: 4rpx 12rpx;
}

.img-op-hover {
  opacity: 0.7;
}

.img-op.danger {
  color: $zhenxinjian-danger;
}

.img-op-danger-hover {
  opacity: 0.7;
}

.img-picker {
  width: 240rpx;
  height: 240rpx;
  border: 1rpx dashed $zhenxinjian-border;
  border-radius: $zhenxinjian-radius-md;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  background: $zhenxinjian-bg;
}

.img-picker-hover {
  border-color: $zhenxinjian-primary;
}

.img-picker-icon {
  width: 48rpx;
  height: 48rpx;
}

.img-picker-text {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
}

.energy-row {
  display: flex;
  gap: 16rpx;
  align-items: center;
}

.energy-input {
  flex: 1;
}

.energy-picker {
  flex-shrink: 0;
}

.energy-unit {
  font-size: 24rpx;
  white-space: nowrap;
}

.energy-hint {
  font-size: 22rpx;
  color: $zhenxinjian-primary;
  margin-top: 8rpx;
  display: block;
}

.tip {
  padding: 0 8rpx 24rpx;
}

.tip text {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  display: block;
  margin-bottom: 4rpx;
}
</style>
