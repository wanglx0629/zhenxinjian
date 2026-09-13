/**
 * 应用入口
 * 作者: wanglx
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { setAuthHooks } from '@/api/request'
import { useUserStore } from '@/store/user'
import './styles/global.css'

const app = createApp(App)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.use(createPinia())
// 组合根：pinia active 后装配认证 hooks（请求期调用天然安全），api 层不反向依赖 store/router
setAuthHooks({
  getToken: () => useUserStore().token,
  onTokenRefreshed: (newToken) => useUserStore().syncToken(newToken),
  onSessionClear: () => {
    useUserStore().clearSession()
    router.push('/login')
  }
})
app.use(router)
app.use(ElementPlus)
app.mount('#app')
