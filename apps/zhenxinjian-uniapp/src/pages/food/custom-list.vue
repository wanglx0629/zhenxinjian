<script setup lang="ts">
/**
 * 我的自定义食物列表：审核状态徽标（待审/已通过只读/已驳回+原因）
 * 已通过(1)转公共只读（不可编辑/删除，仅管理员维护）；待审(0)/已驳回(2)可编辑（驳回件改后重提回 0）
 * 作者: wanglx
 */
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useFoodStore } from '@/store/food'
import type { FoodVO } from '@/api/food'
import FoodThumb from '@/components/FoodThumb.vue'
import { trackPage } from '@/utils/track'
import { iconSrc } from '@/utils/icons'

const plusIcon = iconSrc('plus', '#ffffff')

const foodStore = useFoodStore()

const list = ref<FoodVO[]>([])
const loading = ref(false)
const loaded = ref(false)

/** 拉取本人自定义列表（每次进入刷新，保证删除/编辑/审核态变化后正确） */
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

/** 已通过(1)转公共：原作者只读 */
function isReadOnly(food: FoodVO) {
  return food.auditStatus === 1
}

/** 审核状态徽标（文案 + 配色类） */
function badgeOf(food: FoodVO) {
  if (food.auditStatus === 1) return { text: '已通过 · 公共', cls: 'badge-ok' }
  if (food.auditStatus === 2) return { text: '已驳回', cls: 'badge-reject' }
  return { text: '待审核', cls: 'badge-pending' }
}

function goAdd() {
  uni.navigateTo({ url: '/pages/food/custom-edit' })
}

function goEdit(food: FoodVO) {
  if (isReadOnly(food)) {
    uni.showToast({ title: '已通过的食物为公共内容，仅管理员可维护', icon: 'none' })
    return
  }
  uni.navigateTo({ url: `/pages/food/custom-edit?id=${food.id}` })
}

/** 删除：二次确认后软删，成功刷新列表（既有饮食记录快照不受影响） */
function handleDelete(food: FoodVO) {
  uni.showModal({
    title: '删除食物',
    content: `确定删除「${food.name}」吗？删除后不可再使用该食物记录，已有饮食记录不受影响`,
    confirmColor: '#FF4747',
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
        <view class="add-btn" hover-class="add-btn-hover" @click="goAdd">
          <image class="add-btn-icon" :src="plusIcon" />
          <text>新增</text>
        </view>
      </view>

      <view v-if="list.length" class="food-list">
        <view
          v-for="f in list"
          :key="f.id"
          class="food-row"
          :class="{ readonly: isReadOnly(f) }"
        >
          <FoodThumb :image="f.image" :category-code="f.categoryCode" />
          <view class="food-main" @click="goEdit(f)">
            <view class="name-row">
              <text class="food-name">{{ f.name }}</text>
              <text class="badge" :class="badgeOf(f).cls">{{ badgeOf(f).text }}</text>
            </view>
            <text class="food-sub">{{ f.categoryName }} · {{ f.kcal }} kcal/100g</text>
            <text class="food-sub">碳 {{ f.carb }} / 蛋 {{ f.protein }} / 脂 {{ f.fat }}</text>
            <text v-if="f.auditStatus === 2 && f.auditRemark" class="reject-reason">
              驳回原因：{{ f.auditRemark }}
            </text>
          </view>
          <view v-if="!isReadOnly(f)" class="food-actions">
            <view class="action" hover-class="action-hover" @click="goEdit(f)">编辑</view>
            <view class="action danger" hover-class="action-danger-hover" @click="handleDelete(f)">删除</view>
          </view>
          <text v-else class="lock-hint">只读</text>
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
  border-radius: $zhenxinjian-radius-lg;
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
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 10rpx 24rpx;
  border-radius: $zhenxinjian-radius-pill;
  background: $zhenxinjian-primary;
  color: $zhenxinjian-white;
  font-size: 24rpx;
}

.add-btn-hover {
  background: $zhenxinjian-primary-dark;
}

.add-btn-icon {
  width: 22rpx;
  height: 22rpx;
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
  border-bottom: 1rpx solid $zhenxinjian-divider;
}

.food-row:last-child {
  border-bottom: none;
}

/* 已通过转公共：只读弱化样式 */
.food-row.readonly .food-name {
  color: $zhenxinjian-text-secondary;
}

.food-main {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  margin-left: 20rpx;
  margin-right: 16rpx;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.food-name {
  font-size: 28rpx;
  color: $zhenxinjian-text;
}

.badge {
  flex-shrink: 0;
  font-size: 20rpx;
  padding: 2rpx 12rpx;
  border-radius: $zhenxinjian-radius-pill;
}

.badge-pending {
  color: #9a6b00;
  background: #fff4d6;
}

.badge-ok {
  color: #008a63;
  background: #e6f5ef;
}

.badge-reject {
  color: #d63333;
  background: #fdecec;
}

.food-sub {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  margin-top: 4rpx;
}

.reject-reason {
  font-size: 22rpx;
  color: $zhenxinjian-danger;
  margin-top: 8rpx;
}

.food-actions {
  display: flex;
  gap: 16rpx;
}

.action {
  padding: 10rpx 20rpx;
  border-radius: $zhenxinjian-radius-md;
  border: 1rpx solid $zhenxinjian-border;
  color: $zhenxinjian-text;
  font-size: 24rpx;
}

.action-hover {
  background: $zhenxinjian-primary-bg;
  border-color: $zhenxinjian-primary;
  color: $zhenxinjian-primary;
}

.action.danger {
  border-color: $zhenxinjian-danger-border;
  color: $zhenxinjian-danger;
}

.action-danger-hover {
  background: $zhenxinjian-danger-bg;
  border-color: $zhenxinjian-danger;
}

.lock-hint {
  flex-shrink: 0;
  font-size: 22rpx;
  color: $zhenxinjian-text-placeholder;
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

</style>
