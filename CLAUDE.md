# CLAUDE.md

本文档为 Claude Code (claude.ai/code) 在本仓库中工作提供指导。

## 项目概述

AI 个人助理 (moveon-bot) - 基于 Java 的智能助理，支持文档管理、知识库、任务/待办管理和 RAG 问答。

## 技术栈

| 类别 | 技术 |
|------|------|
| 开发语言 | Java 21 |
| 后端框架 | Spring Boot 3 |
| AI 框架 | LangChain4j |
| 数据库 | PostgreSQL + pgvector |
| 缓存 | Redis |
| 对象存储 | MinIO（存储桶：`moveon-documents`） |
| 文档解析 | Apache Tika + PDFBox + POI |
| 任务调度 | Quartz |
| 安全认证 | Spring Security + JWT (jjwt) |
| API 文档 | SpringDoc OpenAPI |
| 测试框架 | JUnit 5 |
| 部署方式 | Docker + Docker Compose |

## 架构设计

模块化单体架构，包含以下业务模块：
- `auth` - 用户认证与授权
- `document` - 文件上传、解析、摘要
- `knowledge` - 知识库、笔记、批注
- `assistant` - 对话、RAG、记忆、工具调用
- `task` - 待办、提醒、调度
- `notification` - 通知推送
- `infra` - AI 客户端、缓存、存储、公共组件

## 配置说明

### 环境变量

| 变量名 | 说明 |
|--------|------|
| `AI_API_KEY` | 大模型 API 密钥 |
| `AI_MODEL_PROVIDER` | 大模型提供商（如 `aliyun`） |
| `AI_EMBEDDING_MODEL` | Embedding 模型名称 |
| `POSTGRES_URL` | PostgreSQL JDBC 连接 URL |
| `POSTGRES_USER` | PostgreSQL 用户名 |
| `POSTGRES_PASSWORD` | PostgreSQL 密码 |
| `REDIS_URL` | Redis 连接 URL |
| `MINIO_ENDPOINT` | MinIO 端点 |
| `MINIO_ACCESS_KEY` | MinIO 访问密钥 |
| `MINIO_SECRET_KEY` | MinIO 密钥 |
| `MINIO_BUCKET` | MinIO 存储桶（默认：`moveon-documents`） |

### JWT 配置

- 访问令牌有效期：30 分钟
- 刷新令牌有效期：7 天

### 初始管理员账号

- 用户名：`moveon`
- 角色：管理员

## 文件存储结构

```
moveon-documents/
├── {userId}/
│   ├── {date}/
│   │   └── {filename}
```

## 实施计划

项目遵循 10 阶段实施计划（详见 `memory-bank/implementation-plan.md`）：

1. 项目脚手架
2. 本地开发环境（PostgreSQL、Redis、MinIO）
3. 应用骨架与通用能力
4. 认证与授权
5. 文档上传与存储
6. 文档解析与内容入库
7. 向量化与检索
8. RAG 问答
9. 任务与待办管理
10. 提醒、可观测性与验收

## 关键设计决策

- 使用 pgvector 存储向量（1536 维，对应阿里云 Text Embedding 模型）
- 文件存储路径：`{userId}/{date}/{filename}`
- 文档切分：每片段 3-5 段，相邻片段重叠 1-2 段
- 通过数据库初始化脚本创建初始管理员用户 `moveon`

## 文档索引

- `memory-bank/design-document.md` - 设计规格说明
- `memory-bank/tech-stack.md` - 技术栈详细说明
- `memory-bank/implementation-plan.md` - 分阶段实施指南
- `memory-bank/architecture.md` - 架构概览
- `memory-bank/progress.md` - 进度跟踪
