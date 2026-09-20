import { cleanParams, get, post } from './http'
import type { PracticeQuery, Question, SubmitRequest, SubmitResponse } from '@/types'

export function fetchPracticeQuestions(query: PracticeQuery) {
  return get<Question[]>('/practice/questions', cleanParams(query))
}

export function submitPractice(data: SubmitRequest) {
  return post<SubmitResponse>('/practice/submit', data)
}
