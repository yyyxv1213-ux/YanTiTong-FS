import { describe, expect, it } from 'vitest'
import {
  answerText,
  difficultyName,
  difficultyTagType,
  formatDuration,
  formatSeconds,
  optionLabel,
  questionTypeName,
  stripHtml,
  subjectName,
  truncate
} from '../format'

describe('format 工具函数', () => {
  it('subjectName 将科目码映射为中文名', () => {
    expect(subjectName('ds')).toBe('数据结构')
    expect(subjectName('co')).toBe('计算机组成原理')
    expect(subjectName('os')).toBe('操作系统')
    expect(subjectName('cn')).toBe('计算机网络')
    expect(subjectName('')).toBe('全部科目')
    expect(subjectName('xx')).toBe('xx')
  })

  it('questionTypeName 将题型映射为中文名', () => {
    expect(questionTypeName('single')).toBe('单选题')
    expect(questionTypeName('multi')).toBe('多选题')
    expect(questionTypeName('judge')).toBe('判断题')
    expect(questionTypeName(undefined)).toBe('-')
  })

  it('difficultyName 与 difficultyTagType 映射难度', () => {
    expect(difficultyName(1)).toBe('简单')
    expect(difficultyName(2)).toBe('中等')
    expect(difficultyName(3)).toBe('困难')
    expect(difficultyName(9)).toBe('-')
    expect(difficultyTagType(1)).toBe('success')
    expect(difficultyTagType(2)).toBe('warning')
    expect(difficultyTagType(3)).toBe('danger')
  })

  it('formatSeconds 将秒格式化为 mm:ss', () => {
    expect(formatSeconds(0)).toBe('00:00')
    expect(formatSeconds(65)).toBe('01:05')
    expect(formatSeconds(3600)).toBe('60:00')
    expect(formatSeconds(-5)).toBe('00:00')
    expect(formatSeconds(undefined)).toBe('00:00')
  })

  it('formatDuration 输出友好用时', () => {
    expect(formatDuration(90)).toBe('1分30秒')
    expect(formatDuration(3661)).toBe('1小时1分')
  })

  it('stripHtml 去除 HTML 标签', () => {
    expect(stripHtml('<b>栈</b>的特点是<i>先进后出</i>')).toBe('栈的特点是先进后出')
    expect(stripHtml(undefined)).toBe('')
  })

  it('truncate 超长截断并去除标签', () => {
    const long = 'a'.repeat(70)
    expect(truncate(long, 60)).toBe(`${'a'.repeat(60)}…`)
    expect(truncate('<b>短</b>', 60)).toBe('短')
  })

  it('optionLabel 与 answerText 处理选项字母', () => {
    expect(optionLabel(0)).toBe('A')
    expect(optionLabel(3)).toBe('D')
    expect(answerText([2, 0])).toBe('A、C')
    expect(answerText([])).toBe('未作答')
    expect(answerText(undefined)).toBe('未作答')
  })
})
