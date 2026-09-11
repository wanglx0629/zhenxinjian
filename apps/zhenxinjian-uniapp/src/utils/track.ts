/**
 * 埋点采集（内存队列 + storage 持久化；10s/满 20 条 flush；失败保留重试；队列上限 200）
 * 作者: wanglx
 *
 * 口径：clientTime 仅记录不参与统计（后端以 create_time 为准）；
 * 事件码须与后端 TrackEventEnum 一致，非法码后端整批拒收 40901；
 * 上报静默失败不弹提示、不阻塞业务
 */
import { reportEvents, type TrackEventItem } from '@/api/track'

const QUEUE_KEY = 'zxj_track_queue'
const QUEUE_MAX = 200
const FLUSH_SIZE = 20
const FLUSH_BATCH = 50
const FLUSH_INTERVAL = 10_000

let queue: TrackEventItem[] = []
let timer: ReturnType<typeof setInterval> | null = null
let flushing = false

function loadQueue() {
  try {
    const raw = uni.getStorageSync(QUEUE_KEY)
    queue = Array.isArray(raw) ? (raw as TrackEventItem[]).slice(0, QUEUE_MAX) : []
  } catch {
    queue = []
  }
}

function persistQueue() {
  try {
    uni.setStorageSync(QUEUE_KEY, queue)
  } catch {
    // 存储异常不影响业务，仅丢本次持久化
  }
}

function now() {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

/**
 * 上报一个埋点事件（入队，由 flush 批量发送；队列满则丢弃）
 */
export function track(eventCode: string, extra?: Record<string, unknown>, page?: string) {
  if (queue.length === 0) {
    loadQueue()
  }
  if (queue.length >= QUEUE_MAX) {
    return
  }
  queue.push({ eventCode, page, extra, clientTime: now() })
  persistQueue()
  if (queue.length >= FLUSH_SIZE) {
    flushTrackQueue()
  }
}

/** 页面访问埋点（PV） */
export function trackPage(pagePath: string) {
  track('page_view', undefined, pagePath)
}

/**
 * 批量 flush（每次取队首 ≤50 条上报；成功移除、失败保留下次重试）
 */
export function flushTrackQueue() {
  if (flushing) {
    return
  }
  if (queue.length === 0) {
    loadQueue()
  }
  if (queue.length === 0) {
    return
  }
  flushing = true
  const batch = queue.slice(0, FLUSH_BATCH)
  reportEvents(batch)
    .then(() => {
      queue = queue.slice(batch.length)
      persistQueue()
    })
    .catch(() => undefined)
    .finally(() => {
      flushing = false
    })
}

/** 启动定时 flush（App onLaunch 调一次；重复调用不叠加） */
export function startTrackFlushTimer() {
  if (timer) {
    return
  }
  loadQueue()
  flushTrackQueue()
  timer = setInterval(flushTrackQueue, FLUSH_INTERVAL)
}
