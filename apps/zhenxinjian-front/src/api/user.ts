/**
 * 用户管理 API
 * 作者: wanglx
 */
import request from './request'
import type { PageResult, UserInfo, UserQuery } from './types'

/** 分页查询用户 */
export function getUserPage(query: UserQuery = {}) {
  return request.get<PageResult<UserInfo>>('/users', {
    params: {
      page: query.page ?? 1,
      size: query.size ?? 10,
      keyword: query.keyword,
      status: query.status,
      role: query.role
    }
  })
}

/** 新增用户 */
export function addUser(data: Partial<UserInfo> & { password?: string }) {
  return request.post('/users', data)
}

/** 修改用户 */
export function updateUser(data: Partial<UserInfo> & { id: number; password?: string }) {
  return request.put('/users', data)
}

/** 删除用户 */
export function deleteUser(id: number) {
  return request.delete(`/users/${id}`)
}
