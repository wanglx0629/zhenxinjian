<script setup lang="ts">
/**
 * 饮食记录查看页（只读：管理端不开放改/删用户业务数据）
 * 作者: wanglx
 */
import { onMounted, reactive, ref } from 'vue'
import { getDietRecordPage } from '@/api/adminDiet'
import type { AdminDietRecord } from '@/api/adminDiet'

/** 餐别字典（后端 MealTypeEnum：1早 2午 3晚 4加餐） */
const MEAL_MAP: Record<number, { label: string; type: 'success' | 'warning' | 'info' | 'primary' }> = {
  1: { label: '早餐', type: 'success' },
  2: { label: '午餐', type: 'warning' },
  3: { label: '晚餐', type: 'info' },
  4: { label: '加餐', type: 'primary' }
}

/** 来源字典（1内置食物 2自定义食物 3手动输入） */
const SOURCE_MAP: Record<number, string> = { 1: '食物库', 2: '自定义', 3: '手动输入' }

const loading = ref(false)
const tableData = ref<AdminDietRecord[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)
const query = reactive({
  page: 1,
  size: 20,
  userKeyword: '',
  mealType: undefined as number | undefined
})

async function loadRecords() {
  loading.value = true
  try {
    const page = await getDietRecordPage({
      page: query.page,
      size: query.size,
      userKeyword: query.userKeyword || undefined,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined,
      mealType: query.mealType
    })
    tableData.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function displayUser(row: AdminDietRecord) {
  return row.userNickname || `用户#${row.userId}`
}

function handleSearch() {
  query.page = 1
  loadRecords()
}

function handlePageChange(page: number) {
  query.page = page
  loadRecords()
}

onMounted(loadRecords)
</script>

<template>
  <div class="diet-page">
    <div class="toolbar">
      <div class="filters">
        <el-input
          v-model="query.userKeyword"
          clearable
          placeholder="用户 ID / 昵称"
          style="width: 180px"
          @keyup.enter="handleSearch"
        />
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
        <el-select v-model="query.mealType" clearable placeholder="餐别" style="width: 120px">
          <el-option label="早餐" :value="1" />
          <el-option label="午餐" :value="2" />
          <el-option label="晚餐" :value="3" />
          <el-option label="加餐" :value="4" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tableData" stripe border>
      <el-table-column label="用户" min-width="120">
        <template #default="{ row }">{{ displayUser(row) }}</template>
      </el-table-column>
      <el-table-column prop="recordDate" label="日期" width="110" />
      <el-table-column label="餐别" width="90">
        <template #default="{ row }">
          <el-tag :type="MEAL_MAP[row.mealType]?.type || 'info'" size="small">
            {{ MEAL_MAP[row.mealType]?.label ?? '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="foodName" label="食物" min-width="140" />
      <el-table-column prop="amount" label="份量g" width="90" align="right" />
      <el-table-column prop="carb" label="碳水g" width="90" align="right" />
      <el-table-column prop="protein" label="蛋白g" width="90" align="right" />
      <el-table-column prop="fat" label="脂肪g" width="90" align="right" />
      <el-table-column prop="kcal" label="热量kcal" width="100" align="right" />
      <el-table-column label="来源" width="100">
        <template #default="{ row }">{{ SOURCE_MAP[row.source] || '-' }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="记录时间" width="170" />
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.size"
        :current-page="query.page"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.diet-page {
  background: var(--zhenxinjian-white);
  border: 1px solid var(--zhenxinjian-border);
  border-radius: 8px;
  padding: 20px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
