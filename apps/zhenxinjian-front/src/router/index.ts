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
      meta: { public: true }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/view/register/index.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      component: () => import('@/layout/MainLayout.vue'),
      redirect: '/home',
      children: [
        {
          path: 'home',
          name: 'Home',
          component: () => import('@/view/home/index.vue')
        },
        {
          path: 'users',
          name: 'Users',
          component: () => import('@/view/users/index.vue'),
          meta: { admin: true }
        },
        {
          path: 'websocket',
          name: 'WebSocket',
          component: () => import('@/view/websocket/index.vue')
        }
      ]
    }
  ]
})

// 路由守卫：未登录跳转登录页；管理员页校验角色
router.beforeEach(async (to, _from, next) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) {
    next('/login')
    return
  }
  if (token && (to.path === '/login' || to.path === '/register')) {
    next('/')
    return
  }
  if (to.meta.admin) {
    const userStore = useUserStore()
    if (!userStore.userInfo) {
      try {
        await userStore.fetchUserInfo()
      } catch {
        next('/home')
        return
      }
    }
    if (userStore.userInfo?.role !== 'ADMIN') {
      next('/home')
      return
    }
  }
  next()
})

export default router
