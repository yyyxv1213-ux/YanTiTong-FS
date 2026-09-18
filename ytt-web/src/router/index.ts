import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { isTokenExpired } from '@/utils/token'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/student',
    component: () => import('@/layouts/StudentLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Dashboard', component: () => import('@/views/student/Dashboard.vue'), meta: { title: '仪表盘' } },
      { path: 'practice', name: 'Practice', component: () => import('@/views/student/Practice.vue'), meta: { title: '刷题训练' } },
      { path: 'exam', name: 'Exam', component: () => import('@/views/student/Exam.vue'), meta: { title: '模拟考试' } },
      { path: 'wrong', name: 'Wrong', component: () => import('@/views/student/Wrong.vue'), meta: { title: '错题本' } },
      { path: 'stats', name: 'Stats', component: () => import('@/views/student/Stats.vue'), meta: { title: '学习统计' } },
      { path: 'plan', name: 'Plan', component: () => import('@/views/student/Plan.vue'), meta: { title: '学习计划' } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/student/Profile.vue'), meta: { title: '个人中心' } }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'] },
    children: [
      { path: '', redirect: '/admin/users' },
      { path: 'users', name: 'UserManage', component: () => import('@/views/admin/UserManage.vue'), meta: { title: '用户管理' } },
      { path: 'questions', name: 'QuestionManage', component: () => import('@/views/admin/QuestionManage.vue'), meta: { title: '题目管理' } },
      { path: 'categories', name: 'CategoryManage', component: () => import('@/views/admin/CategoryManage.vue'), meta: { title: '分类管理' } }
    ]
  },
  { path: '/', redirect: '/student' },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  const isLogin = userStore.isLogin

  // 本地 Token 已过期：清理会话并要求重新登录
  if (isLogin && isTokenExpired(userStore.accessToken)) {
    userStore.sessionExpired()
    return '/login'
  }

  if (!isLogin && to.path !== '/login' && to.path !== '/register') {
    return '/login'
  }
  if (isLogin && (to.path === '/login' || to.path === '/register')) {
    return userStore.isAdmin ? '/admin' : '/student'
  }
  const needRoles = (to.meta.roles as string[] | undefined) ?? []
  if (needRoles.includes('ADMIN') && !userStore.isAdmin) {
    ElMessage.warning('无权限访问管理后台')
    return '/student'
  }
  if (to.meta.title) {
    document.title = `${to.meta.title} - 研题通·考研408刷题训练系统`
  }
  return true
})

export default router
