/**
 * 管理后台字典层（与后端枚举一一对应；B-T23 自列表页内联声明收敛单一真源）
 * 作者: wanglx
 */

/** 字典项（value 枚举码 / label 显示名 / type el-tag 配色，仅 tag 场景需要） */
export interface DictItem<V extends number | string = number> {
  value: V
  label: string
  type?: 'success' | 'warning' | 'info' | 'primary' | 'danger'
}

/** 字典码表（按 value 索引，tag 文本/配色显示单一入口） */
export function dictMap<V extends number | string>(items: DictItem<V>[]): Record<V, DictItem<V>> {
  const map = {} as Record<V, DictItem<V>>
  for (const item of items) {
    map[item.value] = item
  }
  return map
}

/** 字典标签（value 空或未命中兜底 '-'） */
export function dictLabel<V extends number | string>(
  items: DictItem<V>[],
  value: V | null | undefined
): string {
  if (value === null || value === undefined) return '-'
  return items.find(i => i.value === value)?.label ?? '-'
}

/** 用户状态（后端 UserStatusEnum：0冻结 1正常 2注销） */
export const USER_STATUS_OPTIONS: DictItem[] = [
  { value: 0, label: '冻结', type: 'danger' },
  { value: 1, label: '正常', type: 'success' },
  { value: 2, label: '注销', type: 'info' }
]
export const USER_STATUS_MAP = dictMap(USER_STATUS_OPTIONS)

/** 用户角色（后端 role 字段：ADMIN/USER） */
export const USER_ROLE_OPTIONS: DictItem<string>[] = [
  { value: 'ADMIN', label: '管理员', type: 'danger' },
  { value: 'USER', label: '普通用户', type: 'info' }
]
export const USER_ROLE_MAP = dictMap(USER_ROLE_OPTIONS)

/** 减脂模式（后端 DietModeEnum：1=532 2=碳循环） */
export const DIET_MODE_OPTIONS: DictItem[] = [
  { value: 1, label: '532 碳水渐降' },
  { value: 2, label: '碳循环' }
]
export const DIET_MODE_MAP = dictMap(DIET_MODE_OPTIONS)

/** 活动系数（后端 ActivityLevelEnum：1久坐 2轻度 3中度 4高度） */
export const ACTIVITY_LEVEL_OPTIONS: DictItem[] = [
  { value: 1, label: '久坐' },
  { value: 2, label: '轻度' },
  { value: 3, label: '中度' },
  { value: 4, label: '高度' }
]
export const ACTIVITY_LEVEL_MAP = dictMap(ACTIVITY_LEVEL_OPTIONS)

/** 周期状态（后端 CyclePlanStatusEnum：1进行中 2已完成 3已终止） */
export const PLAN_STATUS_OPTIONS: DictItem[] = [
  { value: 1, label: '进行中' },
  { value: 2, label: '已完成' },
  { value: 3, label: '已终止' }
]
export const PLAN_STATUS_MAP = dictMap(PLAN_STATUS_OPTIONS)

/** 食物分类（后端 FoodCategoryEnum，01–10 顺序固定） */
export const FOOD_CATEGORY_OPTIONS: DictItem<string>[] = [
  { value: '01', label: '谷薯杂豆·主食' },
  { value: '02', label: '畜禽肉及制品' },
  { value: '03', label: '蛋奶及制品' },
  { value: '04', label: '水产及制品' },
  { value: '05', label: '大豆及制品' },
  { value: '06', label: '蔬菜' },
  { value: '07', label: '菌藻' },
  { value: '08', label: '水果' },
  { value: '09', label: '坚果·种子' },
  { value: '10', label: '油脂·调味·饮品' }
]
export const FOOD_CATEGORY_MAP = dictMap(FOOD_CATEGORY_OPTIONS)

/** 食物来源（1内置 2自定义） */
export const FOOD_SOURCE_OPTIONS: DictItem[] = [
  { value: 1, label: '内置', type: 'success' },
  { value: 2, label: '自定义', type: 'info' }
]
export const FOOD_SOURCE_MAP = dictMap(FOOD_SOURCE_OPTIONS)

/** 食物状态（1有效 0停用） */
export const FOOD_STATUS_OPTIONS: DictItem[] = [
  { value: 1, label: '有效', type: 'success' },
  { value: 0, label: '停用', type: 'danger' }
]
export const FOOD_STATUS_MAP = dictMap(FOOD_STATUS_OPTIONS)

/** 餐别（后端 MealTypeEnum：1早 2午 3晚 4加餐） */
export const MEAL_TYPE_OPTIONS: DictItem[] = [
  { value: 1, label: '早餐', type: 'success' },
  { value: 2, label: '午餐', type: 'warning' },
  { value: 3, label: '晚餐', type: 'info' },
  { value: 4, label: '加餐', type: 'primary' }
]
export const MEAL_TYPE_MAP = dictMap(MEAL_TYPE_OPTIONS)

/** 记录来源（1内置食物 2自定义食物 3手动输入） */
export const RECORD_SOURCE_OPTIONS: DictItem[] = [
  { value: 1, label: '食物库' },
  { value: 2, label: '自定义' },
  { value: 3, label: '手动输入' }
]
export const RECORD_SOURCE_MAP = dictMap(RECORD_SOURCE_OPTIONS)
