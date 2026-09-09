/**
 * 身体数据相关 API（P03 录入 / P04 结果）
 * 作者: wanglx
 */
import http from './request'

/** 身体档案保存入参（与后端 BodyProfileSaveDTO 对齐） */
export interface BodyProfileSaveRequest {
  /** 性别:1男 2女 */
  gender: number
  /** 年龄（12-80） */
  age: number
  /** 身高cm（100-250） */
  height: number
  /** 当前体重kg（25-200） */
  weight: number
  /** 目标体重kg（≤ 当前体重 +0.1） */
  targetWeight: number
  /** 活动系数档位:1久坐 2轻度 3中度 4高度 */
  activityLevel: number
  /** 减脂缺口kcal:200/300/400/500，缺省后端默认 200 */
  deficit?: number
  /** 脂肪系数:0.8/1.0，缺省后端默认 0.8（碳循环预留） */
  cfc?: number
}

/** 身体档案 + 核心计算结果（与后端 BodyProfileVO 对齐） */
export interface BodyProfileResult {
  /** 是否已录入（空态标识） */
  recorded: boolean
  gender?: number
  age?: number
  height?: number
  weight?: number
  targetWeight?: number
  activityLevel?: number
  /** 活动系数快照（1.2/1.375/1.55/1.725） */
  activityFactor?: number
  deficit?: number
  cfc?: number
  /** BMR 快照kcal（整数） */
  bmr?: number
  /** TDEE 快照kcal（整数） */
  tdee?: number
  /** 基准热量kcal（TDEE − 缺口） */
  targetKcal?: number
  /** 目标碳水g（1位小数） */
  targetCarb?: number
  /** 目标蛋白g（1位小数） */
  targetProtein?: number
  /** 目标脂肪g（1位小数） */
  targetFat?: number
  /** 低热量风险标记（女 <1200 / 男 <1500，仅提示不阻断） */
  lowKcalRisk?: boolean
  /** 健康免责声明（页面必须展示，不可移除） */
  disclaimer?: string
  updateTime?: string
}

/** 保存身体档案（录入/修改一体，幂等覆盖；返回保存后的档案与计算快照） */
export function saveBodyProfile(data: BodyProfileSaveRequest) {
  return http.put<BodyProfileResult>('/body/profile', data)
}

/** 查询当前身体档案与计算结果；未录入返回空态（recorded=false） */
export function getBodyProfile() {
  return http.get<BodyProfileResult>('/body/profile')
}
