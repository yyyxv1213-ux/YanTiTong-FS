<template>
  <div class="page">
    <h2 class="page-title">用户管理</h2>

    <el-card>
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="用户名 / 姓名" clearable style="width: 220px" @keyup.enter="handleSearch" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button type="success" @click="openEdit()">新增用户</el-button>
      </div>

      <el-table :data="users" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="130" />
        <el-table-column prop="email" label="邮箱" min-width="190">
          <template #default="{ row }">{{ row.email || '-' }}</template>
        </el-table-column>
        <el-table-column prop="realName" label="姓名" min-width="110">
          <template #default="{ row }">{{ row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column label="角色" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r" :type="r === 'ADMIN' ? 'danger' : 'primary'" class="role-tag">
              {{ roleNameMap[r] ?? r }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="380" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openEdit(row)">编辑</el-button>
            <el-button size="small" plain @click="openResetPwd(row)">重置密码</el-button>
            <el-button size="small" plain @click="openRoles(row)">分配角色</el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              plain
              :disabled="row.id === 1"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" plain :disabled="row.id === 1" @click="handleDelete(row)">删除</el-button>
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
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </el-card>

    <!-- 新增 / 编辑用户 -->
    <el-dialog v-model="editVisible" :title="editForm.id ? '编辑用户' : '新增用户'" width="520px" @closed="resetEditForm">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" :disabled="!!editForm.id" placeholder="4-20位字母数字下划线" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="必填，用于邮箱登录" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="editForm.realName" placeholder="选填" />
        </el-form-item>
        <el-form-item label="密码" :prop="editForm.id ? '' : 'password'">
          <el-input
            v-model="editForm.password"
            type="password"
            show-password
            :placeholder="editForm.id ? '留空则不修改密码' : '6-20位密码'"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色" prop="roleCodes">
          <el-checkbox-group v-model="editForm.roleCodes">
            <el-checkbox v-for="r in roles" :key="r.code" :value="r.code">{{ r.name }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="pwdVisible" :title="`重置密码 - ${currentUser?.username}`" width="440px">
      <el-form label-width="90px">
        <el-form-item label="新密码" required>
          <el-input v-model="newPassword" type="password" show-password placeholder="6-20位新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleResetPwd">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色 -->
    <el-dialog v-model="rolesVisible" :title="`分配角色 - ${currentUser?.username}`" width="440px">
      <el-checkbox-group v-model="assignRoleCodes" class="roles-group">
        <el-checkbox v-for="r in roles" :key="r.code" :value="r.code">{{ r.name }}（{{ r.code }}）</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="rolesVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleAssignRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import dayjs from 'dayjs'
import {
  assignUserRoles,
  createUser,
  deleteUser,
  listRoles,
  listUsers,
  resetUserPassword,
  updateUser,
  updateUserStatus
} from '@/api/admin'
import type { Role, SysUser } from '@/types'

const loading = ref(false)
const saving = ref(false)
const users = ref<SysUser[]>([])
const total = ref(0)
const roles = ref<Role[]>([])

const query = reactive({
  keyword: '',
  status: '' as number | '',
  pageNum: 1,
  pageSize: 10
})

const roleNameMap = computed<Record<string, string>>(() => {
  const map: Record<string, string> = {}
  roles.value.forEach((r) => {
    map[r.code] = r.name
  })
  return map
})

function formatTime(t: string): string {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'
}

/* ---------- 列表 ---------- */

async function loadUsers() {
  loading.value = true
  try {
    const res = await listUsers(query)
    users.value = res.records
    total.value = res.total
  } catch {
    // 错误信息已由拦截器提示
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadUsers()
}

/* ---------- 新增 / 编辑 ---------- */

const editVisible = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive({
  id: 0,
  username: '',
  email: '',
  realName: '',
  password: '',
  status: 1,
  roleCodes: [] as string[]
})

const editRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^\w{4,20}$/, message: '用户名为4-20位字母、数字或下划线', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ],
  roleCodes: [
    {
      validator: (_rule, value: string[], callback) => {
        if (!value || value.length === 0) {
          callback(new Error('请至少选择一个角色'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

function openEdit(row?: SysUser) {
  if (row) {
    editForm.id = row.id
    editForm.username = row.username
    editForm.email = row.email || ''
    editForm.realName = row.realName || ''
    editForm.password = ''
    editForm.status = row.status
    editForm.roleCodes = [...row.roles]
  }
  editVisible.value = true
}

function resetEditForm() {
  editForm.id = 0
  editForm.username = ''
  editForm.email = ''
  editForm.realName = ''
  editForm.password = ''
  editForm.status = 1
  editForm.roleCodes = []
  editFormRef.value?.clearValidate()
}

async function handleSave() {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (editForm.id && editForm.password && (editForm.password.length < 6 || editForm.password.length > 20)) {
    ElMessage.warning('密码长度为6-20位')
    return
  }
  saving.value = true
  try {
    if (editForm.id) {
      await updateUser(editForm.id, {
        username: editForm.username,
        email: editForm.email.trim(),
        realName: editForm.realName || undefined,
        status: editForm.status,
        roleCodes: editForm.roleCodes,
        ...(editForm.password ? { password: editForm.password } : {})
      })
      ElMessage.success('用户已更新')
    } else {
      await createUser({
        username: editForm.username,
        email: editForm.email.trim(),
        realName: editForm.realName || undefined,
        password: editForm.password,
        status: editForm.status,
        roleCodes: editForm.roleCodes
      })
      ElMessage.success('用户已创建')
    }
    editVisible.value = false
    loadUsers()
  } catch {
    // 保存失败时弹窗保持打开，具体原因拦截器已提示
  } finally {
    saving.value = false
  }
}

/* ---------- 重置密码 ---------- */

const pwdVisible = ref(false)
const newPassword = ref('')
const currentUser = ref<SysUser | null>(null)

function openResetPwd(row: SysUser) {
  currentUser.value = row
  newPassword.value = ''
  pwdVisible.value = true
}

async function handleResetPwd() {
  if (!currentUser.value) return
  if (newPassword.value.length < 6 || newPassword.value.length > 20) {
    ElMessage.warning('密码长度为6-20位')
    return
  }
  saving.value = true
  try {
    await resetUserPassword(currentUser.value.id, newPassword.value)
    ElMessage.success('密码已重置')
    pwdVisible.value = false
  } catch {
    // 错误信息已由拦截器提示
  } finally {
    saving.value = false
  }
}

/* ---------- 分配角色 ---------- */

const rolesVisible = ref(false)
const assignRoleCodes = ref<string[]>([])

function openRoles(row: SysUser) {
  currentUser.value = row
  assignRoleCodes.value = [...row.roles]
  rolesVisible.value = true
}

async function handleAssignRoles() {
  const u = currentUser.value
  if (!u) return
  if (assignRoleCodes.value.length === 0) {
    ElMessage.warning('请至少选择一个角色')
    return
  }
  saving.value = true
  try {
    // 后端复用完整校验：必须携带该用户当前的 username/realName/status
    await assignUserRoles(u.id, {
      username: u.username,
      email: u.email,
      realName: u.realName || undefined,
      status: u.status,
      roleCodes: assignRoleCodes.value
    })
    ElMessage.success('角色已更新')
    rolesVisible.value = false
    loadUsers()
  } catch {
    // 错误信息已由拦截器提示
  } finally {
    saving.value = false
  }
}

/* ---------- 状态与删除 ---------- */

async function toggleStatus(row: SysUser) {
  const target = row.status === 1 ? 0 : 1
  if (target === 0) {
    try {
      await ElMessageBox.confirm(`确定禁用用户「${row.username}」吗？禁用后该用户将无法登录。`, '禁用确认', {
        confirmButtonText: '禁用',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch {
      return
    }
  }
  try {
    await updateUserStatus(row.id, target)
    ElMessage.success(target === 1 ? '已启用' : '已禁用')
    loadUsers()
  } catch {
    // 错误信息已由拦截器提示
  }
}

async function handleDelete(row: SysUser) {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？该操作不可恢复。`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'error'
    })
  } catch {
    return
  }
  try {
    await deleteUser(row.id)
    ElMessage.success('用户已删除')
    if (users.value.length === 1 && query.pageNum > 1) {
      query.pageNum--
    }
    loadUsers()
  } catch {
    // 错误信息已由拦截器提示
  }
}

onMounted(async () => {
  loadUsers()
  try {
    roles.value = await listRoles()
  } catch {
    // 错误信息已由拦截器提示
  }
})
</script>

<style scoped lang="scss">
.role-tag {
  margin-right: 6px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.roles-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
