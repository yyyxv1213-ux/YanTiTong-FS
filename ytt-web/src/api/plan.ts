import { get, put } from './http'
import type { Plan, PlanSaveRequest } from '@/types'

export function getPlan() {
  return get<Plan>('/plan')
}

export function savePlan(data: PlanSaveRequest) {
  return put<void>('/plan', data)
}
