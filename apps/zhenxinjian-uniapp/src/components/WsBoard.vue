<script setup lang="ts">
/**
 * WebSocket 看板：发消息 + 实时收消息 + 提示音
 * 作者: luote (luote) - https://luote996.cn
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useUserStore } from '@/store/user'
import { connectAuthedSocket, type WsMessage } from '@/utils/ws'
import {
  destroyNotifySound,
  loadNotifySoundPreference,
  playNotifySound,
  setNotifySoundEnabled
} from '@/utils/notify'

const props = withDefaults(
  defineProps<{
    autoConnect?: boolean
    compact?: boolean
  }>(),
  {
    autoConnect: true,
    compact: false
  }
)

const userStore = useUserStore()
const connected = ref(false)
const connecting = ref(false)
const sending = ref(false)
const input = ref('')
const online = ref(0)
const messages = ref<WsMessage[]>([])
const scrollInto = ref('')
const soundOn = ref(true)

let socket: UniNamespace.SocketTask | null = null

const statusText = computed(() => {
  if (connecting.value) {
    return '连接中'
  }
  return connected.value ? '已连接' : '未连接'
})

const myName = computed(
  () => userStore.userInfo?.username || userStore.userInfo?.nickname || ''
)

/**
 * 滚到最新消息
 */
async function scrollToLatest() {
  await nextTick()
  if (messages.value.length === 0) {
    return
  }
  scrollInto.value = ''
  await nextTick()
  scrollInto.value = 'msg-' + (messages.value.length - 1)
}

function pushLocal(type: string, content: string) {
  messages.value.push({
    type,
    from: '本地',
    content,
    time: new Date().toLocaleTimeString('zh-CN', { hour12: false })
  })
  scrollToLatest()
}

/**
 * 收到服务端消息：入列表；他人 chat 播提示音
 */
function handleIncoming(data: WsMessage | string) {
  if (typeof data === 'string') {
    pushLocal('system', data)
    return
  }
  messages.value.push(data)
  if (typeof data.online === 'number') {
    online.value = data.online
  }
  scrollToLatest()

  const type = (data.type || '').toLowerCase()
  if (type === 'chat') {
    const fromSelf = myName.value && data.from === myName.value
    if (!fromSelf) {
      playNotifySound()
    }
  }
}

function connect() {
  if (connected.value || connecting.value) {
    return
  }
  connecting.value = true
  socket = connectAuthedSocket({
    onOpen: () => {
      connecting.value = false
      connected.value = true
      pushLocal('system', 'WebSocket 已连接，可发送消息')
    },
    onMessage: handleIncoming,
    onClose: () => {
      connecting.value = false
      connected.value = false
      pushLocal('system', 'WebSocket 已断开')
    },
    onError: () => {
      connecting.value = false
      uni.showToast({ title: 'WebSocket 连接异常', icon: 'none' })
    }
  })
  if (!socket) {
    connecting.value = false
  }
}

function disconnect() {
  try {
    socket?.close({})
  } catch {
    // ignore
  }
  socket = null
  connected.value = false
}

/**
 * 发送聊天消息（走广播房间）
 */
function sendChat() {
  const content = input.value.trim()
  if (!content) {
    return
  }
  if (!connected.value || !socket) {
    uni.showToast({ title: '请先连接 WebSocket', icon: 'none' })
    return
  }
  if (sending.value) {
    return
  }
  sending.value = true
  const payload = JSON.stringify({ type: 'chat', content })
  socket.send({
    data: payload,
    success: () => {
      input.value = ''
    },
    fail: () => {
      uni.showToast({ title: '发送失败，请重连后再试', icon: 'none' })
    },
    complete: () => {
      sending.value = false
    }
  })
}

function sendPing() {
  if (!connected.value || !socket) {
    uni.showToast({ title: '请先连接 WebSocket', icon: 'none' })
    return
  }
  socket.send({
    data: JSON.stringify({ type: 'ping' }),
    fail: () => {
      uni.showToast({ title: 'Ping 失败', icon: 'none' })
    }
  })
}

function toggleSound() {
  soundOn.value = !soundOn.value
  setNotifySoundEnabled(soundOn.value)
  uni.showToast({
    title: soundOn.value ? '提示音已开启' : '提示音已关闭',
    icon: 'none'
  })
}

onMounted(() => {
  soundOn.value = loadNotifySoundPreference()
  if (props.autoConnect) {
    connect()
  }
})

onBeforeUnmount(() => {
  disconnect()
  destroyNotifySound()
})
</script>

<template>
  <view class="ws-board" :class="{ compact }">
    <view class="toolbar">
      <view class="meta">
        <text class="tag" :class="{ on: connected }">{{ statusText }}</text>
        <text class="online">在线 {{ online }}</text>
        <text class="sound" @click="toggleSound">{{ soundOn ? '提示音开' : '提示音关' }}</text>
      </view>
      <view class="actions">
        <button class="btn primary" size="mini" :disabled="connected || connecting" @click="connect">
          连接
        </button>
        <button class="btn" size="mini" :disabled="!connected" @click="disconnect">断开</button>
        <button class="btn" size="mini" :disabled="!connected" @click="sendPing">Ping</button>
      </view>
    </view>

    <scroll-view
      scroll-y
      class="message-list"
      :scroll-into-view="scrollInto"
      scroll-with-animation
    >
      <view
        v-for="(item, index) in messages"
        :id="'msg-' + index"
        :key="index"
        class="message-item"
        :class="item.type"
      >
        <text class="time">{{ item.time }}</text>
        <text class="from">{{ item.from }}</text>
        <text class="content">{{ item.content }}</text>
      </view>
      <view v-if="messages.length === 0" class="empty">连接后即可收发消息</view>
    </scroll-view>

    <view class="composer">
      <input
        v-model="input"
        class="composer-input"
        maxlength="200"
        placeholder="输入消息后点发送"
        confirm-type="send"
        :disabled="!connected"
        @confirm="sendChat"
      />
      <button
        class="btn primary send"
        :disabled="!connected || sending"
        :loading="sending"
        @click="sendChat"
      >
        发送
      </button>
    </view>
  </view>
</template>

<style scoped lang="scss">
.ws-board {
  background: $zhenxinjian-white;
  border-radius: 16rpx;
  border: 1rpx solid $zhenxinjian-border;
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  min-height: 720rpx;
}

.ws-board.compact {
  min-height: 520rpx;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16rpx;
}

.meta {
  display: flex;
  align-items: center;
  gap: 16rpx;
  flex-wrap: wrap;
}

.tag {
  font-size: 22rpx;
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  background: $zhenxinjian-bg;
  color: $zhenxinjian-text-secondary;
}

.tag.on {
  background: $zhenxinjian-primary-light;
  color: $zhenxinjian-primary;
}

.online {
  font-size: 24rpx;
  color: $zhenxinjian-text;
}

.sound {
  font-size: 22rpx;
  color: $zhenxinjian-primary;
  padding: 4rpx 8rpx;
}

.actions {
  display: flex;
  gap: 12rpx;
}

.btn {
  margin: 0;
  background: $zhenxinjian-bg;
  color: $zhenxinjian-text;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 8rpx;
  font-size: 24rpx;
}

.btn.primary {
  background: $zhenxinjian-primary;
  color: #fff;
  border-color: $zhenxinjian-primary;
}

.btn[disabled] {
  opacity: 0.5;
}

.message-list {
  flex: 1;
  height: 420rpx;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  background: $zhenxinjian-bg;
  padding: 20rpx;
  box-sizing: border-box;
}

.compact .message-list {
  height: 300rpx;
}

.message-item {
  display: flex;
  gap: 12rpx;
  font-size: 24rpx;
  line-height: 1.6;
  margin-bottom: 12rpx;
}

.time {
  color: $zhenxinjian-text-secondary;
  flex-shrink: 0;
}

.from {
  color: $zhenxinjian-primary;
  font-weight: 600;
  flex-shrink: 0;
}

.system .from,
.system .content,
.pong .content {
  color: $zhenxinjian-text-secondary;
  font-weight: 400;
}

.empty {
  text-align: center;
  color: $zhenxinjian-text-secondary;
  font-size: 24rpx;
  padding: 80rpx 0;
}

.composer {
  display: flex;
  gap: 16rpx;
  align-items: center;
}

.composer-input {
  flex: 1;
  height: 72rpx;
  padding: 0 24rpx;
  background: $zhenxinjian-bg;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  font-size: 26rpx;
}

.send {
  padding: 0 28rpx;
  height: 72rpx;
  line-height: 72rpx;
}
</style>
