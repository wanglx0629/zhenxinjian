/**
 * 埋点采集（内存队列 + storage 持久化；10s/满 20 条 flush；失败保留重试；队列上限 200）
 * 作者: wanglx
 *
 * 口径：clientTime 仅记录不参与统计（后端以 create_time 为准）；
 * 事件码须与后端 TrackEventEnum 一致；服务端毒性隔离——非法码随 200 响应的剔除列表返回，
 * 端上随整批移除永久丢弃不再重试；网络/5xx 类失败整批保留下次重试；
 * 队列 ≥80% 饱和时上报一次 track_queue_saturated 自监控告警；
 * 上报静默失败不弹提示、不阻塞业务
 */
import { reportEvents, type TrackEventItem } from '@/api/track'
import { TRACK_EVENT, type TrackEventCode } from '@/config/track-events'

const QUEUE_KEY = 'zxj_track_queue'
const QUEUE_MAX = 200
const FLUSH_SIZE = 20
const FLUSH_BATCH = 50
const FLUSH_INTERVAL = 10_000
/** 队列饱和度告警阈值（≥80% 触发一次自监控上报） */
const SATURATE_THRESHOLD = Math.floor(QUEUE_MAX * 0.8)

let queue: TrackEventItem[] = []
let timer: ReturnType<typeof setInterval> | null = null
let flushing = false
/** 饱和告警已发标记（回落至阈值下复位，避免每 10s 重复告警） */
let saturatedAlerted = false

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

/** 队列饱和度自监控：≥80% 且未告警时入队一条告警事件；回落至阈值下复位标记 */
function checkSaturation() {
  if (queue.length >= SATURATE_THRESHOLD) {
    if (!saturatedAlerted) {
      saturatedAlerted = true
      queue.push({
        eventCode: TRACK_EVENT.TRACK_QUEUE_SATURATED,
        extra: { queueSize: queue.length, queueMax: QUEUE_MAX },
        clientTime: now()
      })
      persistQueue()
    }
  } else if (saturatedAlerted) {
    saturatedAlerted = false
  }
}

/**
 * 上报一个埋点事件（入队，由 flush 批量发送；队列满则丢弃）
 */
export function track(eventCode: TrackEventCode, extra?: Record<string, unknown>, page?: string) {
  if (queue.length === 0) {
    loadQueue()
  }
  if (queue.length >= QUEUE_MAX) {
    return
  }
  queue.push({ eventCode, page, extra, clientTime: now() })
  persistQueue()
  checkSaturation()
  if (queue.length >= FLUSH_SIZE) {
    flushTrackQueue()
  }
}

/** 页面访问埋点（PV） */
export function trackPage(pagePath: string) {
  track(TRACK_EVENT.PAGE_VIEW, undefined, pagePath)
}

/**
 * 批量 flush（每次取队首 ≤50 条上报；
 * 成功：整批移除——合法条已落库、毒条已被服务端剔除随响应丢弃，不再重试；
 * 失败：网络/5xx 可重试失败，整批保留下次重试）
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
      checkSaturation()
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
