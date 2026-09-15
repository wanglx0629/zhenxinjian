/**
 * 食物库相关 API（P10 食物库：搜索/分类/热门/详情/试算 + 自定义食物）
 * 作者: wanglx
 */
import http from './request'

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
  /** 常用单份克数 */
  serving: number
  /** 食物图片 URL（内置 200 张预热图；自定义暂无图为空） */
  image?: string
  /** 来源：1内置 2自定义 */
  source: number
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
  kcal: number
  serving?: number
}

/** 食物搜索（关键字匹配名称或别名，可叠加分类，空关键字返回空集） */
export function searchFoods(params: FoodSearchParams) {
  return http.get<PageVO<FoodVO>>('/food/search', params)
}

/** 分类列表（10 大分类，顺序固定） */
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
