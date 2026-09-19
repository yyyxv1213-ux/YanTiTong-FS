<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <div class="auth-header">
        <h1 class="auth-title">研题通</h1>
        <p class="auth-subtitle">考研408刷题训练系统 · 学员注册</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleRegister">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名（4-20位字母数字下划线）" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱（必填，用于登录）" :prefix-icon="Message" />
        </el-form-item>
        <el-form-item prop="realName">
          <el-input v-model="form.realName" placeholder="姓名（选填）" :prefix-icon="Postcard" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="密码（6-20位）" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="确认密码" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-button class="auth-btn" type="primary" :loading="loading" @click="handleRegister">
            注册并登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="auth-footer">
        <span>已有账号？</span>
        <el-link type="primary" @click="router.push('/login')">返回登录</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Postcard, Message } from '@element-plus/icons-vue'
import { registerApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  username: '',
  email: '',
  realName: '',
  password: '',
  confirmPassword: ''
})

const rules: FormRules = {
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
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await registerApi({
      username: form.username,
      email: form.email.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword,
      realName: form.realName || undefined
    })
    userStore.setLogin(res)
    ElMessage.success('注册成功，已自动登录')
    router.replace('/student')
  } catch {
    // 用户名或邮箱被占用等原因，由拦截器提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #4f5dff, #16b8a6);
  padding: 20px;
}

.auth-card {
  width: 440px;
  border-radius: 12px;
  padding: 12px 16px;
}

.auth-header {
  text-align: center;
  margin-bottom: 24px;
}

.auth-title {
  font-size: 34px;
  margin: 0;
  color: #4f5dff;
  letter-spacing: 4px;
}

.auth-subtitle {
  font-size: 17px;
  color: #909399;
  margin: 8px 0 0;
}

.auth-btn {
  width: 100%;
  font-size: 17px;
}

.auth-footer {
  text-align: center;
  font-size: 15px;
  color: #606266;
}
</style>
