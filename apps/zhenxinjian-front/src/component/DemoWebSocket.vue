<script setup lang="ts">
/**
 * WebSocket Demo 看板（广播聊天板子）
 * 作者: wanglx
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { buildWsUrl, resolveWsPathLabel, resolveWsProtocol } from '@/utils/ws'

interface WsMessage {
  type: string
  from: string
  content: string
  time: string
  online?: number
}

const props = withDefaults(
  defineProps<{
    /** 是否进入即自动连接 */
    autoConnect?: boolean
  }>(),
  {
    autoConnect: true
  }
)

const userStore = useUserStore()
const connected = ref(false)
const connecting = ref(false)
const input = ref('')
const online = ref(0)
const messages = ref<WsMessage[]>([])
const listRef = ref<HTMLDivElement | null>(null)
const pathLabel = resolveWsPathLabel()

let socket: WebSocket | null = null

const statusText = computed(() => {
  if (connecting.value) {
    return '连接中'
  }
  return connected.value ? '已连接' : '未连接'
})

/**
 * 读取当前有效 Token（优先 Pinia，与 HTTP 续期保持一致）
 */
function resolveToken() {
  return userStore.token || localStorage.getItem('token') || ''
}

async function scrollToBottom() {
  await nextTick()
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}

function pushLocal(type: string, content: string) {
  messages.value.push({
    type,
    from: '本地',
    content,
    time: new Date().toLocaleTimeString('zh-CN', { hour12: false })
  })
  scrollToBottom()
}

function connect() {
  if (connected.value || connecting.value) {
    return
  }
  const token = resolveToken()
  if (!token) {
    ElMessage.warning('请先登录')
    return
  }
  connecting.value = true
  const url = buildWsUrl()
  // Sec-WebSocket-Protocol: {protocol}, <jwt>，避免 Token 出现在 URL/日志中
  socket = new WebSocket(url, [resolveWsProtocol(), token])

  socket.onopen = () => {
    connecting.value = false
    connected.value = true
    pushLocal('system', 'WebSocket 已连接')
  }

  socket.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data) as WsMessage
      messages.value.push(data)
      if (typeof data.online === 'number') {
        online.value = data.online
      }
      scrollToBottom()
      // 他人 chat 消息提示音（与 UniApp 行为对齐）
      if ((data.type || '').toLowerCase() === 'chat') {
        const me = userStore.userInfo?.username
        if (!me || data.from !== me) {
          try {
            const audio = new Audio('/notify.wav')
            audio.volume = 0.45
            void audio.play()
          } catch {
            // ignore
          }
        }
      }
    } catch {
      pushLocal('system', String(event.data))
    }
  }

  socket.onerror = () => {
    connecting.value = false
    ElMessage.error('WebSocket 连接异常')
  }

  socket.onclose = () => {
    connecting.value = false
    connected.value = false
    pushLocal('system', 'WebSocket 已断开')
  }
}

function disconnect() {
  socket?.close()
  socket = null
  connected.value = false
}

function sendChat() {
  const content = input.value.trim()
  if (!content) {
    return
  }
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    ElMessage.warning('请先连接 WebSocket')
    return
  }
  socket.send(JSON.stringify({ type: 'chat', content }))
  input.value = ''
}

function sendPing() {
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    ElMessage.warning('请先连接 WebSocket')
    return
  }
  socket.send(JSON.stringify({ type: 'ping' }))
}

onMounted(() => {
  if (props.autoConnect) {
    connect()
  }
})

onBeforeUnmount(() => {
  disconnect()
})
</script>

<template>
  <div class="ws-board">
    <div class="toolbar">
      <div class="meta">
        <el-tag :type="connected ? 'success' : 'info'" size="small">{{ statusText }}</el-tag>
        <span class="online">在线 {{ online }}</span>
        <span class="hint">{{ pathLabel }} · Sec-WebSocket-Protocol</span>
      </div>
      <div class="actions">
        <el-button type="primary" size="small" :disabled="connected || connecting" @click="connect">
          连接
        </el-button>
        <el-button size="small" :disabled="!connected" @click="disconnect">断开</el-button>
        <el-button size="small" :disabled="!connected" @click="sendPing">Ping</el-button>
      </div>
    </div>

    <div ref="listRef" class="message-list">
      <div
        v-for="(item, index) in messages"
        :key="index"
        class="message-item"
        :class="item.type"
      >
        <span class="time">{{ item.time }}</span>
        <span class="from">{{ item.from }}</span>
        <span class="content">{{ item.content }}</span>
      </div>
      <el-empty v-if="messages.length === 0" description="连接后即可收发消息" :image-size="64" />
    </div>

    <div class="composer">
      <el-input
        v-model="input"
        placeholder="输入消息，回车发送"
        maxlength="200"
        clearable
        @keyup.enter="sendChat"
      />
      <el-button type="primary" :disabled="!connected" @click="sendChat">发送</el-button>
    </div>
  </div>
</template>

<style scoped>
.ws-board {
  background: var(--zhenxinjian-white);
  border: 1px solid var(--zhenxinjian-border);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 560px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.online {
  font-size: 13px;
  color: var(--zhenxinjian-text);
}

.hint {
  font-size: 12px;
  color: var(--zhenxinjian-text-secondary);
}

.actions {
  display: flex;
  gap: 8px;
}

.message-list {
  flex: 1;
  min-height: 360px;
  max-height: 520px;
  overflow-y: auto;
  border: 1px solid var(--zhenxinjian-border);
  border-radius: 10px;
  padding: 12px 14px;
  background: var(--zhenxinjian-bg);
}

.message-item {
  display: flex;
  gap: 8px;
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 8px;
}

.message-item .time {
  color: var(--zhenxinjian-text-secondary);
  flex-shrink: 0;
}

.message-item .from {
  color: var(--zhenxinjian-primary);
  font-weight: 600;
  flex-shrink: 0;
}

.message-item.system .from,
.message-item.system .content,
.message-item.pong .content {
  color: var(--zhenxinjian-text-secondary);
  font-weight: 400;
}

.composer {
  display: flex;
  gap: 10px;
}
</style>
