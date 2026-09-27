<template>
  <div class="page">
    <h2 class="page-title">学习计划</h2>

    <el-card class="plan-card" v-loading="loading">
      <el-form label-width="110px" class="plan-form">
        <el-form-item label="每日刷题目标">
          <el-input-number v-model="form.dailyTarget" :min="1" :max="500" />
          <span class="form-tip">题 / 天（1 - 500）</span>
        </el-form-item>
        <el-form-item label="考试日期">
          <el-date-picker
            v-model="form.examDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择考试日期"
            :disabled-date="(d: Date) => d.getTime() < Date.now() - 86400000"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存计划</el-button>
        </el-form-item>
      </el-form>

      <el-divider />
      <div v-if="form.examDate" class="days-box">
        <div class="days-num">{{ daysLeft }}</div>
        <div class="days-text">距离 {{ form.examDate }} 考试还有 {{ daysLeft }} 天，加油！</div>
      </div>
      <el-empty v-else description="还未设置考试日期" :image-size="80" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getPlan, savePlan } from '@/api/plan'

const loading = ref(false)
const saving = ref(false)
const form = reactive({
  dailyTarget: 30,
  examDate: ''
})

const daysLeft = computed(() => {
  if (!form.examDate) return 0
  return Math.max(0, dayjs(form.examDate).startOf('day').diff(dayjs().startOf('day'), 'day'))
})

async function handleSave() {
  if (!form.examDate) {
    ElMessage.warning('请选择考试日期')
    return
  }
  saving.value = true
  try {
    await savePlan({ dailyTarget: form.dailyTarget, examDate: form.examDate })
    ElMessage.success('学习计划已保存')
  } catch {
    // 保存失败由拦截器提示，表单不关闭便于重试
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const plan = await getPlan()
    if (plan) {
      form.dailyTarget = plan.dailyTarget
      form.examDate = plan.examDate
    }
  } catch {
    // 计划读取失败时沿用表单默认值
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.plan-card {
  max-width: 640px;
}

.plan-form {
  max-width: 520px;
}

.form-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 14px;
}

.days-box {
  text-align: center;
  padding: 10px 0 16px;
}

.days-num {
  font-size: 72px;
  font-weight: 700;
  color: #f56c6c;
  line-height: 1.1;
}

.days-text {
  font-size: 16px;
  color: #606266;
  margin-top: 8px;
}
</style>
