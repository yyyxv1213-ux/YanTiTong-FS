<template>
  <div class="page">
    <h2 class="page-title">仪表盘</h2>
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">今日刷题</div>
          <div class="stat-value primary">{{ overview?.todayCount ?? 0 }}</div>
          <div class="stat-foot">目标 {{ overview?.dailyTarget ?? 0 }} 题</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">累计刷题</div>
          <div class="stat-value">{{ overview?.totalCount ?? 0 }}</div>
          <div class="stat-foot">坚持就是胜利</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">累计答对</div>
          <div class="stat-value success">{{ overview?.correctCount ?? 0 }}</div>
          <div class="stat-foot">共 {{ overview?.totalCount ?? 0 }} 题</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">总正确率</div>
          <div class="stat-value warning">{{ (overview?.accuracy ?? 0).toFixed(1) }}%</div>
          <div class="stat-foot">稳步提升中</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="row-gap">
      <el-col :span="12">
        <el-card>
          <template #header><span class="card-header">每日目标进度</span></template>
          <div class="target-progress">
            <el-progress
              type="dashboard"
              :percentage="targetPercent"
              :stroke-width="14"
              :width="180"
            >
              <template #default>
                <div class="progress-text">
                  <div class="progress-num">{{ overview?.todayCount ?? 0 }}/{{ overview?.dailyTarget ?? 0 }}</div>
                  <div class="progress-label">今日完成</div>
                </div>
              </template>
            </el-progress>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span class="card-header">考试倒计时</span></template>
          <div class="countdown-box">
            <div class="countdown-num">{{ overview?.daysLeft ?? '-' }}</div>
            <div class="countdown-unit">天</div>
            <div class="countdown-date">考试日期：{{ overview?.examDate || '未设置' }}</div>
            <el-button type="primary" plain @click="router.push('/student/plan')">调整学习计划</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="row-gap">
      <template #header><span class="card-header">近14天刷题趋势</span></template>
      <TrendChart :data="trend" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TrendChart from '@/components/TrendChart.vue'
import { statsOverview, statsTrend } from '@/api/stats'
import type { StatsOverview, TrendPoint } from '@/types'
import { useBankStore } from '@/stores/bank'

const router = useRouter()
const bankStore = useBankStore()
const overview = ref<StatsOverview | null>(null)
const trend = ref<TrendPoint[]>([])

const targetPercent = computed(() => {
  if (!overview.value || !overview.value.dailyTarget) return 0
  return Math.min(100, Math.round((overview.value.todayCount / overview.value.dailyTarget) * 100))
})

onMounted(async () => {
  try {
    const [o, t] = await Promise.all([
      statsOverview(bankStore.currentCode),
      statsTrend(bankStore.currentCode)
    ])
    overview.value = o
    trend.value = t
  } catch {
    // 统计拉取失败时页面显示空态，原因由拦截器提示
  }
})
</script>

<style scoped lang="scss">
.row-gap {
  margin-top: 16px;
}

.stat-card {
  text-align: center;
}

.stat-label {
  font-size: 15px;
  color: #909399;
}

.stat-value {
  font-size: 40px;
  font-weight: 700;
  margin: 8px 0;
  color: #303133;

  &.primary {
    color: #4f5dff;
  }

  &.success {
    color: #67c23a;
  }

  &.warning {
    color: #e6a23c;
  }
}

.stat-foot {
  font-size: 13px;
  color: #c0c4cc;
}

.card-header {
  font-size: 17px;
  font-weight: 600;
}

.target-progress {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}

.progress-text {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.progress-num {
  font-size: 24px;
  font-weight: 700;
  color: #4f5dff;
}

.progress-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.countdown-box {
  text-align: center;
  padding: 6px 0 12px;
}

.countdown-num {
  font-size: 64px;
  font-weight: 700;
  color: #f56c6c;
  line-height: 1.1;
}

.countdown-unit {
  font-size: 16px;
  color: #909399;
  margin-bottom: 8px;
}

.countdown-date {
  font-size: 15px;
  color: #606266;
  margin-bottom: 12px;
}
</style>
