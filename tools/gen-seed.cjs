/* 一次性脚本：读取旧项目 questions-seed.js，生成 Flyway V2 种子 SQL */
const fs = require('fs');
const path = require('path');

// 1. 模拟浏览器环境加载种子数据
global.window = { YTT: {} };
require(path.resolve(__dirname, '../../YanTiTong408/src/scripts/questions-seed.js'));
const questions = global.window.YTT.SEED_QUESTIONS;

const SUBJECTS = [
  { key: 'ds', name: '数据结构' },
  { key: 'co', name: '计算机组成原理' },
  { key: 'os', name: '操作系统' },
  { key: 'cn', name: '计算机网络' }
];

// SQL 字符串转义（先转义反斜杠防止 MySQL 吃掉 JSON 中的 \"，再单引号 -> ''）
function esc(s) {
  if (s === null || s === undefined) return 'NULL';
  return "'" + String(s).replace(/\\/g, '\\\\').replace(/'/g, "''") + "'";
}

// 2. 按科目归集章节为分类，分类 id 固定 1..N
const categories = [];            // {id, subject, name, sort}
const chapterToCategory = {};     // `${subject}|${chapter}` -> id
let catId = 0;
for (const sub of SUBJECTS) {
  let sort = 0;
  const seen = new Set();
  for (const q of questions) {
    if (q.subject !== sub.key) continue;
    if (seen.has(q.chapter)) continue;
    seen.add(q.chapter);
    catId += 1;
    sort += 1;
    categories.push({ id: catId, subject: sub.key, name: q.chapter, sort });
    chapterToCategory[`${sub.key}|${q.chapter}`] = catId;
  }
}

const lines = [];
lines.push('-- ============================================================');
lines.push('-- V2 初始化种子数据');
lines.push('-- 1) 内置角色 ADMIN/STUDENT 与权限点');
lines.push('-- 2) 题目分类（由内置题库章节归集生成）');
lines.push('-- 3) 内置题库 41 道 408 原创题目（options/answer 以 JSON 存储）');
lines.push('-- ============================================================');
lines.push('');

// 角色
lines.push('-- 内置角色');
lines.push("INSERT INTO `sys_role` (`id`, `code`, `name`) VALUES");
lines.push("(1, 'ADMIN', '系统管理员'),");
lines.push("(2, 'STUDENT', '考研学员');");
lines.push('');

// 权限点
const perms = [
  [1, 'admin:user:list', '用户查询'], [2, 'admin:user:create', '新增用户'],
  [3, 'admin:user:update', '编辑用户'], [4, 'admin:user:delete', '删除用户'],
  [5, 'admin:user:status', '启用/禁用用户'], [6, 'admin:user:reset-password', '重置密码'],
  [7, 'admin:user:assign-role', '分配角色'],
  [8, 'admin:question:list', '题目查询'], [9, 'admin:question:create', '新增题目'],
  [10, 'admin:question:update', '编辑题目'], [11, 'admin:question:delete', '删除题目'],
  [12, 'admin:category:manage', '分类管理']
];
lines.push('-- 后台权限点');
lines.push('INSERT INTO `sys_permission` (`id`, `code`, `name`, `type`) VALUES');
lines.push(perms.map(p => `(${p[0]}, ${esc(p[1])}, ${esc(p[2])}, 'api')`).join(',\n') + ';');
lines.push('');
lines.push('-- ADMIN 角色拥有全部权限');
lines.push('INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES');
lines.push(perms.map(p => `(1, ${p[0]})`).join(',\n') + ';');
lines.push('');

// 分类
lines.push('-- 题目分类（按科目归集章节）');
lines.push('INSERT INTO `question_category` (`id`, `subject`, `name`, `sort`) VALUES');
lines.push(categories.map(c => `(${c.id}, ${esc(c.subject)}, ${esc(c.name)}, ${c.sort})`).join(',\n') + ';');
lines.push('');

// 题目
lines.push('-- 内置题库（41 题）');
for (const q of questions) {
  const cid = chapterToCategory[`${q.subject}|${q.chapter}`];
  const optionsJson = JSON.stringify(q.options);
  const answerJson = JSON.stringify(q.answer);
  lines.push('INSERT INTO `question` (`id`, `type`, `subject`, `category_id`, `chapter`, `difficulty`, `stem`, `options`, `answer`, `analysis`, `source`, `version`, `deleted`) VALUES ('
    + `${Number(q.id)}, ${esc(q.type)}, ${esc(q.subject)}, ${cid}, ${esc(q.chapter)}, ${q.difficulty}, `
    + `${esc(q.stem)}, ${esc(optionsJson)}, ${esc(answerJson)}, ${esc(q.analysis)}, '内置题库', 0, 0);`);
}

const out = lines.join('\r\n') + '\r\n';
const outPath = path.resolve(__dirname, '../ytt-server/src/main/resources/db/migration/V2__seed.sql');
fs.mkdirSync(path.dirname(outPath), { recursive: true });
fs.writeFileSync(outPath, out, 'utf8');
console.log(`OK: ${questions.length} questions, ${categories.length} categories -> ${outPath}`);
