<script setup lang="ts">
/**
 * 通用分页条（B-T23 自三页 pager 逐字副本收敛，含右对齐布局）
 * 作者: wanglx
 */
defineProps<{
  total: number
  page: number
  size: number
}>()

const emit = defineEmits<{
  (e: 'change', page: number): void
}>()
</script>

<template>
  <div class="pager">
    <el-pagination
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="size"
      :current-page="page"
      @current-change="emit('change', $event)"
    />
  </div>
</template>

<style scoped>
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
