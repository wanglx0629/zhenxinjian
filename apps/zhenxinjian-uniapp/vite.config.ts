/**
 * Vite 配置
 * 作者: wanglx
 */
import { defineConfig, loadEnv, type Plugin } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

/**
 * 微信小程序 appid 环境变量登记插件（B-T34 发布就绪）
 * 真实 appid 不入库：构建/开发时经 VITE_MP_WEIXIN_APPID 注入内存中的 manifest
 * （拦截 uni 内部 manifest-json-js 虚拟模块 transform，不落盘、不污染 git 工作区）；
 * 未设置时保持 manifest.json 空串占位（微信开发者工具可用测试号联调）。
 */
function manifestAppidEnvPlugin(appid: string): Plugin {
  return {
    name: 'zhenxinjian:manifest-appid-env',
    enforce: 'pre',
    transform(code, id) {
      if (!appid || !id.endsWith('manifest-json-js')) return
      try {
        const manifest = JSON.parse(code)
        manifest['mp-weixin'] = manifest['mp-weixin'] || {}
        manifest['mp-weixin'].appid = appid
        return { code: JSON.stringify(manifest), map: null }
      } catch {
        return
      }
    }
  }
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  return {
    plugins: [manifestAppidEnvPlugin(env.VITE_MP_WEIXIN_APPID || ''), uni()]
  }
})
