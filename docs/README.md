# 研题通·刷题训练系统（全栈版）

面向考研 408（数据结构、计算机组成原理、操作系统、计算机网络）的在线刷题训练系统，已内置**题库大类体系**：当前上线「考研408·计算机学科专业基础」题库（120 道自研题目），架构上支持按题库无缝扩展软考、计算机等级考试等其他考试大类。前后端分离架构，包含学员端与管理端，覆盖抽题训练、自动判分、错题本、模拟考试、学习统计、学习计划及后台题库/用户管理。

## 功能模块

### 学员端

| 模块 | 说明 |
| --- | --- |
| 登录认证 | 用户名+密码登录、邮箱验证码登录（6 位数字码、5 分钟有效）、双 Token 无感刷新、过期自动退出 |
| 题库切换 | 顶部题库大类下拉一键切换，选择持久化；刷题、组卷、统计均按题库隔离，当前上线考研408 |
| 仪表盘 | 学习总览、分科统计、近 14 天刷题趋势（按当前题库统计） |
| 刷题训练 | 按科目/章节/题型/难度抽题（顺序/随机等模式），提交作答即时判分 |
| 错题本 | 错题自动归集，含题面与解析，支持标记已掌握 |
| 模拟考试 | 自定义题量与时长的组卷（限定在当前题库内）、交卷判分、考试历史与成绩报告 |
| 学习统计 | 总览、分科统计、刷题趋势接口数据（科目按题库定义动态返回） |
| 学习计划 | 每日目标题数与考研初试日期管理 |
| 个人中心 | 个人资料查询/修改、修改密码 |

### 管理端（ADMIN 角色）

| 模块 | 说明 |
| --- | --- |
| 用户账号管理 | 用户分页查询、新增、编辑、启用/禁用、重置密码、分配角色、删除 |
| 题目管理 | 按题库+科目筛选，题目分页查询（关键字/科目/分类/题型/难度）、新增、编辑、删除 |
| 分类管理 | 按题库选择、科目标签页切换，分类查询、新增、编辑、删除 |

## 技术栈

### 后端（ytt-server）

| 技术 | 版本 | 用途 |
| --- | --- | --- |
| JDK | 1.8 | 运行环境 |
| Spring Boot | 2.7.18 | 应用框架（Web / Validation / Security / AOP） |
| Spring Security | 随 Boot 管理 | 认证与授权（JWT 无状态） |
| MyBatis-Plus | 3.5.5 | 持久层 ORM |
| MySQL Connector/J | 8.0.33 | MySQL 驱动（向下兼容 5.7 服务端） |
| Flyway | 7.15.0 | 数据库版本迁移 |
| JJWT | 0.9.1 | JWT 签发与校验 |
| springdoc-openapi-ui | 1.7.0 | Swagger/OpenAPI 接口文档 |
| spring-boot-starter-mail | 随 Boot 管理 | SMTP 邮件发送（登录验证码） |
| H2 | 1.4.200 | 测试期内存数据库（MySQL 兼容模式） |

### 前端（ytt-web）

| 技术 | 版本 | 用途 |
| --- | --- | --- |
| Vue | 3.4 | 前端框架 |
| TypeScript | ~5.2 | 类型系统 |
| Vite | 4.5 | 构建与开发服务器 |
| Element Plus | 2.7 | UI 组件库 |
| Pinia | 2.1 | 状态管理 |
| Vue Router | 4.3 | 路由 |
| Axios | 1.6 | HTTP 客户端 |
| Vitest | 1.4 | 单元测试 |

## 环境要求

- JDK 8（需配置 `JAVA_HOME`）
- Maven 3.6+（项目自带 Maven Wrapper：`ytt-server/mvnw.cmd`，可免装本机 Maven）
- Node.js 16+（含 npm）
- MySQL 5.7+（utf8mb4 字符集）

## 快速启动

### 1. 创建数据库

```sql
CREATE DATABASE ytt_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

首次启动后端时，Flyway 自动执行 `db/migration` 下的迁移脚本建表（`V1__init.sql`）、写入种子数据（`V2__seed.sql`，含内置角色、权限点、题目分类与首批 41 道题目）、`V3__add_email.sql` 为用户表补充邮箱字段、`V4__exam_bank_expand.sql` 建立题库大类体系（`exam_bank` 表、题目/分类的 `bank_code` 归属字段）并将考研408 题库扩充至 **120 题**（数据结构/计算机组成原理/操作系统/计算机网络各 30 题，新增题目难度按 易 23 / 中 37 / 难 19 分布）；随后由启动初始化器 `DataInitializer` 自动创建内置账号（若不存在）：

| 登录用户名 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `123456` | 管理员（ADMIN） |
| `study` | `123456` | 学员（STUDENT） |

> 密码登录以**用户名 + 密码**验证（用户名是唯一标识）；另支持「验证码登录」选项卡——按邮箱接收 6 位验证码登录。存量账号的邮箱由 V3 迁移按 `{用户名}@yantitong.local` 自动回填。

### 2. 启动后端

```powershell
# Windows（需先设置 JAVA_HOME 指向 JDK 8）
cd ytt-server
.\mvnw.cmd spring-boot:run
```

- 服务端口：`8080`
- 上下文路径：`/api`（即接口根地址为 `http://localhost:8080/api`）
- 默认激活 `dev` profile，数据源默认连接 `localhost:3306/ytt_db`（账号/密码 `root/root`，见 `application-dev.yml`）
- 邮箱验证码功能需通过环境变量注入发信账号与 SMTP 授权码（配置文件中不含明文）：PowerShell 下执行 `$env:MAIL_USERNAME="你的邮箱@qq.com"; $env:MAIL_PASSWORD="SMTP授权码"` 后再启动；未设置时应用正常启动，仅发送验证码返回 `50002`

### 3. 启动前端

```powershell
cd ytt-web
npm install
npm run dev
```

- 开发端口：`5173`
- `vite.config.ts` 已配置代理：`/api` → `http://localhost:8080`
- 浏览器访问 `http://localhost:5173`，使用上方演示账号登录

### 4. 在线接口文档

后端启动后访问：<http://localhost:8080/api/swagger-ui.html>

## 目录结构

```
YanTiTong-FS/
├── docs/                          # 交付文档（本目录）
├── tools/                         # 辅助工具脚本
├── ytt-server/                    # 后端（Spring Boot）
│   ├── .mvn/                      # Maven Wrapper 配置
│   ├── src/main/java/com/ytt/     # 业务代码（common/config/entity/mapper/modules/security）
│   ├── src/main/resources/        # application*.yml、db/migration 迁移脚本、logback
│   ├── mvnw.cmd                   # Maven Wrapper（Windows）
│   └── pom.xml                    # 依赖与构建配置
└── ytt-web/                       # 前端（Vue3 + Vite）
    ├── src/api/                   # 后端接口封装
    ├── src/components/            # 通用组件
    ├── src/layouts/               # 页面布局
    ├── src/router/                # 路由定义
    ├── src/stores/                # Pinia 状态
    ├── src/styles/                # 全局样式
    ├── src/types/                 # TS 类型定义
    ├── src/utils/                 # 工具函数
    ├── src/views/                 # 页面视图
    ├── index.html                 # 入口 HTML
    ├── package.json               # 前端依赖与脚本
    └── vite.config.ts             # Vite 配置（端口/代理/测试）
```

## 运行测试

```powershell
# 后端（使用 H2 内存库，无需 MySQL）
cd ytt-server
.\mvnw.cmd test

# 前端
cd ytt-web
npm run test
```

## 产品路线图（后续考试大类）

题库大类采用「数据驱动」设计：`exam_bank` 表每一行定义一个题库（编码、名称、科目 JSON、上线状态），新增考试大类无需改动刷题、组卷、统计等业务代码。上线节奏规划如下（时间为计划值，可能随开发进度调整）：

| 版本 | 考试大类（题库编码） | 计划科目 | 预计上线 | 状态 |
| --- | --- | --- | --- | --- |
| v1.0 | 考研408·计算机学科专业基础（`ky408`） | 数据结构、计算机组成原理、操作系统、计算机网络 | 2026-10 | 已上线（120 题） |
| v1.1 | 软考·软件设计师（中级）（`rk_rj_sj`） | 基础知识、应用技术 | 2026-12 | 计划中 |
| v1.2 | 全国计算机等级考试·二级 C 语言（`ncre_2c`） | 公共基础知识、C 语言程序设计 | 2027-03 | 计划中 |
| v1.3 | 考研数学一（`ky_math1`） | 高等数学、线性代数、概率论与数理统计 | 2027-06 | 规划中 |
| v2.0 | 考研英语一（`ky_eng1`）、公务员行测（`gw_xc`） | 待定 | 2027 年下半年 | 远期规划 |

> 各题库上线前会在 `exam_bank` 中以 `status=0`（未上线）预置，前端切换器只展示已上线题库；上线状态、题目扩充进度以版本发布说明为准。

## 测试与验收

| 层级 | 命令 | 当前结果 |
| --- | --- | --- |
| 后端单元/集成测试（H2 + Flyway V1～V4） | `ytt-server` 下 `.\mvnw.cmd test` | 63 个用例全部通过 |
| 前端单元测试 | `ytt-web` 下 `npm run test` | 14 个用例全部通过 |
| 前端类型检查 | `npm run type-check` | 通过 |
| 前端生产构建 | `npm run build` | 通过 |

题库扩容与切换的关键验收项由 `BankSwitchTest` 自动化覆盖：`/common/banks` 返回已上线题库及科目定义；考研408 库存 120 题、四科各 30 题；刷题/组卷/统计按 `bankCode` 隔离；非法题库编码返回参数错误；组卷时科目不属于该题库会被拒绝。

## 文档索引

| 文档 | 说明 |
| --- | --- |
| [产品需求文档.md](./产品需求文档.md) | 产品定位、功能需求与验收要点、非功能指标、版本路线图 |
| [接口文档.md](./接口文档.md) | 全部 REST 接口明细、统一响应约定与错误码表 |
| [数据库设计.md](./数据库设计.md) | 数据表结构与关系说明 |
| [用户操作手册.md](./用户操作手册.md) | 学员端/管理端逐步操作说明与常见问题 |
| [测试计划.md](./测试计划.md) | 测试范围、用例设计、进度与资源安排 |
| [测试报告.md](./测试报告.md) | 功能/兼容/性能/安全测试结果与缺陷清单 |
| [版本兼容性说明.md](./版本兼容性说明.md) | 各依赖版本选择与兼容性说明 |
| [部署文档.md](./部署文档.md) | 生产构建、Nginx 配置、环境变量与常见问题 |

提交历史与功能对应关系见根目录 [CHANGELOG.md](../CHANGELOG.md)。
