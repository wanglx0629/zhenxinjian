/**
 * 图标库：统一线性 SVG（24 viewBox / round 线帽），经 data-uri 由 <image> 渲染
 * 背景：mp-weixin 不支持内联 <svg> 节点；data-uri 方案 H5/小程序双端兼容
 * 用法：iconSrc('camera') 或 iconSrc('camera', '#00AC7C')
 * 作者: wanglx
 */

/** 图标 inner 路径集合（stroke 风格） */
const PATHS: Record<string, string> = {
  // 餐食相关
  breakfast:
    '<circle cx="12" cy="14" r="3.5"/><path d="M12 7.5V4.5M5.6 9.6L4 8M18.4 9.6L20 8M3 14h2M19 14h2M6 19h12"/>',
  bowl:
    '<path d="M4 11h16a8 8 0 0 1-16 0z"/><path d="M8.5 7.5c0-1 1-1.2 1-2.2M12 7c0-1 1-1.2 1-2.2"/>',
  moon: '<path d="M20 13.5A8 8 0 1 1 10.5 4 6.5 6.5 0 0 0 20 13.5z"/>',
  apple:
    '<path d="M12 8c-1.5-1.5-4-2-6-1-2 1-3 3-3 6 0 4 2.5 8 5 8 1 0 2-.5 3-.5s2 .5 3 .5c2.5 0 5-4 5-8 0-3-1-5-3-6-2-1-4.5-.5-6 1z"/><path d="M12 7.5c0-2 1-3.5 3-4"/>',
  leaf:
    '<path d="M5 19c0-7 5-13 14-14-1 9-7 14-14 14z"/><path d="M5 19c2-4 5-7 9-9"/>',
  flame:
    '<path d="M12 3c.5 3-2 4.5-2 7a2 2 0 0 0 4 0c0-1-.5-1.5-.5-2.5C15.5 9 18 11 18 14a6 6 0 1 1-12 0c0-4 3.5-6.5 6-11z"/>',
  avocado:
    '<path d="M12 3c2.5 0 5 2.2 6 5.5 1.2 4 .3 7.5-1.8 10-1.2 1.5-2.7 2.5-4.2 2.5s-3-1-4.2-2.5C5.7 16 4.8 12.5 6 8.5 7 5.2 9.5 3 12 3z"/><circle cx="12" cy="13" r="2"/>',

  // 操作 / 导航
  camera:
    '<path d="M4 8h3l1.5-2h7L17 8h3a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z"/><circle cx="12" cy="13" r="3.2"/>',
  image:
    '<rect x="3.5" y="4.5" width="17" height="15" rx="2"/><circle cx="9" cy="10" r="1.8"/><path d="M4.5 16.5l4.5-4 4 3.5 3-2.5 4 3"/>',
  calendar:
    '<rect x="3.5" y="5" width="17" height="15.5" rx="2"/><path d="M3.5 9.5h17M8 3v4M16 3v4"/>',
  chart: '<path d="M3 20v-6M9 20V5M15 20v-9M21 20H2"/>',
  search: '<circle cx="11" cy="11" r="6.5"/><path d="M16 16l4.5 4.5"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  minus: '<path d="M5 12h14"/>',
  check: '<path d="M5 12.5l4.5 4.5L19 7"/>',
  x: '<path d="M6 6l12 12M18 6L6 18"/>',
  edit: '<path d="M4 20h4L19 9a2.1 2.1 0 0 0-3-3L5 17v3z"/>',
  trash:
    '<path d="M4 7h16M9 7V5h6v2M6 7l1 13h10l1-13M10 11v6M14 11v6"/>',
  refresh:
    '<path d="M20 11A8 8 0 1 0 18 16M20 7v4h-4"/>',
  arrowLeft: '<path d="M19 12H5M11 6l-6 6 6 6"/>',
  chevronRight: '<path d="M9 5l7 7-7 7"/>',
  chevronLeft: '<path d="M15 5l-7 7 7 7"/>',
  chevronDown: '<path d="M5 9l7 7 7-7"/>',

  // 状态 / 反馈
  alert:
    '<path d="M12 4l9 15.5H3L12 4z"/><path d="M12 10v4M12 17.2h.01"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 8h.01"/>',
  smile:
    '<circle cx="12" cy="12" r="9"/><path d="M8.5 14a4.5 4.5 0 0 0 7 0"/><path d="M9 9.5h.01M15 9.5h.01"/>',
  sparkles:
    '<path d="M12 4l1.5 4L17.5 9.5 13.5 11 12 15l-1.5-4L6.5 9.5l4-1.5L12 4z"/><path d="M18 15l.7 1.8 1.8.7-1.8.7L18 20l-.7-1.8-1.8-.7 1.8-.7L18 15z"/>',
  star:
    '<path d="M12 3.5l2.6 5.2 5.8.9-4.2 4 1 5.7L12 16.6 6.8 19.3l1-5.7-4.2-4 5.8-.9L12 3.5z"/>',
  history:
    '<path d="M3.5 12a8.5 8.5 0 1 0 2.5-6M3.5 4v4h4"/><path d="M12 8v4l3 2"/>',
  gift:
    '<rect x="3.5" y="8" width="17" height="12.5" rx="1.5"/><path d="M3.5 12h17M12 8v12.5"/><path d="M12 8S10 4 7.5 5.5 8.5 8 12 8zm0 0s2-4 4.5-2.5S15.5 8 12 8z"/>',
  package:
    '<path d="M12 3l8 4.5v9L12 21l-8-4.5v-9L12 3z"/><path d="M4 7.5l8 4.5 8-4.5M12 12v9"/>',
  cloud:
    '<path d="M7 18a4 4 0 0 1-.5-7.97A5.5 5.5 0 0 1 17 9.5 3.5 3.5 0 0 1 17 18H7z"/>',
  bell:
    '<path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6z"/><path d="M10 19a2 2 0 0 0 4 0"/>',
  alarm:
    '<circle cx="12" cy="13" r="8"/><path d="M12 9v4l2.5 2M5 4L2.5 6.5M19 4l2.5 2.5"/>',

  // 人物 / 健康
  body: '<circle cx="12" cy="7.5" r="3.2"/><path d="M5.5 20a6.5 6.5 0 0 1 13 0"/>',
  lock:
    '<rect x="4.5" y="10.5" width="15" height="10" rx="2"/><path d="M8 10.5V7.5a4 4 0 0 1 8 0v3"/>',
  scale:
    '<path d="M6 3.5l-2.5 4v13a1.5 1.5 0 0 0 1.5 1.5h14a1.5 1.5 0 0 0 1.5-1.5v-13l-2.5-4z"/><path d="M3.5 7.5h17"/><path d="M12 11.5v5"/><circle cx="12" cy="10" r="1.2"/>',
  droplet:
    '<path d="M12 3.5s6 6.8 6 11a6 6 0 0 1-12 0c0-4.2 6-11 6-11z"/>',
  shield:
    '<path d="M12 3l7 3v5c0 4.5-3 8-7 10-4-2-7-5.5-7-10V6l7-3z"/>',
  target:
    '<circle cx="12" cy="12" r="8"/><circle cx="12" cy="12" r="4.5"/><circle cx="12" cy="12" r="1"/>',
  trend:
    '<path d="M3 17l6-6 4 4 8-8"/><path d="M15 7h6v6"/>',
  activity: '<path d="M3 12h4l3 7 4-14 3 7h4"/>',
  utensils:
    '<path d="M4 3v6a2 2 0 0 0 4 0V3M6 3v18"/><path d="M17 3c-2 0-3 3-3 7s1 5 3 5v6"/>'
}

export type IconName = keyof typeof PATHS

const cache = new Map<string, string>()

/**
 * 生成图标 data-uri
 * @param name 图标名
 * @param color 描边色（默认墨绿黑）
 * @param sw 描边宽度（默认 2）
 */
export function iconSrc(name: IconName, color = '#10312B', sw = 2): string {
  const key = `${name}|${color}|${sw}`
  const hit = cache.get(key)
  if (hit) return hit
  const svg =
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" ` +
    `stroke="${color}" stroke-width="${sw}" stroke-linecap="round" ` +
    `stroke-linejoin="round">${PATHS[name]}</svg>`
  const uri = `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`
  cache.set(key, uri)
  return uri
}

const ringCache = new Map<string, string>()

/**
 * 环形进度 data-uri（动态达成率/颜色，经 <image> 渲染，H5 与 mp-weixin 通用）
 * @param percent 达成率 0-100（内部封顶 100）
 * @param color 进度描边色
 */
export function ringSrc(percent: number, color: string): string {
  const pct = Math.max(0, Math.min(100, Math.round(percent)))
  const key = `${pct}|${color}`
  const hit = ringCache.get(key)
  if (hit) return hit
  const r = 52
  const len = 2 * Math.PI * r
  const offset = len * (1 - pct / 100)
  const svg =
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120">` +
    `<circle cx="60" cy="60" r="${r}" fill="none" stroke="rgba(255,255,255,0.25)" stroke-width="10"/>` +
    `<circle cx="60" cy="60" r="${r}" fill="none" stroke="${color}" stroke-width="10" ` +
    `stroke-linecap="round" stroke-dasharray="${len}" stroke-dashoffset="${offset}" ` +
    `transform="rotate(-90 60 60)"/></svg>`
  const uri = `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`
  ringCache.set(key, uri)
  return uri
}
