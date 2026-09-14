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
  (e: 'size-change', size: number): void
}>()
</script>

<template>
  <div class="pager">
    <el-pagination
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      :page-size="size"
      :current-page="page"
      :page-sizes="[10, 20, 50, 100]"
      @current-change="emit('change', $event)"
      @size-change="emit('size-change', $event)"
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
