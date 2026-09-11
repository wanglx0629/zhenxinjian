<script setup lang="ts">
/**
 * P04 代谢结果页：BMR/TDEE/基准热量/目标三宏 + 低热量风险提示 + 免责声明（不可移除）
 * 数据以后端快照为准；未录入空态引导回录入页
 * 作者: wanglx
 */
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useBodyStore } from '@/store/body'
import { trackPage } from '@/utils/track'

const bodyStore = useBodyStore()
const loading = ref(true)

const p = computed(() => bodyStore.profile)
const isEmpty = computed(() => bodyStore.isEmpty)

/** UI 整数克展示（后端存 1 位小数快照） */
const carbs = computed(() => Math.round(p.value?.targetCarb ?? 0))
const protein = computed(() => Math.round(p.value?.targetProtein ?? 0))
const fat = computed(() => Math.round(p.value?.targetFat ?? 0))

onShow(async () => {
  trackPage('pages/body/result')
  loading.value = true
  try {
    await bodyStore.fetchProfile()
  } catch {
    // request.ts 已统一 toast
  } finally {
    loading.value = false
  }
})

/** 空态引导：去录入页 */
function goProfile() {
  uni.redirectTo({ url: '/pages/body/profile' })
}

/** 修改数据：回录入页（回显当前档案） */
function editProfile() {
  uni.redirectTo({ url: '/pages/body/profile' })
}

/** 去选择模式（P05） */
function goChooseMode() {
  uni.navigateTo({ url: '/pages/mode/select' })
}
</script>

<template>
  <view class="page">
    <!-- 空态：未录入引导 -->
    <view v-if="!loading && isEmpty" class="panel empty">
      <text class="empty-title">还没有身体数据</text>
      <text class="empty-desc">录入身高、体重等基础数据后，才能为你计算每日热量与营养素目标。</text>
      <button class="btn-primary" @click="goProfile">去录入</button>
    </view>

    <template v-else-if="!loading && p?.recorded">
      <!-- 低热量风险提示（按风险标记，仅提示不阻断） -->
      <view v-if="p.lowKcalRisk" class="risk">
        <text class="risk-text">当前每日目标热量偏低，长期坚持可能影响基础代谢，建议适度上调或咨询专业人士。</text>
      </view>

      <view class="panel">
        <text class="panel-title">代谢与热量</text>
        <view class="grid">
          <view class="cell"><text class="num">{{ p.bmr }}</text><text class="label">BMR 基础代谢 kcal</text></view>
          <view class="cell"><text class="num">{{ p.tdee }}</text><text class="label">TDEE 每日消耗 kcal</text></view>
          <view class="cell"><text class="num highlight">{{ p.targetKcal }}</text><text class="label">每日基准热量 kcal</text></view>
        </view>
        <text class="meta">缺口 {{ p.deficit }} kcal · 活动系数 {{ p.activityFactor }}</text>
      </view>

      <view class="panel">
        <text class="panel-title">每日营养目标（532 配比）</text>
        <view class="grid">
          <view class="cell"><text class="num">{{ carbs }}</text><text class="label">碳水 g</text></view>
          <view class="cell"><text class="num">{{ protein }}</text><text class="label">蛋白质 g</text></view>
          <view class="cell"><text class="num">{{ fat }}</text><text class="label">脂肪 g</text></view>
        </view>
      </view>

      <!-- 免责声明：spec 要求内置展示，不可移除 -->
      <view class="disclaimer">
        <text class="disclaimer-text">{{ p.disclaimer }}</text>
      </view>

      <button class="btn-primary" @click="goChooseMode">去选择模式</button>
      <button class="btn-ghost" @click="editProfile">修改身体数据</button>
    </template>
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
  margin-bottom: 24rpx;
}

.panel-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 24rpx;
  display: block;
}

.grid {
  display: flex;
}

.cell {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.num {
  font-size: 40rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
}

.num.highlight {
  color: $zhenxinjian-primary;
}

.label {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  margin-top: 8rpx;
  text-align: center;
}

.meta {
  display: block;
  text-align: center;
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  margin-top: 8rpx;
}

.risk {
  background: #fdf6ec;
  border: 1rpx solid #f5dab1;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 24rpx;
}

.risk-text {
  font-size: 24rpx;
  color: #b88230;
  line-height: 1.5;
}

.disclaimer {
  padding: 0 8rpx 24rpx;
}

.disclaimer-text {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.6;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 64rpx 48rpx;
}

.empty-title {
  font-size: 32rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 16rpx;
}

.empty-desc {
  font-size: 26rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.6;
  margin-bottom: 40rpx;
  text-align: center;
}

.btn-primary {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: $zhenxinjian-primary;
  color: $zhenxinjian-white;
  border-radius: 12rpx;
  font-size: 30rpx;
}

.btn-ghost {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  margin-top: 24rpx;
  background: $zhenxinjian-white;
  color: $zhenxinjian-text;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 12rpx;
  font-size: 30rpx;
}
</style>
