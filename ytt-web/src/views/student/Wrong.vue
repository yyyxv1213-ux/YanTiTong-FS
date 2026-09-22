<template>
  <div class="page">
    <h2 class="page-title">错题本</h2>

    <el-empty v-if="!loading && list.length === 0" description="暂无未掌握的错题，继续保持！" />

    <el-card v-for="item in list" :key="item.id" class="wrong-card">
      <div class="w-head">
        <el-tag>{{ questionTypeName(item.question.type) }}</el-tag>
        <el-tag type="info" effect="plain">{{ subjectName(item.question.subject) }}</el-tag>
        <el-tag v-if="item.question.chapter" type="info" effect="plain">{{ item.question.chapter }}</el-tag>
        <el-tag type="danger" effect="plain">错误 {{ item.wrongCount }} 次</el-tag>
        <span class="w-time">最近作答：{{ answerText(item.lastAnswer) }}</span>
      </div>
      <div class="question-stem" v-html="item.question.stem"></div>

      <!-- 重做区 -->
      <template v-if="redo[item.id]?.active && !redo[item.id]?.submitted">
        <el-radio-group
          v-if="item.question.type !== 'multi'"
          :model-value="redo[item.id]?.selected[0]"
          class="option-group"
          @update:model-value="(v: string | number | boolean | undefined) => setSingle(item.id, v)"
        >
          <el-radio v-for="(opt, i) in item.question.options" :key="i" :value="i" class="option-item">
            <span class="option-label">{{ optionLabel(i) }}.</span>
            <span v-html="opt"></span>
          </el-radio>
        </el-radio-group>
        <el-checkbox-group
          v-else
          :model-value="redo[item.id]?.selected ?? []"
          class="option-group"
          @update:model-value="(v: Array<string | number>) => setMulti(item.id, v)"
        >
          <el-checkbox v-for="(opt, i) in item.question.options" :key="i" :value="i" class="option-item">
            <span class="option-label">{{ optionLabel(i) }}.</span>
            <span v-html="opt"></span>
          </el-checkbox>
        </el-checkbox-group>
      </template>

      <template v-if="redo[item.id]?.submitted">
        <el-alert
          :type="redo[item.id]?.correct ? 'success' : 'error'"
          :title="redo[item.id]?.correct ? '重做正确！可以标记为已掌握' : `仍然错误，正确答案：${answerText(redo[item.id]?.answer)}`"
          :closable="false"
          class="result-alert"
        />
        <div class="analysis-box" :class="redo[item.id]?.correct ? 'correct-bg' : 'wrong-bg'">
          <b>解析：</b>
          <span v-html="redo[item.id]?.analysis || item.question.analysis || '暂无解析'"></span>
        </div>
      </template>

      <template v-else-if="expanded[item.id]">
        <div class="answer-line">
          <b>正确答案：</b>{{ answerText(item.question.answer) }}
        </div>
        <div class="option-review">
          <div
            v-for="(opt, i) in item.question.options"
            :key="i"
            class="option-item-static"
            :class="{ 'is-answer': item.question.answer.includes(i), 'is-last': (item.lastAnswer ?? []).includes(i) && !item.question.answer.includes(i) }"
          >
            <span class="option-label">{{ optionLabel(i) }}.</span>
            <span v-html="opt"></span>
          </div>
        </div>
        <div class="analysis-box correct-bg">
          <b>解析：</b>
          <span v-html="item.question.analysis || '暂无解析'"></span>
        </div>
      </template>

      <div class="w-actions">
        <el-button v-if="!redo[item.id]?.active" size="small" @click="toggleExpand(item.id)">
          {{ expanded[item.id] ? '收起答案' : '查看答案解析' }}
        </el-button>
        <el-button v-if="!redo[item.id]?.active" size="small" type="primary" plain @click="startRedo(item.id)">重做</el-button>
        <template v-if="redo[item.id]?.active && !redo[item.id]?.submitted">
          <el-button size="small" type="primary" :disabled="(redo[item.id]?.selected ?? []).length === 0" @click="submitRedo(item)">
            提交重做
          </el-button>
          <el-button size="small" @click="cancelRedo(item.id)">取消</el-button>
        </template>
        <el-button size="small" type="success" plain @click="handleMaster(item)">标记已掌握</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listWrongQuestions, masterWrongQuestion } from '@/api/wrong'
import { submitPractice } from '@/api/practice'
import type { WrongQuestion } from '@/types'
import { answerText, optionLabel, questionTypeName, subjectName } from '@/utils/format'

interface RedoState {
  active: boolean
  selected: number[]
  submitted: boolean
  correct?: boolean
  answer?: number[]
  analysis?: string
}

const loading = ref(false)
const list = ref<WrongQuestion[]>([])
const expanded = reactive<Record<number, boolean>>({})
const redo = reactive<Record<number, RedoState>>({})

function toggleExpand(id: number) {
  expanded[id] = !expanded[id]
}

function startRedo(id: number) {
  redo[id] = { active: true, selected: [], submitted: false }
}

function cancelRedo(id: number) {
  delete redo[id]
}

function setSingle(id: number, val: string | number | boolean | undefined) {
  if (typeof val === 'number' && redo[id]) {
    redo[id].selected = [val]
  }
}

function setMulti(id: number, val: Array<string | number>) {
  if (redo[id]) {
    redo[id].selected = val.map(Number)
  }
}

async function submitRedo(item: WrongQuestion) {
  const st = redo[item.id]
  if (!st || st.selected.length === 0) return
  try {
    const res = await submitPractice({
      questionId: item.questionId,
      userAnswer: st.selected,
      mode: 'wrong'
    })
    st.submitted = true
    st.correct = res.correct
    st.answer = res.answer
    st.analysis = res.analysis
    if (res.correct) {
      ElMessage.success('回答正确！如已掌握可点击「标记已掌握」')
    }
  } catch {
    // 判分失败的原因由拦截器弹出
  }
}

async function handleMaster(item: WrongQuestion) {
  try {
    await ElMessageBox.confirm('确定该题已经掌握了吗？标记后将从错题本移除。', '标记已掌握', {
      confirmButtonText: '标记',
      cancelButtonText: '取消',
      type: 'success'
    })
  } catch {
    return
  }
  try {
    await masterWrongQuestion(item.id)
    ElMessage.success('已标记为掌握')
    list.value = list.value.filter((w) => w.id !== item.id)
  } catch {
    // 标记失败时拦截器会提示原因，行数据保持不动
  }
}

async function load() {
  loading.value = true
  try {
    list.value = await listWrongQuestions()
  } catch {
    // 错误信息已由拦截器提示
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.wrong-card {
  margin-bottom: 16px;
}

.w-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.w-time {
  margin-left: auto;
  font-size: 13px;
  color: #909399;
}

.option-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 8px;
}

.option-item {
  display: flex;
  align-items: flex-start;
  width: 100%;
  margin-right: 0;
  padding: 8px 12px;
  border-radius: 6px;
  height: auto;

  :deep(.el-radio__label),
  :deep(.el-checkbox__label) {
    font-size: 16px;
    line-height: 1.7;
    white-space: normal;
    word-break: break-word;
  }
}

.option-review {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 4px;
}

.option-item-static {
  padding: 8px 12px;
  border-radius: 6px;
  line-height: 1.7;

  &.is-answer {
    background: #f0f9eb;
  }

  &.is-last {
    background: #fef0f0;
  }
}

.option-label {
  font-weight: 600;
  margin-right: 6px;
}

.answer-line {
  margin-bottom: 10px;
  font-size: 16px;
}

.result-alert {
  margin-top: 12px;
}

.w-actions {
  margin-top: 14px;
  display: flex;
  gap: 10px;
}
</style>
