/**
 * 消息提示音
 * 作者: luote (luote) - https://luote996.cn
 */

let audio: UniNamespace.InnerAudioContext | null = null
let soundEnabled = true

/**
 * 是否开启提示音
 */
export function isNotifySoundEnabled() {
  return soundEnabled
}

/**
 * 设置提示音开关（持久化）
 */
export function setNotifySoundEnabled(enabled: boolean) {
  soundEnabled = enabled
  try {
    uni.setStorageSync('ws_notify_sound', enabled ? '1' : '0')
  } catch {
    // ignore
  }
}

/**
 * 从本地恢复提示音开关（默认开启）
 */
export function loadNotifySoundPreference() {
  try {
    const raw = uni.getStorageSync('ws_notify_sound')
    if (raw === '0' || raw === false) {
      soundEnabled = false
    } else {
      soundEnabled = true
    }
  } catch {
    soundEnabled = true
  }
  return soundEnabled
}

/**
 * 播放收到消息提示音（聊天消息）
 */
export function playNotifySound() {
  if (!soundEnabled) {
    return
  }
  try {
    if (!audio) {
      audio = uni.createInnerAudioContext()
      // 静态资源路径：H5 / 小程序均可用
      audio.src = '/static/notify.wav'
      audio.obeyMuteSwitch = false
    }
    audio.stop()
    audio.seek(0)
    audio.play()
  } catch {
    // 个别端不支持音频时静默失败，不影响收发
  }
  // 微信小程序可短振，增强感知
  try {
    uni.vibrateShort({ type: 'light' })
  } catch {
    // ignore
  }
}

/**
 * 销毁音频实例（页面卸载时调用）
 */
export function destroyNotifySound() {
  if (audio) {
    try {
      audio.destroy()
    } catch {
      // ignore
    }
    audio = null
  }
}
