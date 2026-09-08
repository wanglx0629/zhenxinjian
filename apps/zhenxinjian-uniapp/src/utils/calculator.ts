/**
 * 臻心减核心算法（TypeScript 版，自小程序骨架迁移）
 * 作者: wanglx
 *
 * ★ 本文件与高保真原型的计算逻辑逐行对拍一致，
 *   任何修改都必须同步更新原型与 PRD，并回归 UAT 用例 U-01 ~ U-26。
 *   注意：核心计算以「后端为准」，前端仅作展示与离线兜底。
 */
import {
  RANGES, KCAL_PER_G, MACRO_532, CYCLE, PERIOD_PHASES, STAGE_532,
  type DayType, type PeriodPhaseCfg, type Stage532
} from '@/config/constants'

/* ------------------------------------------------------------------
 * 类型定义
 * ------------------------------------------------------------------ */

/** 用户档案（计算所需字段） */
export interface CalcProfile {
  gender: 'male' | 'female'
  age: number
  height: number
  weight: number
  targetWeight?: number
  /** 活动系数值（1.2 / 1.375 / 1.55 / 1.725） */
  act: number
  /** 热量缺口 X（200 / 300 / 400 / 500） */
  deficit: number
  /** 是否启用经期周期管理（女性） */
  period?: boolean
  /** 末次月经开始日 YYYY-MM-DD */
  periodStart?: string
  /** 周期长度（天，21–35） */
  cycleLen?: number
  /** 经期天数（3–10） */
  periodDays?: number
  /** 碳循环周期长度（天，7–14） */
  cycleDays?: number
  /** 脂肪系数（0.8 默认 / 1.0），持久化不随周期重置 */
  carbFatCoef?: number
  /** 运动日标记（下标 = 周期日序 - 1） */
  train?: boolean[]
}

/** 宏量结果 */
export interface MacroResult {
  kcal: number
  carb: number
  protein: number
  fat: number
}

/** 经期阶段信息 */
export interface PhaseInfo {
  key: string
  dayIdx: number
  cycleLen: number
  periodDays: number
  label: string
  color: string
  carb: number
  kcal: number
  focus: string
  tip: string
}

/** 532 阶段计划项 */
export interface Plan532Item {
  n: number
  name: string
  desc: string
  carb: number
  protein: number
  fat: number
  kcal: number
  phase: PhaseInfo | null
}

/** 饮食记录条目 */
export interface DietRecord {
  carb?: number
  protein?: number
  fat?: number
  kcal?: number
}

/** 每 100g 食物营养（计算用子集） */
export interface FoodMacroInput {
  carb: number
  protein: number
  fat: number
}

/** 体重记录条目 */
export interface WeightRecord {
  weight: number
}

/** 碳循环每日计划 */
export interface CycleDay {
  index: number
  type: DayType
  typeLabel: string
  carb: number
  fat: number
  protein: number
  kcal: number
  isTrain: boolean
}

/** 超标比对结果 */
export interface OverItem {
  key: string
  label: string
  value: number
  unit: string
}

export interface OverResult {
  diff: MacroResult
  overs: OverItem[]
  isOver: boolean
  advice: string
}

/* ------------------------------------------------------------------
 * 工具函数
 * ------------------------------------------------------------------ */

export const round = (n: number): number => Math.round(n)
export const clamp = (n: number, min: number, max: number): number => Math.min(max, Math.max(min, n))

/** 解析 YYYY-MM-DD → Date（本地时区，避免 iOS 上的 new Date(str) 兼容问题） */
export function parseDate(s?: string | null): Date | null {
  if (!s) return null
  const m = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(String(s).trim())
  if (!m) return null
  return new Date(parseInt(m[1], 10), parseInt(m[2], 10) - 1, parseInt(m[3], 10))
}

/** 取今天 00:00 */
export function todayStart(now?: Date | string | number): Date {
  const d = now ? new Date(now) : new Date()
  d.setHours(0, 0, 0, 0)
  return d
}

/* ------------------------------------------------------------------
 * 一、基础代谢与总消耗（PRD §2.2）
 * ------------------------------------------------------------------ */

/** BMR · Mifflin-St Jeor 公式（女 −161 / 男 +5） */
export function bmr(p: CalcProfile): number {
  const base = 10 * p.weight + 6.25 * p.height - 5 * p.age
  return p.gender === 'male' ? base + 5 : base - 161
}

/** TDEE = BMR × 活动系数 */
export function tdee(p: CalcProfile): number {
  return bmr(p) * p.act
}

/** ★ 基准热量 = TDEE − 用户自选缺口 X（532 与碳循环共用，PRD §2.4 / §2.5） */
export function targetKcal(p: CalcProfile): number {
  return tdee(p) - p.deficit
}

/* ------------------------------------------------------------------
 * 二、经期周期管理（女性专属 · F18 / F29，PRD §8）
 * ------------------------------------------------------------------ */

/**
 * 推算当前所处周期阶段
 * @param p     用户档案
 * @param now   基准日（默认今天），用于 UAT 造数
 * @returns 未启用 / 非女性 / 无有效日期时返回 null
 *
 * 阶段划分（L = 周期长度，D = 经期天数，dayIdx = 1..L）：
 *   经期   menstrual   dayIdx ∈ [1, D]        碳水 +15g / +60 kcal
 *   卵泡期 follicular  dayIdx ∈ (D, L−14]     基线
 *   排卵期 ovulation   dayIdx ∈ (L−14, L−12]  碳水 +5g  / +20 kcal
 *   黄体期 luteal      dayIdx ∈ (L−12, L]     碳水 +10g / +120 kcal
 */
export function menstrualPhase(p: CalcProfile, now?: Date | string | number): PhaseInfo | null {
  if (p.gender !== 'female' || !p.period) return null
  const start = parseDate(p.periodStart)
  if (!start) return null

  const L = clamp(p.cycleLen || RANGES.cycleLen.default || 28, RANGES.cycleLen.min, RANGES.cycleLen.max)
  const D = clamp(p.periodDays || RANGES.periodDays.default || 5, RANGES.periodDays.min, RANGES.periodDays.max)

  const n = todayStart(now)
  start.setHours(0, 0, 0, 0)

  let diff = Math.round((n.getTime() - start.getTime()) / 86400000)
  if (diff < 0) diff = 0                 // 开始日设为未来 → 视为第 1 天
  const dayIdx = (diff % L) + 1          // 1..L

  let key: keyof typeof PERIOD_PHASES
  if (dayIdx <= D) key = 'menstrual'
  else if (dayIdx <= L - 14) key = 'follicular'
  else if (dayIdx <= L - 12) key = 'ovulation'
  else key = 'luteal'

  const cfg: PeriodPhaseCfg = PERIOD_PHASES[key]
  return {
    key, dayIdx, cycleLen: L, periodDays: D,
    label: cfg.label, color: cfg.color,
    carb: cfg.carb, kcal: cfg.kcal,
    focus: cfg.focus, tip: cfg.tip
  }
}

/* ------------------------------------------------------------------
 * 三、532 碳水渐降模式（PRD §2.4）
 * ------------------------------------------------------------------ */

/** 532 基准宏量（未叠加经期上浮） */
export function macro532Base(p: CalcProfile): MacroResult {
  const total = targetKcal(p)
  return {
    kcal: round(total),
    carb: round(total * MACRO_532.carb / KCAL_PER_G.carb),
    protein: round(total * MACRO_532.protein / KCAL_PER_G.protein),
    fat: round(total * MACRO_532.fat / KCAL_PER_G.fat)
  }
}

/** 今日 532 目标（叠加经期阶段上浮） */
export function macro532(p: CalcProfile, now?: Date | string | number): MacroResult & { phase: PhaseInfo | null } {
  const b = macro532Base(p)
  const ph = menstrualPhase(p, now)
  if (ph) {
    b.carb += ph.carb
    b.kcal += ph.kcal
  }
  return { ...b, phase: ph }
}

/** 532 四阶段计划（阶段 2 叠加实时经期阶段，阶段 4 固定微调 −20g / −80 kcal） */
export function plan532Stages(p: CalcProfile, now?: Date | string | number): Plan532Item[] {
  const base = macro532Base(p)
  const ph = menstrualPhase(p, now)
  return STAGE_532.map((s: Stage532) => {
    let carb = base.carb + s.carbDelta
    let kcal = base.kcal + s.kcalDelta
    let desc = s.desc
    // 阶段 2：经期阶段自适应
    if (s.n === 2 && ph) {
      carb += ph.carb
      kcal += ph.kcal
      desc = `结合女性经期周期：当前处于「${ph.label}」第 ${ph.dayIdx} 天，${ph.tip}`
    }
    return {
      n: s.n, name: s.name, desc,
      carb, protein: base.protein, fat: base.fat, kcal,
      phase: s.n === 2 ? ph : null
    }
  })
}

/* ------------------------------------------------------------------
 * 四、碳循环模式（PRD §2.5 图片公式）
 * ------------------------------------------------------------------ */

/** 碳循环常量：周期碳水池 / 脂肪池 / 每日蛋白 */
export interface CarbonConst {
  tw: number
  cfc: number
  /** 周期碳水池 = 目标体重 × 2.5 × N */
  pool: number
  /** 周期脂肪池 = 目标体重 × cfc × N */
  poolFat: number
  /** 每日蛋白 = 当前体重 × 1.5（固定） */
  protein: number
}

export function carbonConst(p: CalcProfile): CarbonConst {
  const tw = p.targetWeight || p.weight
  const cfc = p.carbFatCoef || CYCLE.fatCoefDefault
  return {
    tw, cfc,
    pool: tw * CYCLE.carbCoef * (p.cycleDays || 7),
    poolFat: tw * cfc * (p.cycleDays || 7),
    protein: p.weight * CYCLE.proteinCoef
  }
}

export interface CycleAllocOpts {
  /** 运动日标记（下标 = 周期日序 - 1） */
  train?: boolean[]
  /** 基准日（用于经期阶段计算，UAT 造数用） */
  now?: Date | string | number
}

/** 碳循环每日分配 + 运动日智能适配 */
export interface CycleAlloc {
  order: DayType[]
  days: CycleDay[]
  cc: CarbonConst
  nH: number
  nM: number
  nL: number
  perH: { carb: number; fat: number }
  perM: { carb: number; fat: number }
  perL: { carb: number; fat: number }
  protein: number
  phase: PhaseInfo | null
}

/**
 * 碳循环每日分配 + 运动日智能适配
 * 高碳日：碳水 = 总碳水 × 50% ÷ 2.0   脂肪 = 总脂肪 × 15% ÷ 2.0
 * 中碳日：碳水 = 总碳水 × 35% ÷ 2.2   脂肪 = 总脂肪 × 35% ÷ 2.2
 * 低碳日：碳水 = 总碳水 × 15% ÷ 2.0   脂肪 = 总脂肪 × 50% ÷ 2.0
 * （2.0 / 2.2 / 2.0 为 7 天内各类型天数；周期长度按比例放大）
 */
export function cycleAlloc(p: CalcProfile, opts?: CycleAllocOpts): CycleAlloc {
  const o = opts || {}
  const cc = carbonConst(p)
  const N = clamp(p.cycleDays || 7, RANGES.cycleDays.min, RANGES.cycleDays.max)
  const w = N / 7                                   // 周倍数
  const R = CYCLE.ratio

  const nH = R.high.days * w
  const nM = R.mid.days * w
  const nL = R.low.days * w

  const perH = { carb: cc.pool * R.high.carbShare / nH, fat: cc.poolFat * R.high.fatShare / nH }
  const perM = { carb: cc.pool * R.mid.carbShare / nM, fat: cc.poolFat * R.mid.fatShare / nM }
  const perL = { carb: cc.pool * R.low.carbShare / nL, fat: cc.poolFat * R.low.fatShare / nL }

  // 周期日序模板
  const order: DayType[] = []
  for (let i = 0; i < N; i++) order.push(CYCLE.template[i % 7])

  // 运动日智能适配：运动日优先占高碳位，其次中碳位
  const train = o.train || p.train || []
  for (let a = 0; a < N; a++) {
    if (train[a] && order[a] !== 'high') {
      for (let b = 0; b < N; b++) {
        if (!train[b] && order[b] === 'high') { const t = order[a]; order[a] = order[b]; order[b] = t; break }
      }
    }
  }
  for (let a = 0; a < N; a++) {
    if (train[a] && order[a] === 'low') {
      for (let b = 0; b < N; b++) {
        if (!train[b] && order[b] === 'mid') { const t = order[a]; order[a] = order[b]; order[b] = t; break }
      }
    }
  }

  // 展开每日计划（叠加经期阶段上浮）
  const ph = menstrualPhase(p, o.now)
  const days: CycleDay[] = order.map((type, i) => {
    const per = type === 'high' ? perH : type === 'mid' ? perM : perL
    let carb = per.carb
    let fat = per.fat
    let kcal = carb * KCAL_PER_G.carb + cc.protein * KCAL_PER_G.protein + fat * KCAL_PER_G.fat
    if (ph) { carb += ph.carb; kcal += ph.kcal }
    return {
      index: i + 1, type,
      typeLabel: type === 'high' ? '高碳日' : type === 'mid' ? '中碳日' : '低碳日',
      carb: round(carb), fat: round(fat), protein: round(cc.protein), kcal: round(kcal),
      isTrain: !!train[i]
    }
  })

  return {
    order, days, cc, nH, nM, nL,
    perH, perM, perL,
    protein: cc.protein, phase: ph
  }
}

/* ------------------------------------------------------------------
 * 五、饮食记录与超标比对（PRD §2.6 / §2.7）
 * ------------------------------------------------------------------ */

/** 单条记录：重量(g) → 宏量。food 为每 100g 的 {carb, protein, fat} */
export function foodMacro(food: FoodMacroInput, grams: number): MacroResult {
  const r = grams / 100
  return {
    carb: food.carb * r,
    protein: food.protein * r,
    fat: food.fat * r,
    kcal: food.carb * r * KCAL_PER_G.carb + food.protein * r * KCAL_PER_G.protein + food.fat * r * KCAL_PER_G.fat
  }
}

/** 汇总当日记录 */
export function sumRecords(records: DietRecord[]): MacroResult {
  return records.reduce<MacroResult>((acc, r) => {
    acc.carb += r.carb || 0
    acc.protein += r.protein || 0
    acc.fat += r.fat || 0
    acc.kcal += r.kcal || 0
    return acc
  }, { carb: 0, protein: 0, fat: 0, kcal: 0 })
}

/**
 * 超标比对（F26）：返回各宏量的超出量与健康建议
 * PRD §2.7：超标时给出「碳水/脂肪偏高」类提示 + 次日调整建议，不做惩罚式提示
 */
export function overCheck(target: MacroResult, actual: MacroResult): OverResult {
  const diff = {
    carb: actual.carb - target.carb,
    protein: actual.protein - target.protein,
    fat: actual.fat - target.fat,
    kcal: actual.kcal - target.kcal
  }
  const overs: OverItem[] = []
  if (diff.carb > 10) overs.push({ key: 'carb', label: '碳水', value: round(diff.carb), unit: 'g' })
  if (diff.fat > 5) overs.push({ key: 'fat', label: '脂肪', value: round(diff.fat), unit: 'g' })
  if (diff.protein > 15) overs.push({ key: 'protein', label: '蛋白质', value: round(diff.protein), unit: 'g' })

  let advice = '今日配比良好，保持即可。'
  if (overs.length) {
    const names = overs.map(o => o.label).join('、')
    advice = `今日${names}略超，建议明日${overs.map(o => `${o.label}减少约 ${round(o.value / 2)}${o.unit}`).join('，')}，蛋白质保持不变。`
  } else if (diff.kcal < -300) {
    advice = '今日热量缺口偏大，注意补充蛋白质，避免过度节食导致代谢下降。'
  }
  return { diff, overs, isOver: overs.length > 0, advice }
}

/* ------------------------------------------------------------------
 * 六、体重趋势与调碳（PRD §2.8）
 * ------------------------------------------------------------------ */

/**
 * 体重连续停滞判定：近 7 天内体重波动 < 0.3kg 视为平台期
 * 触发后建议进入 532 阶段 4（碳水 −20g / 热量 −80 kcal）
 */
export function isPlateau(weightRecords?: WeightRecord[] | null): boolean {
  if (!weightRecords || weightRecords.length < 7) return false
  const last7 = weightRecords.slice(-7).map(r => r.weight)
  const max = Math.max.apply(null, last7)
  const min = Math.min.apply(null, last7)
  return (max - min) < 0.3
}
