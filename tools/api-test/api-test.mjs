#!/usr/bin/env node
/**
 * 研题通 API 自动化测试脚本（功能测试 + 安全测试）
 * 依赖：Node.js 18+（全局 fetch），无需安装任何 npm 包
 * 运行：node tools/api-test/api-test.mjs [baseUrl]
 * 说明：
 *  - 默认目标 http://localhost:8080/api（dev 环境，账号 admin/123456、study/123456）
 *  - DEF-03 修复后，限流按真实连接 IP（getRemoteAddr）计，X-Forwarded-For 头被忽略。
 *    dev 环境通过 security.rate-limit.multiplier=2 放宽阈值以容纳密集测试调用；
 *    运行前若窗口已被占满，请等待 60 秒窗口滑动后再执行。
 *  - 结果写入 tools/api-test/results/api-test-result.json
 */
import { writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const BASE = (process.argv[2] || 'http://localhost:8080/api').replace(/\/$/, '');
const __dir = dirname(fileURLToPath(import.meta.url));
const resultsDir = join(__dir, 'results');

let caseSeq = 0;
const cases = [];
let currentGroup = '通用';
let testIpSeq = 10;
const RUN_OCTET = Math.floor(Math.random() * 200) + 1; // 每轮运行唯一网段，隔离限流桶
let defaultIp = `10.${RUN_OCTET}.0.10`;

function group(name) { currentGroup = name; defaultIp = `10.${RUN_OCTET}.0.${++testIpSeq}`; }

async function api({ method = 'GET', path, token, body, params, ip, rawHeaders = {} }) {
  let url = BASE + path;
  if (params) {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') qs.append(k, String(v)); });
    const s = qs.toString();
    if (s) url += '?' + s;
  }
  const headers = { 'Content-Type': 'application/json', 'X-Forwarded-For': ip || defaultIp, ...rawHeaders };
  if (token) headers.Authorization = `Bearer ${token}`;
  const started = performance.now();
  let httpStatus = 0, json = null, text = '';
  try {
    const res = await fetch(url, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
    httpStatus = res.status;
    text = await res.text();
    try { json = JSON.parse(text); } catch { /* 非 JSON 响应 */ }
  } catch (e) {
    return { httpStatus: 0, code: -1, data: null, message: String(e), ms: performance.now() - started };
  }
  return { httpStatus, code: json?.code ?? null, data: json?.data ?? null, message: json?.message ?? text.slice(0, 120), ms: performance.now() - started, raw: text };
}

async function test(name, fn) {
  caseSeq++;
  const id = `${String(caseSeq).padStart(3, '0')}`;
  try {
    await fn();
    cases.push({ id, group: currentGroup, name, status: 'PASS' });
    console.log(`  [PASS] ${name}`);
  } catch (e) {
    cases.push({ id, group: currentGroup, name, status: 'FAIL', detail: e.message });
    console.log(`  [FAIL] ${name} -> ${e.message}`);
  }
}

function expect(cond, msg) { if (!cond) throw new Error(msg); }
function expectCode(r, code, what) {
  expect(r.code === code, `${what}：期望 code=${code}，实际 code=${r.code}（${r.message}）`);
}
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

/** 已知缺陷探针：行为符合修复后期望则 PASS；观测到已归档缺陷则记 KNOWN；其余异常 FAIL */
async function testKnown(name, defectId, fn) {
  caseSeq++;
  const id = `${String(caseSeq).padStart(3, '0')}`;
  try {
    const observed = await fn();
    if (observed) {
      cases.push({ id, group: currentGroup, name, status: 'KNOWN', defect: defectId });
      console.log(`  [KNOWN:${defectId}] ${name}（缺陷仍存在）`);
    } else {
      cases.push({ id, group: currentGroup, name, status: 'PASS' });
      console.log(`  [PASS] ${name}（缺陷已修复）`);
    }
  } catch (e) {
    cases.push({ id, group: currentGroup, name, status: 'FAIL', detail: e.message });
    console.log(`  [FAIL] ${name} -> ${e.message}`);
  }
}

// ============================== 测试开始 ==============================
console.log(`\n=== 研题通 API 自动化测试 ===\n目标: ${BASE}\n`);

let adminToken = '', studentToken = '', studentRefresh = '';

// ---------- A. 认证模块 ----------
group('A-认证模块');
await test('A01 管理员登录成功并返回双Token与角色', async () => {
  const r = await api({ method: 'POST', path: '/auth/login', body: { username: 'admin', password: '123456' } });
  expectCode(r, 0, '管理员登录');
  expect(r.data.accessToken && r.data.refreshToken, '缺少 Token 字段');
  expect(r.data.user.roles.includes('ADMIN'), 'admin 应含 ADMIN 角色');
  adminToken = r.data.accessToken;
});
await test('A02 学员登录成功', async () => {
  const r = await api({ method: 'POST', path: '/auth/login', body: { username: 'study', password: '123456' } });
  expectCode(r, 0, '学员登录');
  expect(r.data.user.roles.includes('STUDENT'), 'study 应含 STUDENT 角色');
  studentToken = r.data.accessToken;
  studentRefresh = r.data.refreshToken;
});
await test('A03 密码错误返回 40104', async () => {
  const r = await api({ method: 'POST', path: '/auth/login', body: { username: 'study', password: 'wrong-pass-1' } });
  expectCode(r, 40104, '密码错误');
});
await test('A04 用户不存在返回 40401（注：与密码错误提示不同，存在账号探测面）', async () => {
  const r = await api({ method: 'POST', path: '/auth/login', body: { username: 'no_such_user_qa', password: '123456' } });
  expectCode(r, 40401, '用户不存在');
});
await test('A05 注册参数校验（密码过短）返回 40000', async () => {
  const r = await api({ method: 'POST', path: '/auth/register', body: { username: 'qa_reg1', email: 'qa_reg1@test.local', password: '123', confirmPassword: '123' } });
  expectCode(r, 40000, '密码过短校验');
});
await test('A06 注册两次密码不一致返回 40000', async () => {
  const r = await api({ method: 'POST', path: '/auth/register', body: { username: 'qa_reg1', email: 'qa_reg1@test.local', password: '123456', confirmPassword: '654321' } });
  expectCode(r, 40000, '密码不一致');
});
await test('A07 正常注册成功（注册即登录，分配 STUDENT 角色）', async () => {
  const name = 'qa' + Date.now().toString(36);
  const r = await api({ method: 'POST', path: '/auth/register', body: { username: name, email: `${name}@test.local`, password: 'qa123456', confirmPassword: 'qa123456' } });
  expectCode(r, 0, '注册');
  expect(r.data.user.roles.includes('STUDENT'), '新用户应为 STUDENT');
});
await test('A08 重复用户名注册返回 40901', async () => {
  const r = await api({ method: 'POST', path: '/auth/register', body: { username: 'study', email: 'x' + Date.now() + '@test.local', password: '123456', confirmPassword: '123456' } });
  expectCode(r, 40901, '用户名重复');
});
await test('A09 重复邮箱注册返回 40905', async () => {
  const r = await api({ method: 'POST', path: '/auth/register', body: { username: 'qa_dup_' + Date.now().toString(36), email: 'study@yantitong.local', password: '123456', confirmPassword: '123456' } });
  expectCode(r, 40905, '邮箱重复');
});
await test('A10 refreshToken 轮换签发新 Token', async () => {
  await sleep(1100); // 规避 DEF-02 同秒 Token 相同的干扰
  const r = await api({ method: 'POST', path: '/auth/refresh', body: { refreshToken: studentRefresh } });
  expectCode(r, 0, '刷新令牌');
  expect(r.data.accessToken && r.data.refreshToken, '缺少新 Token');
  studentToken = r.data.accessToken;
  studentRefresh = r.data.refreshToken;
});
await test('A11 已轮换的旧 refreshToken 二次使用被拒绝（40103 黑名单）', async () => {
  await sleep(1100);
  const first = await api({ method: 'POST', path: '/auth/refresh', body: { refreshToken: studentRefresh } });
  expectCode(first, 0, '第一次刷新');
  const replay = await api({ method: 'POST', path: '/auth/refresh', body: { refreshToken: studentRefresh } });
  expectCode(replay, 40103, '旧 refreshToken 重放');
  studentToken = first.data.accessToken;
  studentRefresh = first.data.refreshToken;
});
await test('A12 accessToken 不能用于刷新接口（40101）', async () => {
  const r = await api({ method: 'POST', path: '/auth/refresh', body: { refreshToken: studentToken } });
  expectCode(r, 40101, 'accessToken 误用为 refresh');
});
await test('A13 未注册邮箱发送验证码返回 40401', async () => {
  const r = await api({ method: 'POST', path: '/auth/email/code', body: { email: 'nobody_' + Date.now() + '@test.local' } });
  expectCode(r, 40401, '未注册邮箱');
});
await test('A14 邮箱验证码错误返回 40106/40107', async () => {
  const r = await api({ method: 'POST', path: '/auth/email/login', body: { email: 'study@yantitong.local', code: '000000' } });
  expect([40106, 40107].includes(r.code), `期望 40106/40107，实际 ${r.code}`);
});
// DEF-02 探针：同秒内两次登录签发的 accessToken 应不同（含 jti 才唯一）
// 使用不同 IP 避免互相触发登录限流桶
await testKnown('A15 同秒签发的同用户 Token 应唯一（jti）', 'DEF-02', async () => {
  const [r1, r2] = await Promise.all([
    api({ method: 'POST', path: '/auth/login', body: { username: 'study', password: '123456' }, ip: `10.${RUN_OCTET}.0.100` }),
    api({ method: 'POST', path: '/auth/login', body: { username: 'study', password: '123456' }, ip: `10.${RUN_OCTET}.0.101` })
  ]);
  expectCode(r1, 0, '登录1'); expectCode(r2, 0, '登录2');
  return r1.data.accessToken === r2.data.accessToken; // true = 缺陷仍存在
});

// ---------- B. 公共接口 ----------
group('B-公共接口');
await test('B01 /common/banks 匿名可访问且返回 ky408（含4科目）', async () => {
  const r = await api({ path: '/common/banks' });
  expectCode(r, 0, '题库列表');
  const bank = (r.data || []).find(b => b.code === 'ky408');
  expect(bank, '缺少 ky408 题库');
  expect(bank.subjects.length === 4, `ky408 应含4科目，实际 ${bank.subjects.length}`);
});
await test('B02 未登录访问受保护接口返回 40100', async () => {
  const r = await api({ path: '/stats/overview', ip: defaultIp });
  expectCode(r, 40100, '未登录拦截');
});
await test('B03 伪造 Token 访问返回 40101', async () => {
  const r = await api({ path: '/stats/overview', token: 'forged.token.value' });
  expectCode(r, 40101, '伪造 Token');
});

// ---------- C. 刷题训练 ----------
group('C-刷题训练');
let sampleQuestionId = null, sampleAnswer = null;
await test('C01 默认抽题 10 道且不返回答案', async () => {
  const r = await api({ path: '/practice/questions', token: studentToken });
  expectCode(r, 0, '默认抽题');
  expect(r.data.length === 10, `期望10题，实际 ${r.data.length}`);
  const leaked = r.data.find(q => q.answer !== null && q.answer !== undefined);
  expect(!leaked, '抽题接口泄露了答案字段');
  sampleQuestionId = r.data[0].id;
});
await test('C02 按科目 ds + 难度3 + random 模式抽题', async () => {
  const r = await api({ path: '/practice/questions', token: studentToken, params: { subject: 'ds', difficulty: 3, mode: 'random', limit: 5 } });
  expectCode(r, 0, '条件抽题');
  expect(r.data.every(q => q.subject === 'ds' && q.difficulty === 3), '筛选条件未生效');
});
await test('C03 非法题库编码抽题返回参数错误', async () => {
  const r = await api({ path: '/practice/questions', token: studentToken, params: { bankCode: 'not_exists_bank' } });
  expect(r.code !== 0, '非法题库编码应被拒绝');
});
await test('C04 管理端获取题目答案（用于判分校验）', async () => {
  const r = await api({ path: `/admin/questions/${sampleQuestionId}`, token: adminToken });
  expectCode(r, 0, '题目详情');
  sampleAnswer = r.data.answer;
  expect(Array.isArray(sampleAnswer) && sampleAnswer.length > 0, '答案为空');
});
await test('C05 提交正确答案判分为正确', async () => {
  const r = await api({ method: 'POST', path: '/practice/submit', token: studentToken, body: { questionId: sampleQuestionId, userAnswer: sampleAnswer, mode: 'sequence' } });
  expectCode(r, 0, '提交作答');
  expect(r.data.correct === 1 || r.data.correct === true, `应判正确，实际 ${JSON.stringify(r.data)}`);
});
await test('C06 提交错误答案判分为错误并落入错题本', async () => {
  const wrongAnswer = sampleAnswer[0] === 0 ? [1] : [0];
  const r = await api({ method: 'POST', path: '/practice/submit', token: studentToken, body: { questionId: sampleQuestionId, userAnswer: wrongAnswer, mode: 'sequence' } });
  expectCode(r, 0, '提交作答');
  expect(r.data.correct === 0 || r.data.correct === false, '应判错误');
});
await test('C07 提交不存在的题目返回 40402', async () => {
  const r = await api({ method: 'POST', path: '/practice/submit', token: studentToken, body: { questionId: 999999, userAnswer: [0], mode: 'sequence' } });
  expectCode(r, 40402, '题目不存在');
});
await test('C08 缺参提交返回 40000（JSR-303 校验）', async () => {
  const r = await api({ method: 'POST', path: '/practice/submit', token: studentToken, body: { mode: 'sequence' } });
  expectCode(r, 40000, '缺参校验');
});

// ---------- D. 模拟考试 ----------
group('D-模拟考试');
let examKey = '', examId = null, examTotal = 0;
await test('D01 组卷（5题/10分钟）成功且不下发答案', async () => {
  const r = await api({ method: 'POST', path: '/exam/start', token: studentToken, body: { count: 5, minutes: 10, bankCode: 'ky408' } });
  expectCode(r, 0, '组卷');
  expect(r.data.examKey && r.data.questions.length === 5, '组卷数据异常');
  expect(!r.data.questions.find(q => q.answer != null), '组卷泄露答案');
  examKey = r.data.examKey;
  examTotal = r.data.questions.length;
});
await test('D02 组卷参数越界（count=3）被校验拒绝', async () => {
  const r = await api({ method: 'POST', path: '/exam/start', token: studentToken, body: { count: 3, minutes: 10 } });
  expectCode(r, 40000, 'count 下限校验');
});
await test('D03 交卷判分并生成考试记录', async () => {
  const qs = await api({ method: 'POST', path: '/exam/start', token: studentToken, body: { count: 5, minutes: 10, bankCode: 'ky408' } });
  examKey = qs.data.examKey;
  const answers = qs.data.questions.map(q => ({ questionId: q.id, userAnswer: [] }));
  const r = await api({ method: 'POST', path: '/exam/submit', token: studentToken, body: { examKey, answers, durationSeconds: 60 } });
  expectCode(r, 0, '交卷');
  expect(r.data.recordId, '缺少 recordId');
  expect(r.data.totalCount === examTotal, '题数不符');
  examId = r.data.recordId;
});
await test('D04 同一 examKey 重复交卷被拒绝（防重放）', async () => {
  const r = await api({ method: 'POST', path: '/exam/submit', token: studentToken, body: { examKey, answers: [], durationSeconds: 60 } });
  expect(r.code !== 0, '重复交卷应被拒绝');
});
await test('D05 伪造他人/不存在 examKey 交卷被拒绝', async () => {
  const r = await api({ method: 'POST', path: '/exam/submit', token: studentToken, body: { examKey: 'f'.repeat(32), answers: [], durationSeconds: 1 } });
  expect(r.code !== 0, '伪造 examKey 应被拒绝');
});
await test('D06 考试历史分页返回记录', async () => {
  const r = await api({ path: '/exam/history', token: studentToken, params: { pageNum: 1, pageSize: 10 } });
  expectCode(r, 0, '考试历史');
  expect((r.data.total ?? 0) >= 1, '历史记录为空');
});
await test('D07 成绩报告含逐题明细', async () => {
  const r = await api({ path: `/exam/${examId}`, token: studentToken });
  expectCode(r, 0, '成绩报告');
  const details = r.data.details || r.data.items || [];
  expect(details.length === examTotal, `明细题数不符: ${details.length} != ${examTotal}`);
});
await test('D08 越权访问他人成绩报告被拒绝', async () => {
  const r = await api({ path: `/exam/${examId}`, token: adminToken });
  expect(r.code !== 0, `越权访问应被拒绝，实际 code=${r.code}`);
});

// ---------- E. 错题本 ----------
group('E-错题本');
await test('E01 错题列表返回数据（C06 已制造错题）', async () => {
  const r = await api({ path: '/wrong/list', token: studentToken });
  expectCode(r, 0, '错题列表');
  expect(Array.isArray(r.data) || Array.isArray(r.data?.records) || Array.isArray(r.data?.list), '返回结构异常');
});
await test('E02 标记错题为已掌握', async () => {
  const list = await api({ path: '/wrong/list', token: studentToken });
  const arr = Array.isArray(list.data) ? list.data : (list.data?.records || list.data?.list || []);
  if (arr.length === 0) return; // 无错题则跳过
  const target = arr[0];
  const id = target.id ?? target.questionId;
  const r = await api({ method: 'PUT', path: `/wrong/${id}/master`, token: studentToken });
  expectCode(r, 0, '标记已掌握');
});

// ---------- F. 学习统计 / 计划 / 个人中心 ----------
group('F-统计/计划/个人中心');
await test('F01 学习统计总览返回四科数据结构', async () => {
  const r = await api({ path: '/stats/overview', token: studentToken });
  expectCode(r, 0, '统计总览');
  expect(r.data !== null, '总览为空');
});
await test('F02 分科统计返回', async () => {
  const r = await api({ path: '/stats/subject', token: studentToken });
  expectCode(r, 0, '分科统计');
});
await test('F03 近14天趋势返回', async () => {
  const r = await api({ path: '/stats/trend', token: studentToken });
  expectCode(r, 0, '趋势');
});
await test('F04 获取学习计划（首次自动创建）', async () => {
  const r = await api({ path: '/plan', token: studentToken });
  expectCode(r, 0, '获取计划');
});
await test('F05 更新学习计划', async () => {
  const r = await api({ method: 'PUT', path: '/plan', token: studentToken, body: { dailyTarget: 40, examDate: '2026-12-26' } });
  expectCode(r, 0, '更新计划');
  const check = await api({ path: '/plan', token: studentToken });
  expect(check.data.dailyTarget === 40, `计划未生效: ${JSON.stringify(check.data)}`);
});
await test('F06 获取个人资料', async () => {
  const r = await api({ path: '/profile', token: studentToken });
  expectCode(r, 0, '个人资料');
  expect(r.data.username === 'study', '用户名不符');
});
await test('F07 修改个人资料（真实姓名）', async () => {
  const r = await api({ method: 'PUT', path: '/profile', token: studentToken, body: { realName: 'QA自动化测试' } });
  expectCode(r, 0, '修改资料');
  const check = await api({ path: '/profile', token: studentToken });
  expect(check.data.realName === 'QA自动化测试', '资料未生效');
  await api({ method: 'PUT', path: '/profile', token: studentToken, body: { realName: '演示学员' } });
});
await test('F08 修改密码后旧 Token 失效、新密码可登录', async () => {
  const login = await api({ method: 'POST', path: '/auth/login', body: { username: 'study', password: '123456' } });
  expectCode(login, 0, '改密前登录');
  const tk = login.data.accessToken;
  const change = await api({ method: 'PUT', path: '/profile/password', token: tk, body: { oldPassword: '123456', newPassword: 'qa654321', confirmPassword: 'qa654321' } });
  expectCode(change, 0, '修改密码');
  const reuse = await api({ path: '/profile', token: tk });
  expect(reuse.code === 40103 || reuse.code === 40101 || reuse.code === 40100, `改密后旧Token应失效，实际 ${reuse.code}`);
  await sleep(1100); // 规避 DEF-02 同秒 Token 相同导致新 Token 误中黑名单
  const relogin = await api({ method: 'POST', path: '/auth/login', body: { username: 'study', password: 'qa654321' } });
  expectCode(relogin, 0, '新密码登录');
  // 恢复密码
  const back = await api({ method: 'PUT', path: '/profile/password', token: relogin.data.accessToken, body: { oldPassword: 'qa654321', newPassword: '123456', confirmPassword: '123456' } });
  expectCode(back, 0, '恢复密码');
});
await test('F09 旧密码错误时改密被拒绝', async () => {
  const r = await api({ method: 'PUT', path: '/profile/password', token: studentToken, body: { oldPassword: 'bad-old-pw', newPassword: 'qa654321', confirmPassword: 'qa654321' } });
  expect(r.code !== 0, '旧密码错误应被拒绝');
});

// ---------- G. 管理端 ----------
group('G-管理端');
let createdQid = null;
await test('G01 题目分页查询（关键字+科目筛选）', async () => {
  const r = await api({ path: '/admin/questions', token: adminToken, params: { pageNum: 1, pageSize: 5, subject: 'ds' } });
  expectCode(r, 0, '题目分页');
  expect((r.data.total ?? 0) >= 30, `ds 题数异常: ${r.data.total}`);
});
await test('G02 新增题目（含 XSS 载荷题干，验证存储）', async () => {
  const payload = {
    bankCode: 'ky408', type: 'single', subject: 'ds', categoryId: 1, chapter: 'QA测试章',
    difficulty: 1, stem: 'QA测试题干<img src=x onerror=alert(1)>', options: ['甲', '乙'], answer: [0], analysis: 'QA解析', source: 'QA自动化'
  };
  const r = await api({ method: 'POST', path: '/admin/questions', token: adminToken, body: payload });
  expectCode(r, 0, '新增题目');
});
await test('G03 新增题目出现在列表中且载荷原样存储（服务端不过滤）', async () => {
  const r = await api({ path: '/admin/questions', token: adminToken, params: { pageNum: 1, pageSize: 5, keyword: 'QA测试题干' } });
  expectCode(r, 0, '列表查询');
  const list = r.data.records || r.data.list || [];
  const q = list.find(x => (x.stem || '').includes('QA测试题干'));
  expect(q, '新增题目未出现在列表');
  createdQid = q.id;
  expect(q.stem.includes('<img'), 'XSS 载荷被转义/过滤（与预期不符，需复核）');
});
await test('G04 编辑题目', async () => {
  const detail = await api({ path: `/admin/questions/${createdQid}`, token: adminToken });
  const q = detail.data;
  const r = await api({ method: 'PUT', path: `/admin/questions/${createdQid}`, token: adminToken, body: {
    bankCode: q.bankCode, type: q.type, subject: q.subject, categoryId: q.categoryId, chapter: 'QA测试章(改)',
    difficulty: 2, stem: q.stem, options: JSON.parse(typeof q.options === 'string' ? q.options : JSON.stringify(q.options)),
    answer: JSON.parse(typeof q.answer === 'string' ? q.answer : JSON.stringify(q.answer)), analysis: 'QA解析(改)', source: 'QA自动化'
  } });
  expectCode(r, 0, '编辑题目');
});
await test('G05 删除题目（逻辑删除）', async () => {
  const r = await api({ method: 'DELETE', path: `/admin/questions/${createdQid}`, token: adminToken });
  expectCode(r, 0, '删除题目');
  const detail = await api({ path: `/admin/questions/${createdQid}`, token: adminToken });
  expect(detail.code === 40402 || detail.data === null, '删除后仍可查询到');
});
await test('G06 用户分页列表', async () => {
  const r = await api({ path: '/admin/users', token: adminToken, params: { pageNum: 1, pageSize: 10 } });
  expectCode(r, 0, '用户列表');
  expect((r.data.total ?? 0) >= 7, '用户数异常');
});
await test('G07 角色列表', async () => {
  const r = await api({ path: '/admin/users/roles', token: adminToken });
  expectCode(r, 0, '角色列表');
});
await test('G08 分类列表', async () => {
  const r = await api({ path: '/admin/categories', token: adminToken, params: { bankCode: 'ky408', subject: 'ds' } });
  expectCode(r, 0, '分类列表');
});
await test('G09 新增题目参数校验（题型非法）', async () => {
  const r = await api({ method: 'POST', path: '/admin/questions', token: adminToken, body: {
    bankCode: 'ky408', type: 'essay', subject: 'ds', categoryId: 1, chapter: 'x', difficulty: 1, stem: 'x', options: ['a', 'b'], answer: [0]
  } });
  expectCode(r, 40000, '非法题型校验');
});

// ---------- H. 越权与注入 ----------
group('H-越权与注入');
await sleep(1100); // F08 改密已拉黑 studentToken；等待 1s 确保新 Token 不同（DEF-02）
{
  const r = await api({ method: 'POST', path: '/auth/login', body: { username: 'study', password: '123456' } });
  studentToken = r.data?.accessToken || studentToken;
}
await test('H01 学员访问管理端接口返回 40300', async () => {
  const r = await api({ path: '/admin/users', token: studentToken });
  expectCode(r, 40300, '学员越权访问');
});
await test('H02 学员新增题目被拒绝', async () => {
  const r = await api({ method: 'POST', path: '/admin/questions', token: studentToken, body: {} });
  expectCode(r, 40300, '学员越权写');
});
await test('H03 SQL 注入探测：抽题科目参数', async () => {
  const r = await api({ path: '/practice/questions', token: studentToken, params: { subject: "ds' OR '1'='1", limit: 5 } });
  expect(r.code === 0 || r.code === 40000, `注入探测异常 code=${r.code}`);
  if (r.code === 0) expect(r.data.every(q => q.subject === "ds' OR '1'='1") || r.data.length === 0, '注入疑似生效：返回了非预期数据');
});
await test('H04 SQL 注入探测：登录用户名', async () => {
  const r = await api({ method: 'POST', path: '/auth/login', body: { username: "admin' OR '1'='1' --", password: 'x123456' } });
  expect([40401, 40104].includes(r.code), `注入疑似生效或异常 code=${r.code}`);
});
await test('H05 SQL 注入探测：管理端关键字搜索', async () => {
  const r = await api({ path: '/admin/questions', token: adminToken, params: { pageNum: 1, pageSize: 5, keyword: "%' OR 1=1 --" } });
  expect(r.code === 0, `注入导致服务异常 code=${r.code}`);
});
await test('H06 超大 pageSize 分页压力探测', async () => {
  const r = await api({ path: '/admin/questions', token: adminToken, params: { pageNum: 1, pageSize: 10000 } });
  expect(r.code !== null && r.httpStatus === 200, `服务异常 http=${r.httpStatus}`);
});
await test('H07 非法 JSON 请求体返回 40000', async () => {
  const url = BASE + '/auth/login';
  const res = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-Forwarded-For': defaultIp }, body: '{bad json' });
  const j = await res.json();
  expect(j.code === 40000, `非法 JSON 应返回 40000，实际 ${j.code}`);
});
await test('H08 注销后旧 accessToken 进入黑名单（40103）', async () => {
  // 复用 H 组开头登录的 studentToken（H08 之后不再使用该 Token，可安全注销）
  // 避免再次登录消耗 /auth/login 限流配额
  const tk = studentToken;
  await api({ method: 'POST', path: '/auth/logout', token: tk });
  const reuse = await api({ path: '/profile', token: tk });
  expectCode(reuse, 40103, '注销后 Token 复用');
});

// ---------- I. 限流（最后执行） ----------
group('I-限流');
// 注：DEF-03 修复后限流按真实连接 IP 计，dev 环境 capacity = 5 × multiplier(2) = 10/60s。
// 本组用例与前面 A/F/H 组的登录共用 127.0.0.1 限流桶，12 次内必然触发 42900。
await test('I01 登录限流：同一真实 IP 高频登录被 42900 拦截', async () => {
  let lastCode = 0, limited = false;
  for (let i = 0; i < 12; i++) {
    const r = await api({ method: 'POST', path: '/auth/login', body: { username: 'study', password: '123456' } });
    lastCode = r.code;
    if (r.code === 42900) { limited = true; break; }
  }
  expect(limited, `12 次登录未触发限流（最后一次 code=${lastCode}）`);
});
// DEF-03 探针：伪造 X-Forwarded-For 不应再切换限流桶（已修复则返回 42900）
await testKnown('I02 X-Forwarded-For 伪造可绕过 IP 限流', 'DEF-03', async () => {
  const r = await api({ method: 'POST', path: '/auth/login', ip: '10.99.0.2', body: { username: 'study', password: '123456' } });
  return r.code === 0; // true = 绕过成功 = 缺陷仍存在；42900 = 已修复
});

// ============================== 汇总 ==============================
const pass = cases.filter(c => c.status === 'PASS').length;
const known = cases.filter(c => c.status === 'KNOWN');
const fail = cases.filter(c => c.status === 'FAIL');
const summary = {
  target: BASE,
  time: new Date().toISOString(),
  total: cases.length, pass, known: known.length, fail: fail.length,
  passRate: ((pass / cases.length) * 100).toFixed(1) + '%',
  cases
};
mkdirSync(resultsDir, { recursive: true });
writeFileSync(join(resultsDir, 'api-test-result.json'), JSON.stringify(summary, null, 2), 'utf-8');
console.log(`\n=== 结果: ${pass} 通过 / ${known.length} 已知缺陷 / ${fail.length} 失败（共 ${cases.length} 例）===`);
if (known.length) {
  console.log('已知缺陷（仍复现）:');
  known.forEach(k => console.log(`  [${k.id}] ${k.defect} ${k.name}`));
}
if (fail.length) {
  console.log('失败用例:');
  fail.forEach(f => console.log(`  [${f.id}] ${f.group} ${f.name}: ${f.detail}`));
}
process.exit(fail.length ? 1 : 0);
