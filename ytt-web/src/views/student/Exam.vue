<template>
  <div class="page">
    <h2 class="page-title">模拟考试</h2>

    <!-- 开始考试 -->
    <el-card v-if="phase === 'idle'" class="start-card">
      <div class="start-title">组卷设置</div>
      <el-form label-width="100px" class="start-form">
        <el-form-item label="题目数量">
          <el-input-number v-model="startForm.count" :min="5" :max="100" />
          <span class="form-tip">5 - 100 题</span>
        </el-form-item>
        <el-form-item label="考试时长">
          <el-input-number v-model="startForm.minutes" :min="5" :max="180" />
          <span class="form-tip">分钟（5 - 180）</span>
        </el-form-item>
        <el-form-item label="科目范围">
          <el-select v-model="startForm.subject" placeholder="全部科目" clearable style="width: 220px">
            <el-option v-for="s in bankStore.subjectOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="starting" @click="handleStart">开始考试</el-button>
        </el-form-item>
      </el-form>
      <el-divider />
      <div class="history-entry">
        <el-button text type="primary" @click="openHistory">查看历史成绩</el-button>
      </div>
    </el-card>

    <!-- 考试中 -->
    <template v-else-if="phase === 'running' && currentQuestion">
      <div class="exam-top">
        <div class="exam-progress">第 {{ currentIndex + 1 }} / {{ questions.length }} 题</div>
        <div class="exam-timer" :class="{ danger: remaining <= 60 }">
          <el-icon><Timer /></el-icon>
          {{ formatSeconds(remaining) }}
        </div>
        <el-button type="danger" @click="confirmSubmit">交卷</el-button>
      </div>
      <div class="exam-body">
        <el-card class="exam-question">
          <div class="q-head">
            <el-tag>{{ questionTypeName(currentQuestion.type) }}</el-tag>
            <el-tag :type="difficultyTagType(currentQuestion.difficulty)">{{ difficultyName(currentQuestion.difficulty) }}</el-tag>
            <el-tag type="info" effect="plain">{{ subjectName(currentQuestion.subject) }}</el-tag>
            <el-tag v-if="currentQuestion.chapter" type="info" effect="plain">{{ currentQuestion.chapter }}</el-tag>
          </div>
          <div class="question-stem" v-html="currentQuestion.stem"></div>

          <el-radio-group
            v-if="currentQuestion.type !== 'multi'"
            :model-value="answers[currentQuestion.id]?.[0]"
            class="option-group"
            @update:model-value="onSingleUpdate"
          >
            <el-radio v-for="(opt, i) in currentQuestion.options" :key="i" :value="i" class="option-item">
              <span class="option-label">{{ optionLabel(i) }}.</span>
              <span v-html="opt"></span>
            </el-radio>
          </el-radio-group>
          <el-checkbox-group
            v-else
            :model-value="answers[currentQuestion.id] ?? []"
            class="option-group"
            @update:model-value="onMultiUpdate"
          >
            <el-checkbox v-for="(opt, i) in currentQuestion.options" :key="i" :value="i" class="option-item">
              <span class="option-label">{{ optionLabel(i) }}.</span>
              <span v-html="opt"></span>
            </el-checkbox>
          </el-checkbox-group>

          <div class="q-actions">
            <el-button :disabled="currentIndex === 0" @click="currentIndex--">上一题</el-button>
            <el-button v-if="currentIndex < questions.length - 1" type="primary" @click="currentIndex++">下一题</el-button>
            <el-button v-else type="danger" @click="confirmSubmit">交卷</el-button>
          </div>
        </el-card>

        <el-card class="answer-sheet">
          <template #header><span class="sheet-title">答题卡</span></template>
          <div class="sheet-grid">
            <button
              v-for="(q, i) in questions"
              :key="q.id"
              class="sheet-cell"
              :class="{ answered: (answers[q.id] ?? []).length > 0, current: i === currentIndex }"
              @click="currentIndex = i"
            >
              {{ i + 1 }}
            </button>
          </div>
          <div class="sheet-legend">
            <span><i class="cell-demo answered"></i>已答</span>
            <span><i class="cell-demo"></i>未答</span>
            <span><i class="cell-demo current"></i>当前</span>
          </div>
        </el-card>
      </div>
    </template>

    <!-- 成绩 -->
    <el-card v-else-if="phase === 'result' && result" class="result-card">
      <el-result
        icon="success"
        title="考试完成"
        :sub-title="`得分 ${result.score} 分 · 答对 ${result.correctCount} / ${result.totalCount} 题`"
      >
        <template #extra>
          <el-button type="primary" @click="resetToIdle">再来一套</el-button>
          <el-button @click="viewReport(result.recordId)">查看答题报告</el-button>
          <el-button text type="primary" @click="openHistory">历史成绩</el-button>
        </template>
      </el-result>
    </el-card>

    <!-- 历史成绩 -->
    <el-dialog v-model="historyVisible" title="历史成绩" width="720px">
      <el-table :data="history" v-loading="historyLoading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="score" label="得分" width="90">
          <template #default="{ row }">
            <span class="score-text">{{ row.score }} 分</span>
          </template>
        </el-table-column>
        <el-table-column label="正确" width="110">
          <template #default="{ row }">{{ row.correctCount }} / {{ row.totalCount }}</template>
        </el-table-column>
        <el-table-column label="用时" width="130">
          <template #default="{ row }">{{ formatDuration(row.durationSeconds) }}</template>
        </el-table-column>
        <el-table-column label="考试时间">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="viewReport(row.id)">报告</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="historyPage.pageNum"
          v-model:page-size="historyPage.pageSize"
          :total="historyPage.total"
          layout="total, prev, pager, next"
          @current-change="loadHistory"
        />
      </div>
    </el-dialog>

    <!-- 答题报告 -->
    <el-dialog v-model="reportVisible" title="答题报告" width="640px">
      <template v-if="report">
        <div class="report-summary">
          得分 <b>{{ report.record.score }}</b> 分 · 答对 {{ report.record.correctCount }} / {{ report.record.totalCount }} 题 ·
          用时 {{ formatDuration(report.record.durationSeconds) }}
        </div>
        <el-table :data="report.details" border max-height="420">
          <el-table-column label="题号" width="80">
            <template #default="{ $index }">{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column label="我的答案">
            <template #default="{ row }">{{ answerText(row.userAnswer) }}</template>
          </el-table-column>
          <el-table-column label="结果" width="100">
            <template #default="{ row }">
              <el-tag :type="row.correct ? 'success' : 'danger'">{{ row.correct ? '正确' : '错误' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Timer } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { examHistory, examReport, startExam, submitExam } from '@/api/exam'
import type {
  ExamRecord,
  ExamReport,
  ExamSubmitResponse,
  Question
} from '@/types'
import { useBankStore } from '@/stores/bank'
import {
  answerText,
  difficultyName,
  difficultyTagType,
  formatDuration,
  formatSeconds,
  optionLabel,
  questionTypeName
} from '@/utils/format'

const bankStore = useBankStore()

/** 科目名称按当前题库的科目定义解析 */
function subjectName(code?: string) {
  return bankStore.subjectName(code)
}

type Phase = 'idle' | 'running' | 'result'

const phase = ref<Phase>('idle')
const starting = ref(false)
const startForm = reactive({ count: 20, minutes: 30, subject: '' })

const examKey = ref('')
const questions = ref<Question[]>([])
const currentIndex = ref(0)
const answers = reactive<Record<number, number[]>>({})
const remaining = ref(0)
const totalSeconds = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const result = ref<ExamSubmitResponse | null>(null)

const currentQuestion = computed<Question | undefined>(() => questions.value[currentIndex.value])

function setSingle(questionId: number, val: string | number | boolean | undefined) {
  if (typeof val === 'number') {
    answers[questionId] = [val]
  }
}

function setMulti(questionId: number, val: Array<string | number>) {
  answers[questionId] = val.map(Number)
}

function onSingleUpdate(val: string | number | boolean | undefined) {
  const q = currentQuestion.value
  if (q) setSingle(q.id, val)
}

function onMultiUpdate(val: Array<string | number>) {
  const q = currentQuestion.value
  if (q) setMulti(q.id, val)
}

async function handleStart() {
  starting.value = true
  try {
    const res = await startExam({
      bankCode: bankStore.currentCode,
      count: startForm.count,
      minutes: startForm.minutes,
      subject: startForm.subject
    })
    examKey.value = res.examKey
    questions.value = res.questions
    currentIndex.value = 0
    Object.keys(answers).forEach((k) => delete answers[Number(k)])
    totalSeconds.value = res.minutes * 60
    remaining.value = totalSeconds.value
    phase.value = 'running'
    startTimer()
  } catch {
    // 组卷失败的原因由拦截器弹出，这里只需复位按钮状态
  } finally {
    starting.value = false
  }
}

function startTimer() {
  stopTimer()
  timer = setInterval(() => {
    remaining.value--
    if (remaining.value <= 0) {
      ElMessage.warning('考试时间到，系统自动交卷')
      doSubmit()
    }
  }, 1000)
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

async function confirmSubmit() {
  const unanswered = questions.value.filter((q) => (answers[q.id] ?? []).length === 0).length
  try {
    await ElMessageBox.confirm(
      unanswered > 0 ? `还有 ${unanswered} 题未作答，确定交卷吗？` : '确定要交卷吗？',
      '交卷确认',
      { confirmButtonText: '交卷', cancelButtonText: '继续作答', type: 'warning' }
    )
  } catch {
    return
  }
  await doSubmit()
}

async function doSubmit() {
  stopTimer()
  try {
    const res = await submitExam({
      examKey: examKey.value,
      answers: questions.value.map((q) => ({
        questionId: q.id,
        userAnswer: answers[q.id] ?? []
      })),
      durationSeconds: totalSeconds.value - Math.max(0, remaining.value)
    })
    result.value = res
    phase.value = 'result'
  } catch {
    // 错误信息已由拦截器提示；交卷失败则恢复计时
    if (remaining.value > 0) startTimer()
  }
}

function resetToIdle() {
  phase.value = 'idle'
  result.value = null
  questions.value = []
}

/* ---------- 历史与报告 ---------- */

const historyVisible = ref(false)
const historyLoading = ref(false)
const history = ref<ExamRecord[]>([])
const historyPage = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const reportVisible = ref(false)
const report = ref<ExamReport | null>(null)

function formatTime(t: string): string {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'
}

async function openHistory() {
  historyVisible.value = true
  historyPage.pageNum = 1
  await loadHistory()
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const res = await examHistory(historyPage.pageNum, historyPage.pageSize)
    history.value = res.records
    historyPage.total = res.total
  } catch {
    // 拦截器已提示错误，列表保留上次数据
  } finally {
    historyLoading.value = false
  }
}

async function viewReport(id: number) {
  try {
    report.value = await examReport(id)
    reportVisible.value = true
  } catch {
    // 打开失败时拦截器会说明原因
  }
}

onBeforeUnmount(stopTimer)
</script>

<style scoped lang="scss">
.start-card {
  max-width: 640px;
}

.start-title {
  font-size: 19px;
  font-weight: 600;
  margin-bottom: 18px;
}

.start-form {
  max-width: 480px;
}

.form-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 14px;
}

.history-entry {
  text-align: center;
}

.exam-top {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 14px;
}

.exam-progress {
  font-size: 16px;
  color: #606266;
}

.exam-timer {
  flex: 1;
  font-size: 22px;
  font-weight: 700;
  color: #4f5dff;
  display: flex;
  align-items: center;
  gap: 6px;

  &.danger {
    color: #f56c6c;
  }
}

.exam-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.exam-question {
  flex: 1;
  min-height: 360px;
}

.q-head {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.option-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
}

.option-item {
  display: flex;
  align-items: flex-start;
  width: 100%;
  margin-right: 0;
  padding: 10px 12px;
  border-radius: 6px;
  height: auto;
  min-height: 44px;

  &:hover {
    background: #f5f7ff;
  }

  :deep(.el-radio__label),
  :deep(.el-checkbox__label) {
    font-size: 16px;
    line-height: 1.7;
    white-space: normal;
    word-break: break-word;
  }
}

.option-label {
  font-weight: 600;
  margin-right: 6px;
}

.q-actions {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}

.answer-sheet {
  width: 280px;
  flex-shrink: 0;
}

.sheet-title {
  font-size: 16px;
  font-weight: 600;
}

.sheet-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.sheet-cell {
  height: 38px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  font-size: 15px;
  color: #606266;

  &.answered {
    background: #4f5dff;
    border-color: #4f5dff;
    color: #fff;
  }

  &.current {
    outline: 2px solid #f56c6c;
    outline-offset: 1px;
  }
}

.sheet-legend {
  display: flex;
  gap: 14px;
  margin-top: 14px;
  font-size: 13px;
  color: #909399;

  span {
    display: inline-flex;
    align-items: center;
    gap: 5px;
  }
}

.cell-demo {
  width: 12px;
  height: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 3px;
  display: inline-block;

  &.answered {
    background: #4f5dff;
    border-color: #4f5dff;
  }

  &.current {
    outline: 2px solid #f56c6c;
  }
}

.result-card {
  max-width: 720px;
  margin: 0 auto;
}

.score-text {
  font-weight: 700;
  color: #4f5dff;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.report-summary {
  font-size: 16px;
  margin-bottom: 14px;

  b {
    color: #4f5dff;
    font-size: 20px;
  }
}
</style>
