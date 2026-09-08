/**
 * Vite 配置
 * 作者: wanglx
 */
import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()]
})
