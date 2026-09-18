import { defineStore } from 'pinia'
import { fetchBanks } from '@/api/common'
import type { ExamBank } from '@/types'
import { SUBJECT_OPTIONS } from '@/utils/format'

const BANK_KEY = 'ytt_bank_code'
export const DEFAULT_BANK_CODE = 'ky408'

/**
 * 题库大类状态：
 * 当前选题库持久化到 localStorage（ytt_bank_code），
 * 切换后各学员页面通过 router-view :key 重新挂载并按新题库加载数据。
 */
export const useBankStore = defineStore('bank', {
  state: () => ({
    banks: [] as ExamBank[],
    currentCode: localStorage.getItem(BANK_KEY) || DEFAULT_BANK_CODE,
    loaded: false
  }),
  getters: {
    currentBank(state): ExamBank | undefined {
      return state.banks.find((b) => b.code === state.currentCode)
    },
    /** 当前题库的科目下拉选项；题库列表未加载时回退到考研408 四科 */
    subjectOptions(state): { value: string; label: string }[] {
      const bank = state.banks.find((b) => b.code === state.currentCode)
      if (!bank) return SUBJECT_OPTIONS.map((s) => ({ value: s.value, label: s.label }))
      return bank.subjects.map((s) => ({ value: s.code, label: s.name }))
    }
  },
  actions: {
    /** 拉取已上线题库；本地保存的编码已下线时回退到第一个题库 */
    async loadBanks(force = false) {
      if (this.loaded && !force) return
      try {
        this.banks = await fetchBanks()
        this.loaded = true
        if (!this.banks.some((b) => b.code === this.currentCode)) {
          this.setBank(this.banks[0]?.code || DEFAULT_BANK_CODE)
        }
      } catch {
        // 接口异常时保留默认题库，不阻塞页面使用
        this.loaded = false
      }
    },

    setBank(code: string) {
      this.currentCode = code
      localStorage.setItem(BANK_KEY, code)
    },

    /** 科目编码 -> 科目名称（未命中时回退显示编码） */
    subjectName(code?: string): string {
      if (!code) return '全部科目'
      const found = this.currentBank?.subjects.find((s) => s.code === code)
      return found?.name ?? code
    }
  }
})
