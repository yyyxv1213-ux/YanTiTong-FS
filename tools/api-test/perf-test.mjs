#!/usr/bin/env node
/**
 * 研题通 性能/并发压测脚本（零依赖，Node 18+）
 * 运行：node tools/api-test/perf-test.mjs [baseUrl]
 *
 * 指标：
 *  - 接口响应时间：avg / p50 / p95 / p99 / max
 *  - 吞吐：RPS（请求/秒）
 *  - 并发：1 / 10 / 25 / 50 用户
 *  - 资源占用：压测前后 java 进程内存（需同机运行）
 *
 * 注意：默认使用独立压测 IP（10.200.x.x），不干扰功能测试限流桶。
 */

const BASE = (process.argv[2] || 'http://localhost:8080/api').replace(/\/$/, '');
const RUN_IP = `10.200.${Math.floor(Math.random() * 200) + 1}`;
const RUN_ID = Date.now().toString(36);
let seq = 0;

function nextIp() { return `${RUN_IP}.${(seq++ % 200) + 1}`; }

async function api({ method = 'GET', path, token, body, ip }) {
  const headers = { 'Content-Type': 'application/json', 'X-Forwarded-For': ip || nextIp() };
  if (token) headers.Authorization = `Bearer ${token}`;
  const started = performance.now();
  try {
    const res = await fetch(BASE + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
    const text = await res.text();
    let code = null, data = null;
    try { const j = JSON.parse(text); code = j.code; data = j.data; } catch {}
    return { ok: res.status >= 200 && res.status < 500, status: res.status, code, data, ms: performance.now() - started };
  } catch (e) {
    return { ok: false, status: 0, code: -1, ms: performance.now() - started, err: String(e) };
  }
}

function stats(samples) {
  if (!samples.length) return { count: 0, avg: 0, p50: 0, p95: 0, p99: 0, max: 0 };
  const s = [...samples].sort((a, b) => a - b);
  const sum = s.reduce((a, b) => a + b, 0);
  const pick = (q) => s[Math.min(s.length - 1, Math.ceil(s.length * q) - 1)];
  return {
    count: s.length,
    avg: +(sum / s.length).toFixed(2),
    p50: +pick(0.50).toFixed(2),
    p95: +pick(0.95).toFixed(2),
    p99: +pick(0.99).toFixed(2),
    max: +s[s.length - 1].toFixed(2)
  };
}

async function measureMemory() {
  // 仅在 Windows + PowerShell 下尝试读取 java 进程内存
  const { execSync } = await import('node:child_process');
  try {
    const out = execSync('powershell -NoProfile -Command "(Get-Process java -ErrorAction SilentlyContinue | Measure-Object WorkingSet64 -Sum).Sum"', { encoding: 'utf-8' }).trim();
    const bytes = Number(out || 0);
    return bytes > 0 ? +(bytes / 1024 / 1024).toFixed(1) : null; // MB
  } catch {
    return null;
  }
}

async function login(username, password) {
  const r = await api({ method: 'POST', path: '/auth/login', body: { username, password } });
  if (r.code !== 0) throw new Error(`登录失败 code=${r.code}`);
  return r.data.accessToken;
}

async function runScenario(name, fn, { concurrency = 1, iterations = 20, warmup = 3 } = {}) {
  console.log(`\n--- ${name} (并发=${concurrency}, 迭代=${iterations}) ---`);
  const memBefore = await measureMemory();

  // 预热
  for (let i = 0; i < warmup; i++) await fn();

  const samples = [];
  const errors = [];
  const t0 = performance.now();

  // 简单并发池
  let idx = 0;
  const workers = Array.from({ length: concurrency }, async () => {
    while (idx < iterations) {
      const my = idx++;
      const r = await fn(my);
      samples.push(r.ms);
      if (!r.ok) errors.push({ i: my, status: r.status, code: r.code, err: r.err });
    }
  });
  await Promise.all(workers);

  const totalMs = performance.now() - t0;
  const memAfter = await measureMemory();
  const s = stats(samples);
  const rps = +(samples.length / (totalMs / 1000)).toFixed(2);
  const errRate = +((errors.length / samples.length) * 100).toFixed(2);

  console.log(`  样本=${s.count}  avg=${s.avg}ms  p50=${s.p50}  p95=${s.p95}  p99=${s.p99}  max=${s.max}ms`);
  console.log(`  总耗时=${totalMs.toFixed(0)}ms  RPS=${rps}  错误率=${errRate}%  内存=${memBefore ?? 'N/A'}MB -> ${memAfter ?? 'N/A'}MB`);
  if (errors.length) console.log(`  错误样例: ${JSON.stringify(errors.slice(0, 3))}`);

  return { name, concurrency, iterations, samples: s, totalMs: +totalMs.toFixed(0), rps, errRate, memBefore, memAfter, errors: errors.slice(0, 5) };
}

// ============================== 场景定义 ==============================
console.log(`\n=== 研题通 性能压测 ===\n目标: ${BASE}\n压测IP段: ${RUN_IP}.x\n`);

let adminToken, studentToken;
try {
  adminToken = await login('admin', '123456');
  studentToken = await login('study', '123456');
} catch (e) {
  console.error('登录失败，请确认服务已启动：', e.message);
  process.exit(1);
}

const results = [];

// S1: 登录接口（无状态，含 BCrypt）
results.push(await runScenario('S1 登录接口', async () => {
  const r = await api({ method: 'POST', path: '/auth/login', body: { username: 'study', password: '123456' } });
  return { ...r, ok: r.code === 0 };
}, { concurrency: 1, iterations: 10 }));

// S2: 题库列表（匿名公共接口，高频）
results.push(await runScenario('S2 题库列表（匿名）', async () => {
  const r = await api({ path: '/common/banks' });
  return { ...r, ok: r.code === 0 };
}, { concurrency: 10, iterations: 100 }));

// S3: 学员抽题（带题库过滤 + 随机）
results.push(await runScenario('S3 学员抽题（10题）', async () => {
  const r = await api({ path: '/practice/questions', token: studentToken });
  return { ...r, ok: r.code === 0 && Array.isArray(r.data) };
}, { concurrency: 10, iterations: 100 }));

// S4: 考试历史分页（带索引查询）
results.push(await runScenario('S4 考试历史分页', async () => {
  const r = await api({ path: '/exam/history', token: studentToken, params: { pageNum: 1, pageSize: 10 } });
  return { ...r, ok: r.code === 0 };
}, { concurrency: 10, iterations: 100 }));

// S5: 管理端题目分页（含 keyword LIKE）
results.push(await runScenario('S5 管理端题目分页', async () => {
  const r = await api({ path: '/admin/questions', token: adminToken, params: { pageNum: 1, pageSize: 20, subject: 'ds' } });
  return { ...r, ok: r.code === 0 };
}, { concurrency: 5, iterations: 50 }));

// S6: 高并发抽题（25/50 并发压力）
results.push(await runScenario('S6 高并发抽题（25并发）', async () => {
  const r = await api({ path: '/practice/questions', token: studentToken });
  return { ...r, ok: r.code === 0 };
}, { concurrency: 25, iterations: 125 }));

results.push(await runScenario('S7 高并发抽题（50并发）', async () => {
  const r = await api({ path: '/practice/questions', token: studentToken });
  return { ...r, ok: r.code === 0 };
}, { concurrency: 50, iterations: 150 }));

// ============================== 汇总 ==============================
const { writeFileSync, mkdirSync } = await import('node:fs');
const { join, dirname } = await import('node:path');
const { fileURLToPath } = await import('node:url');
const resultsDir = join(dirname(fileURLToPath(import.meta.url)), 'results');
mkdirSync(resultsDir, { recursive: true });
const outFile = join(resultsDir, 'perf-test-result.json');
writeFileSync(outFile, JSON.stringify({ target: BASE, time: new Date().toISOString(), runId: RUN_ID, scenarios: results }, null, 2), 'utf-8');

console.log('\n=== 性能测试完成 ===');
console.log(`结果文件: ${outFile}`);
const allOk = results.every(r => r.errRate === 0);
console.log(`整体评估: ${allOk ? '全部场景无错误' : '存在错误场景，请检查'}`);
process.exit(allOk ? 0 : 1);
