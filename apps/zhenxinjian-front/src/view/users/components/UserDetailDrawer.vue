<script setup lang="ts">
/**
 * 用户聚合详情抽屉（B-T31 自 users/index.vue 拆出，含档案/模式/周期描述组）
 * 作者: wanglx
 */
import { ref } from 'vue'
import { getUserProfile } from '@/api/adminUser'
import type { UserProfile } from '@/api/adminUser'
import {
  ACTIVITY_LEVEL_OPTIONS,
  DIET_MODE_OPTIONS,
  PLAN_STATUS_OPTIONS,
  USER_ROLE_OPTIONS,
  USER_STATUS_MAP,
  dictLabel
} from '@/constants/dicts'

const drawerVisible = ref(false)
const profileLoading = ref(false)
const profile = ref<UserProfile | null>(null)

/** 打开用户聚合详情抽屉 */
async function open(userId: number) {
  drawerVisible.value = true
  profileLoading.value = true
  profile.value = null
  try {
    profile.value = await getUserProfile(userId)
  } finally {
    profileLoading.value = false
  }
}

defineExpose({ open })
</script>

<template>
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
</template>

<style scoped>
.mt {
  margin-top: 16px;
}
</style>
