<script setup lang="ts">
/**
 * P10 食物库页：搜索（300ms 防抖）+ 分类筛选 + 历史搜索 + 热门食物 + 结果分页
 * 后端不可用时由 store 降级本地 foods.ts 检索；「写入饮食记录」入口留待 Change 5
 * 作者: wanglx
 */
import { computed, ref } from 'vue'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import { useFoodStore } from '@/store/food'
import type { FoodCategoryVO, FoodVO } from '@/api/food'

const foodStore = useFoodStore()

/** 历史搜索本地存储键（上限 10 条） */
const HISTORY_KEY = 'zxj_food_history'
const HISTORY_MAX = 10
/** 分页规格 */
const PAGE_SIZE = 20

const keyword = ref('')
const categoryCode = ref('')
const categories = ref<FoodCategoryVO[]>([])
const hotList = ref<FoodVO[]>([])
const historyList = ref<string[]>([])
const records = ref<FoodVO[]>([])
const total = ref(0)
const pages = ref(0)
const page = ref(1)
const searched = ref(false)

/** 空关键字且未选分类 → 展示历史+热门，不发起搜索 */
const showDiscovery = computed(() => !keyword.value.trim() && !categoryCode.value)

function loadHistory() {
  const raw = uni.getStorageSync(HISTORY_KEY)
  historyList.value = Array.isArray(raw) ? raw.slice(0, HISTORY_MAX) : []
}

function pushHistory(kw: string) {
  const key = kw.trim()
  if (!key) return
  const next = [key, ...historyList.value.filter((h) => h !== key)]
  historyList.value = next.slice(0, HISTORY_MAX)
  uni.setStorageSync(HISTORY_KEY, historyList.value)
}

function clearHistory() {
  historyList.value = []
  uni.removeStorageSync(HISTORY_KEY)
}

/** 执行搜索：重置到第 1 页并请求 */
async function doSearch(targetPage = 1) {
  const kw = keyword.value.trim()
  if (!kw && !categoryCode.value) {
    records.value = []
    total.value = 0
    pages.value = 0
    page.value = 1
    searched.value = false
    return
  }
  searched.value = true
  const res = await foodStore.search({
    keyword: kw,
    categoryCode: categoryCode.value || undefined,
    page: targetPage,
    size: PAGE_SIZE
  })
  page.value = targetPage
  total.value = res.total
  pages.value = res.pages
  if (targetPage === 1) {
    records.value = res.records
  } else {
    const exist = new Set(records.value.map((r) => r.id))
    records.value = records.value.concat(res.records.filter((r) => !exist.has(r.id)))
  }
  if (kw) pushHistory(kw)
}

/** 关键字输入：300ms 防抖后搜索 */
let debounceTimer: ReturnType<typeof setTimeout> | null = null
function handleInput() {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => doSearch(1), 300)
}

function selectCategory(code: string) {
  categoryCode.value = categoryCode.value === code ? '' : code
  doSearch(1)
}

function tapHistory(kw: string) {
  keyword.value = kw
  doSearch(1)
}

function tapHot(food: FoodVO) {
  keyword.value = food.name
  doSearch(1)
}

function openDetail(food: FoodVO) {
  uni.navigateTo({ url: `/pages/food/detail?id=${food.id}` })
}

function goMyCustom() {
  uni.navigateTo({ url: '/pages/food/custom-list' })
}

onLoad(async () => {
  loadHistory()
  categories.value = await foodStore.fetchCategories()
  hotList.value = await foodStore.fetchHot()
})

/** 触底翻页 */
onReachBottom(() => {
  if (searched.value && page.value < pages.value && !foodStore.loading) {
    doSearch(page.value + 1)
  }
})
</script>

<template>
  <view class="page">
    <view class="search-bar">
      <input
        v-model="keyword"
        class="search-input"
        placeholder="搜索食物名称或别名"
        confirm-type="search"
        @input="handleInput"
        @confirm="doSearch(1)"
      />
    </view>

    <scroll-view scroll-x class="category-scroll">
      <view class="category-row">
        <view
          class="cat-item"
          :class="{ active: categoryCode === '' }"
          @click="selectCategory('')"
        >全部</view>
        <view
          v-for="c in categories"
          :key="c.code"
          class="cat-item"
          :class="{ active: categoryCode === c.code }"
          @click="selectCategory(c.code)"
        >{{ c.name }}</view>
      </view>
    </scroll-view>

    <view v-if="foodStore.degraded" class="degrade-tip">网络不可用，已切换为本地食物库</view>

    <template v-if="showDiscovery">
      <view v-if="historyList.length" class="panel">
        <view class="panel-head">
          <text class="panel-title">历史搜索</text>
          <text class="panel-clear" @click="clearHistory">清空</text>
        </view>
        <view class="chips">
          <view v-for="h in historyList" :key="h" class="chip" @click="tapHistory(h)">{{ h }}</view>
        </view>
      </view>

      <view class="panel custom-entry" @click="goMyCustom">
        <text class="custom-entry-text">我的自定义食物</text>
        <text class="custom-entry-arrow">›</text>
      </view>

      <view class="panel">
        <text class="panel-title">热门食物</text>
        <view class="food-list">
          <view v-for="f in hotList" :key="f.id" class="food-row" @click="openDetail(f)">
            <view class="food-main">
              <text class="food-name">{{ f.name }}</text>
              <text v-if="f.alias" class="food-alias">{{ f.alias }}</text>
            </view>
            <view class="food-nut">
              <text class="nut-kcal">{{ f.kcal }} kcal</text>
              <text class="nut-sub">碳 {{ f.carb }} / 蛋 {{ f.protein }} / 脂 {{ f.fat }}</text>
            </view>
          </view>
        </view>
      </view>
    </template>

    <template v-else>
      <view v-if="records.length" class="panel">
        <text class="panel-title">共 {{ total }} 个结果</text>
        <view class="food-list">
          <view v-for="f in records" :key="f.id" class="food-row" @click="openDetail(f)">
            <view class="food-main">
              <view class="food-name-wrap">
                <text class="food-name">{{ f.name }}</text>
                <text v-if="f.source === 2" class="food-tag">自定义</text>
              </view>
              <text class="food-alias">{{ f.categoryName }}</text>
            </view>
            <view class="food-nut">
              <text class="nut-kcal">{{ f.kcal }} kcal</text>
              <text class="nut-sub">碳 {{ f.carb }} / 蛋 {{ f.protein }} / 脂 {{ f.fat }}</text>
            </view>
          </view>
        </view>
        <view v-if="page < pages" class="load-more">上拉加载更多</view>
        <view v-else class="load-more">已加载全部 {{ total }} 条</view>
      </view>
      <view v-else class="empty">
        <text class="empty-title">未找到相关食物</text>
        <text class="empty-desc">换个关键字或分类试试</text>
      </view>
    </template>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx 24rpx 64rpx;
  box-sizing: border-box;
  background: $zhenxinjian-bg;
}

.search-bar {
  margin-bottom: 20rpx;
}

.search-input {
  height: 80rpx;
  background: $zhenxinjian-white;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 40rpx;
  padding: 0 32rpx;
  font-size: 28rpx;
  color: $zhenxinjian-text;
  box-sizing: border-box;
}

.category-scroll {
  white-space: nowrap;
  margin-bottom: 20rpx;
}

.category-row {
  display: inline-flex;
  gap: 16rpx;
}

.cat-item {
  display: inline-block;
  padding: 12rpx 24rpx;
  border-radius: 32rpx;
  background: $zhenxinjian-white;
  border: 1rpx solid $zhenxinjian-border;
  font-size: 24rpx;
  color: $zhenxinjian-text;
}

.cat-item.active {
  background: $zhenxinjian-primary;
  border-color: $zhenxinjian-primary;
  color: $zhenxinjian-white;
}

.degrade-tip {
  padding: 16rpx 24rpx;
  background: $zhenxinjian-primary-light;
  color: $zhenxinjian-primary;
  border-radius: 12rpx;
  font-size: 24rpx;
  margin-bottom: 20rpx;
}

.panel {
  background: $zhenxinjian-white;
  border: 1rpx solid $zhenxinjian-border;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}

.custom-entry {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.custom-entry-text {
  font-size: 28rpx;
  color: $zhenxinjian-text;
}

.custom-entry-arrow {
  font-size: 32rpx;
  color: $zhenxinjian-text-secondary;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 20rpx;
  display: block;
}

.panel-clear {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.chip {
  padding: 10rpx 24rpx;
  border-radius: 24rpx;
  background: $zhenxinjian-bg;
  border: 1rpx solid $zhenxinjian-border;
  color: $zhenxinjian-text;
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

.food-name-wrap {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.food-name {
  font-size: 28rpx;
  color: $zhenxinjian-text;
}

.food-tag {
  font-size: 20rpx;
  color: $zhenxinjian-primary;
  background: $zhenxinjian-primary-light;
  border-radius: 8rpx;
  padding: 2rpx 10rpx;
}

.food-alias {
  font-size: 22rpx;
  color: $zhenxinjian-text-secondary;
  margin-top: 4rpx;
}

.food-nut {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.nut-kcal {
  font-size: 28rpx;
  font-weight: 600;
  color: $zhenxinjian-primary;
}

.nut-sub {
  font-size: 20rpx;
  color: $zhenxinjian-text-secondary;
  margin-top: 4rpx;
}

.load-more {
  text-align: center;
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
  padding-top: 20rpx;
}

.empty {
  padding: 80rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.empty-title {
  font-size: 28rpx;
  color: $zhenxinjian-text;
  margin-bottom: 12rpx;
}

.empty-desc {
  font-size: 24rpx;
  color: $zhenxinjian-text-secondary;
}
</style>
