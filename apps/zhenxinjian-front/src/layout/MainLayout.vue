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
  // 导航属页面职责（store 不感知 router）
  router.push('/login')
}

function goDashboard() {
  router.push('/dashboard')
}

function goUsers() {
  router.push('/users')
}

function goFoods() {
  router.push('/foods')
}

function goDietRecords() {
  router.push('/diet-records')
}

function goConfigs() {
  router.push('/configs')
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
            v-if="isAdmin"
            class="nav-link"
            :class="{ active: route.path === '/dashboard' }"
            href="javascript:void(0)"
            @click="goDashboard"
          >
            数据看板
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
            v-if="isAdmin"
            class="nav-link"
            :class="{ active: route.path === '/foods' }"
            href="javascript:void(0)"
            @click="goFoods"
          >
            食物库
          </a>
          <a
            v-if="isAdmin"
            class="nav-link"
            :class="{ active: route.path === '/diet-records' }"
            href="javascript:void(0)"
            @click="goDietRecords"
          >
            饮食记录
          </a>
          <a
            v-if="isAdmin"
            class="nav-link"
            :class="{ active: route.path === '/configs' }"
            href="javascript:void(0)"
            @click="goConfigs"
          >
            系统配置
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
      <div v-if="route.meta?.title" class="breadcrumb">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
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
  padding: 20px 32px 40px;
  width: 100%;
}

.breadcrumb {
  margin-bottom: 16px;
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
