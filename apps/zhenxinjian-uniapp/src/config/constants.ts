/**
 * 全局常量（所有口径与 PRD / 高保真原型严格一致，修改前请同步更新 PRD）
 * 作者: wanglx
 */

/** 活动系数（PRD §2.2） */
export const ACTIVITY = [
  { key: 'sedentary', label: '久坐（几乎不运动）', value: 1.2 },
  { key: 'light', label: '轻度活动（每周 1-3 次）', value: 1.375 },
  { key: 'moderate', label: '中度活动（每周 3-5 次）', value: 1.55 },
  { key: 'active', label: '高度活动（每周 6-7 次）', value: 1.725 }
] as const

/** 活动系数键 */
export type ActivityKey = (typeof ACTIVITY)[number]['key']

/** 热量缺口档位（PRD §2.3，用户自选 X；默认 200 温和档） */
export const DEFICIT_OPTIONS = [200, 300, 400, 500] as const

/** 532 模式宏量配比（PRD §2.4，硬性写死） */
export const MACRO_532 = { carb: 0.5, protein: 0.3, fat: 0.2 } as const

/** 每克宏量产能（kcal） */
export const KCAL_PER_G = { carb: 4, protein: 4, fat: 9 } as const

/** 碳循环：碳水池系数 / 脂肪池系数（PRD §2.5 图片公式） */
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

/** 日型键 */
export type DayType = 'high' | 'mid' | 'low'

/** 经期四阶段（PRD §8 / 原型 F29） */
export interface PeriodPhaseCfg {
  label: string
  color: string
  carb: number
  kcal: number
  focus: string
  tip: string
}

export const PERIOD_PHASES: Record<string, PeriodPhaseCfg> = {
  menstrual: {
    label: '经期', color: '#DB2777', carb: 15, kcal: 60,
    focus: '补铁 · 温补 · 避寒凉：推荐红肉 / 动物肝脏 / 菠菜 / 红枣；适度增加碳水缓解乏力、情绪波动与暴食。',
    tip: '经期身体对碳水利用更友好，上浮碳水 +15g（+60 kcal），蛋白脂肪不变。'
  },
  follicular: {
    label: '卵泡期', color: '#0891B2', carb: 0, kcal: 0,
    focus: '雌激素上升、代谢与恢复力佳，按基准热量稳步推进缺口，可正常训练。',
    tip: '基线阶段，无需额外加碳，安心推进减脂。'
  },
  ovulation: {
    label: '排卵期', color: '#7C3AED', carb: 5, kcal: 20,
    focus: '食欲与体温回升，注意控量；补充镁、锌，缓解腹胀。',
    tip: '排卵期轻微上浮碳水 +5g（+20 kcal），平稳过渡。'
  },
  luteal: {
    label: '黄体期', color: '#EA580C', carb: 10, kcal: 120,
    focus: '经前综合征高发：控盐控水肿、补钙镁；适度回补热量缓解 cravings，避免极端节食。',
    tip: '黄体期回补热量 +120 kcal、碳水 +10g，降低暴食风险，保护代谢。'
  }
}

/** 录入区间校验（PRD §2.2 / §8.1，越界前端拒绝并兜底；后端二次兜底） */
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

/** 532 四阶段（PRD §2.4 / 原型 P08） */
export interface Stage532 {
  n: number
  name: string
  carbDelta: number
  kcalDelta: number
  desc: string
}

export const STAGE_532: Stage532[] = [
  {
    n: 1, name: '阶段 1 · 月初适应期', carbDelta: 0, kcalDelta: 0,
    desc: '维持标准 50%/30%/20% 固定配比，让身体适应减脂饮食，不做任何降碳调整。'
  },
  {
    n: 2, name: '阶段 2 · 稳步减脂期（经期适配）', carbDelta: 0, kcalDelta: 0,
    desc: '结合女性经期周期，按所处阶段自动上浮碳水与热量，降低减脂压力，规避水肿、乏力、暴食。'
  },
  {
    n: 3, name: '阶段 3 · 经后高效期', carbDelta: 0, kcalDelta: 0,
    desc: '经期结束后稳步梯度小幅降碳，温和放大热量缺口，高效减脂。'
  },
  {
    n: 4, name: '阶段 4 · 平台突破期', carbDelta: -20, kcalDelta: -80,
    desc: '针对体重停滞用户触发固定微调：碳水 −20g、总热量 −80 kcal，蛋白脂肪保持不变，平稳突破平台。'
  }
]

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

/**
 * 按当前时段取默认餐别（05–10 早 / 10–15 午 / 15–20:30 晚 / 其余加餐）
 * @param hour 当前小时（0–23），可含小数表示分钟
 */
export function defaultMealType(hour?: number): number {
  const h = hour ?? new Date().getHours() + new Date().getMinutes() / 60
  if (h >= 5 && h < 10) return 1
  if (h >= 10 && h < 15) return 2
  if (h >= 15 && h < 20.5) return 3
  return 4
}

/** 三色进度阈值（达成率 %）：80–100 绿 / <80 黄 / >100 红 */
export const PROGRESS_THRESHOLD = { green: 80, red: 100 } as const

/** 三色进度颜色 */
export const PROGRESS_COLORS = { green: '#67C23A', yellow: '#E6A23C', red: '#F56C6C' } as const

/** 微调建议文案（按超标项动态拼接，spec：超标微调建议） */
export const OVER_ADVICE = {
  fat: '脂肪超标：建议换清蒸/水煮做法、去肥肉油碟，以鸡胸/虾/豆腐替代部分肥肉。',
  carb: '碳水超标：建议主食减半或换糙米/红薯，先菜肉后饭。',
  protein: '蛋白超标：问题不大，压回脂肪碳水即可，无需刻意减少蛋白。',
  kcal: '总热量超标：建议散步 20–30 分钟，不要跳过下一餐。',
  disclaimer: '以上建议为通用饮食调整方向，非医疗建议。如有特殊健康状况请咨询专业医师。'
} as const
