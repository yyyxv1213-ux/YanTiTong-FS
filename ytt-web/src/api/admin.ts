import { cleanParams, del, get, post, put } from './http'
import type {
  Category,
  CategorySaveRequest,
  PageResult,
  Question,
  QuestionSaveRequest,
  Role,
  Subject,
  SysUser,
  UserSaveRequest
} from '@/types'

/* ---------- 用户管理 ---------- */

export interface UserQuery {
  keyword?: string
  status?: number | ''
  pageNum: number
  pageSize: number
}

export function listUsers(query: UserQuery) {
  return get<PageResult<SysUser>>('/admin/users', cleanParams(query))
}

export function getUser(id: number) {
  return get<SysUser>(`/admin/users/${id}`)
}

export function listRoles() {
  return get<Role[]>('/admin/users/roles')
}

export function createUser(data: UserSaveRequest) {
  return post<void>('/admin/users', data)
}

export function updateUser(id: number, data: UserSaveRequest) {
  return put<void>(`/admin/users/${id}`, data)
}

export function updateUserStatus(id: number, status: number) {
  return put<void>(`/admin/users/${id}/status`, { status })
}

export function resetUserPassword(id: number, password: string) {
  return put<void>(`/admin/users/${id}/password`, { password })
}

export function assignUserRoles(id: number, data: UserSaveRequest) {
  return put<void>(`/admin/users/${id}/roles`, data)
}

export function deleteUser(id: number) {
  return del<void>(`/admin/users/${id}`)
}

/* ---------- 题目管理 ---------- */

export interface QuestionQuery {
  bankCode?: string
  keyword?: string
  subject?: string
  categoryId?: number | ''
  type?: string
  difficulty?: number | ''
  pageNum: number
  pageSize: number
}

export function listQuestions(query: QuestionQuery) {
  return get<PageResult<Question>>('/admin/questions', cleanParams(query))
}

export function getQuestion(id: number) {
  return get<Question>(`/admin/questions/${id}`)
}

export function createQuestion(data: QuestionSaveRequest) {
  return post<void>('/admin/questions', data)
}

export function updateQuestion(id: number, data: QuestionSaveRequest) {
  return put<void>(`/admin/questions/${id}`, data)
}

export function deleteQuestion(id: number) {
  return del<void>(`/admin/questions/${id}`)
}

/* ---------- 分类管理 ---------- */

export function listCategories(bankCode: string, subject?: Subject | '') {
  return get<Category[]>('/admin/categories', cleanParams({ bankCode, subject }))
}

export function createCategory(data: CategorySaveRequest) {
  return post<void>('/admin/categories', data)
}

export function updateCategory(id: number, data: CategorySaveRequest) {
  return put<void>(`/admin/categories/${id}`, data)
}

export function deleteCategory(id: number) {
  return del<void>(`/admin/categories/${id}`)
}
