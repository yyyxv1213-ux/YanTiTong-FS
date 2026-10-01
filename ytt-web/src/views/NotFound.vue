<template>
  <div class="not-found">
    <div class="code">404</div>
    <p class="tip">页面不存在或已被移除</p>
    <el-button type="primary" @click="goHome">返回首页</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

function goHome() {
  if (!userStore.isLogin) {
    router.replace('/login')
  } else {
    router.replace(userStore.isAdmin ? '/admin' : '/student')
  }
}
</script>

<style scoped lang="scss">
.not-found {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.code {
  font-size: 96px;
  font-weight: 700;
  color: #4f5dff;
}

.tip {
  font-size: 18px;
  color: #909399;
  margin: 0;
}
</style>
