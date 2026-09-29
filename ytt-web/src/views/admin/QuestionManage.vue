<template>
  <div class="page">
    <h2 class="page-title">题目管理</h2>

    <el-card>
      <div class="filter-bar">
        <el-select v-model="query.bankCode" placeholder="题库" style="width: 210px" @change="onFilterBankChange">
          <el-option v-for="b in bankStore.banks" :key="b.code" :label="b.name" :value="b.code" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="题干关键字" clearable style="width: 200px" @keyup.enter="handleSearch" />
        <el-select v-model="query.subject" placeholder="科目" clearable style="width: 170px" @change="onFilterSubjectChange">
          <el-option v-for="s in bankStore.subjectOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-select v-model="query.categoryId" placeholder="分类" clearable style="width: 160px">
          <el-option v-for="c in filterCategories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="query.type" placeholder="题型" clearable style="width: 130px">
          <el-option label="单选题" value="single" />
          <el-option label="多选题" value="multi" />
          <el-option label="判断题" value="judge" />
        </el-select>
        <el-select v-model="query.difficulty" placeholder="难度" clearable style="width: 120px">
          <el-option label="简单" :value="1" />
          <el-option label="中等" :value="2" />
          <el-option label="困难" :value="3" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button type="success" @click="openEdit()">新增题目</el-button>
      </div>

      <el-table :data="questions" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="题型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ questionTypeName(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="科目" width="140">
          <template #default="{ row }">{{ subjectName(row.subject) }}</template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="120">
          <template #default="{ row }">{{ row.categoryName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="chapter" label="章节" width="130">
          <template #default="{ row }">{{ row.chapter || '-' }}</template>
        </el-table-column>
        <el-table-column label="难度" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="difficultyTagType(row.difficulty)">{{ difficultyName(row.difficulty) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="题干" min-width="280">
          <template #default="{ row }">{{ truncate(row.stem, 60) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadQuestions"
          @current-change="loadQuestions"
        />
      </div>
    </el-card>

    <!-- 新增 / 编辑题目 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑题目' : '新增题目'" width="70%" top="4vh" @closed="resetForm">
      <el-form label-width="90px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="题型" required>
              <el-radio-group v-model="form.type" @change="onTypeChange">
                <el-radio value="single">单选题</el-radio>
                <el-radio value="multi">多选题</el-radio>
                <el-radio value="judge">判断题</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="科目" required>
              <el-select v-model="form.subject" style="width: 100%" @change="onFormSubjectChange">
                <el-option v-for="s in bankStore.subjectOptions" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分类" required>
              <el-select v-model="form.categoryId" style="width: 100%" placeholder="先选择科目">
                <el-option v-for="c in formCategories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="章节">
              <el-input v-model="form.chapter" placeholder="如：栈与队列" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="难度" required>
              <el-radio-group v-model="form.difficulty">
                <el-radio-button :value="1">简单</el-radio-button>
                <el-radio-button :value="2">中等</el-radio-button>
                <el-radio-button :value="3">困难</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="题干" required>
          <el-input
            v-model="form.stem"
            type="textarea"
            :rows="4"
            placeholder="支持 HTML 富文本标签，如 <b>加粗</b>、<code>代码</code>"
          />
        </el-form-item>

        <el-form-item label="选项" required>
          <div class="options-editor">
            <div v-for="(opt, i) in form.options" :key="i" class="option-row">
              <span class="option-letter">{{ optionLabel(i) }}</span>
              <el-input v-model="form.options[i]" :disabled="form.type === 'judge'" placeholder="选项内容" />
              <el-button
                v-if="form.type !== 'judge'"
                type="danger"
                plain
                :disabled="form.options.length <= 2"
                @click="removeOption(i)"
              >
                删除
              </el-button>
            </div>
            <el-button v-if="form.type !== 'judge'" size="small" plain @click="addOption">添加选项</el-button>
            <span v-else class="judge-tip">判断题选项固定为「正确 / 错误」</span>
          </div>
        </el-form-item>

        <el-form-item label="正确答案" required>
          <el-radio-group
            v-if="form.type !== 'multi'"
            :model-value="form.answer[0]"
            @update:model-value="setSingleAnswer"
          >
            <el-radio v-for="(_opt, i) in form.options" :key="i" :value="i" class="answer-choice">
              {{ optionLabel(i) }}
            </el-radio>
          </el-radio-group>
          <el-checkbox-group v-else v-model="form.answer">
            <el-checkbox v-for="(_opt, i) in form.options" :key="i" :value="i" class="answer-choice">
              {{ optionLabel(i) }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" :rows="3" placeholder="答案解析（选填，支持 HTML）" />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="form.source" placeholder="如：2023年408统考真题（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createQuestion,
  deleteQuestion,
  getQuestion,
  listCategories,
  listQuestions,
  updateQuestion
} from '@/api/admin'
import type { Category, Question, QuestionType, Subject } from '@/types'
import { useBankStore, DEFAULT_BANK_CODE } from '@/stores/bank'
import {
  difficultyName,
  difficultyTagType,
  optionLabel,
  questionTypeName,
  truncate
} from '@/utils/format'

const bankStore = useBankStore()
bankStore.loadBanks()

/** 科目名称按题库科目定义解析 */
function subjectName(code?: string) {
  return bankStore.subjectName(code)
}

const loading = ref(false)
const saving = ref(false)
const questions = ref<Question[]>([])
const total = ref(0)
const filterCategories = ref<Category[]>([])

const query = reactive({
  bankCode: DEFAULT_BANK_CODE,
  keyword: '',
  subject: '',
  categoryId: '' as number | '',
  type: '',
  difficulty: '' as number | '',
  pageNum: 1,
  pageSize: 10
})

/** 切换题库：清空科目/分类筛选并重新查询 */
async function onFilterBankChange() {
  query.subject = ''
  query.categoryId = ''
  filterCategories.value = []
  handleSearch()
}

async function loadQuestions() {
  loading.value = true
  try {
    const res = await listQuestions(query)
    questions.value = res.records
    total.value = res.total
  } catch {
    // 错误信息已由拦截器提示
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadQuestions()
}

async function onFilterSubjectChange() {
  query.categoryId = ''
  filterCategories.value = query.subject
    ? await listCategories(query.bankCode, query.subject as Subject).catch(() => [])
    : []
}

/* ---------- 编辑对话框 ---------- */

const editVisible = ref(false)
const formCategories = ref<Category[]>([])

const form = reactive({
  id: 0,
  bankCode: DEFAULT_BANK_CODE,
  type: 'single' as QuestionType,
  subject: 'ds' as Subject,
  categoryId: 0,
  chapter: '',
  difficulty: 2,
  stem: '',
  options: ['', ''] as string[],
  answer: [] as number[],
  analysis: '',
  source: ''
})

/** 当前题库的第一个科目编码（题库列表未加载时由 store 回退到考研408 科目） */
function firstSubjectOf(bankCode: string): Subject {
  const bank = bankStore.banks.find((b) => b.code === bankCode)
  const code = bank?.subjects[0]?.code ?? bankStore.subjectOptions[0]?.value ?? ''
  return code as Subject
}

function resetForm() {
  form.id = 0
  form.type = 'single'
  form.bankCode = query.bankCode || DEFAULT_BANK_CODE
  form.subject = firstSubjectOf(form.bankCode)
  form.categoryId = 0
  form.chapter = ''
  form.difficulty = 2
  form.stem = ''
  form.options = ['', '']
  form.answer = []
  form.analysis = ''
  form.source = ''
  formCategories.value = []
}

async function loadFormCategories(bankCode: string, subject: Subject) {
  formCategories.value = await listCategories(bankCode, subject).catch(() => [])
}

async function onFormSubjectChange() {
  form.categoryId = 0
  await loadFormCategories(form.bankCode, form.subject)
}

function onTypeChange() {
  // 切换题型时清空已选答案
  form.answer = []
  if (form.type === 'judge') {
    form.options = ['正确', '错误']
  } else if (form.options.length < 2 || form.options.every((o) => o === '正确' || o === '错误')) {
    form.options = ['', '']
  }
}

function addOption() {
  form.options.push('')
}

function removeOption(index: number) {
  form.options.splice(index, 1)
  form.answer = form.answer.filter((a) => a !== index).map((a) => (a > index ? a - 1 : a))
}

function setSingleAnswer(val: string | number | boolean | undefined) {
  form.answer = typeof val === 'number' ? [val] : []
}

async function openEdit(row?: Question) {
  editVisible.value = true
  if (row) {
    try {
      const q = await getQuestion(row.id)
      form.id = q.id
      form.bankCode = q.bankCode || query.bankCode
      form.type = q.type
      form.subject = q.subject
      form.categoryId = q.categoryId
      form.chapter = q.chapter || ''
      form.difficulty = q.difficulty
      form.stem = q.stem
      form.options = [...(q.options ?? [])]
      form.answer = [...(q.answer ?? [])]
      form.analysis = q.analysis || ''
      form.source = q.source || ''
      await loadFormCategories(form.bankCode, q.subject)
    } catch {
      editVisible.value = false
    }
  } else {
    form.bankCode = query.bankCode || DEFAULT_BANK_CODE
    form.subject = firstSubjectOf(form.bankCode)
    await loadFormCategories(form.bankCode, form.subject)
  }
}

async function handleSave() {
  if (!form.stem.trim()) {
    ElMessage.warning('请输入题干')
    return
  }
  if (!form.categoryId) {
    ElMessage.warning('请选择分类')
    return
  }
  if (form.options.length < 2 || form.options.some((o) => !o.trim())) {
    ElMessage.warning('至少需要2个选项且选项内容不能为空')
    return
  }
  if (form.answer.length === 0) {
    ElMessage.warning('请选择正确答案')
    return
  }
  if (form.type !== 'multi' && form.answer.length !== 1) {
    ElMessage.warning('单选题 / 判断题只能选择一个答案')
    return
  }
  if (form.answer.some((a) => a < 0 || a >= form.options.length)) {
    ElMessage.warning('答案超出选项范围')
    return
  }
  saving.value = true
  const body = {
    bankCode: form.bankCode,
    type: form.type,
    subject: form.subject,
    categoryId: form.categoryId,
    chapter: form.chapter,
    difficulty: form.difficulty,
    stem: form.stem,
    options: form.options,
    answer: [...form.answer].sort((a, b) => a - b),
    analysis: form.analysis || undefined,
    source: form.source || undefined
  }
  try {
    if (form.id) {
      await updateQuestion(form.id, body)
      ElMessage.success('题目已更新')
    } else {
      await createQuestion(body)
      ElMessage.success('题目已创建')
    }
    editVisible.value = false
    loadQuestions()
  } catch {
    // 错误信息已由拦截器提示
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: Question) {
  try {
    await ElMessageBox.confirm(`确定删除题目 #${row.id} 吗？该操作不可恢复。`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'error'
    })
  } catch {
    return
  }
  try {
    await deleteQuestion(row.id)
    ElMessage.success('题目已删除')
    if (questions.value.length === 1 && query.pageNum > 1) {
      query.pageNum--
    }
    loadQuestions()
  } catch {
    // 错误信息已由拦截器提示
  }
}

onMounted(loadQuestions)
</script>

<style scoped lang="scss">
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.options-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.option-letter {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #edeeff;
  color: #4f5dff;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.judge-tip {
  font-size: 13px;
  color: #909399;
}

.answer-choice {
  margin-right: 18px;
}
</style>
