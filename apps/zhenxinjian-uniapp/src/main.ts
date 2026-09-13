/**
 * UniApp 入口
 * 作者: wanglx
 */
import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { setAuthHooks } from '@/api/request'
import { useUserStore } from '@/store/user'

export function createApp() {
  const app = createSSRApp(App)
  app.use(createPinia())
  // 组合根装配：api 层会话事件钩子注入 user store（消除 request.ts→store 反向依赖；
  // hooks 闭包在请求期才调用，pinia 必已 active）
  setAuthHooks({
    onSessionClear: () => useUserStore().clearSession(),
    onTokenRefreshed: (t) => useUserStore().syncToken(t)
  })
  return {
    app
  }
}
