# 项目架构文档

## 整体架构

本项目采用**模块化单体架构**，所有模块在同一个应用中运行，但按业务边界清晰划分。

## 目录结构

```
moveon-bot/
├── src/main/java/com/moveon/
│   ├── MoveonBotApplication.java    # 主应用入口
│   │
│   ├── auth/                         # 认证与授权模块
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   └── config/
│   │
│   ├── document/                     # 文档管理模块
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   └── config/
│   │
│   ├── knowledge/                    # 知识库模块
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   └── config/
│   │
│   ├── assistant/                    # AI 助理问答模块
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   └── config/
│   │
│   ├── task/                         # 任务待办模块
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   └── config/
│   │
│   ├── notification/                 # 通知推送模块
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   └── config/
│   │
│   └── infra/                        # 基础设施模块
│       ├── controller/               # 公共接口（健康检查等）
│       ├── service/                  # 公共服务
│       ├── repository/               # 公共数据访问
│       ├── entity/                   # 公共实体
│       ├── dto/                      # 公共 DTO（ApiResponse, PageResponse）
│       ├── config/                   # 公共配置（OpenAPI 等）
│       └── exception/                # 异常处理
│           ├── BusinessException.java
│           ├── ResourceNotFoundException.java
│           ├── InvalidArgumentException.java
│           ├── ErrorResponse.java
│           └── GlobalExceptionHandler.java
│
├── src/main/resources/
│   ├── application.yml               # 默认配置
│   ├── application-dev.yml           # 开发环境配置
│   └── application-test.yml          # 测试环境配置
│
├── src/test/java/com/moveon/        # 测试代码
│
├── infra/docker/
│   ├── docker-compose.yml            # 本地依赖环境编排
│   └── init-db/
│       └── 01-init.sql               # 数据库初始化脚本
│
└── memory-bank/                      # 项目文档
```

## 模块职责

### auth（认证与授权模块）
- 用户注册、登录、注销
- JWT 令牌签发与验证
- 用户信息与权限管理
- 管理员专属操作（创建新用户）

### document（文档管理模块）
- 文件上传（支持 TXT、PDF、DOCX）
- 文件存储到 MinIO
- 文档元数据管理
- 文档状态跟踪（待解析、已解析、失败）

### knowledge（知识库模块）
- 知识条目管理
- 笔记与批注
- 版本管理
- 收藏功能

### assistant（AI 助理问答模块）
- RAG 问答主流程
- 对话管理
- 问答历史记录
- AI 模型配置与调用

### task（任务待办模块）
- 任务 CRUD 操作
- 任务优先级与截止时间
- 任务筛选与查询
- 任务状态管理

### notification（通知推送模块）
- 提醒触发与记录
- 推送管理
- 通知历史

### infra（基础设施模块）
- 全局异常处理
- 统一响应格式
- 健康检查接口
- OpenAPI 文档配置
- 公共工具类

## 分层架构

每个模块内部按标准分层：

```
模块/
├── controller/     # REST API 层，处理 HTTP 请求和响应
├── service/        # 业务逻辑层，处理核心业务
├── repository/     # 数据访问层，与数据库交互
├── entity/         # 实体类，对应数据库表
├── dto/            # 数据传输对象，用于 API 输入输出
└── config/         # 模块配置
```

## 技术架构

```
┌─────────────────────────────────────────────────────────┐
│                      用户交互层                          │
│              （REST API / Swagger UI）                   │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                      应用服务层                          │
│  ┌──────┬──────┬──────┬──────┬──────┬──────┬──────┐    │
│  │ auth │document│knowledge│assistant│ task │notif │    │
│  └──────┴──────┴──────┴──────┴──────┴──────┴──────┘    │
│                     │  infra  │                         │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                      数据存储层                          │
│  ┌──────────┬──────────┬──────────┬──────────────────┐  │
│  │PostgreSQL│ pgvector │  Redis   │      MinIO       │  │
│  └──────────┴──────────┴──────────┴──────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## 数据流

### 文档上传流程
```
用户上传 → Controller → Service → MinIO 存储 → 写入元数据 → 返回响应
                                    │
                                    ▼
                              异步解析任务
```

### RAG 问答流程
```
用户提问 → Controller → Service → 检索相关片段 (pgvector)
                                      │
                                      ▼
                                 LangChain4j → AI 模型
                                      │
                                      ▼
                                 返回答案 + 引用
```

## 配置管理

### 环境分层
- `application.yml` - 基础配置，包含默认值
- `application-dev.yml` - 本地开发配置，覆盖基础配置
- `application-test.yml` - 测试配置，使用 H2 内存数据库

### 配置优先级
1. 环境变量（最高优先级）
2. 激活的 profile 配置
3. 基础配置

## 异常处理

所有异常通过 `GlobalExceptionHandler` 统一处理：

```
异常类型                  →  HTTP 状态码
─────────────────────────────────────────
NoResourceFoundException  →  404 Not Found
InvalidArgumentException  →  400 Bad Request
ResourceNotFoundException →  404 Not Found
BusinessException         →  400 Bad Request
BadCredentialsException   →  401 Unauthorized
AccessDeniedException     →  403 Forbidden
MethodArgumentNotValidException → 400 Bad Request
Exception (generic)       →  500 Internal Server Error
```

## API 响应格式

### 成功响应
```json
{
  "code": 200,
  "message": "Success",
  "data": { ... }
}
```

### 错误响应
```json
{
  "timestamp": "2026-03-25T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "code": "RESOURCE_NOT_FOUND",
  "message": "Document not found with id: 123",
  "path": "/api/documents/123"
}
```

## 文件存储结构

MinIO 中的文件按以下结构存储：
```
moveon-documents/
├── {userId}/
│   ├── 2026-03-25/
│   │   └── document-name.pdf
│   └── 2026-03-26/
│       └── another-doc.docx
```

## 数据库表设计（待完成）

### 已规划表
- `users` - 用户表
- `documents` - 文档元数据表
- `document_fragments` - 文档片段表
- `document_vectors` - 向量表
- `knowledge_entries` - 知识条目表
- `tasks` - 任务表
- `reminders` - 提醒表
- `qa_logs` - 问答日志表

---

*最后更新：2026-03-25*
