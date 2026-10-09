<script setup lang="ts">
/**
 * 食物库维护页（基础食物可增删改停用；用户共建走审核/维护入口）
 * 作者: wanglx
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Switch } from '@element-plus/icons-vue'
import {
  auditFood,
  changeCustomFoodStatus,
  changeFoodStatus,
  deleteCustomFood,
  deleteFood,
  getFoodPage
} from '@/api/adminFood'
import type { AdminFood } from '@/api/adminFood'
import PagePager from '@/component/PagePager.vue'
import { usePageQuery, type PageQueryBase } from '@/composables/usePageQuery'
import FoodEditDialog from './components/FoodEditDialog.vue'
import FoodAuditFixDialog from './components/FoodAuditFixDialog.vue'
import {
  FOOD_AI_VERDICT_MAP,
  FOOD_AI_VERDICT_OPTIONS,
  FOOD_AUDIT_STATUS_MAP,
  FOOD_AUDIT_STATUS_OPTIONS,
  FOOD_CATEGORY_OPTIONS,
  FOOD_SOURCE_MAP,
  FOOD_SOURCE_OPTIONS,
  FOOD_STATUS_MAP,
  FOOD_STATUS_OPTIONS,
  dictLabel
} from '@/constants/dicts'

interface FoodListQuery extends PageQueryBase {
  keyword: string
  categoryCode: string
  source: number | undefined
  status: number | undefined
  auditStatus: number | undefined
  aiVerdict: string | undefined
}

const query = reactive<FoodListQuery>({
  page: 1,
  size: 20,
  keyword: '',
  categoryCode: '',
  source: undefined,
  status: undefined,
  auditStatus: undefined,
  aiVerdict: undefined
})

/** 初始筛选快照（重置用） */
const INITIAL_FILTERS: Omit<FoodListQuery, 'page' | 'size'> = {
  keyword: '',
  categoryCode: '',
  source: undefined,
  status: undefined,
  auditStatus: undefined,
  aiVerdict: undefined
}

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
    status: q.status,
    auditStatus: q.auditStatus,
    aiVerdict: q.aiVerdict
  })
)

/** B-T31：新增/编辑弹窗拆为独立组件，父页仅编排 */
const editDialogRef = ref<InstanceType<typeof FoodEditDialog>>()

/** 共建「修正后通过」弹窗 */
const fixDialogRef = ref<InstanceType<typeof FoodAuditFixDialog>>()

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

// ==================== 共建审核（通过/驳回/修正后通过） ====================

/** 通过：转公共可见 */
async function handleApprove(row: AdminFood) {
  try {
    await ElMessageBox.confirm(
      `确认通过「${row.name}」？通过后所有用户可在食物库搜索到该食物`,
      '审核通过',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await auditFood(row.id, { action: 'APPROVE' })
  ElMessage.success('已通过，食物转为公共可见')
  await load()
}

/** 驳回：必填原因，退回用户私有可改 */
async function handleReject(row: AdminFood) {
  let reason: string
  try {
    const { value } = await ElMessageBox.prompt('驳回后退回提交人私有可编辑，请填写驳回原因', '驳回共建食物', {
      type: 'warning',
      inputPlaceholder: '必填，将展示给提交人',
      inputValidator: v => {
        if (!v || !v.trim()) {
          return '驳回原因必填'
        }
        if (v.trim().length > 255) {
          return '驳回原因最长 255 字'
        }
        return true
      }
    })
    reason = value.trim()
  } catch {
    return
  }
  await auditFood(row.id, { action: 'REJECT', remark: reason })
  ElMessage.success('已驳回，等待提交人修改后重提')
  await load()
}

/** 修正后通过：弹窗编辑字段 */
function openFix(row: AdminFood) {
  fixDialogRef.value?.open(row)
}

// ==================== 已通过共建维护（停用/软删，走明确维护入口） ====================

async function handleToggleCustomStatus(row: AdminFood) {
  await changeCustomFoodStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已停用，C 端搜索不再可见' : '已启用')
  await load()
}

async function handleDeleteCustom(row: AdminFood) {
  try {
    await ElMessageBox.confirm(
      `确认删除共建食物「${row.name}」？历史饮食记录引用不受影响`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await deleteCustomFood(row.id)
  ElMessage.success('删除成功')
  await reloadAfterDelete()
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
        <el-select v-model="query.source" clearable placeholder="数据来源" class="filter-w-sm">
          <el-option v-for="o in FOOD_SOURCE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.status" clearable placeholder="状态" class="filter-w-sm">
          <el-option v-for="o in FOOD_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.auditStatus" clearable placeholder="审核状态" class="filter-w-sm">
          <el-option v-for="o in FOOD_AUDIT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.aiVerdict" clearable placeholder="AI 结论" class="filter-w-sm">
          <el-option v-for="o in FOOD_AI_VERDICT_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset({ ...INITIAL_FILTERS })">重置</el-button>
      </div>
      <el-button type="primary" @click="openCreate">新增基础食物</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" stripe border>
      <template #empty>
        <el-empty description="暂无食物数据，调整筛选条件或点击右上角「新增基础食物」" :image-size="120" />
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
      <el-table-column label="来源" width="100">
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
      <el-table-column label="审核状态" width="130">
        <template #default="{ row }">
          <el-tag :type="FOOD_AUDIT_STATUS_MAP[row.auditStatus]?.type || 'info'" size="small">
            {{ FOOD_AUDIT_STATUS_MAP[row.auditStatus]?.label ?? '未知' }}
          </el-tag>
          <el-tooltip
            v-if="row.auditStatus === 2 && row.auditRemark"
            :content="`驳回原因：${row.auditRemark}`"
            placement="top"
          >
            <span class="reject-reason">原因</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="AI 结论" width="110">
        <template #default="{ row }">
          <el-tooltip
            v-if="row.aiVerdict && row.aiVerdict !== 'none' && row.aiSuggestion"
            :content="row.aiSuggestion"
            placement="top"
          >
            <el-tag :type="FOOD_AI_VERDICT_MAP[row.aiVerdict]?.type || 'info'" size="small">
              {{ FOOD_AI_VERDICT_MAP[row.aiVerdict]?.label ?? row.aiVerdict }}
            </el-tag>
          </el-tooltip>
          <el-tag
            v-else-if="row.aiVerdict"
            :type="FOOD_AI_VERDICT_MAP[row.aiVerdict]?.type || 'info'"
            size="small"
          >
            {{ FOOD_AI_VERDICT_MAP[row.aiVerdict]?.label ?? row.aiVerdict }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="submitterName" label="提交人" width="110">
        <template #default="{ row }">{{ row.submitterName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="submitTime" label="提交时间" width="170">
        <template #default="{ row }">{{ row.submitTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <template v-if="row.source === 1">
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" :icon="Switch" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
          <template v-else-if="row.auditStatus === 0 || row.auditStatus === 2">
            <el-button link type="success" @click="handleApprove(row)">通过</el-button>
            <el-button link type="danger" @click="handleReject(row)">驳回</el-button>
            <el-button link type="primary" @click="openFix(row)">修正后通过</el-button>
          </template>
          <template v-else-if="row.auditStatus === 1">
            <el-button link type="warning" :icon="Switch" @click="handleToggleCustomStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDeleteCustom(row)">删除</el-button>
          </template>
          <span v-else class="readonly-tip">-</span>
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

    <p class="data-source">
      数据来源：基础食物含台湾卫生福利部食品药物管理署（TFDA）开放资料，依政府资料开放授权条款（OGL）使用
    </p>

    <FoodEditDialog ref="editDialogRef" @saved="load" />
    <FoodAuditFixDialog ref="fixDialogRef" @audited="load" />
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

.reject-reason {
  margin-left: 4px;
  color: var(--el-color-danger);
  font-size: 12px;
  cursor: help;
  border-bottom: 1px dashed var(--el-color-danger);
}

.data-source {
  margin-top: 12px;
  color: var(--zhenxinjian-text-placeholder);
  font-size: 12px;
  text-align: center;
}
</style>
