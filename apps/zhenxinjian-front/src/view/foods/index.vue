<script setup lang="ts">
/**
 * 食物库维护页（内置食物可增删改停用；自定义食物只读置灰）
 * 作者: wanglx
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { addFood, changeFoodStatus, deleteFood, getFoodPage, updateFood } from '@/api/adminFood'
import type { AdminFood } from '@/api/adminFood'
import PagePager from '@/component/PagePager.vue'
import { usePageQuery } from '@/composables/usePageQuery'
import {
  FOOD_CATEGORY_OPTIONS,
  FOOD_SOURCE_MAP,
  FOOD_SOURCE_OPTIONS,
  FOOD_STATUS_MAP,
  FOOD_STATUS_OPTIONS,
  dictLabel
} from '@/constants/dicts'

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
  categoryCode: '',
  source: undefined as number | undefined,
  status: undefined as number | undefined
})

/** 列表查询骨架（B-T23：loading/数据/分页回调/删后回退收敛 composable） */
const { loading, tableData, total, load, handleSearch, handlePageChange, reloadAfterDelete } =
  usePageQuery(query, q =>
    getFoodPage({
      page: q.page,
      size: q.size,
      keyword: q.keyword || undefined,
      categoryCode: q.categoryCode || undefined,
      source: q.source,
      status: q.status
    })
  )

const dialogVisible = ref(false)
const dialogTitle = ref('新增内置食物')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: undefined as number | undefined,
  name: '',
  categoryCode: '',
  alias: '',
  carb: 0,
  protein: 0,
  fat: 0,
  kcal: 0,
  serving: 100
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入食物名称', trigger: 'blur' },
    { max: 64, message: '名称不超过 64 字', trigger: 'blur' }
  ],
  categoryCode: [{ required: true, message: '请选择分类', trigger: 'change' }],
  carb: [{ required: true, message: '请输入碳水', trigger: 'blur' }],
  protein: [{ required: true, message: '请输入蛋白质', trigger: 'blur' }],
  fat: [{ required: true, message: '请输入脂肪', trigger: 'blur' }],
  kcal: [{ required: true, message: '请输入热量', trigger: 'blur' }]
}

function resetForm() {
  form.id = undefined
  form.name = ''
  form.categoryCode = ''
  form.alias = ''
  form.carb = 0
  form.protein = 0
  form.fat = 0
  form.kcal = 0
  form.serving = 100
}

function openCreate() {
  resetForm()
  dialogTitle.value = '新增内置食物'
  dialogVisible.value = true
}

function openEdit(row: AdminFood) {
  resetForm()
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
    await load()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: AdminFood) {
  try {
    await ElMessageBox.confirm(`确认删除食物「${row.name}」？历史饮食记录快照不受影响`, '提示', {
      type: 'warning'
    })
  } catch {
    return
  }
  await deleteFood(row.id)
  ElMessage.success('删除成功')
  await reloadAfterDelete()
}

async function handleToggleStatus(row: AdminFood) {
  await changeFoodStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已停用，C 端搜索不再可见' : '已启用')
  await load()
}
</script>

<template>
  <div class="list-page">
    <div class="list-toolbar">
      <div class="list-filters">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="名称 / 别名"
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.categoryCode" clearable placeholder="分类" style="width: 160px">
          <el-option v-for="o in FOOD_CATEGORY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.source" clearable placeholder="来源" style="width: 120px">
          <el-option v-for="o in FOOD_SOURCE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 120px">
          <el-option v-for="o in FOOD_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
      <el-button type="primary" @click="openCreate">新增内置食物</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" stripe border>
      <el-table-column prop="code" label="编号" width="90">
        <template #default="{ row }">{{ row.code || '-' }}</template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column label="分类" width="140">
        <template #default="{ row }">{{ dictLabel(FOOD_CATEGORY_OPTIONS, row.categoryCode) }}</template>
      </el-table-column>
      <el-table-column prop="carb" label="碳水g" width="90" align="right" />
      <el-table-column prop="protein" label="蛋白g" width="90" align="right" />
      <el-table-column prop="fat" label="脂肪g" width="90" align="right" />
      <el-table-column prop="kcal" label="热量kcal" width="100" align="right" />
      <el-table-column label="来源" width="90">
        <template #default="{ row }">
          <el-tag :type="FOOD_SOURCE_MAP[row.source]?.type || 'info'" size="small">
            {{ FOOD_SOURCE_MAP[row.source]?.label ?? '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="FOOD_STATUS_MAP[row.status]?.type || 'info'" size="small">
            {{ FOOD_STATUS_MAP[row.status]?.label ?? '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <template v-if="row.source === 1">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
          <span v-else class="readonly-tip">自定义食物只读</span>
        </template>
      </el-table-column>
    </el-table>

    <PagePager :total="total" :page="query.page" :size="query.size" @change="handlePageChange" />

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
  </div>
</template>

<style scoped>
.readonly-tip {
  color: var(--zhenxinjian-text-sub, #8a8f99);
  font-size: 12px;
}
</style>
