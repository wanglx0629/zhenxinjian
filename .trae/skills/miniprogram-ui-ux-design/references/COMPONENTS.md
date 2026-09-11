# 组件模式

所有组件使用原生 WXML + WXSS，不依赖任何第三方库。

---

## 分类一：基础类

### 1. Button（按钮）

```xml
<!-- 主按钮 -->
<button class="btn btn-primary" hover-class="btn-press" bindtap="onTap">确认</button>

<!-- 次按钮 -->
<button class="btn btn-secondary" hover-class="btn-press">取消</button>

<!-- 文字按钮 -->
<button class="btn btn-text" hover-class="btn-press">了解更多</button>

<!-- 禁用态 -->
<button class="btn btn-primary" disabled>已提交</button>
```

```css
.btn {
  padding: 24rpx 48rpx;
  border-radius: var(--radius-full);
  font-size: 32rpx;
  text-align: center;
  transition: all 0.15s ease;
}
.btn-primary {
  background: var(--color-primary);
  color: #fff;
}
.btn-secondary {
  background: var(--bg-card);
  color: var(--text-primary);
  border: 2rpx solid var(--color-primary);
}
.btn-text {
  background: transparent;
  color: var(--color-primary);
}
.btn-press {
  transform: scale(0.97);
  opacity: 0.85;
}
.btn[disabled] {
  background: #ccc;
  color: #999;
}
```

---

### 2. Icon（图标）

```xml
<!-- iconfont 图标 -->
<text class="iconfont icon-search" style="color: var(--text-secondary); font-size: 48rpx;"></text>

<!-- 内联 SVG -->
<image src="/assets/icons/egg.svg" style="width: 48rpx; height: 48rpx;"></image>
```

**尺寸体系：** sm(32rpx) / md(48rpx) / lg(64rpx) / xl(96rpx)

---

### 3. Text（文本）

```xml
<!-- 单行截断 -->
<text class="text-ellipsis">这是一段很长的文本...</text>

<!-- 多行省略（2行） -->
<text class="text-clamp-2">这是一个多行文本示例...</text>

<!-- 可复制 -->
<text selectable="{{true}}">长按可复制这段文字</text>
```

```css
.text-ellipsis { 
  overflow: hidden; 
  text-overflow: ellipsis; 
  white-space: nowrap; 
}
.text-clamp-2 { 
  display: -webkit-box; 
  -webkit-box-orient: vertical; 
  -webkit-line-clamp: 2; 
  overflow: hidden; 
}
```

---

### 4. Image（图片）

```xml
<!-- 头像模式 -->
<image src="{{avatar}}" mode="aspectFill" class="avatar" lazy-load="{{true}}"
  binderror="onImgError" />

<!-- 文章配图 -->
<image src="{{image}}" mode="widthFix" lazy-load="{{true}}" />
```

```css
.avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: var(--radius-full);
  background: var(--bg-card); /* 加载前占位 */
}
```

---

## 分类二：表单类

### 5. Input（输入框）

```xml
<view class="input-group">
  <text class="input-label">手机号</text>
  <input class="input" type="number" placeholder="请输入手机号"
    placeholder-class="input-placeholder" bindinput="onInput" />
  <text class="input-error" wx:if="{{error}}">请输入正确的手机号</text>
</view>
```

```css
.input-group { padding: var(--spacing-md) var(--spacing-xl); }
.input-label { font-size: 28rpx; color: var(--text-primary); margin-bottom: var(--spacing-xs); }
.input {
  padding: 20rpx var(--spacing-md);
  border: 2rpx solid #ddd;
  border-radius: var(--radius-md);
  font-size: 32rpx;
}
.input:focus { border-color: var(--color-primary); }
.input-placeholder { color: #bbb; }
.input-error { font-size: 24rpx; color: var(--color-error); margin-top: 8rpx; }
```

---

### 6. Textarea（多行输入）

```xml
<view class="textarea-wrapper">
  <textarea class="textarea" maxlength="{{200}}" placeholder="请输入内容"
    bindinput="onInput" />
  <text class="textarea-count">{{content.length}}/200</text>
</view>
```

---

### 7. Picker（选择器）

```xml
<picker mode="selector" range="{{options}}" bindchange="onPickerChange">
  <view class="picker-value">
    <text>{{selected || '请选择'}}</text>
    <text class="iconfont icon-arrow-down"></text>
  </view>
</picker>
```

---

### 8. Slider（滑块）

```xml
<slider min="0" max="100" value="{{value}}" bindchange="onSliderChange"
  activeColor="var(--color-primary)" block-size="24" />
```

---

### 9. Switch（开关）

```xml
<switch checked="{{checked}}" bindchange="onSwitchChange"
  color="var(--color-primary)" />
```

---

## 分类三：选择类

### 10. Radio Group（单选组）

```xml
<view class="radio-group">
  <view class="radio-item {{selected === item.value ? 'radio-active' : ''}}"
    wx:for="{{options}}" wx:key="value" bindtap="onSelect" data-value="{{item.value}}">
    <view class="radio-dot {{selected === item.value ? 'radio-dot-active' : ''}}"></view>
    <text>{{item.label}}</text>
  </view>
</view>
```

```css
.radio-group { display: flex; flex-wrap: wrap; gap: var(--spacing-sm); }
.radio-item {
  display: flex; align-items: center; gap: 12rpx;
  padding: 16rpx 32rpx;
  border: 2rpx solid #ddd;
  border-radius: var(--radius-md);
  transition: all 0.2s;
}
.radio-active { border-color: var(--color-primary); background: var(--color-primary-light); }
.radio-dot { width: 24rpx; height: 24rpx; border-radius: 50%; border: 2rpx solid #ccc; }
.radio-dot-active { background: var(--color-primary); border-color: var(--color-primary); }
```

---

### 11. Checkbox Group（复选框组）

```xml
<view class="checkbox-group">
  <view class="checkbox-item {{item.checked ? 'check-active' : ''}}"
    wx:for="{{options}}" wx:key="value" bindtap="onToggle" data-index="{{index}}">
    <view class="check-box {{item.checked ? 'check-box-active' : ''}}">
      <text wx:if="{{item.checked}}" class="iconfont icon-check"></text>
    </view>
    <text>{{item.label}}</text>
  </view>
</view>
```

---

## 分类四：导航类

### 12. Segmented Control（分段控制器）

```xml
<view class="segmented">
  <view class="seg-item {{mode === 'simple' ? 'seg-active' : ''}}" bindtap="switchMode" data-mode="simple">
    普通模式
  </view>
  <view class="seg-item {{mode === 'smart' ? 'seg-active' : ''}}" bindtap="switchMode" data-mode="smart">
    智能多熟度
  </view>
</view>
```

```css
.segmented {
  display: flex;
  background: var(--bg-card);
  border-radius: var(--radius-full);
  padding: 4rpx;
}
.seg-item {
  flex: 1; text-align: center;
  padding: 16rpx 0;
  border-radius: var(--radius-full);
  font-size: 28rpx;
  transition: all 0.2s;
}
.seg-active { background: var(--color-primary); color: #fff; }
```

---

### 13. NavBar（自定义导航栏）

```xml
<view class="navbar" style="padding-top: {{statusBarHeight}}px;">
  <view class="navbar-content">
    <view class="navbar-left">
      <text class="iconfont icon-back" bindtap="goBack" wx:if="{{showBack}}"></text>
    </view>
    <view class="navbar-title"><text>{{title}}</text></view>
    <view class="navbar-right">
      <slot name="right"></slot>
    </view>
  </view>
</view>
```

```css
.navbar { background: var(--bg-card); }
.navbar-content {
  display: flex; align-items: center; justify-content: space-between;
  height: 88rpx; padding: 0 var(--spacing-xl);
}
.navbar-title { font-size: 36rpx; font-weight: 600; }
```

---

### 14. TabBar（自定义标签栏）

```xml
<view class="tabbar" style="padding-bottom: {{safeBottom}}px;">
  <view class="tab-item {{current === index ? 'tab-active' : ''}}"
    wx:for="{{tabs}}" wx:key="index" bindtap="switchTab" data-index="{{index}}">
    <image src="{{current === index ? item.iconActive : item.icon}}" class="tab-icon" />
    <text>{{item.label}}</text>
  </view>
</view>
```

---

## 分类五：展示类

### 15. Card（卡片）

```xml
<view class="card">
  <view class="card-header">
    <text class="card-title">卡片标题</text>
    <text class="card-desc">卡片描述</text>
  </view>
  <view class="card-body">
    <slot />
  </view>
  <view class="card-footer">
    <button class="btn btn-text">取消</button>
    <button class="btn btn-primary">确认</button>
  </view>
</view>
```

```css
.card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-1);
  padding: var(--spacing-xl);
}
.card-title { font-size: var(--font-title); font-weight: 600; }
.card-desc { font-size: var(--font-body-sm); color: var(--text-secondary); margin-top: 8rpx; }
.card-footer { display: flex; justify-content: flex-end; gap: var(--spacing-sm); margin-top: var(--spacing-lg); }
```

---

### 16. List / Cell（列表项）

```xml
<view class="cell" hover-class="cell-hover" bindtap="onTap">
  <image src="{{icon}}" class="cell-icon" wx:if="{{icon}}" />
  <view class="cell-content">
    <text class="cell-title">{{title}}</text>
    <text class="cell-desc" wx:if="{{desc}}">{{desc}}</text>
  </view>
  <view class="cell-right">
    <text wx:if="{{value}}">{{value}}</text>
    <text class="iconfont icon-arrow-right"></text>
  </view>
</view>
```

---

### 17. Grid（宫格布局）

```xml
<view class="grid">
  <view class="grid-item" wx:for="{{items}}" wx:key="index">
    <image src="{{item.icon}}" class="grid-icon" />
    <text class="grid-label">{{item.label}}</text>
  </view>
</view>
```

```css
.grid { display: flex; flex-wrap: wrap; }
.grid-item {
  width: calc((100vw - 64rpx) / 3);
  display: flex; flex-direction: column; align-items: center;
  padding: var(--spacing-lg) 0;
}
.grid-icon { width: 72rpx; height: 72rpx; }
.grid-label { font-size: 24rpx; margin-top: var(--spacing-xs); }
```

---

### 18. Tag / Badge（标签/徽章）

```xml
<!-- 实心标签 -->
<text class="tag tag-primary">热门</text>
<!-- 线框标签 -->
<text class="tag tag-outline">新品</text>
<!-- 徽章 -->
<view class="badge">{{count}}</view>
```

```css
.tag { padding: 4rpx 16rpx; border-radius: var(--radius-sm); font-size: 20rpx; }
.tag-primary { background: var(--color-primary); color: #fff; }
.tag-outline { border: 2rpx solid var(--color-primary); color: var(--color-primary); }
.badge {
  min-width: 32rpx; height: 32rpx; line-height: 32rpx;
  text-align: center; border-radius: 50%;
  background: var(--color-error); color: #fff; font-size: 20rpx;
}
```

---

### 19. Avatar（头像）

```xml
<!-- 圆形头像 -->
<image src="{{avatar}}" mode="aspectFill" class="avatar avatar-circle" />

<!-- 方形头像 -->
<image src="{{avatar}}" mode="aspectFill" class="avatar avatar-square" />

<!-- 文字 fallback -->
<view class="avatar avatar-circle avatar-fallback" wx:else>
  <text>{{nickname[0]}}</text>
</view>
```

```css
.avatar { background: #eee; }
.avatar-circle { border-radius: 50%; width: 96rpx; height: 96rpx; }
.avatar-square { border-radius: var(--radius-sm); width: 96rpx; height: 96rpx; }
.avatar-fallback { 
  display: flex; align-items: center; justify-content: center;
  background: var(--color-primary-light); color: var(--color-primary); font-size: 40rpx; 
}
```

---

### 20. Progress（进度条）

```xml
<!-- 原生 progress -->
<progress percent="{{60}}" stroke-width="8" activeColor="var(--color-primary)" backgroundColor="#eee" />

<!-- 自定义圆形进度 -->
<view class="circle-progress">
  <view class="circle-bg"></view>
  <text class="circle-value">{{percent}}%</text>
</view>
```

---

## 分类六：反馈类

### 21. Modal / Dialog（弹窗）

```xml
<view class="modal-mask" wx:if="{{visible}}" bindtap="onClose">
  <view class="modal-box" catchtap="">
    <text class="modal-title">确认删除？</text>
    <text class="modal-content">此操作不可撤销</text>
    <view class="modal-actions">
      <button class="btn btn-secondary" bindtap="onClose">取消</button>
      <button class="btn btn-danger" bindtap="onConfirm">删除</button>
    </view>
  </view>
</view>
```

```css
.modal-mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); z-index: 1000;
  display: flex; align-items: center; justify-content: center;
}
.modal-box {
  width: 600rpx; background: var(--bg-card);
  border-radius: var(--radius-lg); padding: var(--spacing-xl);
  animation: modalIn 0.2s ease-out;
}
@keyframes modalIn { from { opacity: 0; transform: scale(0.95); } }
.modal-actions { display: flex; gap: var(--spacing-sm); margin-top: var(--spacing-lg); }
```

---

### 22. ActionSheet（动作面板）

```xml
<view class="sheet-mask" wx:if="{{visible}}" bindtap="onClose">
  <view class="sheet-panel" catchtap="">
    <view class="sheet-item" wx:for="{{options}}" wx:key="index" bindtap="onSelect" data-index="{{index}}">
      {{item}}
    </view>
    <view class="sheet-cancel" bindtap="onClose">取消</view>
  </view>
</view>
```

```css
.sheet-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); z-index: 1000; }
.sheet-panel {
  position: absolute; bottom: 0; left: 0; right: 0;
  background: var(--bg-card); border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  padding-bottom: env(safe-area-inset-bottom);
  animation: slideUp 0.25s ease-out;
}
@keyframes slideUp { from { transform: translateY(100%); } }
.sheet-item { text-align: center; padding: var(--spacing-lg); border-bottom: 1rpx solid #eee; font-size: 32rpx; }
.sheet-cancel { text-align: center; padding: var(--spacing-lg); color: var(--text-secondary); }
```

---

### 23. Toast / Loading（轻提示）

```xml
<!-- 使用系统 toast -->
<text>{{wx.showToast({ title: '保存成功', icon: 'success' })}}</text>

<!-- 自定义 loading -->
<view class="loading" wx:if="{{loading}}">
  <view class="loading-spinner"></view>
  <text>加载中...</text>
</view>
```

```css
.loading-spinner {
  width: 48rpx; height: 48rpx;
  border: 4rpx solid #eee;
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
```

---

## 分类七：数据展示类

### 24. Swiper / Carousel（轮播）

```xml
<swiper indicator-dots="{{true}}" autoplay="{{true}}" circular="{{true}}">
  <swiper-item wx:for="{{banners}}" wx:key="index">
    <image src="{{item}}" mode="aspectFill" class="swiper-img" />
  </swiper-item>
</swiper>
```

```css
swiper { width: 100%; height: 400rpx; }
.swiper-img { width: 100%; height: 100%; }
```

---

### 25. Steps（步骤条）

```xml
<view class="steps">
  <view class="step {{index <= active ? 'step-done' : ''}}" wx:for="{{steps}}" wx:key="index">
    <view class="step-icon">
      <text wx:if="{{index < active}}">✓</text>
      <text wx:else>{{index + 1}}</text>
    </view>
    <text class="step-label">{{item}}</text>
    <view class="step-line" wx:if="{{index < steps.length - 1}}"></view>
  </view>
</view>
```

```css
.steps { display: flex; }
.step { flex: 1; display: flex; flex-direction: column; align-items: center; position: relative; }
.step-icon { 
  width: 48rpx; height: 48rpx; border-radius: 50%; background: #ddd;
  display: flex; align-items: center; justify-content: center; font-size: 24rpx; color: #999;
}
.step-done .step-icon { background: var(--color-primary); color: #fff; }
.step-line { 
  position: absolute; top: 24rpx; left: 50%; width: 100%; height: 2rpx; background: #ddd; z-index: -1;
}
.step-done .step-line { background: var(--color-primary); }
```

---

## 分类八：状态类

### 26. Skeleton（骨架屏）

```xml
<view class="skeleton-card" wx:if="{{loading}}">
  <view class="skeleton-avatar"></view>
  <view class="skeleton-lines">
    <view class="skeleton-line w-3-4"></view>
    <view class="skeleton-line w-1-2"></view>
  </view>
</view>
```

```css
.skeleton-avatar { width: 96rpx; height: 96rpx; border-radius: 50%; background: #eee; }
.skeleton-line { height: 24rpx; border-radius: 4rpx; background: #eee; margin-bottom: 16rpx; }
.w-3-4 { width: 75%; }
.w-1-2 { width: 50%; }
.skeleton-avatar, .skeleton-line {
  animation: pulse 1.5s ease-in-out infinite;
}
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
```

---

### 27. Empty State（空状态）

```xml
<view class="empty" wx:if="{{list.length === 0}}">
  <image src="/assets/images/empty.svg" class="empty-img" />
  <text class="empty-text">暂无数据</text>
  <button class="btn btn-primary" bindtap="onRetry">重新加载</button>
</view>
```

```css
.empty { display: flex; flex-direction: column; align-items: center; padding: 120rpx 0; }
.empty-img { width: 240rpx; height: 240rpx; opacity: 0.6; }
.empty-text { font-size: 28rpx; color: var(--text-secondary); margin: var(--spacing-lg) 0; }
```

---

### 28. Error / Retry（错误重试）

```xml
<view class="error" wx:if="{{error}}">
  <text class="error-text">加载失败，请检查网络</text>
  <button class="btn btn-secondary" bindtap="onRetry">重试</button>
</view>
```

```css
.error { display: flex; flex-direction: column; align-items: center; padding: 120rpx 0; }
.error-text { font-size: 28rpx; color: var(--color-error); margin-bottom: var(--spacing-lg); }
```

---

## 分类九：搜索类

### 29. SearchBar（搜索栏）

```xml
<view class="search-bar">
  <view class="search-input-wrap">
    <text class="iconfont icon-search"></text>
    <input class="search-input" placeholder="搜索" bindinput="onSearch"
      confirm-type="search" bindconfirm="onSearchConfirm" />
    <text class="iconfont icon-close" wx:if="{{keyword}}" bindtap="onClear"></text>
  </view>
  <text class="search-cancel" wx:if="{{focused}}" bindtap="onCancel">取消</text>
</view>
```

```css
.search-bar { display: flex; align-items: center; padding: var(--spacing-md) var(--spacing-xl); }
.search-input-wrap {
  flex: 1; display: flex; align-items: center;
  background: #f5f5f5; border-radius: var(--radius-full); padding: 12rpx 24rpx;
}
.search-input { flex: 1; font-size: 28rpx; }
.search-cancel { margin-left: var(--spacing-md); font-size: 28rpx; color: var(--color-primary); }
```

---

## 分类十：媒体类

### 30. ImagePicker（图片选择）

```xml
<view class="image-picker">
  <view class="img-preview" wx:for="{{images}}" wx:key="path">
    <image src="{{item}}" mode="aspectFill" />
    <view class="img-delete" bindtap="onDelete" data-index="{{index}}">
      <text class="iconfont icon-close"></text>
    </view>
  </view>
  <view class="img-add" wx:if="{{images.length < maxCount}}" bindtap="onChooseImage">
    <text class="iconfont icon-plus" style="font-size: 48rpx; color: #ccc;"></text>
  </view>
</view>
```

```css
.image-picker { display: flex; flex-wrap: wrap; gap: var(--spacing-sm); }
.img-preview, .img-add {
  width: calc((100vw - 4 * 32rpx) / 3);
  height: calc((100vw - 4 * 32rpx) / 3);
  border-radius: var(--radius-sm);
  overflow: hidden;
}
.img-preview { position: relative; }
.img-delete {
  position: absolute; top: 0; right: 0;
  width: 40rpx; height: 40rpx; background: rgba(0,0,0,0.5);
  border-radius: 0 0 0 var(--radius-sm);
  display: flex; align-items: center; justify-content: center;
}
.img-add {
  border: 2rpx dashed #ddd;
  display: flex; align-items: center; justify-content: center;
}
```
