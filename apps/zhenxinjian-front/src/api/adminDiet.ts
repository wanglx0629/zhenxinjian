/**
 * 管理端饮食记录查看 API（只读，不开放改/删用户业务数据）
 * 作者: wanglx
 */
import request from './request'

/** 饮食记录列表项（管理端只读视图，字段与后端快照列对齐） */
export interface AdminDietRecord {
  id: number
  userId: number
  userNickname: string | null
  recordDate: string
  mealType: number
  source: number
  foodName: string
  amount: number
  carb: number
  protein: number
  fat: number
  kcal: number
  createTime: string
}

/** 分页查询参数 */
export interface AdminDietQuery {
  page: number
  size: number
  userKeyword?: string
  startDate?: string
  endDate?: string
  mealType?: number
}

/** 分页查询饮食记录（只读） */
export function getDietRecordPage(params: AdminDietQuery) {
  return request.get<{ records: AdminDietRecord[]; total: number }>('/admin/diet-records', {
    params
  })
}
