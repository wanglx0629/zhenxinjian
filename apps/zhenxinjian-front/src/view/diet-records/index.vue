<script setup lang="ts">
/**
 * 饮食记录查看页（只读：管理端不开放改/删用户业务数据）
 * 作者: wanglx
 */
import { reactive, ref } from 'vue'
import { getDietRecordPage } from '@/api/adminDiet'
import PagePager from '@/component/PagePager.vue'
import { usePageQuery } from '@/composables/usePageQuery'
import {
  MEAL_TYPE_MAP,
  MEAL_TYPE_OPTIONS,
  RECORD_SOURCE_OPTIONS,
  dictLabel
} from '@/constants/dicts'

const dateRange = ref<[string, string] | null>(null)
const query = reactive({
  page: 1,
  size: 20,
  userKeyword: '',
  mealType: undefined as number | undefined
})

/** 列表查询骨架（B-T23：loading/数据/分页回调收敛 composable；只读页无删后回退） */
const { loading, tableData, total, handleSearch, handlePageChange } = usePageQuery(query, q =>
  getDietRecordPage({
    page: q.page,
    size: q.size,
    userKeyword: q.userKeyword || undefined,
    startDate: dateRange.value?.[0] || undefined,
    endDate: dateRange.value?.[1] || undefined,
    mealType: q.mealType
  })
)

function displayUser(row: { userId: number; userNickname: string | null }) {
  return row.userNickname || `用户#${row.userId}`
}
</script>

<template>
  <div class="list-page">
    <div class="list-toolbar">
      <div class="list-filters">
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
          <el-option v-for="o in MEAL_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tableData" stripe border>
      <template #empty>
        <el-empty description="暂无饮食记录，调整筛选条件后重试" :image-size="120" />
      </template>
      <el-table-column label="用户" min-width="120">
        <template #default="{ row }">{{ displayUser(row) }}</template>
      </el-table-column>
      <el-table-column prop="recordDate" label="日期" width="110" />
      <el-table-column label="餐别" width="90">
        <template #default="{ row }">
          <el-tag :type="MEAL_TYPE_MAP[row.mealType]?.type || 'info'" size="small">
            {{ MEAL_TYPE_MAP[row.mealType]?.label ?? '未知' }}
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
        <template #default="{ row }">{{ dictLabel(RECORD_SOURCE_OPTIONS, row.source) }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="记录时间" width="170" />
    </el-table>

    <PagePager :total="total" :page="query.page" :size="query.size" @change="handlePageChange" />
  </div>
</template>

<style scoped>
</style>
