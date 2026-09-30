<template>
  <div class="page">
    <h2 class="page-title">个人中心</h2>

    <el-row :gutter="16">
      <el-col :span="10">
        <el-card v-loading="loading">
          <template #header><span class="card-header">基本信息</span></template>
          <div class="info-line">
            <span class="info-label">用户名</span>
            <span>{{ profile?.username }}</span>
          </div>
          <div class="info-line">
            <span class="info-label">姓名</span>
            <span>{{ profile?.realName || '未设置' }}</span>
          </div>
          <div class="info-line">
            <span class="info-label">角色</span>
            <el-tag v-for="r in profile?.roles ?? []" :key="r" :type="r === 'ADMIN' ? 'danger' : 'primary'" class="role-tag">
              {{ roleName(r) }}
            </el-tag>
          </div>
          <div class="info-line">
            <span class="info-label">注册时间</span>
            <span>{{ profile?.createTime ? dayjs(profile.createTime).format('YYYY-MM-DD HH:mm') : '-' }}</span>
          </div>

          <el-divider />
          <el-form label-width="70px" @submit.prevent>
            <el-form-item label="姓名">
              <el-input v-model="nameForm.realName" placeholder="请输入姓名" maxlength="20" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingName" @click="handleSaveName">保存姓名</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card>
          <template #header><span class="card-header">修改密码</span></template>
          <el-alert type="warning" :closable="false" title="修改密码成功后需要重新登录" class="pwd-alert" />
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px" class="pwd-form">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-20位新密码" />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingPwd" @click="handleChangePwd">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import dayjs from 'dayjs'
import { changePassword, getProfile, updateProfile } from '@/api/profile'
import { useUserStore } from '@/stores/user'
import type { SysUser } from '@/types'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const profile = ref<SysUser | null>(null)

const savingName = ref(false)
const nameForm = reactive({ realName: '' })

const pwdFormRef = ref<FormInstance>()
const savingPwd = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

function roleName(code: string): string {
  if (code === 'ADMIN') return '管理员'
  if (code === 'STUDENT') return '学员'
  return code
}

async function handleSaveName() {
  if (!nameForm.realName.trim()) {
    ElMessage.warning('请输入姓名')
    return
  }
  savingName.value = true
  try {
    await updateProfile({ realName: nameForm.realName.trim() })
    ElMessage.success('姓名已更新')
    await loadProfile()
    if (userStore.userInfo) {
      userStore.userInfo.realName = nameForm.realName.trim()
      localStorage.setItem('ytt_user_info', JSON.stringify(userStore.userInfo))
    }
  } catch {
    // 更新失败由拦截器统一提示
  } finally {
    savingName.value = false
  }
}

async function handleChangePwd() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  savingPwd.value = true
  try {
    await changePassword({ ...pwdForm })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.clear()
    router.replace('/login')
  } catch {
    // 错误信息已由拦截器提示
  } finally {
    savingPwd.value = false
  }
}

async function loadProfile() {
  loading.value = true
  try {
    profile.value = await getProfile()
    nameForm.realName = profile.value.realName || ''
  } catch {
    // 读取失败时表单留空，原因看拦截器提示
  } finally {
    loading.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped lang="scss">
.card-header {
  font-size: 17px;
  font-weight: 600;
}

.info-line {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  font-size: 16px;
}

.info-label {
  width: 80px;
  color: #909399;
  flex-shrink: 0;
}

.role-tag {
  margin-right: 6px;
}

.pwd-alert {
  margin-bottom: 16px;
}

.pwd-form {
  max-width: 480px;
}
</style>
