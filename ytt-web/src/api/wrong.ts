import { get, put } from './http'
import type { WrongQuestion } from '@/types'

export function listWrongQuestions() {
  return get<WrongQuestion[]>('/wrong/list')
}

export function masterWrongQuestion(id: number) {
  return put<void>(`/wrong/${id}/master`)
}
