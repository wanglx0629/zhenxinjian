/**
 * 食物弹窗共享表单模型与校验规则（DESIGN-T07 收敛新增/编辑 + 审核修正两弹窗重复）
 * 作者: wanglx
 */
import type { FormRules } from 'element-plus'

/** 两弹窗共用的食物宏量表单字段（id/kj/unit 由各弹窗自行扩展） */
export interface FoodFormModel {
  name: string
  categoryCode: string
  alias: string
  carb: number
  protein: number
  fat: number
  kcal: number
  serving: number
}

/** 新建共享表单初值 */
export function createFoodForm(): FoodFormModel {
  return {
    name: '',
    categoryCode: '',
    alias: '',
    carb: 0,
    protein: 0,
    fat: 0,
    kcal: 0,
    serving: 100
  }
}

/** 共享校验规则工厂（name 最大长度两弹窗不同，按参数传入） */
export function foodFormRules(nameMax: number): FormRules {
  return {
    name: [
      { required: true, message: '请输入食物名称', trigger: 'blur' },
      { max: nameMax, message: `名称不超过 ${nameMax} 字`, trigger: 'blur' }
    ],
    categoryCode: [{ required: true, message: '请选择分类', trigger: 'change' }],
    carb: [{ required: true, message: '请输入碳水', trigger: 'blur' }],
    protein: [{ required: true, message: '请输入蛋白质', trigger: 'blur' }],
    fat: [{ required: true, message: '请输入脂肪', trigger: 'blur' }],
    kcal: [{ required: true, message: '请输入热量', trigger: 'blur' }]
  }
}