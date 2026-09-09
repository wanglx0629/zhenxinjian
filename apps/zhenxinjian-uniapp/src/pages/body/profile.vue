<script setup lang="ts">
/**
 * P03 身体数据录入页：性别/年龄/身高/体重/目标体重/活动系数/热量缺口
 * 提交前复用 RANGES/ACTIVITY/DEFICIT_OPTIONS 校验；录入页实时预览仅作参考，以服务端结果为准
 * 作者: wanglx
 */
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useBodyStore } from '@/store/body'
import { ACTIVITY, DEFICIT_OPTIONS, RANGES } from '@/config/constants'
import { bmr, tdee, macro532Base, type CalcProfile } from '@/utils/calculator'
import { checkAll, checkTargetWeight } from '@/utils/validate'
import { canSubmit } from '@/utils/throttle'

const bodyStore = useBodyStore()

/** 性别选项（值与后端 GenderEnum 对齐：1男 2女） */
const GENDERS = [
  { label: '男', value: 1 },
  { label: '女', value: 2 }
]

const form = reactive({
  gender: 2,
  age: '',
  height: '',
  weight: '',
  targetWeight: '',
  activityLevel: 2,
  deficit: 200
})

const errors = reactive<Record<string, string>>({})

/** 录入页实时预览（calculator.ts 本地口径；★ 仅展示，正式结果以后端返回为准） */
const preview = computed(() => {
  const age = Number(form.age)
  const height = Number(form.height)
  const weight = Number(form.weight)
  if (!age || !height || !weight) return null
  if (age < RANGES.age.min || age > RANGES.age.max) return null
  if (height < RANGES.height.min || height > RANGES.height.max) return null
  if (weight < RANGES.weight.min || weight > RANGES.weight.max) return null
  const p: CalcProfile = {
    gender: form.gender === 1 ? 'male' : 'female',
    age,
    height,
    weight,
    act: ACTIVITY[form.activityLevel - 1].value,
    deficit: form.deficit
  }
  const m = macro532Base(p)
  return {
    bmr: Math.round(bmr(p)),
    tdee: Math.round(tdee(p)),
    kcal: m.kcal,
    carb: m.carb,
    protein: m.protein,
    fat: m.fat
  }
})

/** 已录入用户进入时回显当前档案 */
onLoad(async () => {
  try {
    await bodyStore.fetchProfile()
  } catch {
    return
  }
  const p = bodyStore.profile
  if (p?.recorded) {
    form.gender = p.gender || 2
    form.age = String(p.age ?? '')
    form.height = String(p.height ?? '')
    form.weight = String(p.weight ?? '')
    form.targetWeight = String(p.targetWeight ?? '')
    form.activityLevel = p.activityLevel || 2
    form.deficit = p.deficit || 200
  }
})

/** 提交：前端区间/交叉校验 → 后端保存 → 跳结果页 */
async function handleSubmit() {
  Object.keys(errors).forEach((k) => delete errors[k])
  const { ok, errors: errs } = checkAll({
    age: form.age,
    height: form.height,
    weight: form.weight,
    targetWeight: form.targetWeight
  })
  Object.assign(errors, errs)
  if (ok) {
    const cross = checkTargetWeight(form.weight, form.targetWeight)
    if (!cross.ok) {
      errors.targetWeight = cross.msg
    }
  }
  if (Object.keys(errors).length > 0) {
    uni.showToast({ title: '请检查填写内容', icon: 'none' })
    return
  }
  if (bodyStore.submitting || !canSubmit()) return
  try {
    await bodyStore.saveProfile({
      gender: form.gender,
      age: Number(form.age),
      height: Number(form.height),
      weight: Number(form.weight),
      targetWeight: Number(form.targetWeight),
      activityLevel: form.activityLevel,
      deficit: form.deficit
    })
    uni.redirectTo({ url: '/pages/body/result' })
  } catch {
    // request.ts 已统一 toast；此处仅保持表单可重试
  }
}
</script>

<template>
  <view class="page">
    <view class="panel">
      <text class="panel-title">身体数据</text>

      <view class="field">
        <text class="label">性别</text>
        <view class="seg">
          <view
            v-for="g in GENDERS"
            :key="g.value"
            class="seg-item"
            :class="{ active: form.gender === g.value }"
            @click="form.gender = g.value"
          >{{ g.label }}</view>
        </view>
      </view>

      <view class="field">
        <text class="label">年龄（{{ RANGES.age.min }}-{{ RANGES.age.max }}{{ RANGES.age.unit }}）</text>
        <input v-model="form.age" class="input" type="number" placeholder="请输入年龄" />
        <text v-if="errors.age" class="err">{{ errors.age }}</text>
      </view>

      <view class="field">
        <text class="label">身高（{{ RANGES.height.min }}-{{ RANGES.height.max }}{{ RANGES.height.unit }}）</text>
        <input v-model="form.height" class="input" type="digit" placeholder="请输入身高" />
        <text v-if="errors.height" class="err">{{ errors.height }}</text>
      </view>

      <view class="field">
        <text class="label">当前体重（{{ RANGES.weight.min }}-{{ RANGES.weight.max }}{{ RANGES.weight.unit }}）</text>
        <input v-model="form.weight" class="input" type="digit" placeholder="请输入当前体重" />
        <text v-if="errors.weight" class="err">{{ errors.weight }}</text>
      </view>

      <view class="field">
        <text class="label">目标体重（{{ RANGES.targetWeight.unit }}）</text>
        <input v-model="form.targetWeight" class="input" type="digit" placeholder="目标体重需不高于当前体重" />
        <text v-if="errors.targetWeight" class="err">{{ errors.targetWeight }}</text>
      </view>

      <view class="field">
        <text class="label">活动量</text>
        <view class="opts">
          <view
            v-for="(a, i) in ACTIVITY"
            :key="a.key"
            class="opt-item"
            :class="{ active: form.activityLevel === i + 1 }"
            @click="form.activityLevel = i + 1"
          >{{ a.label }}</view>
        </view>
      </view>

      <view class="field">
        <text class="label">每日热量缺口（默认 200 kcal 温和档）</text>
        <view class="seg">
          <view
            v-for="d in DEFICIT_OPTIONS"
            :key="d"
            class="seg-item"
            :class="{ active: form.deficit === d }"
            @click="form.deficit = d"
          >{{ d }}</view>
        </view>
      </view>
    </view>

    <view v-if="preview" class="panel preview">
      <text class="panel-title">实时预览</text>
      <view class="preview-grid">
        <view class="pv"><text class="pv-num">{{ preview.bmr }}</text><text class="pv-label">BMR kcal</text></view>
        <view class="pv"><text class="pv-num">{{ preview.tdee }}</text><text class="pv-label">TDEE kcal</text></view>
        <view class="pv"><text class="pv-num">{{ preview.kcal }}</text><text class="pv-label">基准热量 kcal</text></view>
        <view class="pv"><text class="pv-num">{{ preview.carb }}</text><text class="pv-label">碳水 g</text></view>
        <view class="pv"><text class="pv-num">{{ preview.protein }}</text><text class="pv-label">蛋白 g</text></view>
        <view class="pv"><text class="pv-num">{{ preview.fat }}</text><text class="pv-label">脂肪 g</text></view>
      </view>
      <text class="preview-tip">预览为本地估算，正式结果以服务端计算为准</text>
    </view>

    <button class="btn-primary" :loading="bodyStore.submitting" @click="handleSubmit">
      保存并查看结果
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
  margin-bottom: 24rpx;
  display: block;
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
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 30rpx;
  color: $zhenxinjian-text;
  box-sizing: border-box;
}

.err {
  font-size: 22rpx;
  color: #f56c6c;
  margin-top: 8rpx;
  display: block;
}

.seg {
  display: flex;
  gap: 16rpx;
}

.seg-item {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: $zhenxinjian-text;
}

.seg-item.active {
  background: $zhenxinjian-primary-light;
  border-color: $zhenxinjian-primary;
  color: $zhenxinjian-primary;
}

.opts {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.opt-item {
  padding: 20rpx 24rpx;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: $zhenxinjian-text;
}

.opt-item.active {
  background: $zhenxinjian-primary-light;
  border-color: $zhenxinjian-primary;
  color: $zhenxinjian-primary;
}

.preview-grid {
  display: flex;
  flex-wrap: wrap;
}

.pv {
  width: 33.33%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24rpx;
}

.pv-num {
  font-size: 36rpx;
  font-weight: 600;
  color: $zhenxinjian-primary;
}

.pv-label {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  margin-top: 4rpx;
}

.preview-tip {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
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
