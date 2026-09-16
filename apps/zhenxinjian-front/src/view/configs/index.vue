<script setup lang="ts">
/**
 * 系统配置页（project_config：组件凭据/运营开关；SECRET 脱敏 ******）
 * 作者: wanglx
 */
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import { addConfig, getConfigPage, updateConfig, valueTypeLabel } from '@/api/adminConfig'
import type { ProjectConfig } from '@/api/adminConfig'
import PagePager from '@/component/PagePager.vue'
import { usePageQuery } from '@/composables/usePageQuery'

const query = reactive({
  page: 1,
  size: 20,
  keyword: ''
})

const { loading, tableData, total, load, handleSearch, handlePageChange } = usePageQuery(
  query,
  q => getConfigPage({ page: q.page, size: q.size, keyword: q.keyword || undefined })
)

const dialogVisible = ref(false)
const dialogTitle = ref('新增配置')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: undefined as number | undefined,
  configKey: '',
  valueType: 1,
  configValue: '',
  remark: ''
})

const rules: FormRules = {
  configKey: [
    { required: true, message: '请输入配置键', trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9-]*(\.[a-z][a-z0-9-]*)+$/,
      message: '小写点分格式，如 a.b.c',
      trigger: 'blur'
    }
  ],
  valueType: [{ required: true, message: '请选择值类型', trigger: 'change' }]
}

function resetForm() {
  form.id = undefined
  form.configKey = ''
  form.valueType = 1
  form.configValue = ''
  form.remark = ''
}

function openCreate() {
  resetForm()
  dialogTitle.value = '新增配置'
  dialogVisible.value = true
}

/** 打开编辑：SECRET 下发即为 ******，原样保留=不改值 */
function openEdit(row: ProjectConfig) {
  resetForm()
  dialogTitle.value = '编辑配置'
  form.id = row.id
  form.configKey = row.configKey
  form.valueType = row.valueType
  form.configValue = row.configValue || ''
  form.remark = row.remark || ''
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
    if (form.id) {
      await updateConfig(form.id, {
        configValue: form.configValue,
        remark: form.remark
      })
      ElMessage.success('修改成功')
    } else {
      await addConfig({
        configKey: form.configKey,
        valueType: form.valueType,
        configValue: form.configValue || undefined,
        remark: form.remark || undefined
      })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function handleToggleStatus(row: ProjectConfig) {
  const next = row.status === 1 ? 0 : 1
  await updateConfig(row.id, { status: next })
  ElMessage.success(next === 1 ? '已启用' : '已停用，读取方按缺省值兜底')
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
          placeholder="配置键 / 备注"
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
      <el-button type="primary" @click="openCreate">新增配置</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" stripe border>
      <template #empty>
        <el-empty description="暂无配置数据，点击右上角「新增配置」创建" :image-size="120" />
      </template>
      <el-table-column prop="configKey" label="配置键" min-width="200" show-overflow-tooltip />
      <el-table-column label="配置值" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <span :class="{ 'secret-value': row.valueType === 5 }">{{ row.configValue || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="值类型" width="90">
        <template #default="{ row }">{{ valueTypeLabel(row.valueType) }}</template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="handleToggleStatus(row)" />
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="更新时间" width="170" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <PagePager :total="total" :page="query.page" :size="query.size" @change="handlePageChange" />

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="form.configKey" :disabled="!!form.id" maxlength="100" placeholder="如 sensitive.filter.enabled" />
        </el-form-item>
        <el-form-item label="值类型" prop="valueType">
          <el-select v-model="form.valueType" :disabled="!!form.id" style="width: 100%">
            <el-option label="字符串" :value="1" />
            <el-option label="数字" :value="2" />
            <el-option label="布尔" :value="3" />
            <el-option label="JSON" :value="4" />
            <el-option label="密文" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input
            v-model="form.configValue"
            type="textarea"
            :rows="3"
            maxlength="4000"
            :placeholder="form.valueType === 5 ? '密文类型：留空或保持 ****** 表示不修改，输入新值则重新加密' : '请输入配置值'"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" maxlength="255" placeholder="配置用途说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.secret-value {
  color: var(--zhenxinjian-text-sub, #8a8f99);
  letter-spacing: 2px;
}
</style>
