/**
 * 应用入口样式与生命周期
 * 作者: wanglx
 */
<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { getToken } from '@/utils/storage'
import { startTrackFlushTimer } from '@/utils/track'
import { preloadMpPages } from '@/utils/preload'

// #ifdef MP-WEIXIN
/** 微信隐私授权检查（B-T34 发布就绪）：未授权时触发微信官方隐私授权弹窗；
 *  弹窗协议名取自小程序后台「用户隐私保护指引」配置，无需自定义弹窗 UI */
function initPrivacyAuth() {
  if (typeof uni.getPrivacySetting !== 'function') return
  uni.getPrivacySetting({
    success: (res) => {
      if (res.needAuthorization && typeof uni.requirePrivacyAuthorize === 'function') {
        uni.requirePrivacyAuthorize({})
      }
    }
  })
}

/** 版本升级检查（B-T34 发布就绪）：新版本下载就绪后提示重启生效，稍后则下次冷启动自动应用 */
function checkAppUpdate() {
  if (typeof uni.getUpdateManager !== 'function') return
  const updateManager = uni.getUpdateManager()
  updateManager.onUpdateReady(() => {
    uni.showModal({
      title: '更新提示',
      content: '新版本已就绪，是否重启应用？',
      confirmText: '立即重启',
      cancelText: '稍后',
      success: (res) => {
        if (res.confirm) {
          updateManager.applyUpdate()
        }
      }
    })
  })
}
// #endif

onLaunch(() => {
  // 启动埋点队列定时上报（补发上次未发成功的事件）
  startTrackFlushTimer()
  // #ifdef MP-WEIXIN
  initPrivacyAuth()
  checkAppUpdate()
  preloadMpPages()
  // #endif
  // pages.json 首页为 auth/guide（P01）；已登录（游客或正式）直接进首页
  if (getToken()) {
    uni.switchTab({ url: '/pages/home/index' })
  }
})
</script>

<style lang="scss">
page {
  background-color: $zhenxinjian-bg;
  color: $zhenxinjian-text;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    sans-serif;
}

/* ========== 卡片面板（全局单一真源） ========== */
.panel {
  background: $zhenxinjian-white;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: $zhenxinjian-radius-lg;
  padding: $zhenxinjian-sp-4;
  margin-bottom: $zhenxinjian-sp-3;
  box-shadow: $zhenxinjian-shadow-card;
}

/* 紧凑卡（首页/我的/食物库） */
.card {
  background: $zhenxinjian-white;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: $zhenxinjian-radius-lg;
  padding: 28rpx;
  margin-bottom: $zhenxinjian-sp-3;
  box-shadow: $zhenxinjian-shadow-card;
}

.panel-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: $zhenxinjian-sp-3;
  display: block;
}

/* ========== 按钮体系（高 88rpx） ========== */
.btn-primary {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: $zhenxinjian-gradient-cta;
  color: $zhenxinjian-white;
  border-radius: $zhenxinjian-radius-md;
  font-size: 30rpx;
  font-weight: 600;
  box-shadow: 0 4rpx 12rpx rgba(255, 176, 32, 0.24);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.btn-primary:active {
  transform: scale(0.97);
  background: $zhenxinjian-cta-active;
  box-shadow: 0 2rpx 6rpx rgba(255, 176, 32, 0.18);
}

.btn-primary.disabled,
.btn-primary[disabled] {
  opacity: 0.55;
}

/* 次级按钮（叶绿描边） */
.btn-secondary {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: $zhenxinjian-white;
  color: $zhenxinjian-primary;
  border: 1rpx solid $zhenxinjian-primary;
  border-radius: $zhenxinjian-radius-md;
  font-size: 30rpx;
  font-weight: 600;
  transition: background 0.15s ease;
}

.btn-secondary:active {
  background: $zhenxinjian-primary-bg;
}

/* 危险按钮（白底红描边） */
.btn-danger {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: $zhenxinjian-white;
  color: $zhenxinjian-danger;
  border: 1rpx solid $zhenxinjian-danger;
  border-radius: $zhenxinjian-radius-md;
  font-size: 30rpx;
  font-weight: 600;
}

.btn-danger:active {
  background: $zhenxinjian-danger-bg;
}

/* ========== 通用工具类 ========== */

/* 可点击区域：统一按压缩放反馈（配合 hover-class 使用） */
.tap {
  transition: transform 0.15s ease, background 0.15s ease;
}

.tap-active {
  transform: scale(0.97);
}

/* 文字操作按钮：保证 ≥56rpx 触控目标（编辑/删除等） */
.action-text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 64rpx;
  min-height: 56rpx;
  padding: 0 12rpx;
  font-size: 24rpx;
  border-radius: $zhenxinjian-radius-sm;
}

/* 数字/展示字体 + 等宽数字 */
.num {
  font-family: $zhenxinjian-font-num;
  font-variant-numeric: tabular-nums;
}

/* 底部分割线 */
.hairline {
  height: 1rpx;
  background: $zhenxinjian-divider;
}

/* 页面底部安全区间距 */
.safe-bottom {
  padding-bottom: calc(48rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(48rpx + env(safe-area-inset-bottom));
}

/* 固定 FAB 底部安全区 */
.fab-safe {
  bottom: calc(180rpx + constant(safe-area-inset-bottom));
  bottom: calc(180rpx + env(safe-area-inset-bottom));
}

/* ========== 动效 ========== */

/* 弹窗进出场 */
.modal-mask-enter-active,
.modal-mask-leave-active {
  transition: opacity 0.2s ease;
}

.modal-mask-enter-from,
.modal-mask-leave-to {
  opacity: 0;
}

.modal-pop-enter-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.modal-pop-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.modal-pop-enter-from {
  opacity: 0;
  transform: scale(0.94) translateY(12rpx);
}

.modal-pop-leave-to {
  opacity: 0;
  transform: scale(0.97);
}

/* 骨架屏微光（仅在加载时短暂出现） */
@keyframes skeleton-shimmer {
  0% {
    opacity: 0.65;
  }
  50% {
    opacity: 1;
  }
  100% {
    opacity: 0.65;
  }
}

.skeleton-block {
  background: $zhenxinjian-track;
  animation: skeleton-shimmer 1.4s ease-in-out infinite;
}
</style>
