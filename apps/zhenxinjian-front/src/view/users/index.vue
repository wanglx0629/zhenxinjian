<script setup lang="ts">
/**
 * 用户管理页（管理员）
 * 作者: wanglx
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteUser, getUserPage } from '@/api/user'
import type { UserInfo } from '@/api/types'
import PagePager from '@/component/PagePager.vue'
import { usePageQuery } from '@/composables/usePageQuery'
import UserDetailDrawer from './components/UserDetailDrawer.vue'
import UserEditDialog from './components/UserEditDialog.vue'
import {
  USER_ROLE_MAP,
  USER_ROLE_OPTIONS,
  USER_STATUS_MAP,
  USER_STATUS_OPTIONS
} from '@/constants/dicts'

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

/** B-T31：编辑弹窗/详情抽屉拆为独立组件，父页仅编排 */
const editDialogRef = ref<InstanceType<typeof UserEditDialog>>()
const detailDrawerRef = ref<InstanceType<typeof UserDetailDrawer>>()

function openCreate() {
  editDialogRef.value?.open()
}

function openEdit(row: UserInfo) {
  editDialogRef.value?.open(row)
}

function openDetail(row: UserInfo) {
  detailDrawerRef.value?.open(row.id)
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

    <UserEditDialog ref="editDialogRef" @saved="load" />
    <UserDetailDrawer ref="detailDrawerRef" />
  </div>
</template>
