<script setup lang="ts">
/**
 * 用户新增/编辑弹窗（B-T31 自 users/index.vue 拆出）
 * 作者: wanglx
 */
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { addUser, updateUser } from '@/api/user'
import type { UserInfo } from '@/api/types'
import { USER_ROLE_OPTIONS, USER_STATUS_OPTIONS } from '@/constants/dicts'

const emit = defineEmits<{ saved: [] }>()

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

/** 打开弹窗：不传 row 为新增，传 row 为编辑 */
function open(row?: UserInfo) {
  resetForm()
  if (row) {
    dialogTitle.value = '编辑用户'
    form.id = row.id
    form.username = row.username
    form.nickname = row.nickname || ''
    form.email = row.email || ''
    form.phone = row.phone || ''
    form.role = row.role || 'USER'
    form.status = row.status ?? 1
  } else {
    dialogTitle.value = '新增用户'
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
    emit('saved')
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
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
</template>
