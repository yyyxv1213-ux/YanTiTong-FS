const SUBJECT_MAP: Record<string, string> = {
  ds: '数据结构',
  co: '计算机组成原理',
  os: '操作系统',
  cn: '计算机网络'
}

const TYPE_MAP: Record<string, string> = {
  single: '单选题',
  multi: '多选题',
  judge: '判断题'
}

export const SUBJECT_OPTIONS = [
  { value: 'ds', label: '数据结构' },
  { value: 'co', label: '计算机组成原理' },
  { value: 'os', label: '操作系统' },
  { value: 'cn', label: '计算机网络' }
] as const

export function subjectName(code?: string): string {
  if (!code) return '全部科目'
  return SUBJECT_MAP[code] ?? code
}

export function questionTypeName(type?: string): string {
  if (!type) return '-'
  return TYPE_MAP[type] ?? type
}

export function difficultyName(d?: number): string {
  if (d === 1) return '简单'
  if (d === 2) return '中等'
  if (d === 3) return '困难'
  return '-'
}

export function difficultyTagType(d?: number): 'success' | 'warning' | 'danger' | 'info' {
  if (d === 1) return 'success'
  if (d === 2) return 'warning'
  if (d === 3) return 'danger'
  return 'info'
}

/** 秒 -> mm:ss */
export function formatSeconds(total?: number): string {
  const s = Math.max(0, Math.floor(total ?? 0))
  const mm = String(Math.floor(s / 60)).padStart(2, '0')
  const ss = String(s % 60).padStart(2, '0')
  return `${mm}:${ss}`
}

/** 秒 -> 友好的用时描述 */
export function formatDuration(total?: number): string {
  const s = Math.max(0, Math.floor(total ?? 0))
  const m = Math.floor(s / 60)
  if (m < 60) return `${m}分${s % 60}秒`
  const h = Math.floor(m / 60)
  return `${h}小时${m % 60}分`
}

export function stripHtml(html?: string): string {
  return (html ?? '').replace(/<[^>]*>/g, '')
}

export function truncate(text?: string, len = 60): string {
  const t = stripHtml(text)
  return t.length > len ? `${t.slice(0, len)}…` : t
}

/** 选项下标 -> 字母（0->A） */
export function optionLabel(index: number): string {
  return String.fromCharCode(65 + index)
}

/** 答案下标数组 -> 字母串，如 [0,2] -> "A、C" */
export function answerText(answer?: number[]): string {
  if (!answer || answer.length === 0) return '未作答'
  return [...answer].sort((a, b) => a - b).map(optionLabel).join('、')
}
