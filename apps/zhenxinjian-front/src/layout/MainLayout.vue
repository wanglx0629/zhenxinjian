<script setup lang="ts">
/**
 * 主布局 - 上导航栏
 * 作者: wanglx
 */
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import TeLogo from '@/component/TeLogo.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')

onMounted(() => {
  userStore.fetchUserInfo()
})

async function handleLogout() {
  await userStore.logout()
}

function goHome() {
  router.push('/home')
}

function goUsers() {
  router.push('/users')
}

function goWebSocket() {
  router.push('/websocket')
}
</script>

<template>
  <div class="layout">
    <header class="navbar">
      <div class="navbar-left">
        <TeLogo />
        <span class="brand">zhenxinjian</span>
        <nav class="nav-links">
          <a
            class="nav-link"
            :class="{ active: route.path === '/home' }"
            href="javascript:void(0)"
            @click="goHome"
          >
            首页
          </a>
          <a
            v-if="isAdmin"
            class="nav-link"
            :class="{ active: route.path === '/users' }"
            href="javascript:void(0)"
            @click="goUsers"
          >
            用户管理
          </a>
          <a
            class="nav-link"
            :class="{ active: route.path === '/websocket' }"
            href="javascript:void(0)"
            @click="goWebSocket"
          >
            WebSocket
          </a>
        </nav>
      </div>
      <div class="navbar-right">
        <el-dropdown trigger="click">
          <span class="user-name">
            {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.navbar {
  height: 56px;
  background: var(--zhenxinjian-white);
  border-bottom: 1px solid var(--zhenxinjian-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand {
  font-size: 18px;
  font-weight: 600;
  color: var(--zhenxinjian-primary);
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 20px;
}

.nav-link {
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 14px;
  color: var(--zhenxinjian-text);
}

.nav-link:hover,
.nav-link.active {
  color: var(--zhenxinjian-primary);
  background: var(--zhenxinjian-primary-light);
}

.navbar-right {
  display: flex;
  align-items: center;
}

.user-name {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--zhenxinjian-text);
  font-size: 14px;
}

.main-content {
  flex: 1;
  padding: 28px 32px 40px;
  width: 100%;
}

@media (max-width: 768px) {
  .navbar {
    padding: 0 14px;
  }

  .nav-links {
    margin-left: 10px;
  }

  .main-content {
    padding: 16px 14px 28px;
  }
}
</style>
