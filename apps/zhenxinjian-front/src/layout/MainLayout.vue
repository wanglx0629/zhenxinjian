<script setup lang="ts">
/**
 * 主布局 - 顶部导航（浅色科技风：能量指示线 / 响应式抽屉菜单）
 * 作者: wanglx
 */
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  DataAnalysis,
  User,
  Apple,
  Document,
  Setting,
  Menu
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import TeLogo from '@/component/TeLogo.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')

/** 导航项（图标统一 16px 线性） */
const navItems = [
  { path: '/dashboard', title: '数据看板', icon: DataAnalysis },
  { path: '/users', title: '用户管理', icon: User },
  { path: '/foods', title: '食物库', icon: Apple },
  { path: '/diet-records', title: '饮食记录', icon: Document },
  { path: '/configs', title: '系统配置', icon: Setting }
]

/** 移动端抽屉菜单 */
const drawerOpen = ref(false)

watch(
  () => route.fullPath,
  () => {
    drawerOpen.value = false
  }
)

onMounted(() => {
  if (!userStore.userInfo?.id) {
    userStore.fetchUserInfo()
  }
})

async function handleLogout() {
  await userStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="layout">
    <header class="navbar">
      <div class="navbar-left">
        <el-icon class="menu-btn" @click="drawerOpen = true"><Menu /></el-icon>
        <router-link to="/dashboard" class="brand-box">
          <TeLogo />
          <span class="brand">臻心减</span>
          <span class="brand-sub">运营控制台</span>
        </router-link>
        <nav class="nav-links">
          <router-link
            v-for="item in navItems"
            v-show="isAdmin"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ active: route.path.startsWith(item.path) }"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
            <i class="nav-indicator" />
          </router-link>
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

    <!-- 移动端抽屉 -->
    <el-drawer v-model="drawerOpen" title="导航" direction="ltr" size="240px">
      <div class="drawer-nav">
        <router-link
          v-for="item in navItems"
          v-show="isAdmin"
          :key="item.path"
          :to="item.path"
          class="drawer-link"
          :class="{ active: route.path.startsWith(item.path) }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </router-link>
      </div>
    </el-drawer>

    <main class="main-content">
      <div v-if="route.meta?.title" class="breadcrumb">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
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
  height: 60px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--zhenxinjian-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.menu-btn {
  display: none;
  font-size: 22px;
  cursor: pointer;
  color: var(--zhenxinjian-text);
}

.brand-box {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand {
  font-size: 17px;
  font-weight: 700;
  color: var(--zhenxinjian-text);
}

.brand-sub {
  font-size: 12px;
  color: var(--zhenxinjian-text-secondary);
  padding: 2px 8px;
  border: 1px solid var(--zhenxinjian-border);
  border-radius: 999px;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: 28px;
}

.nav-link {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: var(--zhenxinjian-radius-md);
  font-size: 14px;
  color: var(--zhenxinjian-text-secondary);
  transition: color 0.2s ease, background-color 0.2s ease;
}

.nav-link .el-icon {
  font-size: 15px;
}

.nav-link:hover {
  color: var(--zhenxinjian-primary);
  background: var(--zhenxinjian-primary-bg);
}

.nav-link.active {
  color: var(--zhenxinjian-primary);
  background: var(--zhenxinjian-primary-bg);
  font-weight: 600;
}

/* active 底部能量指示线 */
.nav-indicator {
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: -16px;
  height: 2px;
  border-radius: 2px;
  background: linear-gradient(90deg, #00AC7C, rgba(255, 176, 32, 0.7));
  opacity: 0;
  transition: opacity 0.2s ease;
}

.nav-link.active .nav-indicator {
  opacity: 1;
}

.navbar-right {
  display: flex;
  align-items: center;
}

.user-name {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--zhenxinjian-text);
  font-size: 14px;
  padding: 6px 10px;
  border-radius: var(--zhenxinjian-radius-md);
  transition: background-color 0.2s ease;
}

.user-name:hover {
  background: var(--zhenxinjian-primary-bg);
}

.main-content {
  flex: 1;
  padding: 20px 28px 40px;
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
}

.breadcrumb {
  margin-bottom: 16px;
}

.drawer-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 12px;
}

.drawer-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  border-radius: var(--zhenxinjian-radius-md);
  font-size: 15px;
  color: var(--zhenxinjian-text-secondary);
}

.drawer-link:hover {
  background: var(--zhenxinjian-primary-bg);
  color: var(--zhenxinjian-primary);
}

.drawer-link.active {
  background: var(--zhenxinjian-primary-bg);
  color: var(--zhenxinjian-primary);
  font-weight: 600;
}

@media (max-width: 900px) {
  .menu-btn {
    display: inline-flex;
  }

  .nav-links {
    display: none;
  }

  .brand-sub {
    display: none;
  }

  .main-content {
    padding: 16px 14px 28px;
  }
}
</style>
