<script setup lang="ts">
/**
 * 用户管理页（管理员）
 * 作者: wanglx
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { addUser, deleteUser, getUserPage, updateUser } from '@/api/user'
import { getUserProfile } from '@/api/adminUser'
import type { UserProfile } from '@/api/adminUser'
import type { UserInfo } from '@/api/types'
import PagePager from '@/component/PagePager.vue'
import { usePageQuery } from '@/composables/usePageQuery'
import {
  ACTIVITY_LEVEL_OPTIONS,
  DIET_MODE_OPTIONS,
  PLAN_STATUS_OPTIONS,
  USER_ROLE_MAP,
  USER_ROLE_OPTIONS,
  USER_STATUS_MAP,
  USER_STATUS_OPTIONS,
  dictLabel
} from '@/constants/dicts'

const drawerVisible = ref(false)
const profileLoading = ref(false)
const profile = ref<UserProfile | null>(null)

/** 打开用户聚合详情抽屉 */
async function openDetail(row: UserInfo) {
  drawerVisible.value = true
  profileLoading.value = true
  profile.value = null
  try {
    profile.value = await getUserProfile(row.id)
  } finally {
    profileLoading.value = false
  }
}

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: undefined as number | undefined,
  role: ''
})

/** 列表查询骨架（B-T23：loading/数据/分页回调/删后回退收敛 composable） */
const { loading, tableData, total, load, handleSearch, handlePageChange, reloadAfterDelete } =
  usePageQuery(query, q =>
    getUserPage({
      page: q.page,
      size: q.size,
      keyword: q.keyword || undefined,
      status: q.status,
      role: q.role || undefined
    })
  )

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
    await load()
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
          placeholder="用户名 / 昵称 / 邮箱"
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 120px">
          <el-option v-for="o in USER_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.role" clearable placeholder="角色" style="width: 120px">
          <el-option v-for="o in USER_ROLE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <el-tag :type="USER_ROLE_MAP[row.role]?.type || 'info'" size="small">
            {{ USER_ROLE_MAP[row.role]?.label ?? '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="USER_STATUS_MAP[row.status]?.type || 'info'" size="small">
            {{ USER_STATUS_MAP[row.status]?.label ?? '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <PagePager :total="total" :page="query.page" :size="query.size" @change="handlePageChange" />

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
            <el-option v-for="o in USER_ROLE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio v-for="o in USER_STATUS_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawerVisible" title="用户详情" size="420px" destroy-on-close>
      <div v-loading="profileLoading">
        <template v-if="profile">
          <el-descriptions title="基础信息" :column="1" border>
            <el-descriptions-item label="ID">{{ profile.user.id }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ profile.user.username }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ profile.user.nickname || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              {{ USER_STATUS_MAP[profile.user.status ?? -1]?.label ?? '未知' }}
            </el-descriptions-item>
            <el-descriptions-item label="角色">
              {{ dictLabel(USER_ROLE_OPTIONS, profile.user.role) }}
            </el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ profile.user.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="最近登录">{{ profile.user.lastLoginTime || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-descriptions title="身体档案" :column="1" border class="mt">
            <template v-if="profile.bodyProfile && profile.bodyProfile.recorded !== false">
              <el-descriptions-item label="性别">
                {{ profile.bodyProfile.gender === 1 ? '男' : profile.bodyProfile.gender === 2 ? '女' : '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="年龄">{{ profile.bodyProfile.age ?? '-' }}</el-descriptions-item>
              <el-descriptions-item label="身高">{{ profile.bodyProfile.height ?? '-' }} cm</el-descriptions-item>
              <el-descriptions-item label="当前体重">{{ profile.bodyProfile.weight ?? '-' }} kg</el-descriptions-item>
              <el-descriptions-item label="目标体重">{{ profile.bodyProfile.targetWeight ?? '-' }} kg</el-descriptions-item>
              <el-descriptions-item label="活动系数">
                {{ dictLabel(ACTIVITY_LEVEL_OPTIONS, profile.bodyProfile.activityLevel) }}
              </el-descriptions-item>
              <el-descriptions-item label="减脂缺口">{{ profile.bodyProfile.deficit ?? '-' }} kcal</el-descriptions-item>
              <el-descriptions-item label="BMR / TDEE">
                {{ profile.bodyProfile.bmr ?? '-' }} / {{ profile.bodyProfile.tdee ?? '-' }} kcal
              </el-descriptions-item>
              <el-descriptions-item label="基准热量">{{ profile.bodyProfile.targetKcal ?? '-' }} kcal</el-descriptions-item>
              <el-descriptions-item label="目标宏量（碳/蛋/脂）">
                {{ profile.bodyProfile.targetCarb ?? '-' }} / {{ profile.bodyProfile.targetProtein ?? '-' }} /
                {{ profile.bodyProfile.targetFat ?? '-' }} g
              </el-descriptions-item>
            </template>
            <el-descriptions-item v-else label="档案">未录入</el-descriptions-item>
          </el-descriptions>

          <el-descriptions title="当前模式与周期" :column="1" border class="mt">
            <el-descriptions-item label="当前模式">
              {{ dictLabel(DIET_MODE_OPTIONS, profile.currentMode) }}
            </el-descriptions-item>
            <template v-if="profile.currentPlan">
              <el-descriptions-item label="周期天数">{{ profile.currentPlan.cycleDays ?? '-' }} 天</el-descriptions-item>
              <el-descriptions-item label="起止日期">
                {{ profile.currentPlan.startDate || '-' }} ~ {{ profile.currentPlan.endDate || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="周期状态">
                {{ dictLabel(PLAN_STATUS_OPTIONS, profile.currentPlan.status) }}
              </el-descriptions-item>
            </template>
            <el-descriptions-item v-else label="进行中周期">无</el-descriptions-item>
          </el-descriptions>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.mt {
  margin-top: 16px;
}
</style>
