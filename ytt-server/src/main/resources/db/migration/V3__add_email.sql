-- ============================================================
-- V3 用户表新增邮箱字段
-- 1) email 作为邮箱登录的唯一标识，必填、全局唯一
-- 2) 存量账号（如 admin/study）先加列（默认空串）再按用户名规则回填，
--    保证 NOT NULL 与唯一约束可以直接建立
-- 兼容 MySQL 5.7/8.x 与 H2（测试库，MODE=MySQL），故不使用列内联 COMMENT
-- ============================================================

-- 1. 新增邮箱列（必填，默认空串）
ALTER TABLE `sys_user` ADD COLUMN `email` VARCHAR(128) NOT NULL DEFAULT '';

-- 2. 回填存量账号：用户名 + @yantitong.local，保证唯一
UPDATE `sys_user`
SET `email` = CONCAT(`username`, '@yantitong.local')
WHERE `email` IS NULL OR `email` = '';

-- 3. 建立唯一约束（登录、注册均依赖此约束兜底）
ALTER TABLE `sys_user` ADD CONSTRAINT `uk_sys_user_email` UNIQUE (`email`);
