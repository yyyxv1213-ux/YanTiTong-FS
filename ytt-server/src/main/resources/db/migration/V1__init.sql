-- ============================================================
-- V1 初始化数据库结构
-- 研题通·考研408刷题训练系统
-- 说明：兼容 MySQL 5.7/8.x 与 H2（测试库，MODE=MySQL）
--       时间列统一 DEFAULT CURRENT_TIMESTAMP，不使用 ON UPDATE
-- ============================================================

-- ----------------------------
-- 1. 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(64) NOT NULL COMMENT '登录用户名',
  `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
  `real_name`   VARCHAR(64) DEFAULT NULL COMMENT '真实姓名/昵称',
  `status`      INT         NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `version`     INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     INT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 2. 角色表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id`   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(64) NOT NULL COMMENT '角色编码（如 ADMIN/STUDENT）',
  `name` VARCHAR(64) NOT NULL COMMENT '角色名称',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ----------------------------
-- 3. 用户-角色关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id`      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ----------------------------
-- 4. 权限点表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_permission` (
  `id`   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(128) NOT NULL COMMENT '权限编码',
  `name` VARCHAR(64) NOT NULL COMMENT '权限名称',
  `type` VARCHAR(16) NOT NULL DEFAULT 'api' COMMENT '类型：menu菜单/api接口',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_permission_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限点表';

-- ----------------------------
-- 5. 角色-权限关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id`       BIGINT NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ----------------------------
-- 6. 题目分类表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `question_category` (
  `id`      BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject` VARCHAR(16) NOT NULL COMMENT '科目：ds/co/os/cn',
  `name`    VARCHAR(64) NOT NULL COMMENT '分类名称',
  `sort`    INT         NOT NULL DEFAULT 0 COMMENT '排序号',
  PRIMARY KEY (`id`),
  KEY `idx_category_subject` (`subject`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目分类表';

-- ----------------------------
-- 7. 题目表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `question` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type`        VARCHAR(16) NOT NULL COMMENT '题型：single单选/multi多选/judge判断',
  `subject`     VARCHAR(16) NOT NULL COMMENT '科目：ds/co/os/cn',
  `category_id` BIGINT      DEFAULT NULL COMMENT '所属分类ID',
  `chapter`     VARCHAR(64) DEFAULT NULL COMMENT '章节',
  `difficulty`  INT         NOT NULL DEFAULT 1 COMMENT '难度：1简单 2中等 3困难',
  `stem`        TEXT        NOT NULL COMMENT '题干（支持富文本HTML）',
  `options`     TEXT        NOT NULL COMMENT '选项列表（JSON数组）',
  `answer`      TEXT        NOT NULL COMMENT '正确选项下标（JSON数组）',
  `analysis`    TEXT        DEFAULT NULL COMMENT '答案解析',
  `source`      VARCHAR(64) DEFAULT NULL COMMENT '来源',
  `create_by`   BIGINT      DEFAULT NULL COMMENT '创建人',
  `version`     INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     INT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_question_subject` (`subject`),
  KEY `idx_question_category` (`category_id`),
  KEY `idx_question_difficulty` (`difficulty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- ----------------------------
-- 8. 刷题记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `practice_record` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT NOT NULL COMMENT '用户ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `user_answer` TEXT   DEFAULT NULL COMMENT '用户作答（JSON数组）',
  `correct`     INT    NOT NULL DEFAULT 0 COMMENT '是否正确：1对 0错',
  `mode`        VARCHAR(16) DEFAULT 'normal' COMMENT '模式：normal顺序/wrong错题/random随机',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作答时间',
  PRIMARY KEY (`id`),
  KEY `idx_practice_user` (`user_id`),
  KEY `idx_practice_question` (`question_id`),
  KEY `idx_practice_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='刷题记录表';

-- ----------------------------
-- 9. 错题本表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `wrong_question` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT NOT NULL COMMENT '用户ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `wrong_count` INT    NOT NULL DEFAULT 1 COMMENT '累计错误次数',
  `last_answer` TEXT   DEFAULT NULL COMMENT '最近一次作答（JSON数组）',
  `mastered`    INT    NOT NULL DEFAULT 0 COMMENT '是否已掌握：1是 0否',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wrong_user_question` (`user_id`, `question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本表';

-- ----------------------------
-- 10. 模拟考试记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `exam_record` (
  `id`               BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`          BIGINT NOT NULL COMMENT '用户ID',
  `total_count`      INT    NOT NULL COMMENT '试卷题目数',
  `correct_count`    INT    NOT NULL COMMENT '答对题数',
  `score`            INT    NOT NULL COMMENT '得分',
  `duration_seconds` INT    NOT NULL DEFAULT 0 COMMENT '考试用时（秒）',
  `create_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '交卷时间',
  PRIMARY KEY (`id`),
  KEY `idx_exam_user` (`user_id`),
  KEY `idx_exam_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模拟考试记录表';

-- ----------------------------
-- 11. 考试答题明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `exam_detail` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `exam_id`     BIGINT NOT NULL COMMENT '考试记录ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `user_answer` TEXT   DEFAULT NULL COMMENT '用户作答（JSON数组）',
  `correct`     INT    NOT NULL DEFAULT 0 COMMENT '是否正确：1对 0错',
  PRIMARY KEY (`id`),
  KEY `idx_exam_detail_exam` (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试答题明细表';

-- ----------------------------
-- 12. 学习计划表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `study_plan` (
  `id`           BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`      BIGINT NOT NULL COMMENT '用户ID',
  `daily_target` INT    NOT NULL DEFAULT 30 COMMENT '每日目标题数',
  `exam_date`    DATE   DEFAULT NULL COMMENT '考研初试日期',
  `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_study_plan_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习计划表';

-- ----------------------------
-- 13. 登录日志表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `login_log` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT      DEFAULT NULL COMMENT '用户ID（登录失败时可能为空）',
  `username`    VARCHAR(64) NOT NULL COMMENT '登录用户名',
  `ip`          VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
  `success`     INT         NOT NULL DEFAULT 0 COMMENT '是否成功：1成功 0失败',
  `message`     VARCHAR(255) DEFAULT NULL COMMENT '结果说明',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_login_log_user` (`user_id`),
  KEY `idx_login_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';
