#!/usr/bin/env node
/**
 * DEF-02 / DEF-03 修复验证脚本（快速冒烟）
 */
const BASE = 'http://localhost:8080/api';

async function login(ip) {
  const headers = { 'Content-Type': 'application/json' };
  if (ip) headers['X-Forwarded-For'] = ip;
  const res = await fetch(`${BASE}/auth/login`, {
    method: 'POST', headers, body: JSON.stringify({ username: 'study', password: '123456' })
  });
  const j = await res.json();
  return j;
}

console.log('=== 验证 DEF-02：同秒两次登录 Token 应不同 ===');
const [r1, r2] = await Promise.all([login('10.1.1.1'), login('10.1.1.2')]);
if (r1.code === 0 && r2.code === 0) {
  const same = r1.data.accessToken === r2.data.accessToken;
  console.log(`Token1 === Token2 ? ${same}`);
  console.log(same ? '[FAIL] DEF-02 仍存在' : '[PASS] DEF-02 已修复');
} else {
  console.log(`[ERROR] 登录失败 code=${r1.code}/${r2.code} msg=${r1.message}/${r2.message}`);
}

console.log('\n=== 验证 DEF-03：伪造 XFF 不应绕过限流 ===');
// 用真实连接（无 XFF）打满登录限流：dev capacity = 5 × multiplier(2) = 10, period=60s
let hit = false;
for (let i = 0; i < 12; i++) {
  const r = await login(null); // 不带 XFF，使用真实 remoteAddr (127.0.0.1)
  process.stdout.write(`  第${i + 1}次 code=${r.code}`);
  if (r.code === 42900) { hit = true; console.log(' <- 触发限流'); break; }
  console.log('');
}
if (!hit) { console.log('[WARN] 7 次内未触发限流，可能窗口已滑动，重试或检查 capacity'); }
else {
  // 现在伪造不同 XFF，若修复生效，应仍被限流（42900）
  const spoof = await login('10.99.99.99');
  console.log(`  伪造 XFF=10.99.99.99 后请求: code=${spoof.code}`);
  console.log(spoof.code === 42900 ? '[PASS] DEF-03 已修复（伪造 XFF 无效，仍按真实 IP 限流）' : '[FAIL] DEF-03 仍存在（伪造 XFF 绕过了限流）');
}
