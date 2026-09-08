<script setup lang="ts">
/**
 * 用户管理页（管理员）
 * 作者: wanglx
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { addUser, deleteUser, getUserPage, updateUser } from '@/api/user'
import type { UserInfo } from '@/api/types'

const loading = ref(false)
const tableData = ref<UserInfo[]>([])
const total = ref(0)

/** 用户状态字典（后端 UserStatusEnum：0冻结 1正常 2注销） */
const STATUS_MAP: Record<number, { label: string; type: 'success' | 'danger' | 'info' }> = {
  0: { label: '冻结', type: 'danger' },
  1: { label: '正常', type: 'success' },
  2: { label: '注销', type: 'info' }
}

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: undefined as number | undefined,
  role: ''
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: undefined as number | undefined,
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  role: 'USER',
  status: 1
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度 3-32', trigger: 'blur' }
  ],
  password: [
    {
      validator: (_rule, value, callback) => {
        if (!form.id && !value) {
          callback(new Error('请输入密码'))
          return
        }
        if (value && (value.length < 8 || value.length > 32)) {
          callback(new Error('密码长度 8-32'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

async function loadUsers() {
  loading.value = true
  try {
    const page = await getUserPage({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status,
      role: query.role || undefined
    })
    tableData.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = undefined
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.email = ''
  form.phone = ''
  form.role = 'USER'
  form.status = 1
}

function openCreate() {
  resetForm()
  dialogTitle.value = '新增用户'
  dialogVisible.value = true
}

function openEdit(row: UserInfo) {
  resetForm()
  dialogTitle.value = '编辑用户'
  form.id = row.id
  form.username = row.username
  form.nickname = row.nickname || ''
  form.email = row.email || ''
  form.phone = row.phone || ''
  form.role = row.role || 'USER'
  form.status = row.status ?? 1
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
      await updateUser({
        id: form.id,
        username: form.username,
        password: form.password || undefined,
        nickname: form.nickname || undefined,
        email: form.email || undefined,
        phone: form.phone || undefined,
        role: form.role,
        status: form.status
      })
      ElMessage.success('修改成功')
    } else {
      await addUser({
        username: form.username,
        password: form.password,
        nickname: form.nickname || undefined,
        email: form.email || undefined,
        phone: form.phone || undefined,
        role: form.role,
        status: form.status
      })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await loadUsers()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: UserInfo) {
  try {
    await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '提示', {
      type: 'warning'
    })
  } catch {
    return
  }
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  if (tableData.value.length === 1 && query.page > 1) {
    query.page -= 1
  }
  await loadUsers()
}

function handleSearch() {
  query.page = 1
  loadUsers()
}

function handlePageChange(page: number) {
  query.page = page
  loadUsers()
}

onMounted(loadUsers)
</script>

<template>
  <div class="users-page">
    <div class="toolbar">
      <div class="filters">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="用户名 / 昵称 / 邮箱"
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 120px">
          <el-option label="冻结" :value="0" />
          <el-option label="正常" :value="1" />
          <el-option label="注销" :value="2" />
        </el-select>
        <el-select v-model="query.role" clearable placeholder="角色" style="width: 120px">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="普通用户" value="USER" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
      <el-button type="primary" @click="openCreate">新增用户</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" stripe border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column prop="email" label="邮箱" min-width="160" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">
            {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="STATUS_MAP[row.status]?.type || 'info'" size="small">
            {{ STATUS_MAP[row.status]?.label ?? '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" maxlength="32" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="form.id ? '不修改请留空' : '至少 8 位'"
            maxlength="32"
          />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" maxlength="64" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" maxlength="128" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">冻结</el-radio>
            <el-radio :value="2">注销</el-radio>
          </el-radio-group>
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
.users-page {
  background: var(--zhenxinjian-white);
  border: 1px solid var(--zhenxinjian-border);
  border-radius: 8px;
  padding: 20px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
