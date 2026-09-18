import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage, ElNotification } from 'element-plus'
import type { Result, TokenResponse } from '@/types'

const TOKEN_KEY = 'ytt_access_token'
const REFRESH_KEY = 'ytt_refresh_token'
const USER_KEY = 'ytt_user_info'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

interface Waiter {
  resolve: (value: unknown) => void
  reject: (reason?: unknown) => void
  config: InternalAxiosRequestConfig
}

let refreshing = false
let waiters: Waiter[] = []

function flushWaiters(token: string | null, err?: unknown) {
  const pending = waiters
  waiters = []
  pending.forEach((w) => {
    if (token) {
      w.config.headers.Authorization = `Bearer ${token}`
      http(w.config).then(w.resolve).catch(w.reject)
    } else {
      w.reject(err)
    }
  })
}

function clearLoginState() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(USER_KEY)
}

/**
 * 会话失效统一处理：通知用户 -> 经 Store 清理全部会话数据 -> 跳转登录页。
 */
async function forceLogout(message = '登录已过期，请重新登录') {
  ElNotification.error({ title: '登录已过期', message, duration: 4000 })
  try {
    const { useUserStore } = await import('@/stores/user')
    useUserStore().clear()
  } catch {
    clearLoginState()
  }
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

/** 使用独立 axios 实例刷新 token，避免拦截器循环 */
async function doRefreshToken(): Promise<string> {
  const refreshToken = localStorage.getItem(REFRESH_KEY)
  if (!refreshToken) {
    throw new Error('无刷新令牌')
  }
  const raw = axios.create({ baseURL: '/api', timeout: 15000 })
  const res = await raw.post<Result<TokenResponse>>('/auth/refresh', { refreshToken })
  if (res.data.code !== 0 || !res.data.data) {
    throw new Error(res.data.message || '登录已过期')
  }
  const data = res.data.data
  localStorage.setItem(TOKEN_KEY, data.accessToken)
  localStorage.setItem(REFRESH_KEY, data.refreshToken)
  if (data.user) {
    localStorage.setItem(USER_KEY, JSON.stringify(data.user))
  }
  // 同步 Pinia 状态并重排过期定时器
  try {
    const { useUserStore } = await import('@/stores/user')
    useUserStore().syncTokens(data.accessToken, data.refreshToken, data.user)
  } catch {
    // Store 不可用时 localStorage 已更新，不阻塞请求
  }
  return data.accessToken
}

/**
 * access token 过期处理：尝试用 refreshToken 无感刷新并重放请求；
 * 已重试过 / 刷新接口自身 / 刷新失败时，强制登出。
 */
async function handleTokenExpired(config: InternalAxiosRequestConfig): Promise<unknown> {
  const original = config as InternalAxiosRequestConfig & { _retry?: boolean }
  const isRefreshCall = !!original.url && original.url.includes('/auth/refresh')
  if (original._retry || isRefreshCall) {
    await forceLogout()
    return Promise.reject(new Error('登录已过期'))
  }
  if (refreshing) {
    return new Promise((resolve, reject) => {
      waiters.push({ resolve, reject, config: original })
    })
  }
  refreshing = true
  original._retry = true
  try {
    const token = await doRefreshToken()
    flushWaiters(token)
    original.headers.Authorization = `Bearer ${token}`
    return await http(original)
  } catch (e) {
    flushWaiters(null, e)
    await forceLogout()
    return Promise.reject(e)
  } finally {
    refreshing = false
  }
}

http.interceptors.response.use(
  (response) => {
    const body = response.data as Result
    if (body && typeof body.code === 'number') {
      if (body.code === 0) {
        return body.data
      }
      // access token 已过期：尝试无感刷新
      if (body.code === 40102 && response.config) {
        return handleTokenExpired(response.config)
      }
      // 未登录 / Token 无效 / 已注销黑名单 / 账号被禁用：强制登出
      if ([40100, 40101, 40103, 40105].includes(body.code)) {
        void forceLogout(body.message)
        return Promise.reject(new Error(body.message || '登录状态无效'))
      }
      ElMessage.error(body.message || '操作失败')
      return Promise.reject(new Error(body.message || '操作失败'))
    }
    return response.data
  },
  async (error: AxiosError<Result>) => {
    const { response, config } = error
    if (!response) {
      ElMessage.error('网络异常，请稍后重试')
      return Promise.reject(error)
    }
    const status = response.status
    // 网关/容器层 401：同样走刷新或登出
    if (status === 401 && config) {
      return handleTokenExpired(config)
    }
    if (status === 403) {
      ElMessage.error('无权限访问')
    } else {
      const msg = response.data && response.data.message
      ElMessage.error(msg || `请求失败(${status})`)
    }
    return Promise.reject(error)
  }
)

export function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  return http.get(url, { params }) as unknown as Promise<T>
}

export function post<T>(url: string, data?: unknown): Promise<T> {
  return http.post(url, data) as unknown as Promise<T>
}

export function put<T>(url: string, data?: unknown): Promise<T> {
  return http.put(url, data) as unknown as Promise<T>
}

export function del<T>(url: string): Promise<T> {
  return http.delete(url) as unknown as Promise<T>
}

/** 过滤空值查询参数 */
export function cleanParams<T extends object>(params: T): Record<string, unknown> {
  const out: Record<string, unknown> = {}
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      out[key] = value
    }
  })
  return out
}

export default http
