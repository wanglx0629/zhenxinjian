/**
 * 饮食记录相关 API（P11 添加饮食 / P12 当日记录 + 累计进度）
 * 作者: wanglx
 */
import http from './request'

/** 饮食记录项（与后端 DietRecordVO 对齐） */
export interface DietRecordVO {
  id: number
  recordDate: string
  /** 餐别：1早餐 2午餐 3晚餐 4加餐 */
  mealType: number
  mealName: string
  /** 来源：1内置食物 2自定义食物 3手动输入 */
  source: number
  foodId: number | null
  foodName: string
  /** 份量克数（手动输入为占位 1） */
  amountG: number
  /** 实际摄入碳水 g */
  carbG: number
  /** 实际摄入蛋白 g */
  proteinG: number
  /** 实际摄入脂肪 g */
  fatG: number
  /** 实际摄入能量 kcal */
  kcal: number
  remark: string | null
  createTime: string
}

/** 餐别分组（含小计） */
export interface MealGroup {
  mealType: number
  mealName: string
  records: DietRecordVO[]
  carb: number
  protein: number
  fat: number
  kcal: number
}

/** 按日饮食记录（与后端 DietDayVO 对齐） */
export interface DietDayVO {
  date: string
  meals: MealGroup[]
  totalCarb: number
  totalProtein: number
  totalFat: number
  totalKcal: number
}

/** 当日累计与目标进度（与后端 DietSummaryVO 对齐） */
export interface DietSummaryVO {
  date: string
  /** 是否有已录入的身体档案（空态标记） */
  recorded: boolean
  /** 模式（当前恒 532） */
  mode: string
  carbActual: number
  proteinActual: number
  fatActual: number
  kcalActual: number
  carbTarget: number | null
  proteinTarget: number | null
  fatTarget: number | null
  kcalTarget: number | null
  carbRate: number | null
  proteinRate: number | null
  fatRate: number | null
  kcalRate: number | null
}

/** 新增饮食记录入参（与后端 DietRecordCreateDTO 对齐） */
export interface DietRecordCreateRequest {
  /** 餐别：1早餐 2午餐 3晚餐 4加餐 */
  mealType: number
  /** 来源：1内置食物 2自定义食物 3手动输入 */
  source: number
  /** 食物ID（source=1/2 必填） */
  foodId?: number
  /** 份量克数（source=1/2 必填，>0 且 ≤5000） */
  amountG?: number
  /** 食物名称（source=3 必填，≤50字） */
  name?: string
  /** 碳水 g（source=3 必填，实际吃下总量） */
  carb?: number
  /** 蛋白 g（source=3 必填） */
  protein?: number
  /** 脂肪 g（source=3 必填） */
  fat?: number
  /** 能量 kcal（source=3 必填） */
  kcal?: number
  /** 记录日期（可空默认当日；不允许未来日期） */
  recordDate?: string
  /** 备注（可空，≤100字） */
  remark?: string
}

/** 编辑饮食记录入参（与后端 DietRecordUpdateDTO 对齐） */
export interface DietRecordUpdateRequest {
  /** 餐别 */
  mealType: number
  /** 份量克数（食物来源必填） */
  amountG?: number
  /** 食物名称（手动输入必填） */
  name?: string
  carb?: number
  protein?: number
  fat?: number
  kcal?: number
  remark?: string
}

/** 新增饮食记录 */
export function createDietRecord(data: DietRecordCreateRequest) {
  return http.post<DietRecordVO>('/diet/records', data)
}

/** 编辑饮食记录 */
export function updateDietRecord(id: number, data: DietRecordUpdateRequest) {
  return http.put<DietRecordVO>(`/diet/records/${id}`, data)
}

/** 删除饮食记录（软删） */
export function deleteDietRecord(id: number) {
  return http.delete<void>(`/diet/records/${id}`)
}

/** 按日查询饮食记录（空日期默认当日） */
export function listDietRecords(date?: string) {
  return http.get<DietDayVO>('/diet/records', date ? { date } : {})
}

/** 当日累计与目标进度 */
export function getDietSummary(date?: string) {
  return http.get<DietSummaryVO>('/diet/summary', date ? { date } : {})
}
