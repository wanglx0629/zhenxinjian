<script setup lang="ts">
/**
 * WebSocket 连通状态卡片（仅探测，不聊天）
 * 作者: luote (luote) - https://luote996.cn
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { buildWsUrl, resolveWsPathLabel, resolveWsProtocol } from '@/utils/ws'

type ProbeStatus = 'idle' | 'checking' | 'online' | 'offline'

const userStore = useUserStore()
const router = useRouter()

const status = ref<ProbeStatus>('idle')
const latencyMs = ref<number | null>(null)
const lastCheckedAt = ref('')
const errorHint = ref('')

let probeSocket: WebSocket | null = null
let probeTimer: ReturnType<typeof setTimeout> | null = null

const pathLabel = resolveWsPathLabel()

const statusLabel = computed(() => {
  if (status.value === 'checking') {
    return '检测中'
  }
  if (status.value === 'online') {
    return '服务可用'
  }
  if (status.value === 'offline') {
    return '服务不可用'
  }
  return '未检测'
})

const statusTone = computed(() => {
  if (status.value === 'online') {
    return 'ok'
  }
  if (status.value === 'offline') {
    return 'bad'
  }
  if (status.value === 'checking') {
    return 'busy'
  }
  return 'idle'
})

function clearProbeTimer() {
  if (probeTimer) {
    clearTimeout(probeTimer)
    probeTimer = null
  }
}

function closeProbe() {
  clearProbeTimer()
  if (probeSocket) {
    probeSocket.onopen = null
    probeSocket.onerror = null
    probeSocket.onclose = null
    probeSocket.onmessage = null
    try {
      probeSocket.close()
    } catch {
      // 探测关闭失败可忽略
    }
    probeSocket = null
  }
}

function markChecked() {
  lastCheckedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
}

/**
 * 短暂握手探测连通性，成功后立即关闭，避免与演示页双连接长期并存
 */
function checkStatus() {
  if (status.value === 'checking') {
    return
  }
  const token = userStore.token || localStorage.getItem('token') || ''
  if (!token) {
    status.value = 'offline'
    errorHint.value = '未登录，无法鉴权握手'
    latencyMs.value = null
    markChecked()
    return
  }

  closeProbe()
  status.value = 'checking'
  errorHint.value = ''
  latencyMs.value = null

  const startedAt = performance.now()
  const url = buildWsUrl()
  try {
    probeSocket = new WebSocket(url, [resolveWsProtocol(), token])
  } catch {
    status.value = 'offline'
    errorHint.value = '无法创建连接'
    markChecked()
    return
  }

  probeTimer = setTimeout(() => {
    status.value = 'offline'
    errorHint.value = '握手超时'
    latencyMs.value = null
    markChecked()
    closeProbe()
  }, 8000)

  probeSocket.onopen = () => {
    latencyMs.value = Math.round(performance.now() - startedAt)
    status.value = 'online'
    errorHint.value = ''
    markChecked()
    closeProbe()
  }

  probeSocket.onerror = () => {
    status.value = 'offline'
    errorHint.value = '握手失败'
    latencyMs.value = null
    markChecked()
  }

  probeSocket.onclose = () => {
    if (status.value === 'checking') {
      status.value = 'offline'
      errorHint.value = '连接已关闭'
      latencyMs.value = null
      markChecked()
    }
    clearProbeTimer()
    probeSocket = null
  }
}

function goDemo() {
  router.push('/websocket')
}

onMounted(() => {
  checkStatus()
})

onBeforeUnmount(() => {
  closeProbe()
})
</script>

<template>
  <section class="ws-status-card">
    <div class="card-head">
      <div>
        <h3 class="title">WebSocket 连接状态</h3>
        <p class="desc">首页仅展示连通性，聊天演示请进入 WebSocket 页</p>
      </div>
      <div class="head-actions">
        <el-button size="small" :loading="status === 'checking'" @click="checkStatus">
          重新检测
        </el-button>
        <el-button type="primary" size="small" @click="goDemo">打开演示</el-button>
      </div>
    </div>

    <div class="status-row">
      <div class="status-main">
        <span class="dot" :class="statusTone" />
        <span class="status-text">{{ statusLabel }}</span>
      </div>
      <div class="metrics">
        <div class="metric">
          <span class="metric-label">端点</span>
          <span class="metric-value mono">{{ pathLabel }}</span>
        </div>
        <div class="metric">
          <span class="metric-label">鉴权</span>
          <span class="metric-value">Sec-WebSocket-Protocol</span>
        </div>
        <div class="metric">
          <span class="metric-label">延迟</span>
          <span class="metric-value">{{ latencyMs == null ? '--' : `${latencyMs} ms` }}</span>
        </div>
        <div class="metric">
          <span class="metric-label">最近检测</span>
          <span class="metric-value">{{ lastCheckedAt || '--' }}</span>
        </div>
      </div>
    </div>

    <p v-if="errorHint" class="hint error">{{ errorHint }}</p>
    <p v-else-if="status === 'online'" class="hint ok">握手成功，服务可接受实时连接</p>
  </section>
</template>

<style scoped>
.ws-status-card {
  background: var(--zhenxinjian-white);
  border: 1px solid var(--zhenxinjian-border);
  border-radius: 12px;
  padding: 20px 22px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
  color: var(--zhenxinjian-text);
}

.desc {
  margin: 0;
  font-size: 13px;
  color: var(--zhenxinjian-text-secondary);
  line-height: 1.5;
}

.head-actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
}

.status-row {
  display: grid;
  grid-template-columns: minmax(140px, 180px) 1fr;
  gap: 20px;
  align-items: center;
}

.status-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 48px;
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--zhenxinjian-bg);
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--zhenxinjian-text-secondary);
  flex-shrink: 0;
}

.dot.ok {
  background: #67c23a;
  box-shadow: 0 0 0 4px rgba(103, 194, 58, 0.18);
}

.dot.bad {
  background: #f56c6c;
  box-shadow: 0 0 0 4px rgba(245, 108, 108, 0.16);
}

.dot.busy {
  background: var(--zhenxinjian-primary);
  animation: pulse 1.2s ease-in-out infinite;
}

.dot.idle {
  background: var(--zhenxinjian-text-secondary);
}

.status-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--zhenxinjian-text);
}

.metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
}

.metric {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.metric-label {
  font-size: 12px;
  color: var(--zhenxinjian-text-secondary);
}

.metric-value {
  font-size: 13px;
  color: var(--zhenxinjian-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-value.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.hint {
  margin: 14px 0 0;
  font-size: 13px;
  line-height: 1.5;
}

.hint.ok {
  color: #67c23a;
}

.hint.error {
  color: #f56c6c;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.55;
    transform: scale(0.92);
  }
}

@media (max-width: 768px) {
  .card-head {
    flex-direction: column;
  }

  .status-row {
    grid-template-columns: 1fr;
  }

  .metrics {
    grid-template-columns: 1fr;
  }
}
</style>
