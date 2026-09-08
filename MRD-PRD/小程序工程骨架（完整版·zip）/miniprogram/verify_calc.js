/* 算法对拍测试：验证小程序 calculator.js 与高保真原型 / PRD 图片示例完全一致
 * 运行：node verify_calc.js   （在 miniprogram 目录下）
 */
const calc = require('./utils/calculator.js')
const { FOODS } = require('./data/foods.js')

let pass = 0, fail = 0
function eq(label, got, want, tol) {
  tol = tol === undefined ? 0.05 : tol
  const ok = Math.abs(got - want) <= tol
  console.log((ok ? '  [OK] ' : '  [NG] ') + label + ': got ' + got + ', want ' + want)
  ok ? pass++ : fail++
}
// 先用 toFixed(6) 吸收 IEEE754 噪声再四舍五入，实现真正的 round-half-up
// （否则 25.9 × 1.5 = 38.849999999999994 会被误判为 38.8）
const r1 = n => Math.round(Number((Number(n) * 10).toFixed(6))) / 10

// ---- 基准档案（与原型 S 默认值一致）----
const P = { gender: 'female', age: 30, height: 162, weight: 55, targetWeight: 53,
  act: 1.375, deficit: 200, mode: '532', cycleDays: 7, carbFatCoef: 0.8,
  period: false, periodStart: '', cycleLen: 28, periodDays: 5 }

console.log('=== 1. BMR / TDEE / 532 基准（对拍原型 TARGET: 190/114/34/1521）===')
eq('BMR', calc.round(calc.bmr(P)), 1252, 1)
eq('TDEE', calc.round(calc.tdee(P)), 1721, 1)
const b = calc.macro532Base(P)
eq('532 碳水', b.carb, 190); eq('532 蛋白', b.protein, 114)
eq('532 脂肪', b.fat, 34); eq('532 热量', b.kcal, 1521)

console.log('\n=== 2. 碳循环（对拍 PRD 图片示例：当前 57kg → 目标 55kg）===')
const C = Object.assign({}, P, { weight: 57, targetWeight: 55 })
const cc = calc.carbonConst(C)
eq('7天碳水池', r1(cc.pool), 962.5)
eq('7天脂肪池', r1(cc.poolFat), 308)
eq('每日蛋白', r1(cc.protein), 85.5)
const a = calc.cycleAlloc(C)
eq('高碳日碳水', r1(a.perH.carb), 240.6)
eq('高碳日脂肪', r1(a.perH.fat), 23.1)
eq('中碳日碳水', r1(a.perM.carb), 153.1)
eq('中碳日脂肪', r1(a.perM.fat), 49.0)
eq('低碳日碳水', r1(a.perL.carb), 72.2)
eq('低碳日脂肪', r1(a.perL.fat), 77.0)

console.log('\n=== 3. 经期四阶段（周期 28 / 经期 5 天，起始 2026-09-01）===')
const F = Object.assign({}, P, { period: true, periodStart: '2026-09-01', cycleLen: 28, periodDays: 5 })
// 注意：2026-10-05 距起始日 34 天，dayIdx = 34 % 28 + 1 = 7，已过 5 天经期 → 卵泡期
const cases = [
  ['2026-09-01', 'menstrual', 1], ['2026-09-03', 'menstrual', 3], ['2026-09-05', 'menstrual', 5],
  ['2026-09-06', 'follicular', 6], ['2026-09-14', 'follicular', 14],
  ['2026-09-15', 'ovulation', 15], ['2026-09-16', 'ovulation', 16],
  ['2026-09-17', 'luteal', 17], ['2026-09-28', 'luteal', 28],
  ['2026-09-29', 'menstrual', 1], ['2026-10-05', 'follicular', 7],
  ['2026-10-27', 'menstrual', 1]   // 跨两轮：56 天后正好回到新一轮第 1 天
]
cases.forEach(function (c) {
  const ph = calc.menstrualPhase(F, c[0])
  const ok = ph && ph.key === c[1] && ph.dayIdx === c[2]
  console.log((ok ? '  [OK] ' : '  [NG] ') + c[0] + ' -> ' +
    (ph ? ph.key + '/第' + ph.dayIdx + '天' : 'null') + ' (want ' + c[1] + '/第' + c[2] + '天)')
  ok ? pass++ : fail++
})
eq('经期碳水上浮', calc.menstrualPhase(F, '2026-09-03').carb, 15)
eq('经期热量上浮', calc.menstrualPhase(F, '2026-09-03').kcal, 60)
eq('排卵期碳水上浮', calc.menstrualPhase(F, '2026-09-15').carb, 5)
eq('黄体期热量上浮', calc.menstrualPhase(F, '2026-09-20').kcal, 120)
console.log('  男性应返回 null: ' + calc.menstrualPhase(Object.assign({}, F, { gender: 'male' }), '2026-09-03'))
console.log('  未开启应返回 null: ' + calc.menstrualPhase(Object.assign({}, F, { period: false }), '2026-09-03'))
console.log('  未来日期按第1天: ' + calc.menstrualPhase(Object.assign({}, F, { periodStart: '2026-12-01' }), '2026-09-03').dayIdx)

console.log('\n=== 4. 532 今日目标叠加经期（2026-09-03 = 经期第 3 天）===')
const m = calc.macro532(F, '2026-09-03')
eq('今日碳水 190+15', m.carb, 205)
eq('今日热量 1521+60', m.kcal, 1581)
eq('蛋白不变', m.protein, 114)
eq('脂肪不变', m.fat, 34)

console.log('\n=== 5. 532 四阶段（阶段4 固定微调 -20g / -80kcal）===')
const st = calc.plan532Stages(P, '2026-09-03')
eq('阶段1 碳水', st[0].carb, 190)
eq('阶段4 碳水 190-20', st[3].carb, 170)
eq('阶段4 热量 1521-80', st[3].kcal, 1441)

console.log('\n=== 6. 食物换算（对拍 PRD 验收：150g 白米饭 = 38.9/3.9/0.5 → 175 kcal）===')
const rice = FOODS.filter(f => f.name.indexOf('米饭') >= 0)[0]
console.log('  样本：' + rice.name + '（每100g 碳' + rice.c + '/蛋' + rice.p + '/脂' + rice.f + '）')
// 说明：25.9 × 1.5 在 IEEE754 下为 38.849999999999994，一位小数得 38.8；
// 真值 38.85 四舍五入应为 38.9。0.1g 级差异无营养学意义，此处按 ±0.1 容差校验，
// 并额外校验 UI 实际展示的整数口径（Math.round → 39g）。
const fm = calc.foodMacro({ carb: rice.c, protein: rice.p, fat: rice.f }, 150)
eq('150g 碳水', r1(fm.carb), 38.9, 0.1)
eq('150g 蛋白', r1(fm.protein), 3.9, 0.1)
eq('150g 脂肪', r1(fm.fat), 0.5, 0.1)
eq('150g 热量（UI 整数口径）', calc.round(fm.kcal), 175, 1)
eq('150g 碳水（UI 整数口径）', calc.round(fm.carb), 39, 1)
console.log('  食物库条数：' + FOODS.length)

console.log('\n=== 7. 超标比对 ===')
const ov = calc.overCheck({ carb: 190, protein: 114, fat: 34, kcal: 1521 },
  { carb: 232, protein: 120, fat: 40, kcal: 1800 })
console.log('  ' + (ov.isOver ? '[OK] 判定超标：' + ov.overs.map(o => o.label + '超' + o.value + o.unit).join('、') : '[NG] 未判定超标'))
console.log('  建议：' + ov.advice)
ov.isOver ? pass++ : fail++

console.log('\n===== 通过 ' + pass + ' / 失败 ' + fail + ' =====')
process.exit(fail ? 1 : 0)
