/**
 * 应用入口样式与生命周期
 * 作者: wanglx
 */
<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { getToken } from '@/utils/storage'
import { startTrackFlushTimer } from '@/utils/track'

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

/* 公共样式层（B-T29）：面板/面板标题/主按钮全局单一真源；
   页面漂移属性经 scoped 覆盖（scoped 选择器优先级高于全局），漂移覆盖处附注释 */
.panel {
  background: $zhenxinjian-white;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}

.panel-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 24rpx;
  display: block;
}

.btn-primary {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: $zhenxinjian-primary;
  color: $zhenxinjian-white;
  border-radius: 12rpx;
  font-size: 30rpx;
}
</style>
