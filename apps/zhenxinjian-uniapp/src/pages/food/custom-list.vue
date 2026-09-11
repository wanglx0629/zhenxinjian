<script setup lang="ts">
/**
 * 我的自定义食物列表：编辑 / 删除（二次确认，软删）
 * 删除后从列表与搜索中消失；新增入口跳转录入页
 * 作者: wanglx
 */
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useFoodStore } from '@/store/food'
import type { FoodVO } from '@/api/food'
import { trackPage } from '@/utils/track'

const foodStore = useFoodStore()

const list = ref<FoodVO[]>([])
const loading = ref(false)
const loaded = ref(false)

/** 拉取本人自定义列表（每次进入刷新，保证删除/编辑后可见性正确） */
async function loadList() {
  loading.value = true
  try {
    list.value = await foodStore.fetchMyCustom()
    loaded.value = true
  } catch {
    // request.ts 已统一 toast
  } finally {
    loading.value = false
  }
}

function goAdd() {
  uni.navigateTo({ url: '/pages/food/custom-edit' })
}

function goEdit(food: FoodVO) {
  uni.navigateTo({ url: `/pages/food/custom-edit?id=${food.id}` })
}

/** 删除：二次确认后软删，成功刷新列表（既有饮食记录快照不受影响） */
function handleDelete(food: FoodVO) {
  uni.showModal({
    title: '删除食物',
    content: `确定删除「${food.name}」吗？删除后不可再使用该食物记录，已有饮食记录不受影响`,
    confirmColor: '#f56c6c',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await foodStore.removeCustom(food.id)
        list.value = list.value.filter((f) => f.id !== food.id)
        uni.showToast({ title: '已删除', icon: 'success' })
      } catch {
        // request.ts 已统一 toast
      }
    }
  })
}

onShow(() => {
  trackPage('pages/food/custom-list')
  loadList()
})
</script>

<template>
  <view class="page">
    <view class="panel">
      <view class="panel-head">
        <text class="panel-title">我的自定义食物</text>
        <view class="add-btn" @click="goAdd">+ 新增</view>
      </view>

      <view v-if="list.length" class="food-list">
        <view v-for="f in list" :key="f.id" class="food-row">
          <view class="food-main" @click="goEdit(f)">
            <text class="food-name">{{ f.name }}</text>
            <text class="food-sub">{{ f.categoryName }} · {{ f.kcal }} kcal/100g</text>
            <text class="food-sub">碳 {{ f.carb }} / 蛋 {{ f.protein }} / 脂 {{ f.fat }}</text>
          </view>
          <view class="food-actions">
            <view class="action" @click="goEdit(f)">编辑</view>
            <view class="action danger" @click="handleDelete(f)">删除</view>
          </view>
        </view>
      </view>

      <view v-else-if="loaded && !loading" class="empty">
        <text class="empty-title">还没有自定义食物</text>
        <text class="empty-desc">内置库没有的食物，可以自己创建哦</text>
        <button class="btn-primary" @click="goAdd">创建第一个食物</button>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx 32rpx 64rpx;
  box-sizing: border-box;
  background: $zhenxinjian-bg;
}

.panel {
  background: $zhenxinjian-white;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 16rpx;
  padding: 32rpx;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.panel-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.add-btn {
  padding: 10rpx 24rpx;
  border-radius: 24rpx;
  background: $zhenxinjian-primary;
  color: $zhenxinjian-white;
  font-size: 24rpx;
}

.food-list {
  display: flex;
  flex-direction: column;
}

.food-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid $zhenxinjian-bg;
}

.food-row:last-child {
  border-bottom: none;
}

.food-main {
  display: flex;
  flex-direction: column;
  flex: 1;
  margin-right: 16rpx;
}

.food-name {
  font-size: 28rpx;
  color: $zhenxinjian-text;
}

.food-sub {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  margin-top: 4rpx;
}

.food-actions {
  display: flex;
  gap: 16rpx;
}

.action {
  padding: 10rpx 20rpx;
  border-radius: 12rpx;
  border: 1rpx solid $zhenxinjian-border;
  color: $zhenxinjian-text;
  font-size: 24rpx;
}

.action.danger {
  border-color: #f56c6c;
  color: #f56c6c;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 0;
}

.empty-title {
  font-size: 28rpx;
  color: $zhenxinjian-text;
  margin-bottom: 12rpx;
}

.empty-desc {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
  margin-bottom: 32rpx;
}

.btn-primary {
  width: 80%;
  height: 88rpx;
  line-height: 88rpx;
  background: $zhenxinjian-primary;
  color: $zhenxinjian-white;
  border-radius: 12rpx;
  font-size: 30rpx;
}
</style>
