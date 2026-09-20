<template>
  <div class="page">
    <h2 class="page-title">刷题训练</h2>

    <el-card class="filter-card">
      <div class="filter-bar">
        <el-select v-model="query.subject" placeholder="科目" clearable style="width: 180px">
          <el-option v-for="s in bankStore.subjectOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-input v-model="query.chapter" placeholder="章节（如：栈与队列）" clearable style="width: 200px" />
        <el-select v-model="query.type" placeholder="题型" clearable style="width: 140px">
          <el-option label="单选题" value="single" />
          <el-option label="多选题" value="multi" />
          <el-option label="判断题" value="judge" />
        </el-select>
        <el-select v-model="query.difficulty" placeholder="难度" clearable style="width: 130px">
          <el-option label="简单" :value="1" />
          <el-option label="中等" :value="2" />
          <el-option label="困难" :value="3" />
        </el-select>
        <el-select v-model="query.mode" style="width: 140px">
          <el-option label="顺序练习" value="sequence" />
          <el-option label="随机练习" value="random" />
          <el-option label="错题练习" value="wrong" />
        </el-select>
        <el-input-number v-model="query.limit" :min="1" :max="50" style="width: 150px" />
        <el-button type="primary" :loading="loading" @click="loadQuestions">开始刷题</el-button>
      </div>
    </el-card>

    <el-empty v-if="!loading && questions.length === 0" description="请选择条件后点击「开始刷题」抽题" />

    <template v-else-if="current">
      <div class="progress-line">
        <span>第 {{ currentIndex + 1 }} / {{ questions.length }} 题</span>
        <el-progress :percentage="Math.round(((currentIndex + 1) / questions.length) * 100)" :stroke-width="10" class="line-progress" />
      </div>

      <el-card class="question-card">
        <div class="q-head">
          <el-tag>{{ questionTypeName(current.type) }}</el-tag>
          <el-tag :type="difficultyTagType(current.difficulty)">{{ difficultyName(current.difficulty) }}</el-tag>
          <el-tag type="info" effect="plain">{{ subjectName(current.subject) }}</el-tag>
          <el-tag v-if="current.chapter" type="info" effect="plain">{{ current.chapter }}</el-tag>
        </div>
        <div class="question-stem" v-html="current.stem"></div>

        <!-- 单选 / 判断 -->
        <el-radio-group
          v-if="current.type !== 'multi'"
          :model-value="currentState.selected[0]"
          class="option-group"
          :disabled="currentState.submitted"
          @update:model-value="setSingle"
        >
          <el-radio v-for="(opt, i) in current.options" :key="i" :value="i" class="option-item" :class="optionClass(i)">
            <span class="option-label">{{ optionLabel(i) }}.</span>
            <span v-html="opt"></span>
          </el-radio>
        </el-radio-group>

        <!-- 多选 -->
        <el-checkbox-group
          v-else
          :model-value="currentState.selected"
          class="option-group"
          :disabled="currentState.submitted"
          @update:model-value="setMulti"
        >
          <el-checkbox v-for="(opt, i) in current.options" :key="i" :value="i" class="option-item" :class="optionClass(i)">
            <span class="option-label">{{ optionLabel(i) }}.</span>
            <span v-html="opt"></span>
          </el-checkbox>
        </el-checkbox-group>

        <div class="q-actions">
          <el-button :disabled="currentIndex === 0" @click="currentIndex--">上一题</el-button>
          <el-button
            v-if="!currentState.submitted"
            type="primary"
            :disabled="currentState.selected.length === 0"
            :loading="submitting"
            @click="handleSubmit"
          >
            提交
          </el-button>
          <el-button v-if="currentIndex < questions.length - 1" type="primary" plain @click="currentIndex++">下一题</el-button>
          <el-button v-else type="success" plain @click="loadQuestions">换一批</el-button>
        </div>

        <template v-if="currentState.submitted">
          <el-alert
            :type="currentState.correct ? 'success' : 'error'"
            :title="currentState.correct ? '回答正确！' : `回答错误，正确答案：${answerText(currentState.answer)}`"
            :closable="false"
            class="result-alert"
          />
          <div class="analysis-box" :class="currentState.correct ? 'correct-bg' : 'wrong-bg'">
            <b>解析：</b>
            <span v-html="currentState.analysis || '暂无解析'"></span>
          </div>
        </template>
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { fetchPracticeQuestions, submitPractice } from '@/api/practice'
import type { PracticeMode, Question } from '@/types'
import { useBankStore } from '@/stores/bank'
import {
  answerText,
  difficultyName,
  difficultyTagType,
  optionLabel,
  questionTypeName
} from '@/utils/format'

const bankStore = useBankStore()

/** 科目名称按当前题库的科目定义解析 */
function subjectName(code?: string) {
  return bankStore.subjectName(code)
}

interface QuestionState {
  selected: number[]
  submitted: boolean
  correct?: boolean
  answer?: number[]
  analysis?: string
}

const loading = ref(false)
const submitting = ref(false)
const questions = ref<Question[]>([])
const currentIndex = ref(0)
const states = reactive<Record<number, QuestionState>>({})

const query = reactive<{
  subject: string
  chapter: string
  type: string
  difficulty: number | ''
  mode: PracticeMode
  limit: number
}>({
  subject: '',
  chapter: '',
  type: '',
  difficulty: '',
  mode: 'sequence',
  limit: 10
})

const current = computed<Question | undefined>(() => questions.value[currentIndex.value])

const currentState = computed<QuestionState>(() => {
  const q = current.value
  if (!q) return { selected: [], submitted: false }
  if (!states[q.id]) {
    states[q.id] = { selected: [], submitted: false }
  }
  return states[q.id]
})

function setSingle(val: string | number | boolean | undefined) {
  if (typeof val === 'number') {
    currentState.value.selected = [val]
  }
}

function setMulti(val: Array<string | number>) {
  currentState.value.selected = val.map(Number)
}

function optionClass(index: number): string {
  const st = currentState.value
  if (!st.submitted) return ''
  if (st.answer?.includes(index)) return 'option-right'
  if (st.selected.includes(index)) return 'option-wrong'
  return ''
}

async function loadQuestions() {
  loading.value = true
  try {
    const list = await fetchPracticeQuestions({
      bankCode: bankStore.currentCode,
      subject: query.subject,
      chapter: query.chapter,
      type: query.type,
      difficulty: query.difficulty,
      mode: query.mode,
      limit: query.limit
    })
    questions.value = list
    currentIndex.value = 0
    Object.keys(states).forEach((k) => delete states[Number(k)])
  } catch {
    // 拉题失败时拦截器已给出原因
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  const q = current.value
  const st = currentState.value
  if (!q || st.selected.length === 0) return
  submitting.value = true
  try {
    const res = await submitPractice({
      questionId: q.id,
      userAnswer: st.selected,
      mode: query.mode
    })
    st.submitted = true
    st.correct = res.correct
    st.answer = res.answer
    st.analysis = res.analysis
  } catch {
    // 提交判分失败，具体原因拦截器已提示
  } finally {
    submitting.value = false
  }
}

onMounted(loadQuestions)
</script>

<style scoped lang="scss">
.filter-card {
  margin-bottom: 16px;
}

.progress-line {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
  font-size: 15px;
  color: #606266;
}

.line-progress {
  flex: 1;
}

.question-card {
  min-height: 320px;
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

.option-right {
  background: #f0f9eb !important;
}

.option-wrong {
  background: #fef0f0 !important;
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

.result-alert {
  margin-top: 16px;
}
</style>
