/**
 * 应用入口样式与生命周期
 * 作者: luote (luote) - https://luote996.cn
 */
<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { getToken } from '@/utils/storage'

onLaunch(() => {
  // 已登录则进首页，否则留在登录页（pages.json 首页为 login）
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
