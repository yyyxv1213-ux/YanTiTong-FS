<template>
  <el-container class="layout">
    <el-aside width="230px" class="aside">
      <div class="logo">
        <span class="logo-name">研题通</span>
        <span class="logo-sub">管理后台</span>
      </div>
      <el-menu :default-active="route.path" router class="menu">
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/questions">
          <el-icon><Collection /></el-icon>
          <span>题目管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/categories">
          <el-icon><Files /></el-icon>
          <span>分类管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-title">{{ route.meta.title || '管理后台' }}</div>
        <el-dropdown @command="handleCommand">
          <span class="user-dropdown">
            <el-icon class="avatar"><UserFilled /></el-icon>
            {{ userStore.userInfo?.realName || userStore.userInfo?.username }}
            <el-tag size="small" type="danger" class="admin-tag">管理员</el-tag>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="student">返回学员端</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { User, UserFilled, ArrowDown, Collection, Files } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

async function handleCommand(command: string) {
  if (command === 'student') {
    router.push('/student')
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

.header-title {
  font-size: 18px;
  font-weight: 600;
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

.admin-tag {
  margin-left: 4px;
}

.main {
  background: #f5f6fa;
  padding: 0;
}
</style>
