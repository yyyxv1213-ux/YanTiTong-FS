<template>
  <el-container class="layout">
    <el-aside width="230px" class="aside">
      <div class="logo">
        <span class="logo-name">研题通</span>
        <span class="logo-sub">考研408刷题训练系统</span>
      </div>
      <el-menu :default-active="route.path" router class="menu">
        <el-menu-item index="/student">
          <el-icon><Odometer /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/student/practice">
          <el-icon><EditPen /></el-icon>
          <span>刷题训练</span>
        </el-menu-item>
        <el-menu-item index="/student/exam">
          <el-icon><Timer /></el-icon>
          <span>模拟考试</span>
        </el-menu-item>
        <el-menu-item index="/student/wrong">
          <el-icon><Notebook /></el-icon>
          <span>错题本</span>
        </el-menu-item>
        <el-menu-item index="/student/stats">
          <el-icon><DataAnalysis /></el-icon>
          <span>学习统计</span>
        </el-menu-item>
        <el-menu-item index="/student/plan">
          <el-icon><Calendar /></el-icon>
          <span>学习计划</span>
        </el-menu-item>
        <el-menu-item index="/student/profile">
          <el-icon><User /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/admin">
          <el-icon><Setting /></el-icon>
          <span>进入管理后台</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <div class="header-title">{{ route.meta.title || '研题通' }}</div>
          <div class="bank-switch">
            <span class="bank-label">题库：</span>
            <el-select
              v-model="bankStore.currentCode"
              class="bank-select"
              size="large"
              @change="onBankChange"
            >
              <el-option
                v-for="b in bankStore.banks"
                :key="b.code"
                :label="b.name"
                :value="b.code"
              />
            </el-select>
          </div>
        </div>
        <el-dropdown @command="handleCommand">
          <span class="user-dropdown">
            <el-icon class="avatar"><UserFilled /></el-icon>
            {{ userStore.userInfo?.realName || userStore.userInfo?.username }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view :key="bankStore.currentCode" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Odometer,
  EditPen,
  Timer,
  Notebook,
  DataAnalysis,
  Calendar,
  User,
  UserFilled,
  ArrowDown,
  Setting
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useBankStore } from '@/stores/bank'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const bankStore = useBankStore()

/** 切换题库：持久化选择并通过 router-view 的 key 让当前页面按新题库重新加载 */
function onBankChange(code: string) {
  bankStore.setBank(code)
}

bankStore.loadBanks()

async function handleCommand(command: string) {
  if (command === 'profile') {
    router.push('/student/profile')
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch {
      return
    }
    await userStore.logout()
    router.replace('/login')
  }
}
</script>

<style scoped lang="scss">
.layout {
  height: 100vh;
}

.aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.logo {
  padding: 22px 16px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  border-bottom: 1px solid #f0f0f0;
}

.logo-name {
  font-size: 24px;
  font-weight: 700;
  color: #4f5dff;
  letter-spacing: 2px;
}

.logo-sub {
  font-size: 12px;
  color: #909399;
}

.menu {
  border-right: none;
  flex: 1;
}

.header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
}

.bank-switch {
  display: flex;
  align-items: center;
}

.bank-label {
  font-size: 16px;
  color: #606266;
  margin-right: 4px;
}

.bank-select {
  width: 260px;

  :deep(.el-input__wrapper) {
    padding: 2px 14px;
  }

  :deep(.el-input__inner) {
    font-size: 16px;
    font-weight: 600;
    color: #4f5dff;
  }
}

.user-dropdown {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 16px;
  color: #303133;
  outline: none;
}

.avatar {
  font-size: 22px;
  color: #4f5dff;
}

.main {
  background: #f5f6fa;
  padding: 0;
}
</style>
