import { cleanParams, get } from './http'
import type { StatsOverview, SubjectStat, TrendPoint } from '@/types'

export function statsOverview(bankCode?: string) {
  return get<StatsOverview>('/stats/overview', cleanParams({ bankCode }))
}

export function statsBySubject(bankCode?: string) {
  return get<SubjectStat[]>('/stats/subject', cleanParams({ bankCode }))
}

export function statsTrend(bankCode?: string) {
  return get<TrendPoint[]>('/stats/trend', cleanParams({ bankCode }))
}
