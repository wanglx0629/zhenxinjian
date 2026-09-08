/**
 * 应用入口样式与生命周期
 * 作者: wanglx
 */
<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { getToken } from '@/utils/storage'

onLaunch(() => {
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
</style>
