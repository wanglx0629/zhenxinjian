<script setup lang="ts">
/**
 * 共建食物「修正后通过」弹窗（ADMIN_FIX；预填当前值，空字段沿用原值，后端整体校验）
 * 作者: wanglx
 */
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { auditFood } from '@/api/adminFood'
import type { AdminFood } from '@/api/adminFood'
import { FOOD_CATEGORY_OPTIONS } from '@/constants/dicts'
import { createFoodForm, foodFormRules } from '@/view/foods/foodForm'

const emit = defineEmits<{ audited: [] }>()

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
/** 当前审核目标（提交时取 id） */
const target = ref<AdminFood | null>(null)
const aiSuggestion = ref('')

const form = reactive({
  ...createFoodForm(),
  kj: null as number | null,
  unit: ''
})

const rules = foodFormRules(100)

/** 打开弹窗：预填当前值（未改动字段提交后等同沿用原值） */
function open(row: AdminFood) {
  target.value = row
  aiSuggestion.value = row.aiSuggestion || ''
  form.name = row.name
  form.alias = row.alias || ''
  form.categoryCode = row.categoryCode
  form.carb = row.carb
  form.protein = row.protein
  form.fat = row.fat
  form.kcal = row.kcal
  form.kj = row.kj
  form.unit = row.unit || ''
  form.serving = row.serving || 100
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value || !target.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    await auditFood(target.value.id, {
      action: 'ADMIN_FIX',
      fix: {
        name: form.name,
        alias: form.alias || undefined,
        categoryCode: form.categoryCode,
        carb: form.carb,
        protein: form.protein,
        fat: form.fat,
        kcal: form.kcal,
        kj: form.kj ?? undefined,
        unit: form.unit || undefined,
        serving: form.serving
      }
    })
    ElMessage.success('已修正并通过，食物转为公共可见')
    dialogVisible.value = false
    emit('audited')
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="dialogVisible" title="修正后通过" width="520px" destroy-on-close>
    <el-alert
      v-if="aiSuggestion"
      :title="`AI 建议：${aiSuggestion}`"
      type="warning"
      :closable="false"
      show-icon
      class="fix-ai-tip"
    />
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" maxlength="100" />
      </el-form-item>
      <el-form-item label="分类" prop="categoryCode">
        <el-select v-model="form.categoryCode" style="width: 100%">
          <el-option v-for="o in FOOD_CATEGORY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="别名">
        <el-input v-model="form.alias" maxlength="100" placeholder="可选，搜索辅助词" />
      </el-form-item>
      <el-form-item label="碳水（每100g）" prop="carb">
        <el-input-number v-model="form.carb" :precision="1" :min="0" :max="100" style="width: 100%" />
      </el-form-item>
      <el-form-item label="蛋白（每100g）" prop="protein">
        <el-input-number v-model="form.protein" :precision="1" :min="0" :max="100" style="width: 100%" />
      </el-form-item>
      <el-form-item label="脂肪（每100g）" prop="fat">
        <el-input-number v-model="form.fat" :precision="1" :min="0" :max="100" style="width: 100%" />
      </el-form-item>
      <el-form-item label="热量（每100g）" prop="kcal">
        <el-input-number v-model="form.kcal" :precision="0" :min="0" :max="900" style="width: 100%" />
      </el-form-item>
      <el-form-item label="千焦（每100g）">
        <el-input-number v-model="form.kj" :precision="0" :min="0" :max="3800" style="width: 100%" />
      </el-form-item>
      <el-form-item label="单位">
        <el-input v-model="form.unit" maxlength="16" placeholder="可选，如：个 / 杯 / 包" />
      </el-form-item>
      <el-form-item label="参考份量 g">
        <el-input-number v-model="form.serving" :min="5" :max="1000" style="width: 100%" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">修正并通过</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.fix-ai-tip {
  margin-bottom: 16px;
}
</style>
