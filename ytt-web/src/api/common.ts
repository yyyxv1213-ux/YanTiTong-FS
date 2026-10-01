import { get } from './http'
import type { ExamBank } from '@/types'

/** 查询已上线的题库大类列表（公开接口，无需登录） */
export function fetchBanks() {
  return get<ExamBank[]>('/common/banks')
}
