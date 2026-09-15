/**
 * AI 能力相关 API（拍照识别食物）
 * 作者: wanglx
 *
 * 选 uni.uploadFile：multipart 文件上传在小程序端仅 uploadFile 支持；
 * 响应解包逻辑与 request.ts 保持一致（Result.code === 200 取 data，否则 toast 并 reject）。
 */
import { BASE_URL } from './request'
import type { Result } from './types'
import { getToken } from '@/utils/storage'

/** 食物识别候选项（与后端 FoodRecognizeVO 对齐，营养值为每 100g 口径，kcal 已按 4/4/9 重算） */
export interface FoodRecognizeVO {
  name: string
  /** 碳水 g/100g */
  carb: number
  /** 蛋白 g/100g */
  protein: number
  /** 脂肪 g/100g */
  fat: number
  /** 能量 kcal/100g */
  kcal: number
}

/**
 * 拍照识别食物（POST /food/recognize，multipart 字段名 file）
 * 登录态（含游客）可用；非食物图返回空数组（业务空态，不 reject）
 */
export function recognizeFood(filePath: string) {
  return new Promise<FoodRecognizeVO[]>((resolve, reject) => {
    const header: Record<string, string> = {}
    const token = getToken()
    if (token) {
      header.Authorization = `Bearer ${token}`
    }
    uni.uploadFile({
      url: `${BASE_URL}/food/recognize`,
      filePath,
      name: 'file',
      header,
      timeout: 30000,
      success: (res) => {
        if (res.statusCode === 401) {
          uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          reject(new Error('unauthorized'))
          return
        }
        if (res.statusCode !== 200) {
          uni.showToast({ title: '识别失败，请重试', icon: 'none' })
          reject(new Error(`http ${res.statusCode}`))
          return
        }
        let body: Result<FoodRecognizeVO[]> | null = null
        try {
          body = JSON.parse(res.data) as Result<FoodRecognizeVO[]>
        } catch {
          body = null
        }
        if (!body || typeof body !== 'object') {
          uni.showToast({ title: '响应异常', icon: 'none' })
          reject(new Error('invalid response'))
          return
        }
        if (body.code !== 200) {
          uni.showToast({ title: body.message || '识别失败', icon: 'none' })
          reject(new Error(body.message || 'recognize failed'))
          return
        }
        resolve(body.data || [])
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}
