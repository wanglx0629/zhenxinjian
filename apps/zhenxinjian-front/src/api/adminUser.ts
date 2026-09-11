/**
 * 管理端用户聚合视图 API
 * 作者: wanglx
 */
import request from './request'
import type { UserInfo } from './types'

/** 身体档案（字段与后端 BodyProfileVO 对齐，管理端只读展示） */
export interface AdminBodyProfile {
  recorded?: boolean
  gender?: number
  age?: number
  height?: number
  weight?: number
  targetWeight?: number
  activityLevel?: number
  activityFactor?: number
  deficit?: number
  cfc?: number
  mode?: number
  bmr?: number
  tdee?: number
  targetKcal?: number
  targetCarb?: number
  targetProtein?: number
  targetFat?: number
  lowKcalRisk?: boolean
  updateTime?: string
}

/** 当前周期计划（字段与后端 CyclePlanVO 对齐，管理端只读展示） */
export interface AdminCurrentPlan {
  id?: number
  cycleDays?: number
  cfc?: number
  startDate?: string
  endDate?: string
  carbPool?: number
  fatPool?: number
  dailyProtein?: number
  status?: number
}

/** 用户聚合视图（基础信息 + 身体档案 + 当前模式/周期） */
export interface UserProfile {
  user: UserInfo
  bodyProfile: AdminBodyProfile | null
  currentMode: number | null
  currentPlan: AdminCurrentPlan | null
}

/** 查询用户聚合视图 */
export function getUserProfile(id: number) {
  return request.get<UserProfile>(`/admin/users/${id}/profile`)
}
