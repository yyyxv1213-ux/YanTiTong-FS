<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <div class="auth-header">
        <h1 class="auth-title">研题通</h1>
        <p class="auth-subtitle">考研408刷题训练系统</p>
      </div>

      <el-tabs v-model="loginType" class="login-tabs" stretch>
        <!-- 密码登录 -->
        <el-tab-pane label="密码登录" name="password">
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" @keyup.enter="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="pwdForm.username" placeholder="用户名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="pwdForm.password" type="password" show-password placeholder="密码" :prefix-icon="Lock" />
            </el-form-item>
            <el-form-item>
              <el-button class="auth-btn" type="primary" :loading="loading" @click="handleLogin">
                登 录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 邮箱验证码登录 -->
        <el-tab-pane label="验证码登录" name="emailCode">
          <el-form ref="codeFormRef" :model="codeForm" :rules="codeRules" @keyup.enter="handleLogin">
            <el-form-item prop="email">
              <el-input v-model="codeForm.email" placeholder="邮箱" :prefix-icon="Message" />
            </el-form-item>
            <el-form-item prop="code">
              <div class="code-row">
                <el-input v-model="codeForm.code" placeholder="6 位验证码" maxlength="6" :prefix-icon="Key" />
                <el-button class="code-btn" :disabled="countdown > 0" @click="handleSendCode">
                  {{ countdown > 0 ? `${countdown} 秒后重发` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button class="auth-btn" type="primary" :loading="loading" @click="handleLogin">
                登 录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="auth-footer">
        <span>还没有账号？</span>
        <el-link type="primary" @click="router.push('/register')">立即注册</el-link>
      </div>
      <el-divider class="demo-divider">演示账号</el-divider>
      <div class="demo-accounts">
        <p>管理员：admin / 123456</p>
        <p>学　员：study / 123456</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Message, Lock, Key } from '@element-plus/icons-vue'
import { emailLoginApi, loginApi, sendEmailCodeApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

/** 当前登录方式：密码 / 邮箱验证码 */
const loginType = ref<'password' | 'emailCode'>('password')
const loading = ref(false)

const pwdFormRef = ref<FormInstance>()
const codeFormRef = ref<FormInstance>()

const pwdForm = reactive({ username: '', password: '' })
const codeForm = reactive({ email: '', code: '' })

const emailRule = [
  { required: true, message: '请输入邮箱', trigger: 'blur' },
  { type: 'email' as const, message: '邮箱格式不正确', trigger: 'blur' }
]

const pwdRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const codeRules: FormRules = {
  email: emailRule,
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为 6 位数字', trigger: 'blur' }
  ]
}

/* ---------- 发送验证码与倒计时 ---------- */
const countdown = ref(0)
let timer: number | undefined

function startCountdown() {
  countdown.value = 60
  timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0 && timer) {
      window.clearInterval(timer)
      timer = undefined
    }
  }, 1000)
}

async function handleSendCode() {
  // 仅校验邮箱字段，通过后才请求后端
  const valid = await codeFormRef.value
    ?.validateField('email')
    .then(() => true)
    .catch(() => false)
  if (!valid) {
    ElMessage.warning('请先填写正确的邮箱')
    return
  }
  try {
    await sendEmailCodeApi(codeForm.email.trim())
    ElMessage.success('验证码已发送，请查收邮件（5 分钟内有效）')
    startCountdown()
  } catch {
    // 错误信息（未注册/发送过频等）已由拦截器提示
  }
}

/* ---------- 登录提交 ---------- */
async function handleLogin() {
  if (loginType.value === 'password') {
    const valid = await pwdFormRef.value?.validate().catch(() => false)
    if (!valid) return
    loading.value = true
    try {
      const res = await loginApi({ username: pwdForm.username.trim(), password: pwdForm.password })
      afterLoginSuccess(res)
    } catch {
      // 用户名或密码错误时，具体提示由拦截器给出
    } finally {
      loading.value = false
    }
  } else {
    const valid = await codeFormRef.value?.validate().catch(() => false)
    if (!valid) return
    loading.value = true
    try {
      const res = await emailLoginApi({ email: codeForm.email.trim(), code: codeForm.code.trim() })
      afterLoginSuccess(res)
    } catch {
      // 错误信息已由拦截器提示
    } finally {
      loading.value = false
    }
  }
}

/** 登录成功通用处理：保存会话 -> 欢迎提示 -> 按角色跳转 */
function afterLoginSuccess(res: Awaited<ReturnType<typeof loginApi>>) {
  userStore.setLogin(res)
  ElMessage.success(`欢迎回来，${res.user.realName || res.user.username}`)
  router.replace(userStore.isAdmin ? '/admin' : '/student')
}

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
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
  margin-bottom: 12px;
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

.login-tabs {
  margin-top: 4px;
}

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;

  .code-btn {
    flex-shrink: 0;
    white-space: nowrap;
  }
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

.demo-divider {
  font-size: 14px;
}

.demo-accounts {
  text-align: center;
  color: #909399;
  font-size: 14px;
  line-height: 1.6;

  p {
    margin: 2px 0;
  }
}
</style>
