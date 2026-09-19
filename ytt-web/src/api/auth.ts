import { post } from './http'
import type { EmailCodeLoginRequest, LoginRequest, RegisterRequest, TokenResponse } from '@/types'

export function loginApi(data: LoginRequest) {
  return post<TokenResponse>('/auth/login', data)
}

export function sendEmailCodeApi(email: string) {
  return post<void>('/auth/email/code', { email })
}

export function emailLoginApi(data: EmailCodeLoginRequest) {
  return post<TokenResponse>('/auth/email/login', data)
}

export function registerApi(data: RegisterRequest) {
  return post<TokenResponse>('/auth/register', data)
}

export function refreshApi(refreshToken: string) {
  return post<TokenResponse>('/auth/refresh', { refreshToken })
}

export function logoutApi() {
  return post<void>('/auth/logout')
}
