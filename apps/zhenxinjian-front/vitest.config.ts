import { defineConfig } from 'vitest/config'
import { resolve } from 'path'

/**
 * vitest 配置（DESIGN-T04 引入纯函数单测安全网）
 * 作者: wanglx
 */
export default defineConfig({
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts']
  }
})