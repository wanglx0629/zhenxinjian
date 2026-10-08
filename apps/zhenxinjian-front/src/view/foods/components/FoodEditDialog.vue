<script setup lang="ts">
/**
 * 内置食物新增/编辑弹窗（B-T31 自 foods/index.vue 拆出）
 * 作者: wanglx
 */
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { addFood, updateFood } from '@/api/adminFood'
import type { AdminFood } from '@/api/adminFood'
import { FOOD_CATEGORY_OPTIONS, dictLabel } from '@/constants/dicts'
import { createFoodForm, foodFormRules } from '@/view/foods/foodForm'

const emit = defineEmits<{ saved: [] }>()

const dialogVisible = ref(false)
const dialogTitle = ref('新增内置食物')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: undefined as number | undefined,
  ...createFoodForm()
})

const rules = foodFormRules(64)

function resetForm() {
  form.id = undefined
  Object.assign(form, createFoodForm())
}

/** 打开弹窗：不传 row 为新增，传 row 为编辑 */
function open(row?: AdminFood) {
  resetForm()
  if (row) {
    dialogTitle.value = '编辑内置食物'
    form.id = row.id
    form.name = row.name
    form.categoryCode = row.categoryCode
    form.alias = row.alias || ''
    form.carb = row.carb
    form.protein = row.protein
    form.fat = row.fat
    form.kcal = row.kcal
    form.serving = row.serving || 100
  } else {
    dialogTitle.value = '新增内置食物'
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    const payload = {
      name: form.name,
      categoryCode: form.categoryCode,
      categoryName: dictLabel(FOOD_CATEGORY_OPTIONS, form.categoryCode),
      alias: form.alias || undefined,
      carb: form.carb,
      protein: form.protein,
      fat: form.fat,
      kcal: form.kcal,
      serving: form.serving
    }
    if (form.id) {
      await updateFood(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await addFood(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    emit('saved')
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" maxlength="64" />
      </el-form-item>
      <el-form-item label="分类" prop="categoryCode">
        <el-select v-model="form.categoryCode" style="width: 100%">
          <el-option v-for="o in FOOD_CATEGORY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="别名">
        <el-input v-model="form.alias" maxlength="128" placeholder="可选，搜索辅助词" />
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
        <el-input-number v-model="form.kcal" :precision="0" :min="0" :max="5000" style="width: 100%" />
      </el-form-item>
      <el-form-item label="参考份量 g">
        <el-input-number v-model="form.serving" :min="1" :max="10000" style="width: 100%" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>
