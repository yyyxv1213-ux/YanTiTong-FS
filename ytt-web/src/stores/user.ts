import { defineStore } from 'pinia'
import { ElNotification } from 'element-plus'
import type { TokenResponse, UserInfo } from '@/types'
import { getTokenExpireAt, isTokenExpired } from '@/utils/token'

const TOKEN_KEY = 'ytt_access_token'
const REFRESH_KEY = 'ytt_refresh_token'
const USER_KEY = 'ytt_user_info'

/** 过期前提前预警时间（毫秒） */
const WARN_BEFORE_MS = 60_000

function readUserInfo(): UserInfo | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as UserInfo) : null
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    accessToken: localStorage.getItem(TOKEN_KEY) || '',
    refreshToken: localStorage.getItem(REFRESH_KEY) || '',
    userInfo: readUserInfo()
  }),
  getters: {
    isLogin: (state) => !!state.accessToken,
    isAdmin: (state) => !!state.userInfo && state.userInfo.roles.includes('ADMIN')
  },
  actions: {
    setLogin(res: TokenResponse) {
      this.accessToken = res.accessToken
      this.refreshToken = res.refreshToken
      this.userInfo = res.user
      localStorage.setItem(TOKEN_KEY, res.accessToken)
      localStorage.setItem(REFRESH_KEY, res.refreshToken)
      localStorage.setItem(USER_KEY, JSON.stringify(res.user))
      this.startExpiryWatch()
    },

    /**
     * 静默刷新后同步令牌（不重置全部登录态，仅刷新 Token 与过期定时器）。
     */
    syncTokens(accessToken: string, refreshToken: string, userInfo?: UserInfo) {
      this.accessToken = accessToken
      this.refreshToken = refreshToken
      if (userInfo) {
        this.userInfo = userInfo
        localStorage.setItem(USER_KEY, JSON.stringify(userInfo))
      }
      localStorage.setItem(TOKEN_KEY, accessToken)
      localStorage.setItem(REFRESH_KEY, refreshToken)
      this.startExpiryWatch()
    },

    /**
     * 启动 Token 过期监听：
     * 到期前 1 分钟预警，到期时自动登出并跳转登录页。
     */
    startExpiryWatch() {
      stopTimers()
      const expireAt = getTokenExpireAt(this.accessToken)
      if (expireAt === null) {
        return
      }
      const ttl = expireAt - Date.now()
      if (ttl <= 0) {
        this.sessionExpired()
        return
      }
      // 临期预警
      if (ttl > WARN_BEFORE_MS) {
        const warnTimer = window.setTimeout(() => {
          ElNotification.warning({
            title: '登录即将过期',
            message: '你的登录状态将在 1 分钟后过期，请及时保存当前操作。',
            duration: WARN_BEFORE_MS
          })
        }, ttl - WARN_BEFORE_MS)
        timers.push(warnTimer)
      }
      // 到期自动登出
      const expireTimer = window.setTimeout(() => this.sessionExpired(), ttl)
      timers.push(expireTimer)
    },

    /**
     * 会话过期处理：通知用户 -> 清理全部本地会话数据 -> 跳转登录页。
     */
    sessionExpired(message = '登录已过期，请重新登录') {
      this.clear()
      ElNotification.error({ title: '登录已过期', message, duration: 4000 })
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    },

    /**
     * 应用启动时恢复会话：已过期则直接登出，否则启动过期监听。
     */
    initSession() {
      if (!this.accessToken) {
        return
      }
      if (isTokenExpired(this.accessToken)) {
        this.sessionExpired()
      } else {
        this.startExpiryWatch()
      }
    },

    /** 清理内存态、本地存储与过期定时器 */
    clear() {
      stopTimers()
      this.accessToken = ''
      this.refreshToken = ''
      this.userInfo = null
      // 统一清理本系统所有本地数据，避免残留
      Object.keys(localStorage)
        .filter((key) => key.startsWith('ytt_'))
        .forEach((key) => localStorage.removeItem(key))
    },

    /** 调后端注销接口（token 进黑名单）后清空本地登录态 */
    async logout() {
      if (this.accessToken) {
        try {
          const { logoutApi } = await import('@/api/auth')
          await logoutApi()
        } catch {
          // 注销接口失败不阻塞本地登出
        }
      }
      this.clear()
    }
  }
})

/* 过期定时器集合（模块级，不进入响应式 state） */
const timers: number[] = []

function stopTimers() {
  timers.splice(0).forEach((timer) => window.clearTimeout(timer))
}
