<template>
  <div class="page">
    <h2 class="page-title">学习统计</h2>

    <el-card>
      <template #header>
        <span class="card-header">{{ bankStore.currentBank?.name || '题库' }} · 各科正确率</span>
      </template>
      <el-table :data="subjects" v-loading="loading" border>
        <el-table-column label="科目" width="200">
          <template #default="{ row }">{{ subjectName(row.subject) }}</template>
        </el-table-column>
        <el-table-column prop="total" label="刷题数" width="140" />
        <el-table-column prop="correct" label="答对数" width="140" />
        <el-table-column label="正确率">
          <template #default="{ row }">
            <div class="acc-cell">
              <el-progress
                :percentage="Math.round(row.accuracy)"
                :stroke-width="16"
                :color="accColor(row.accuracy)"
                class="acc-bar"
              />
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="row-gap">
      <template #header><span class="card-header">近14天刷题趋势</span></template>
      <TrendChart :data="trend" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import TrendChart from '@/components/TrendChart.vue'
import { statsBySubject, statsTrend } from '@/api/stats'
import type { SubjectStat, TrendPoint } from '@/types'
import { useBankStore } from '@/stores/bank'

const bankStore = useBankStore()

/** 科目名称按当前题库的科目定义解析 */
function subjectName(code?: string) {
  return bankStore.subjectName(code)
}

const loading = ref(false)
const subjects = ref<SubjectStat[]>([])
const trend = ref<TrendPoint[]>([])

function accColor(accuracy: number): string {
  if (accuracy >= 80) return '#67c23a'
  if (accuracy >= 60) return '#4f5dff'
  return '#f56c6c'
}

onMounted(async () => {
  loading.value = true
  try {
    const [s, t] = await Promise.all([
      statsBySubject(bankStore.currentCode),
      statsTrend(bankStore.currentCode)
    ])
    subjects.value = s
    trend.value = t
  } catch {
    // 图表数据拉不到就显示空态，报错交给拦截器提示
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.row-gap {
  margin-top: 16px;
}

.card-header {
  font-size: 17px;
  font-weight: 600;
}

.acc-cell {
  padding-right: 12px;
}

.acc-bar {
  width: 100%;
}
</style>
