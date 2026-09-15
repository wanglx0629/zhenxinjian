/**
 * 管理端系统配置 API（project_config；SECRET 值后端统一脱敏为 ****** 下发）
 * 作者: wanglx
 */
import request from './request'
import type { PageResult } from './types'

/** 配置项 */
export interface ProjectConfig {
  id: number
  configKey: string
  /** SECRET 类型恒为 ******（明文不下发） */
  configValue: string | null
  /** 值类型：1字符串 2数字 3布尔 4JSON 5密文 */
  valueType: number
  remark: string | null
  /** 状态：0停用 1有效 */
  status: number
  updateTime: string
}

/** 新增入参（key 与 valueType 创建后不可改） */
export interface ProjectConfigCreatePayload {
  configKey: string
  valueType: number
  configValue?: string
  remark?: string
}

/** 修改入参（value/备注/状态；SECRET 传 ****** 或空 = 保留原值） */
export interface ProjectConfigUpdatePayload {
  configValue?: string
  remark?: string
  status?: number
}

/** 分页查询配置（关键词匹配 key/备注） */
export function getConfigPage(params: { page: number; size: number; keyword?: string }) {
  return request.get<PageResult<ProjectConfig>>('/admin/configs', { params })
}

/** 新增配置（key 活跃唯一；SECRET 值加密落库） */
export function addConfig(data: ProjectConfigCreatePayload) {
  return request.post<ProjectConfig>('/admin/configs', data)
}

/** 修改配置 */
export function updateConfig(id: number, data: ProjectConfigUpdatePayload) {
  return request.put<void>(`/admin/configs/${id}`, data)
}

/** 值类型选项（对应 ConfigValueTypeEnum） */
export const CONFIG_VALUE_TYPE_OPTIONS = [
  { value: 1, label: '字符串' },
  { value: 2, label: '数字' },
  { value: 3, label: '布尔' },
  { value: 4, label: 'JSON' },
  { value: 5, label: '密文' }
]

/** 值类型 label */
export function valueTypeLabel(valueType: number) {
  return CONFIG_VALUE_TYPE_OPTIONS.find(o => o.value === valueType)?.label ?? '未知'
}
