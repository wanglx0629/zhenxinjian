/**
 * 全局常量（所有口径与 PRD / 高保真原型严格一致，修改前请同步更新 PRD）
 * 作者: wanglx
 */

/** 活动系数（PRD §2.2；对照后端 ActivityLevelEnum：数组顺序即后端 code 1-4，value 与 factor 一一对应，改动须同步后端枚举） */
export const ACTIVITY = [
  { key: 'sedentary', label: '久坐（几乎不运动）', value: 1.2 },
  { key: 'light', label: '轻度活动（每周 1-3 次）', value: 1.375 },
  { key: 'moderate', label: '中度活动（每周 3-5 次）', value: 1.55 },
  { key: 'active', label: '高度活动（每周 6-7 次）', value: 1.725 }
] as const

/** 活动系数键 */
export type ActivityKey = (typeof ACTIVITY)[number]['key']

/** 热量缺口档位（PRD §2.3，用户自选 X；默认 200 温和档；对照后端 DeficitOptionEnum 四档 code，改动须同步后端枚举） */
export const DEFICIT_OPTIONS = [200, 300, 400, 500] as const

/** 532 模式宏量配比（PRD §2.4，硬性写死） */
export const MACRO_532 = { carb: 0.5, protein: 0.3, fat: 0.2 } as const

/** 每克宏量产能（kcal） */
export const KCAL_PER_G = { carb: 4, protein: 4, fat: 9 } as const

/** 碳循环：碳水池系数 / 脂肪池系数（PRD §2.5 图片公式；对照后端 CycleCalcService：CARB_POOL_FACTOR=2.5 / DEFAULT_CFC=0.8 / CFC_OPTIONS={0.8,1.0} / 蛋白 1.5 / DayTypeParam 占比与天数 / TEMPLATE 日序，改动须同步后端） */
export const CYCLE = {
  /** 7 天总碳水 = 目标体重 × 2.5 × 周期天数 */
  carbCoef: 2.5,
  /** 7 天总脂肪 = 目标体重 × 脂肪系数 × 周期天数（可切 1.0 更易执行，持久化不随周期重置） */
  fatCoefDefault: 0.8,
  fatCoefOptions: [0.8, 1.0],
  /** 每日蛋白质 = 当前体重 × 1.5（周期内固定） */
  proteinCoef: 1.5,
  /** 高 / 中 / 低碳日的碳水 & 脂肪占比 */
  ratio: {
    high: { carbShare: 0.5, fatShare: 0.15, days: 2.0 },
    mid: { carbShare: 0.35, fatShare: 0.35, days: 2.2 },
    low: { carbShare: 0.15, fatShare: 0.5, days: 2.0 }
  },
  /** 周期日序模板（7 天一轮，按周期长度循环） */
  template: ['high', 'mid', 'low', 'low', 'mid', 'high', 'low']
} as const

/** 录入区间校验（PRD §2.2 / §8.1，越界前端拒绝并兜底；后端二次兜底。对照：年龄 12-80 = 后端 BodyProfileSaveDTO @Min/@Max；体重 25-200 = 后端 WeightService WEIGHT_MIN/MAX；经期 cycleLen 21-35 / periodDays 3-10 = 后端 MenstrualService CYCLE_LEN/PERIOD_DAYS_MIN/MAX；碳循环天数 7-14 = 后端 CyclePlanService，改动须同步后端） */
export interface RangeCfg {
  min: number
  max: number
  unit: string
  default?: number
}

export const RANGES: Record<string, RangeCfg> = {
  age: { min: 12, max: 80, unit: '岁' },
  height: { min: 100, max: 250, unit: 'cm' },
  weight: { min: 25, max: 200, unit: 'kg' },
  targetWeight: { min: 25, max: 200, unit: 'kg' },
  cycleLen: { min: 21, max: 35, unit: '天', default: 28 },
  periodDays: { min: 3, max: 10, unit: '天', default: 5 },
  cycleDays: { min: 7, max: 14, unit: '天', default: 7 }
}

/* ------------------------------------------------------------------ */
/* Change 5 饮食记录：餐别 / 三色进度 / 微调建议（与后端 MealTypeEnum 对齐） */
/* ------------------------------------------------------------------ */

/** 餐别列表（code 与后端 MealTypeEnum 一致） */
export interface MealTypeCfg {
  code: number
  name: string
}

export const MEAL_TYPES: MealTypeCfg[] = [
  { code: 1, name: '早餐' },
  { code: 2, name: '午餐' },
  { code: 3, name: '晚餐' },
  { code: 4, name: '加餐' }
]

/** 餐别 emoji（code 与 MEAL_TYPES 对齐，首页餐次 chip / 记录页餐别头共用，兜底 🍽️） */
export const MEAL_EMOJI: Record<number, string> = { 1: '🍳', 2: '🍱', 3: '🌙', 4: '🍎' }

/** 三色进度阈值（达成率 %）：80–100 绿 / <80 黄 / >100 红 */
export const PROGRESS_THRESHOLD = { green: 80, red: 100 } as const

/** 三色进度颜色（对齐设计系统 V2.0 充足绿/不足黄/超标红） */
export const PROGRESS_COLORS = { green: '#00AC7C', yellow: '#FFB020', red: '#FF4747' } as const

/** 微调建议文案（按超标项动态拼接，spec：超标微调建议） */
export const OVER_ADVICE = {
  fat: '脂肪超标：建议换清蒸/水煮做法、去肥肉油碟，以鸡胸/虾/豆腐替代部分肥肉。',
  carb: '碳水超标：建议主食减半或换糙米/红薯，先菜肉后饭。',
  protein: '蛋白超标：问题不大，压回脂肪碳水即可，无需刻意减少蛋白。',
  kcal: '总热量超标：建议散步 20–30 分钟，不要跳过下一餐。',
  disclaimer: '以上建议为通用饮食调整方向，非医疗建议。如有特殊健康状况请咨询专业医师。'
} as const

/* ------------------------------------------------------------------ */
/* Change 6 碳循环：模式字典 / 日型字典（code 与后端枚举对齐） */
/* ------------------------------------------------------------------ */

/** 减脂模式（与后端 DietModeEnum 对齐） */
export interface DietModeCfg {
  code: number
  name: string
  desc: string
}

export const DIET_MODES: DietModeCfg[] = [
  { code: 1, name: '532', desc: '碳水渐降 · 固定配比 50/30/20，适合稳健起步' },
  { code: 2, name: '碳循环', desc: '高/中/低碳日轮播，运动日自动高配，易坚持' }
]

/** 碳循环日型字典（与后端 CycleDayTypeEnum 对齐；色值与原型 P07 一致） */
export interface CycleDayTypeCfg {
  code: number
  name: string
  short: string
  color: string
  bg: string
}

export const CYCLE_DAY_TYPES: Record<number, CycleDayTypeCfg> = {
  1: { code: 1, name: '高碳日', short: '高', color: '#FF4747', bg: '#ffefef' },
  2: { code: 2, name: '中碳日', short: '中', color: '#00AC7C', bg: '#E3EFE9' },
  3: { code: 3, name: '低碳日', short: '低', color: '#475569', bg: '#edf1ef' }
}

/** 周期天数区间（运动日多选随天数裁剪上限） */
export const CYCLE_DAYS_RANGE = { min: 7, max: 14, default: 7 } as const

/* ------------------------------------------------------------------ */
/* Change 7 首页与我的：问候语时段（餐次名复用 MEAL_TYPES，模式/日型复用上方字典） */
/* ------------------------------------------------------------------ */

/** 分时段问候语时段表（05–11 早上好 / 11–14 中午好 / 14–18 下午好 / 18–23 晚上好） */
export const GREETING_SLOTS = [
  { from: 5, to: 11, text: '早上好' },
  { from: 11, to: 14, text: '中午好' },
  { from: 14, to: 18, text: '下午好' },
  { from: 18, to: 23, text: '晚上好' }
] as const

/** 默认问候语（23–05 夜深了） */
export const GREETING_DEFAULT = '夜深了'
