<template>
  <div class="trend-chart">
    <div v-if="!data.length" class="empty">暂无数据</div>
    <div v-else class="chart-body">
      <div v-for="item in data" :key="item.date" class="bar-item" :title="`${item.date} 共${item.total}题 答对${item.correct}题`">
        <div class="bar-wrap">
          <div class="bar-total" :style="{ height: barHeight(item.total) }">
            <div class="bar-correct" :style="{ height: correctHeight(item) }"></div>
          </div>
          <span class="bar-value">{{ item.total }}</span>
        </div>
        <span class="bar-date">{{ item.date.slice(5) }}</span>
      </div>
    </div>
    <div v-if="data.length" class="legend">
      <span class="legend-item"><i class="dot dot-total"></i>刷题数</span>
      <span class="legend-item"><i class="dot dot-correct"></i>答对数</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TrendPoint } from '@/types'

const props = defineProps<{ data: TrendPoint[] }>()

const CHART_HEIGHT = 220

const maxTotal = computed(() => Math.max(1, ...props.data.map((d) => d.total)))

function barHeight(total: number): string {
  return `${Math.max(2, Math.round((total / maxTotal.value) * CHART_HEIGHT))}px`
}

function correctHeight(item: TrendPoint): string {
  if (item.total <= 0) return '0px'
  const totalPx = Math.max(2, Math.round((item.total / maxTotal.value) * CHART_HEIGHT))
  return `${Math.round((item.correct / item.total) * totalPx)}px`
}
</script>

<style scoped lang="scss">
.trend-chart {
  width: 100%;
}

.empty {
  text-align: center;
  color: #909399;
  padding: 60px 0;
  font-size: 15px;
}

.chart-body {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  overflow-x: auto;
  padding: 8px 4px 0;
}

.bar-item {
  flex: 1;
  min-width: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.bar-wrap {
  height: 260px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: center;
}

.bar-total {
  width: 22px;
  background: #c8ccff;
  border-radius: 4px 4px 0 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  overflow: hidden;
}

.bar-correct {
  width: 100%;
  background: #4f5dff;
}

.bar-value {
  font-size: 12px;
  color: #606266;
  margin-top: 2px;
}

.bar-date {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.legend {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 12px;
  font-size: 14px;
  color: #606266;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  display: inline-block;
}

.dot-total {
  background: #c8ccff;
}

.dot-correct {
  background: #4f5dff;
}
</style>
