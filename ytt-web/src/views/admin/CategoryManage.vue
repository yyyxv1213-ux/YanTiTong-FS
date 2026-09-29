<template>
  <div class="page">
    <h2 class="page-title">分类管理</h2>

    <el-card>
      <div class="bank-bar">
        <span class="bank-bar-label">题库：</span>
        <el-select v-model="activeBank" style="width: 260px" @change="onBankChange">
          <el-option v-for="b in bankStore.banks" :key="b.code" :label="b.name" :value="b.code" />
        </el-select>
      </div>
      <div class="cat-top">
        <el-tabs v-model="activeSubject" @tab-change="loadCategories">
          <el-tab-pane v-for="s in bankStore.subjectOptions" :key="s.value" :label="s.label" :name="s.value" />
        </el-tabs>
        <el-button type="success" class="add-btn" @click="openEdit()">新增分类</el-button>
      </div>

      <el-table :data="categories" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="name" label="分类名称" min-width="220" />
        <el-table-column prop="sort" label="排序号" width="120" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑分类' : '新增分类'" width="480px" @closed="resetForm">
      <el-form label-width="90px">
        <el-form-item label="科目" required>
          <el-select v-model="form.subject" style="width: 100%">
            <el-option v-for="s in bankStore.subjectOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="分类名称，如：线性表" maxlength="30" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
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
import { createCategory, deleteCategory, listCategories, updateCategory } from '@/api/admin'
import type { Category, Subject } from '@/types'
import { useBankStore, DEFAULT_BANK_CODE } from '@/stores/bank'

const bankStore = useBankStore()
bankStore.loadBanks()

const loading = ref(false)
const saving = ref(false)
const activeBank = ref(DEFAULT_BANK_CODE)
const activeSubject = ref<Subject>('ds')
const categories = ref<Category[]>([])

const editVisible = ref(false)
const form = reactive({
  id: 0,
  bankCode: DEFAULT_BANK_CODE,
  subject: 'ds' as Subject,
  name: '',
  sort: 0
})

/** 切换题库：科目标签切到该题库第一个科目并重新加载 */
async function onBankChange() {
  activeSubject.value = (bankStore.subjectOptions[0]?.value ?? '') as Subject
  await loadCategories()
}

async function loadCategories() {
  loading.value = true
  try {
    categories.value = await listCategories(activeBank.value, activeSubject.value)
  } catch {
    // 错误信息已由拦截器提示
  } finally {
    loading.value = false
  }
}

function openEdit(row?: Category) {
  if (row) {
    form.id = row.id
    form.bankCode = row.bankCode
    form.subject = row.subject
    form.name = row.name
    form.sort = row.sort
  } else {
    form.bankCode = activeBank.value
    form.subject = activeSubject.value
  }
  editVisible.value = true
}

function resetForm() {
  form.id = 0
  form.bankCode = activeBank.value
  form.subject = activeSubject.value
  form.name = ''
  form.sort = 0
}

async function handleSave() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await updateCategory(form.id, {
        bankCode: form.bankCode,
        subject: form.subject,
        name: form.name.trim(),
        sort: form.sort
      })
      ElMessage.success('分类已更新')
    } else {
      await createCategory({
        bankCode: form.bankCode,
        subject: form.subject,
        name: form.name.trim(),
        sort: form.sort
      })
      ElMessage.success('分类已创建')
    }
    editVisible.value = false
    if (form.subject !== activeSubject.value) {
      activeSubject.value = form.subject
    }
    loadCategories()
  } catch {
    // 错误信息已由拦截器提示
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: Category) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？已被题目引用的分类无法删除。`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await deleteCategory(row.id)
    ElMessage.success('分类已删除')
    loadCategories()
  } catch {
    // 后端会提示"该分类已被题目引用"等信息，由拦截器弹出
  }
}

onMounted(loadCategories)
</script>

<style scoped lang="scss">
.bank-bar {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.bank-bar-label {
  font-size: 16px;
  color: #606266;
  margin-right: 4px;
}

.cat-top {
  position: relative;
}

.add-btn {
  position: absolute;
  right: 0;
  top: 0;
}
</style>
