/**
 * 小程序页面资源预加载
 * 作者: wanglx
 */

/** 需要预热的非首屏路由（首屏 pages/auth/guide 已随包加载，无需重复预热） */
const PRELOAD_ROUTES = [
  '/pages/home/index',
  '/pages/record/index',
  '/pages/record/add',
  '/pages/record/recognize',
  '/pages/food/index',
  '/pages/food/detail',
  '/pages/food/custom-edit',
  '/pages/food/custom-list',
  '/pages/mine/index',
  '/pages/body/profile',
  '/pages/body/result',
  '/pages/mode/select',
  '/pages/cycle/setting',
  '/pages/cycle/plan',
  '/pages/reminder/index',
  '/pages/taper/plan',
  '/pages/weight/index',
  '/pages/menstrual/index',
  '/pages/auth/expire'
]

let started = false

/**
 * 在首屏渲染后再逐条预热页面代码，避免阻塞启动；
 * 仅基础库支持 preloadPage 时生效，低版本静默跳过。
 */
export function preloadMpPages() {
  if (started) return
  started = true

  const preload = (
    uni as unknown as { preloadPage?: (options: { url: string }) => void }
  ).preloadPage
  if (typeof preload !== 'function') return

  PRELOAD_ROUTES.forEach((url, index) => {
    setTimeout(() => {
      try {
        preload({ url })
      } catch {
        // 基础库兼容性异常时静默忽略，不影响业务跳转
      }
    }, 300 + index * 80)
  })
}
