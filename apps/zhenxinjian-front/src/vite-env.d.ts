/// <reference types="vite/client" />

/**
 * Vue 类型声明
 * 作者: luote (luote) - https://luote996.cn
 */
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string
  readonly VITE_API_BASE_URL: string
  readonly VITE_WS_PATH: string
  readonly VITE_WS_PROTOCOL: string
  readonly VITE_APP_PORT: string
  readonly VITE_API_PROXY_TARGET: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
