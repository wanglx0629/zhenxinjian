<script setup lang="ts">
/**
 * 食物库维护页（内置食物可增删改停用；自定义食物只读置灰）
 * 作者: wanglx
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Delete, Switch } from '@element-plus/icons-vue'
import { changeFoodStatus, deleteFood, getFoodPage } from '@/api/adminFood'
import type { AdminFood } from '@/api/adminFood'
import PagePager from '@/component/PagePager.vue'
import { usePageQuery } from '@/composables/usePageQuery'
import FoodEditDialog from './components/FoodEditDialog.vue'
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

/** 初始筛选快照（重置用） */
const INITIAL_FILTERS = { keyword: '', categoryCode: '', source: undefined, status: undefined }

const {
  loading,
  tableData,
  total,
  load,
  handleSearch,
  handlePageChange,
  handleSizeChange,
  handleReset,
  reloadAfterDelete
} = usePageQuery(query, q =>
    getFoodPage({
      page: q.page,
      size: q.size,
      keyword: q.keyword || undefined,
      categoryCode: q.categoryCode || undefined,
      source: q.source,
      status: q.status
    })
  )

/** B-T31：新增/编辑弹窗拆为独立组件，父页仅编排 */
const editDialogRef = ref<InstanceType<typeof FoodEditDialog>>()

function openCreate() {
  editDialogRef.value?.open()
}

function openEdit(row: AdminFood) {
  editDialogRef.value?.open(row)
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
          class="filter-w-md"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.categoryCode" clearable placeholder="分类" class="filter-w-md">
          <el-option v-for="o in FOOD_CATEGORY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.source" clearable placeholder="来源" class="filter-w-sm">
          <el-option v-for="o in FOOD_SOURCE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.status" clearable placeholder="状态" class="filter-w-sm">
          <el-option v-for="o in FOOD_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset({ ...INITIAL_FILTERS })">重置</el-button>
      </div>
      <el-button type="primary" @click="openCreate">新增内置食物</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" stripe border>
      <template #empty>
        <el-empty description="暂无食物数据，调整筛选条件或点击右上角「新增内置食物」" :image-size="120" />
      </template>
      <el-table-column prop="code" label="编号" width="90">
        <template #default="{ row }">{{ row.code || '-' }}</template>
      </el-table-column>
      <el-table-column label="图片" width="72" align="center">
        <template #default="{ row }">
          <el-image
            v-if="row.image"
            :src="row.image"
            :preview-src-list="[row.image]"
            preview-teleported
            fit="cover"
            class="food-image"
          />
          <span v-else>-</span>
        </template>
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
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" :icon="Switch" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
          <span v-else class="readonly-tip">自定义食物只读</span>
        </template>
      </el-table-column>
    </el-table>

    <PagePager
      :total="total"
      :page="query.page"
      :size="query.size"
      @change="handlePageChange"
      @size-change="handleSizeChange"
    />

    <FoodEditDialog ref="editDialogRef" @saved="load" />
  </div>
</template>

<style scoped>
.readonly-tip {
  color: var(--zhenxinjian-text-placeholder);
  font-size: 12px;
}

.food-image {
  width: 40px;
  height: 40px;
  border-radius: var(--zhenxinjian-radius-sm);
  display: block;
}
</style>
