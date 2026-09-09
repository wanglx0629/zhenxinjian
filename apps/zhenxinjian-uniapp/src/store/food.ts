/**
 * 食物库状态管理（P10 食物库；后端失败降级本地 foods.ts 检索，联网自动切回）
 * 作者: wanglx
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { FOODS, type FoodItem } from '@/data/foods'
import {
  calcFood, deleteCustomFood, getFoodCategories, getFoodDetail, getHotFoods,
  listMyCustomFoods, saveCustomFood, searchFoods,
  type CustomFoodSaveRequest, type FoodCalcVO, type FoodCategoryVO,
  type FoodSearchParams, type FoodVO, type PageVO
} from '@/api/food'

/** 本地合成ID：降级模式下食物用负数索引标识（-1 对应 FOODS[0]） */
function localId(index: number) {
  return -(index + 1)
}

/** 本地条目 → FoodVO（分类串「编号 名称」拆分，与后端口径一致） */
function toLocalVO(item: FoodItem, index: number): FoodVO {
  const categoryCode = item.category.substring(0, 2)
  const categoryName = item.category.substring(3).trim()
  return {
    id: localId(index),
    code: item.id,
    categoryCode,
    categoryName,
    name: item.name,
    alias: item.alias,
    carb: item.carb,
    protein: item.protein,
    fat: item.fat,
    kcal: item.kcal,
    serving: item.serving,
    source: 1
  }
}

/** 预映射本地全量食物（200 条） */
const LOCAL_VOS: FoodVO[] = FOODS.map(toLocalVO)

/** 本地分类列表（与后端枚举同序 01–10） */
const LOCAL_CATEGORIES: FoodCategoryVO[] = LOCAL_VOS.reduce<FoodCategoryVO[]>((acc, vo) => {
  if (!acc.some((c) => c.code === vo.categoryCode)) {
    acc.push({ code: vo.categoryCode, name: vo.categoryName })
  }
  return acc
}, [])

/** 本地热门清单（与后端 FoodHotConstant 同序同编号） */
const LOCAL_HOT_CODES = ['F002', 'F007', 'F016', 'F022', 'F056', 'F063', 'F046', 'F039', 'F075', 'F082', 'F092', 'F113']

export const useFoodStore = defineStore('food', () => {
  /** 降级标记：后端请求失败，当前使用本地内置数据 */
  const degraded = ref(false)
  /** 加载中标记 */
  const loading = ref(false)

  /**
   * 食物搜索（名称/别名模糊 + 分类筛选 + 分页）
   * 后端失败时降级本地检索并置降级标记；恢复联网请求成功后自动切回
   */
  async function search(params: FoodSearchParams): Promise<PageVO<FoodVO>> {
    loading.value = true
    try {
      const page = await searchFoods(params)
      degraded.value = false
      return page
    } catch {
      degraded.value = true
      return localSearch(params)
    } finally {
      loading.value = false
    }
  }

  /** 分类列表（后端失败降级本地） */
  async function fetchCategories(): Promise<FoodCategoryVO[]> {
    try {
      const list = await getFoodCategories()
      degraded.value = false
      return list
    } catch {
      degraded.value = true
      return LOCAL_CATEGORIES
    }
  }

  /** 热门食物（后端失败降级本地清单） */
  async function fetchHot(): Promise<FoodVO[]> {
    try {
      const list = await getHotFoods()
      degraded.value = false
      return list
    } catch {
      degraded.value = true
      return LOCAL_HOT_CODES.map((code) => LOCAL_VOS.find((v) => v.code === code)).filter(Boolean) as FoodVO[]
    }
  }

  /**
   * 食物详情：在线走后端；降级模式（本地合成ID为负）查本地
   */
  async function detail(id: number): Promise<FoodVO> {
    if (id < 0) {
      return LOCAL_VOS[-id - 1]
    }
    try {
      const vo = await getFoodDetail(id)
      degraded.value = false
      return vo
    } catch (e) {
      degraded.value = true
      throw e
    }
  }

  /**
   * 份量试算：在线优先后端；降级或后端失败时本地换算（每 100g 值 × 克数 ÷ 100）
   */
  async function calc(food: FoodVO, grams: number): Promise<FoodCalcVO> {
    if (food.id > 0 && !degraded.value) {
      try {
        return await calcFood(food.id, grams)
      } catch {
        // 后端失败降级本地换算
      }
    }
    return localCalc(food, grams)
  }

  /** 本地检索（关键字匹配名称/别名，叠加分类，内存分页） */
  function localSearch(params: FoodSearchParams): PageVO<FoodVO> {
    const keyword = params.keyword.trim()
    const list = LOCAL_VOS.filter((vo) => {
      const matchCategory = !params.categoryCode || vo.categoryCode === params.categoryCode
      const matchKeyword = vo.name.includes(keyword) || (vo.alias || '').includes(keyword)
      return matchCategory && matchKeyword
    })
    const size = params.size
    const start = (params.page - 1) * size
    return {
      records: list.slice(start, start + size),
      total: list.length,
      size,
      current: params.page,
      pages: Math.ceil(list.length / size)
    }
  }

  /** 本地试算（宏量保留 2 位小数，与后端口径一致） */
  function localCalc(food: FoodVO, grams: number): FoodCalcVO {
    const ratio = grams / 100
    const two = (v: number) => Math.round(v * 100) / 100
    return {
      foodId: food.id,
      grams,
      carb: two(food.carb * ratio),
      protein: two(food.protein * ratio),
      fat: two(food.fat * ratio),
      kcal: Math.round(food.kcal * ratio)
    }
  }

  /** 新增/编辑自定义食物（仅在线，失败由 request 层提示） */
  async function saveCustom(data: CustomFoodSaveRequest) {
    return saveCustomFood(data)
  }

  /** 我的自定义列表 */
  async function fetchMyCustom() {
    return listMyCustomFoods()
  }

  /** 删除自定义食物（软删） */
  async function removeCustom(id: number) {
    return deleteCustomFood(id)
  }

  return {
    degraded,
    loading,
    search,
    fetchCategories,
    fetchHot,
    detail,
    calc,
    saveCustom,
    fetchMyCustom,
    removeCustom
  }
})
