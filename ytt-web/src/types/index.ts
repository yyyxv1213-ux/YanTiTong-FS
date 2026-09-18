/** 统一响应包 */
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页结构 */
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export interface UserInfo {
  id: number
  username: string
  email: string
  realName: string
  roles: string[]
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: UserInfo
}

export interface LoginRequest {
  username: string
  password: string
}

export interface EmailCodeLoginRequest {
  email: string
  code: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  confirmPassword: string
  realName?: string
}

export interface SysUser {
  id: number
  username: string
  email: string
  realName: string
  status: number
  version: number
  createTime: string
  updateTime: string
  roles: string[]
}

export interface Role {
  id: number
  code: string
  name: string
}

export interface UserSaveRequest {
  username: string
  email: string
  realName?: string
  password?: string
  status: number
  roleCodes: string[]
}

export type QuestionType = 'single' | 'multi' | 'judge'
export type Subject = 'ds' | 'co' | 'os' | 'cn'
export type PracticeMode = 'sequence' | 'wrong' | 'random'

/** 题库内的科目定义（由后端 exam_bank.subjects 下发） */
export interface BankSubject {
  code: string
  name: string
}

/** 题库大类（考研408、软考等） */
export interface ExamBank {
  id: number
  code: string
  name: string
  subjects: BankSubject[]
  status: number
  sort: number
}

export interface Question {
  id: number
  bankCode: string
  type: QuestionType
  subject: Subject
  categoryId: number
  chapter: string
  difficulty: number
  stem: string
  options: string[]
  answer: number[]
  analysis: string
  source: string
  version: number
  createTime: string
  updateTime: string
  categoryName: string
}

export interface QuestionSaveRequest {
  bankCode: string
  type: QuestionType
  subject: Subject
  categoryId: number
  chapter: string
  difficulty: number
  stem: string
  options: string[]
  answer: number[]
  analysis?: string
  source?: string
}

export interface Category {
  id: number
  bankCode: string
  subject: Subject
  name: string
  sort: number
}

export interface CategorySaveRequest {
  bankCode: string
  subject: Subject
  name: string
  sort?: number
}

export interface PracticeQuery {
  bankCode?: string
  subject?: string
  chapter?: string
  type?: string
  difficulty?: number | ''
  mode?: PracticeMode
  limit?: number
}

export interface SubmitRequest {
  questionId: number
  userAnswer: number[]
  mode?: string
}

export interface SubmitResponse {
  correct: boolean
  answer: number[]
  analysis: string
}

export interface WrongQuestion {
  id: number
  userId: number
  questionId: number
  wrongCount: number
  lastAnswer: number[]
  mastered: boolean
  createTime: string
  updateTime: string
  question: Question
}

export interface ExamStartRequest {
  bankCode?: string
  count: number
  minutes: number
  subject?: string
}

export interface ExamStartResponse {
  examKey: string
  minutes: number
  totalCount: number
  questions: Question[]
}

export interface ExamAnswerItem {
  questionId: number
  userAnswer: number[]
}

export interface ExamSubmitRequest {
  examKey: string
  answers: ExamAnswerItem[]
  durationSeconds: number
}

export interface ExamSubmitResponse {
  recordId: number
  score: number
  correctCount: number
  totalCount: number
}

export interface ExamRecord {
  id: number
  totalCount: number
  correctCount: number
  score: number
  durationSeconds: number
  createTime: string
}

export interface ExamDetail {
  id: number
  examId: number
  questionId: number
  userAnswer: number[]
  correct: boolean
}

export interface ExamReport {
  record: ExamRecord
  details: ExamDetail[]
}

export interface StatsOverview {
  todayCount: number
  totalCount: number
  correctCount: number
  accuracy: number
  dailyTarget: number
  examDate: string
  daysLeft: number
}

export interface SubjectStat {
  subject: string
  total: number
  correct: number
  accuracy: number
}

export interface TrendPoint {
  date: string
  total: number
  correct: number
}

export interface Plan {
  id: number
  userId: number
  dailyTarget: number
  examDate: string
  createTime: string
  updateTime: string
}

export interface PlanSaveRequest {
  dailyTarget: number
  examDate: string
}

export interface ProfileUpdateRequest {
  realName: string
}

export interface PasswordChangeRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}
