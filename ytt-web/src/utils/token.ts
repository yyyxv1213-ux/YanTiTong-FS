/**
 * JWT 前端解析工具。
 * 仅解析 Payload（Base64URL）用于读取过期时间，不做签名校验——
 * 任何安全判断以后端校验为准，本工具只用于过期预警与会话清理。
 */

/** Base64URL 解码为 UTF-8 字符串 */
function base64UrlDecode(segment: string): string {
  const base64 = segment.replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
  const binary = window.atob(padded)
  // 逐字节解码，避免中文字符乱码
  const bytes = Uint8Array.from(binary, (ch) => ch.charCodeAt(0))
  return new TextDecoder('utf-8').decode(bytes)
}

/** 解析 JWT Payload，非法 Token 返回 null */
export function parseJwtPayload<T = Record<string, unknown>>(token: string): T | null {
  if (!token || token.split('.').length !== 3) {
    return null
  }
  try {
    return JSON.parse(base64UrlDecode(token.split('.')[1])) as T
  } catch {
    return null
  }
}

/** 读取 Token 过期时间戳（毫秒），缺失或非法返回 null */
export function getTokenExpireAt(token: string): number | null {
  const payload = parseJwtPayload<{ exp?: number }>(token)
  if (!payload || typeof payload.exp !== 'number') {
    return null
  }
  return payload.exp * 1000
}

/**
 * 判断 Token 是否已过期。
 * @param skewMs 提前量（毫秒），可在过期前提前判定
 */
export function isTokenExpired(token: string, skewMs = 0): boolean {
  const expireAt = getTokenExpireAt(token)
  if (expireAt === null) {
    // 无法解析过期时间视为不可用，避免持有非法 Token
    return true
  }
  return Date.now() >= expireAt - skewMs
}
