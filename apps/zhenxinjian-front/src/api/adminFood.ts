/**
 * 管理端食物库维护 API（基础食物可增删改停用；用户共建走审核/维护入口）
 * 作者: wanglx
 */
import request from './request'
import type { PageResult } from './types'

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
  /** 能量千焦 kJ/100g（可空） */
  kj: number | null
  serving: number | null
  /** 填报单位（可空） */
  unit: string | null
  source: number
  status: number
  /** 审核状态：0待审核 1已通过 2已驳回 3无需审核 */
  auditStatus: number
  /** 最近一次驳回原因（已驳回时展示） */
  auditRemark: string | null
  /** AI 校验结论：pass/suspect/reject/none（可空） */
  aiVerdict: string | null
  /** AI 校验建议（可空） */
  aiSuggestion: string | null
  /** 最近一次提交/重提时间（可空） */
  submitTime: string | null
  /** 提交人昵称（仅共建食物有值） */
  submitterName: string | null
  /** 图片 URL（foods.image 优先；内置食物为预热图；无图前端占位） */
  image: string | null
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
  /** 审核状态：0待审核 1已通过 2已驳回 3无需审核 */
  auditStatus?: number
  /** AI 结论：pass/suspect/reject/none */
  aiVerdict?: string
}

/** 审核修正值（仅 ADMIN_FIX；空字段沿用原值，落库前后端整体校验） */
export interface FoodAuditFixPayload {
  name?: string
  alias?: string
  categoryCode?: string
  carb?: number
  protein?: number
  fat?: number
  kcal?: number
  kj?: number
  unit?: string
  serving?: number
}

/** 审核入参（APPROVE 通过 / REJECT 驳回必填原因 / ADMIN_FIX 修正后通过） */
export interface FoodAuditPayload {
  action: 'APPROVE' | 'REJECT' | 'ADMIN_FIX'
  /** 驳回原因（REJECT 必填）；APPROVE/ADMIN_FIX 可作为备注 */
  remark?: string
  /** 修正值（仅 ADMIN_FIX） */
  fix?: FoodAuditFixPayload
}

/** 分页查询食物 */
export function getFoodPage(params: AdminFoodQuery) {
  return request.get<PageResult<AdminFood>>('/admin/foods', { params })
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

/** 审核共建食物（仅 source=2 且状态 0/2 可审） */
export function auditFood(id: number, data: FoodAuditPayload) {
  return request.post<void>(`/admin/foods/${id}/audit`, data)
}

/** 停用/启用已通过的共建食物（维护入口） */
export function changeCustomFoodStatus(id: number, status: number) {
  return request.put<void>(`/admin/foods/${id}/custom-status`, null, { params: { status } })
}

/** 软删已通过的共建食物（维护入口；逻辑删除不断链） */
export function deleteCustomFood(id: number) {
  return request.delete<void>(`/admin/foods/${id}/custom`)
}
