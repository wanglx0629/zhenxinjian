/**
 * 食物库相关 API（P10 食物库：搜索/分类/热门/详情/试算 + 自定义食物）
 * 作者: wanglx
 */
import http, { BASE_URL } from './request'
import { getToken } from '@/utils/storage'
import type { Result } from './types'

/** 食物条目（与后端 FoodVO 对齐，营养值为每 100g 口径） */
export interface FoodVO {
  id: number
  /** 食物编号（内置 F001–F200；自定义为空） */
  code?: string
  categoryCode: string
  categoryName: string
  name: string
  alias?: string
  /** 碳水 g/100g */
  carb: number
  /** 蛋白 g/100g */
  protein: number
  /** 脂肪 g/100g */
  fat: number
  /** 能量 kcal/100g */
  kcal: number
  /** 能量千焦 kJ/100g（可空） */
  kj?: number
  /** 常用单份克数 */
  serving: number
  /** 填报单位（份/个/杯/包…，可空） */
  unit?: string
  /** 食物图片 URL（foods.image 优先；内置食物为 food_images 预热图；无图为空走分类占位） */
  image?: string
  /** 来源：1内置 2自定义 */
  source: number
  /** 审核状态：0待审核 1已通过 2已驳回 3无需审核 */
  auditStatus?: number
  /** 最近一次驳回原因（已驳回时展示） */
  auditRemark?: string
  /** AI校验结论：pass/suspect/reject/none */
  aiVerdict?: string
  /** AI校验建议 */
  aiSuggestion?: string
  /** 最近一次提交/重提时间 */
  submitTime?: string
}

/** 食物分类项 */
export interface FoodCategoryVO {
  code: string
  name: string
}

/** 份量试算结果 */
export interface FoodCalcVO {
  foodId: number
  grams: number
  carb: number
  protein: number
  fat: number
  kcal: number
}

/** MyBatis-Plus 分页结构 */
export interface PageVO<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 食物搜索条件 */
export interface FoodSearchParams {
  keyword: string
  categoryCode?: string
  page: number
  size: number
}

/** 自定义食物新增/编辑入参（与后端 CustomFoodSaveDTO 对齐） */
export interface CustomFoodSaveRequest {
  /** 食物ID（编辑必填，新增为空） */
  id?: number
  name: string
  alias?: string
  categoryCode?: string
  carb: number
  protein: number
  fat: number
  /** 能量 kcal/100g（0-900；KJ 录入时前端按 4.184 联动换算后提交，后端以 kcal 为入库基准） */
  kcal: number
  /** 能量千焦 kJ/100g（0-3800；用户以 KJ 录入时随 kcal 一并提交） */
  kj?: number
  /** 能量录入单位：KJ 或 KCAL（缺省 KCAL） */
  energyUnit?: 'KJ' | 'KCAL'
  /** 填报单位（份/个/杯/包…，可空） */
  unit?: string
  /** 食物图片相对路径（统一文件上传返回的 objectKey，可空） */
  image?: string
  serving?: number
}

/** 食物搜索（关键字匹配名称或别名，可叠加分类，空关键字返回空集） */
export function searchFoods(params: FoodSearchParams) {
  return http.get<PageVO<FoodVO>>('/food/search', params)
}

/** 分类列表（18 大分类，顺序固定） */
export function getFoodCategories() {
  return http.get<FoodCategoryVO[]>('/food/categories')
}

/** 热门食物（静态清单 ≤12 条） */
export function getHotFoods() {
  return http.get<FoodVO[]>('/food/hot')
}

/** 食物详情（内置全员可见，自定义仅本人） */
export function getFoodDetail(id: number) {
  return http.get<FoodVO>(`/food/${id}`)
}

/** 份量试算（每 100g 值 × 克数 ÷ 100，克数 1–10000） */
export function calcFood(id: number, grams: number) {
  return http.get<FoodCalcVO>(`/food/${id}/calc`, { grams })
}

/** 新增/编辑自定义食物（id 为空新增） */
export function saveCustomFood(data: CustomFoodSaveRequest) {
  return http.post<FoodVO>('/food/custom', data)
}

/** 我的自定义食物列表 */
export function listMyCustomFoods() {
  return http.get<FoodVO[]>('/food/custom/mine')
}

/** 删除自定义食物（软删） */
export function deleteCustomFood(id: number) {
  return http.delete<void>(`/food/custom/${id}`)
}

/** 统一文件上传响应（与后端 FileUploadVO 对齐） */
export interface FileUploadVO {
  /** 访问URL（绝对地址） */
  url: string
  /** 对象Key（相对路径，入库基准） */
  objectKey: string
  /** 存储提供方: minio/oss */
  provider: string
  /** 原始文件名 */
  originalFilename: string
}

/**
 * 上传食物图片（POST /files/upload，multipart 字段名 file）
 * <p>选择 uni.uploadFile 而非 uni.request：multipart 文件上传在小程序端仅 uploadFile 支持；
 * 响应解包逻辑与 request.ts 保持一致（Result.code === 200 取 data，否则 toast 并 reject）</p>
 *
 * @param filePath 本地临时文件路径（uni.chooseImage 返回的 tempFilePaths）
 */
export function uploadFoodImage(filePath: string) {
  return new Promise<FileUploadVO>((resolve, reject) => {
    const token = getToken()
    uni.uploadFile({
      url: `${BASE_URL}/files/upload`,
      filePath,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      timeout: 15000,
      success: (res) => {
        if (res.statusCode !== 200) {
          uni.showToast({ title: '图片上传失败，请重试', icon: 'none' })
          reject(new Error(`http ${res.statusCode}`))
          return
        }
        let body: Result<FileUploadVO> | null = null
        try {
          body = JSON.parse(res.data) as Result<FileUploadVO>
        } catch {
          body = null
        }
        if (!body || typeof body !== 'object') {
          uni.showToast({ title: '响应异常', icon: 'none' })
          reject(new Error('invalid response'))
          return
        }
        if (body.code !== 200) {
          uni.showToast({ title: body.message || '图片上传失败', icon: 'none' })
          reject(new Error(body.message || 'upload failed'))
          return
        }
        resolve(body.data)
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}
