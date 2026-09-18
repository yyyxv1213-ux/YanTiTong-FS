import { describe, expect, it } from 'vitest'
import { getTokenExpireAt, isTokenExpired, parseJwtPayload } from '../token'

/** 用给定 exp（秒）构造结构合法的 JWT（签名段任意） */
function makeToken(expSeconds: number): string {
  const header = window.btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
  const payload = window.btoa(JSON.stringify({ sub: 'tester', exp: expSeconds }))
  return `${header}.${payload}.fakesignature`
}

describe('JWT 解析工具', () => {
  it('能正确解析 payload', () => {
    const token = makeToken(2000000000)
    const payload = parseJwtPayload<{ sub: string }>(token)
    expect(payload?.sub).toBe('tester')
  })

  it('非法 token 返回 null', () => {
    expect(parseJwtPayload('not-a-jwt')).toBeNull()
    expect(parseJwtPayload('a.b')).toBeNull()
  })

  it('能读取过期时间戳（毫秒）', () => {
    const token = makeToken(2000000000)
    expect(getTokenExpireAt(token)).toBe(2000000000000)
  })

  it('未过期 token 判定有效', () => {
    const token = makeToken(Math.floor(Date.now() / 1000) + 3600)
    expect(isTokenExpired(token)).toBe(false)
  })

  it('已过期 token 判定过期', () => {
    const token = makeToken(Math.floor(Date.now() / 1000) - 10)
    expect(isTokenExpired(token)).toBe(true)
  })

  it('支持提前量判定', () => {
    // 50 秒后过期，提前 60 秒判定为已过期
    const token = makeToken(Math.floor(Date.now() / 1000) + 50)
    expect(isTokenExpired(token, 60_000)).toBe(true)
  })
})
