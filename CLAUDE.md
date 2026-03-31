# CLAUDE.md

本文档为 Claude Code (claude.ai/code) 在本仓库中工作提供指导。

## 项目概述

AI 个人助理 (moveon-bot) - 基于 Java 21 和 Spring Boot 3.2.3 的智能助理，支持文档管理（上传、解析、向量化）、语义检索和 RAG 问答。已完成 8/10 个实施阶段。

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 开发语言 | Java | 21 |
| 后端框架 | Spring Boot | 3.2.3 |
| AI 框架 | LangChain4j | 0.29.1 |
| Embedding 模型 | BAAI/bge-m3 (SiliconFlow) | 1024 维 |
| Chat 模型 | Qwen3.5-plus (DashScope) | - |
| 数据库 | PostgreSQL 16 + pgvector | - |
| 缓存 | Redis 7 | - |
| 对象存储 | MinIO | 存储桶：`moveon-documents` |
| 文档解析 | Apache Tika 2.9.1 + PDFBox + POI | - |
| 任务调度 | Quartz | - |
| 安全认证 | Spring Security + JWT (jjwt 0.12.5) | - |
| API 文档 | SpringDoc OpenAPI 2.3.0 | - |
| 监控 | Micrometer Prometheus + Spring Actuator | - |
| 测试框架 | JUnit 5 + TestContainers + H2 | - |
| 部署方式 | Docker + Docker Compose | - |

## 架构设计

模块化单体架构，包含以下业务模块：

### 已实现模块

- `auth` - 用户认证与授权（JWT 登录/刷新、BCrypt 密码、角色权限）
- `document` - 文件上传、解析（TXT/PDF/DOCX）、文本分段、向量化、语义检索
- `assistant` - RAG 问答（检索增强生成）、问答审计日志
- `task` - 任务/待办管理（CRUD、状态流转、优先级、筛选）
- `notification` - 任务到期提醒（定时扫描、即将到期/已过期提醒、应用内记录）
- `infra` - AI 客户端配置、MinIO/Redis 配置、健康检查、统一异常处理、请求日志、指标监控

### 待实现模块

- `knowledge` - 知识库、笔记、批注（后续扩展）

## API 端点一览

### 认证 (`/api/auth`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/auth/login` | 用户登录，返回访问令牌和刷新令牌 | 否 |
| POST | `/auth/refresh` | 刷新访问令牌 | 否 |
| GET | `/auth/me` | 获取当前用户信息 | 是 |
| POST | `/auth/users` | 创建用户（仅管理员） | 是（ADMIN） |

### 文档管理 (`/api/documents`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/documents/upload` | 上传文档（TXT/PDF/DOCX，最大 50MB），自动触发解析和向量化 | 是 |
| GET | `/documents` | 获取当前用户文档列表（分页、状态筛选、排序） | 是 |
| GET | `/documents/{id}` | 获取文档详情 | 是 |
| POST | `/documents/{id}/parse` | 手动重新解析文档 | 是 |
| GET | `/documents/{id}/fragments` | 获取文档的文本片段 | 是 |
| POST | `/documents/{id}/embed` | 手动重新向量化文档 | 是 |
| GET | `/documents/search?query=&topK=` | 语义搜索文档 | 是 |

### AI 助理 (`/api/assistant`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/assistant/ask` | RAG 问答（检索相关文档片段 + LLM 生成答案） | 是 |
| POST | `/assistant/ask/stream` | 流式 RAG 问答 | 是 |

### 任务管理 (`/api/tasks`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/tasks` | 创建任务 | 是 |
| GET | `/tasks` | 获取任务列表（分页、按状态/优先级/截止时间筛选） | 是 |
| GET | `/tasks/{id}` | 获取任务详情 | 是 |
| PUT | `/tasks/{id}` | 更新任务 | 是 |
| POST | `/tasks/{id}/complete` | 标记任务完成 | 是 |
| POST | `/tasks/{id}/cancel` | 取消任务 | 是 |
| DELETE | `/tasks/{id}` | 删除任务 | 是 |

### 系统 (`/api`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/health` | 基础健康检查 | 否 |
| GET | `/health/detailed` | 详细健康检查（DB/Redis/MinIO 状态） | 否 |

## 数据库表结构

| 表名 | 说明 |
|------|------|
| `users` | 用户账号（id, username, password, status, role） |
| `documents` | 文档元数据（id, user_id, file_name, status, storage_path 等） |
| `document_fragments` | 文档文本片段（id, document_id, fragment_index, content） |
| `document_vectors` | 向量嵌入（id, fragment_id, embedding vector(1024), status） |
| `qa_logs` | 问答审计日志（id, user_id, question, answer, hit_document_ids 等） |
| `tasks` | 任务/待办（id, user_id, title, description, priority, status, due_date, completed_at） |
| `reminders` | 任务提醒记录（id, task_id, user_id, title, message, remind_type, status, remind_at, sent_at） |

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `AI_EMBEDDING_API_KEY` | Embedding 模型 API 密钥（SiliconFlow） | - |
| `AI_EMBEDDING_BASE_URL` | Embedding API 端点 | `https://api.siliconflow.cn/v1` |
| `AI_EMBEDDING_MODEL` | Embedding 模型名称 | `BAAI/bge-m3` |
| `AI_CHAT_API_KEY` | Chat 模型 API 密钥（DashScope） | - |
| `AI_CHAT_BASE_URL` | Chat API 端点 | `https://coding.dashscope.aliyuncs.com/v1` |
| `AI_CHAT_MODEL` | Chat 模型名称 | `qwen3.5-plus` |
| `POSTGRES_URL` | PostgreSQL JDBC 连接 URL | `jdbc:postgresql://localhost:5432/moveon` |
| `POSTGRES_USER` | PostgreSQL 用户名 | `moveon` |
| `POSTGRES_PASSWORD` | PostgreSQL 密码 | `moveon` |
| `REDIS_URL` | Redis 连接 URL | `redis://localhost:6379` |
| `MINIO_ENDPOINT` | MinIO 端点 | `http://localhost:9000` |
| `MINIO_ACCESS_KEY` | MinIO 访问密钥 | `minioadmin` |
| `MINIO_SECRET_KEY` | MinIO 密钥 | `minioadmin` |
| `MINIO_BUCKET` | MinIO 存储桶 | `moveon-documents` |
| `JWT_SECRET` | JWT 密钥 | (开发环境默认值) |

### RAG 参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `rag.default-top-k` | 5 | 默认检索数量 |
| `rag.max-top-k` | 20 | 最大检索数量 |
| `rag.max-question-length` | 1000 | 问题最大长度 |
| `rag.max-answer-length` | 4000 | 答案最大长度 |
| `rag.min-score-threshold` | 0.3 | 最低相似度阈值 |

### JWT 配置

- 访问令牌有效期：30 分钟
- 刷新令牌有效期：7 天

### 初始管理员账号

- 用户名：`moveon` / 密码：`moveon123` / 角色：管理员
- 通过 `UserInitializer`（应用启动时）或 `01-init.sql`（数据库初始化脚本）自动创建

## 文件存储结构

```
moveon-documents/
└── {userId}/
    └── {date}/
        └── {timestamp}-{filename}
```

## 核心业务流程

### 文档处理流水线

```
上传文件 → MinIO 存储 → 异步解析(Apache Tika) → 文本分段 → 异步向量化(BGE-M3) → 就绪
```

状态流转：`PENDING → PARSING → PARSED → EMBEDDING → COMPLETED`（任一阶段可 `FAILED`，支持重试）

### RAG 问答流程

```
用户提问 → 输入校验 → 语义检索(pgvector 余弦距离) → 过滤低分结果(min_score ≥ 0.3)
         → 构建 RAG 提示 → Chat Model 生成答案 → 附带引用来源 → 记录审计日志
```

## 关键设计决策

- 使用 pgvector 存储向量（1024 维，对应 BAAI/bge-m3 模型）
- 文件存储路径：`{userId}/{date}/{timestamp}-{filename}`
- 文档切分：每片段约 4 段，相邻片段重叠 1 段
- 向量检索使用 pgvector 余弦距离（`<=>` 操作符）
- Embedding 和 Chat 使用不同模型/提供商，通过 `ai.embedding.*` 和 `ai.chat.*` 分别配置
- AI 配置分离：Embedding（SiliconFlow 免费模型）和 Chat（阿里云 DashScope）独立配置
- 通过数据库初始化脚本 + `UserInitializer` 双重保障创建初始管理员用户
- 问答审计日志保存失败不影响主流程

## 实施计划

项目遵循 10 阶段实施计划（详见 `memory-bank/implementation-plan.md`）：

1. ✅ 项目脚手架
2. ✅ 本地开发环境（PostgreSQL、Redis、MinIO）
3. ✅ 应用骨架与通用能力
4. ✅ 认证与授权
5. ✅ 文档上传与存储
6. ✅ 文档解析与内容入库
7. ✅ 向量化与检索
8. ✅ RAG 问答
9. ✅ 任务与待办管理
10. ✅ 提醒、可观测性与验收

## 文档索引

- `memory-bank/design-document.md` - 设计规格说明
- `memory-bank/tech-stack.md` - 技术栈详细说明
- `memory-bank/implementation-plan.md` - 分阶段实施指南
- `memory-bank/architecture.md` - 架构概览
- `memory-bank/progress.md` - 进度跟踪（含各阶段详细完成记录）
