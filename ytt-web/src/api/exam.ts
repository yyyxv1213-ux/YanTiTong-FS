import { cleanParams, get, post } from './http'
import type {
  ExamReport,
  ExamRecord,
  ExamStartRequest,
  ExamStartResponse,
  ExamSubmitRequest,
  ExamSubmitResponse,
  PageResult
} from '@/types'

export function startExam(data: ExamStartRequest) {
  return post<ExamStartResponse>('/exam/start', data)
}

export function submitExam(data: ExamSubmitRequest) {
  return post<ExamSubmitResponse>('/exam/submit', data)
}

export function examHistory(pageNum: number, pageSize: number) {
  return get<PageResult<ExamRecord>>('/exam/history', cleanParams({ pageNum, pageSize }))
}

export function examReport(id: number) {
  return get<ExamReport>(`/exam/${id}`)
}
