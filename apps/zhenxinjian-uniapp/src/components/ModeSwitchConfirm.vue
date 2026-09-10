<script setup lang="ts">
/**
 * P16 模式切换确认弹窗：碳循环切出时二次确认「终止周期并清空进度」
 * 作者: wanglx
 */

defineProps<{
  /** 是否显示 */
  visible: boolean
}>()

const emit = defineEmits<{
  /** 确认切换（调用方执行 PUT /api/body/mode） */
  (e: 'confirm'): void
  /** 取消 */
  (e: 'cancel'): void
}>()

function onConfirm() {
  emit('confirm')
}

function onCancel() {
  emit('cancel')
}
</script>

<template>
  <view v-if="visible" class="modal-mask" @click="onCancel">
    <view class="modal" @click.stop>
      <text class="modal-title">切换为 532 模式？</text>
      <text class="modal-desc">切换将终止当前碳循环周期并清空进度，历史周期仍可在计划中查看。</text>
      <view class="modal-actions">
        <view class="modal-btn cancel" @click="onCancel">再想想</view>
        <view class="modal-btn confirm" @click="onConfirm">确认切换</view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal {
  width: 560rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 48rpx 32rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.modal-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $zhenxinjian-text;
  margin-bottom: 16rpx;
}

.modal-desc {
  font-size: 26rpx;
  color: $zhenxinjian-text-secondary;
  line-height: 1.6;
  margin-bottom: 32rpx;
  text-align: center;
}

.modal-actions {
  display: flex;
  gap: 24rpx;
  width: 100%;
}

.modal-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border-radius: 12rpx;
  font-size: 28rpx;
}

.modal-btn.cancel {
  border: 1rpx solid $zhenxinjian-border;
  color: $zhenxinjian-text;
}

.modal-btn.confirm {
  background: #f56c6c;
  color: #fff;
}
</style>
