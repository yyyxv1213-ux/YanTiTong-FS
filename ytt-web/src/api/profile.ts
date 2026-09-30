import { get, put } from './http'
import type { PasswordChangeRequest, ProfileUpdateRequest, SysUser } from '@/types'

export function getProfile() {
  return get<SysUser>('/profile')
}

export function updateProfile(data: ProfileUpdateRequest) {
  return put<void>('/profile', data)
}

export function changePassword(data: PasswordChangeRequest) {
  return put<void>('/profile/password', data)
}
