/**
 * 管理端食物库维护 API（仅内置食物可写；自定义食物只读）
 * 作者: wanglx
 */
import request from './request'

/** 食物列表项（管理端） */
export interface AdminFood {
  id: number
  code: string | null
  categoryCode: string
  categoryName: string
  name: string
  alias: string | null
  carb: number
  protein: number
  fat: number
  kcal: number
  serving: number
  source: number
  status: number
}

/** 新增/编辑入参（宏量每 100g；热量与 4/4/9 换算偏差超 ±10% 后端拒收） */
export interface AdminFoodSavePayload {
  name: string
  categoryCode: string
  categoryName: string
  alias?: string
  carb: number
  protein: number
  fat: number
  kcal: number
  serving?: number
}

/** 分页查询参数 */
export interface AdminFoodQuery {
  page: number
  size: number
  keyword?: string
  categoryCode?: string
  source?: number
  status?: number
}

/** 分页查询食物 */
export function getFoodPage(params: AdminFoodQuery) {
  return request.get<{ records: AdminFood[]; total: number }>('/admin/foods', { params })
}

/** 新增内置食物（code 自动顺延） */
export function addFood(data: AdminFoodSavePayload) {
  return request.post<void>('/admin/foods', data)
}

/** 编辑内置食物 */
export function updateFood(id: number, data: AdminFoodSavePayload) {
  return request.put<void>(`/admin/foods/${id}`, data)
}

/** 软删内置食物 */
export function deleteFood(id: number) {
  return request.delete<void>(`/admin/foods/${id}`)
}

/** 停用/启用内置食物（停用后 C 端搜索不可见） */
export function changeFoodStatus(id: number, status: number) {
  return request.put<void>(`/admin/foods/${id}/status`, null, { params: { status } })
}
