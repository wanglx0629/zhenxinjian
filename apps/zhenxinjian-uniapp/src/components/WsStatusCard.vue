<script setup lang="ts">
/**
 * WebSocket 连通状态卡片（仅探测，不聊天）
 * 作者: luote (luote) - https://luote996.cn
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { buildWsUrl, connectAuthedSocket, resolveWsProtocol } from '@/utils/ws'
import { getToken } from '@/utils/storage'

type ProbeStatus = 'idle' | 'checking' | 'online' | 'offline'

const status = ref<ProbeStatus>('idle')
const latencyMs = ref<number | null>(null)
const lastCheckedAt = ref('')
const errorHint = ref('')

let probeTask: UniApp.SocketTask | null = null
let probeTimer: ReturnType<typeof setTimeout> | null = null

const pathLabel = computed(() => {
  const wsPath = import.meta.env.VITE_WS_PATH || '/api/ws/demo'
  return wsPath.startsWith('/') ? wsPath : `/${wsPath}`
})

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
  if (probeTask) {
    try {
      probeTask.close({})
    } catch {
      // 探测关闭失败可忽略
    }
    probeTask = null
  }
}

function markChecked() {
  lastCheckedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
}

/**
 * 短暂握手探测连通性，成功后立即关闭
 */
function checkStatus() {
  if (status.value === 'checking') {
    return
  }
  if (!getToken()) {
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
  const startedAt = Date.now()

  probeTimer = setTimeout(() => {
    status.value = 'offline'
    errorHint.value = '握手超时'
    latencyMs.value = null
    markChecked()
    closeProbe()
  }, 8000)

  probeTask = connectAuthedSocket({
    onOpen: () => {
      latencyMs.value = Date.now() - startedAt
      status.value = 'online'
      errorHint.value = ''
      markChecked()
      closeProbe()
    },
    onError: () => {
      status.value = 'offline'
      errorHint.value = '握手失败'
      latencyMs.value = null
      markChecked()
      clearProbeTimer()
    },
    onClose: () => {
      if (status.value === 'checking') {
        status.value = 'offline'
        errorHint.value = '连接已关闭'
        latencyMs.value = null
        markChecked()
      }
      clearProbeTimer()
      probeTask = null
    }
  })

  if (!probeTask) {
    status.value = 'offline'
    errorHint.value = '无法创建连接'
    markChecked()
    clearProbeTimer()
  }
}

function goDemo() {
  uni.switchTab({ url: '/pages/websocket/index' })
}

onMounted(() => {
  checkStatus()
})

onBeforeUnmount(() => {
  closeProbe()
})
</script>

<template>
  <view class="ws-status-card">
    <view class="card-head">
      <view class="head-text">
        <text class="title">WebSocket 连接状态</text>
        <text class="desc">首页仅展示连通性，聊天请进入消息页</text>
      </view>
      <view class="head-actions">
        <button class="btn ghost" size="mini" :loading="status === 'checking'" @click="checkStatus">
          重新检测
        </button>
        <button class="btn primary" size="mini" @click="goDemo">打开演示</button>
      </view>
    </view>

    <view class="status-main">
      <view class="dot" :class="statusTone" />
      <text class="status-text">{{ statusLabel }}</text>
    </view>

    <view class="metrics">
      <view class="metric">
        <text class="metric-label">端点</text>
        <text class="metric-value">{{ pathLabel }}</text>
      </view>
      <view class="metric">
        <text class="metric-label">鉴权</text>
        <text class="metric-value">{{ resolveWsProtocol() }} / Authorization</text>
      </view>
      <view class="metric">
        <text class="metric-label">延迟</text>
        <text class="metric-value">{{ latencyMs == null ? '--' : `${latencyMs} ms` }}</text>
      </view>
      <view class="metric">
        <text class="metric-label">最近检测</text>
        <text class="metric-value">{{ lastCheckedAt || '--' }}</text>
      </view>
    </view>

    <text v-if="errorHint" class="hint error">{{ errorHint }}</text>
    <text v-else-if="status === 'online'" class="hint ok">握手成功，服务可接受实时连接</text>
    <text class="endpoint-hint">探测地址：{{ buildWsUrl() }}</text>
  </view>
</template>

<style scoped lang="scss">
.ws-status-card {
  background: $zhenxinjian-white;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 16rpx;
  padding: 28rpx;
}

.card-head {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  margin-bottom: 24rpx;
}

.title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 8rpx;
}

.desc {
  display: block;
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.5;
}

.head-actions {
  display: flex;
  gap: 16rpx;
}

.btn {
  margin: 0;
  border-radius: 10rpx;
  font-size: 24rpx;
}

.btn.ghost {
  background: $zhenxinjian-bg;
  color: $zhenxinjian-text;
  border: 1rpx solid $zhenxinjian-border;
}

.btn.primary {
  background: $zhenxinjian-primary;
  color: #fff;
  border: none;
}

.status-main {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 22rpx;
  border-radius: 12rpx;
  background: $zhenxinjian-bg;
  margin-bottom: 20rpx;
}

.dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: $zhenxinjian-text-secondary;
  flex-shrink: 0;
}

.dot.ok {
  background: #67c23a;
  box-shadow: 0 0 0 6rpx rgba(103, 194, 58, 0.18);
}

.dot.bad {
  background: #f56c6c;
  box-shadow: 0 0 0 6rpx rgba(245, 108, 108, 0.16);
}

.dot.busy {
  background: $zhenxinjian-primary;
  animation: pulse 1.2s ease-in-out infinite;
}

.status-text {
  font-size: 28rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx 20rpx;
}

.metric {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  min-width: 0;
}

.metric-label {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
}

.metric-value {
  font-size: 24rpx;
  color: $zhenxinjian-text;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hint {
  display: block;
  margin-top: 20rpx;
  font-size: 24rpx;
  line-height: 1.5;
}

.hint.ok {
  color: #67c23a;
}

.hint.error {
  color: #f56c6c;
}

.endpoint-hint {
  display: block;
  margin-top: 12rpx;
  font-size: 20rpx;
  color: $zhenxinjian-text-secondary;
  word-break: break-all;
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
</style>
