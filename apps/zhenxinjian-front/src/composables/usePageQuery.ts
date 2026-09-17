/**
 * 列表页查询骨架（B-T23 自 users/foods/diet-records 三页逐字副本收敛）：
 * loading/表格数据/总数托管 + 查询重置页码 + 分页回调 + 删后回退页码 + 挂载首载
 * 作者: wanglx
 */
import { onMounted, ref } from 'vue'
import type { PageResult } from '@/api/types'

/** 分页查询参数基座（各页在此上扩展筛选字段） */
export interface PageQueryBase {
  page: number
  size: number
}

export function usePageQuery<T, Q extends PageQueryBase>(
  query: Q,
  fetcher: (query: Q) => Promise<PageResult<T>>
) {
  const loading = ref(false)
  const tableData = ref<T[]>([])
  const total = ref(0)

  async function load() {
    loading.value = true
    try {
      const page = await fetcher(query)
      tableData.value = page.records || []
      total.value = page.total || 0
    } finally {
      loading.value = false
    }
  }

  /** 查询（重置回第一页） */
  function handleSearch() {
    query.page = 1
    load()
  }

  /** 翻页 */
  function handlePageChange(page: number) {
    query.page = page
    load()
  }

  /** 每页条数变更（回到第一页重新查询） */
  function handleSizeChange(size: number) {
    query.size = size
    query.page = 1
    load()
  }

  /** 重置筛选（恢复初始查询条件后回到第一页） */
  function handleReset(resetQuery: Partial<Q>) {
    Object.assign(query, resetQuery, { page: 1 })
    load()
  }

  /** 删后重载（当前页最后一条被删且非首页时回退一页，避免空页滞留） */
  async function reloadAfterDelete() {
    if (tableData.value.length === 1 && query.page > 1) {
      query.page -= 1
    }
    await load()
  }

  onMounted(load)

  return {
    loading,
    tableData,
    total,
    load,
    handleSearch,
    handlePageChange,
    handleSizeChange,
    handleReset,
    reloadAfterDelete
  }
}
