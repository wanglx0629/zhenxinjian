/**
 * 路由配置
 * 作者: wanglx
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/view/login/index.vue'),
      meta: { public: true, title: '登录' }
    },
    {
      path: '/',
      component: () => import('@/layout/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/view/dashboard/index.vue'),
          meta: { admin: true, title: '数据看板' }
        },
        {
          path: 'users',
          name: 'Users',
          component: () => import('@/view/users/index.vue'),
          meta: { admin: true, title: '用户管理' }
        },
        {
          path: 'foods',
          name: 'Foods',
          component: () => import('@/view/foods/index.vue'),
          meta: { admin: true, title: '食物库' }
        },
        {
          path: 'diet-records',
          name: 'DietRecords',
          component: () => import('@/view/diet-records/index.vue'),
          meta: { admin: true, title: '饮食记录' }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/view/error/404.vue'),
      meta: { public: true, title: '页面不存在' }
    }
  ]
})

// 路由守卫：未登录跳转登录页；管理员页校验角色（登录态一律经 store 读取，不直读 localStorage）
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  if (!to.meta.public && !userStore.token) {
    next('/login')
    return
  }
  if (userStore.token && to.path === '/login') {
    next('/')
    return
  }
  if (to.meta.admin) {
    if (!userStore.userInfo) {
      try {
        await userStore.fetchUserInfo()
      } catch {
        // 拉取用户信息失败视为会话失效，清会话回登录页
        userStore.clearSession()
        next('/login')
        return
      }
    }
    if (userStore.userInfo?.role !== 'ADMIN') {
      // 非管理员无可见页面，清除会话回登录页
      userStore.clearSession()
      next('/login')
      return
    }
  }
  next()
})

// 标题同步
router.afterEach((to) => {
  if (to.meta?.title) {
    document.title = `${to.meta.title} · 臻心减管理后台`
  }
})

export default router
