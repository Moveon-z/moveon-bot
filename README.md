# Moveon Bot - AI Personal Assistant

一个基于 Java 21 和 Spring Boot 3 的智能 AI 助理，提供文档管理（上传、解析、向量化）、语义检索和 RAG 问答功能。

## 功能特性

- **用户认证** - JWT 登录/刷新、角色权限控制（ADMIN/USER）
- **文档管理** - 上传 TXT/PDF/DOCX 文件，自动解析、分段、向量化
- **语义检索** - 基于向量相似度的文档内容检索（pgvector）
- **RAG 问答** - 检索增强生成，仅基于文档内容回答，附带引用来源
- **问答审计** - 记录每次问答的详细信息（问题、答案、命中文档、响应时间）

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 开发语言 | Java | 21 |
| 后端框架 | Spring Boot | 3.2.3 |
| AI 框架 | LangChain4j | 0.29.1 |
| Embedding 模型 | BAAI/bge-m3 (SiliconFlow) | 1024 维 |
| Chat 模型 | Qwen3.5-plus (阿里云 DashScope) | - |
| 数据库 | PostgreSQL 16 + pgvector | - |
| 缓存 | Redis 7 | - |
| 对象存储 | MinIO | - |
| 文档解析 | Apache Tika + PDFBox + POI | - |
| 安全认证 | Spring Security + JWT | - |
| API 文档 | SpringDoc OpenAPI / Swagger UI | - |
| 测试 | JUnit 5 + TestContainers + H2 | - |

## 项目结构

```
moveon-bot/
├── src/main/java/com/moveon/
│   ├── MoveonBotApplication.java    # 主应用入口
│   ├── auth/                         # 认证与授权模块
│   │   ├── controller/               # AuthController
│   │   ├── service/                  # AuthService, JwtService, UserInitializer
│   │   ├── entity/                   # User, UserStatus, UserRole
│   │   ├── repository/               # UserRepository
│   │   ├── dto/                      # Login/Refresh/CreateUser DTO
│   │   └── config/                   # AuthConfig, JwtAuthenticationFilter
│   ├── document/                     # 文档管理模块
│   │   ├── controller/               # DocumentController
│   │   ├── service/                  # DocumentService, Parsing, Embedding, Search
│   │   ├── entity/                   # Document, Fragment, Vector + 枚举
│   │   ├── repository/               # Document, Fragment, Vector Repository
│   │   └── dto/                      # Upload/Response/SearchResult DTO
│   ├── assistant/                    # AI 助理问答模块
│   │   ├── controller/               # AssistantController
│   │   ├── service/                  # RagService
│   │   ├── entity/                   # QaLog
│   │   ├── repository/               # QaLogRepository
│   │   ├── dto/                      # AskRequest/Response, Citation
│   │   └── config/                   # RagConfig
│   ├── knowledge/                    # 知识库模块（待实现）
│   ├── task/                         # 任务待办模块（待实现）
│   ├── notification/                 # 通知推送模块（待实现）
│   └── infra/                        # 基础设施模块
│       ├── config/                   # SecurityConfig, AiConfig, InfraConfig, OpenApiConfig
│       ├── service/                  # HealthCheckService
│       ├── controller/               # HealthController
│       └── exception/                # 统一异常处理
├── src/main/resources/
│   ├── application.yml               # 默认配置
│   ├── application-dev.yml           # 开发环境配置
│   └── application-test.yml          # 测试环境配置
├── src/test/java/com/moveon/         # 测试代码
├── infra/docker/
│   ├── docker-compose.yml            # 本地依赖环境（PostgreSQL/Redis/MinIO）
│   └── init-db/
│       └── 01-init.sql               # 数据库初始化脚本
├── memory-bank/                      # 项目文档
└── pom.xml                           # Maven 配置
```

## 快速开始

### 前置要求

- Java 21+
- Maven 3.8+
- Docker + Docker Compose

### 1. 启动依赖环境

```bash
cd infra/docker
docker-compose up -d
```

启动服务：
- PostgreSQL 16（带 pgvector 扩展）- 端口 5432
- Redis 7 - 端口 6379
- MinIO - API 端口 9000，控制台端口 9001

### 2. 构建项目

```bash
mvn clean install
```

### 3. 运行应用

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. 访问应用

- API 根路径：`http://localhost:8080/api`
- Swagger UI：`http://localhost:8080/api/swagger-ui.html`
- 健康检查：`http://localhost:8080/api/health`
- 详细健康检查：`http://localhost:8080/api/health/detailed`

### 5. 快速验证

```bash
# 登录获取令牌
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"moveon","password":"moveon123"}' | jq -r '.data.accessToken')

# 上传文档
curl -X POST http://localhost:8080/api/documents/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@your-document.pdf"

# RAG 问答
curl -X POST http://localhost:8080/api/assistant/ask \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"question":"文档中提到了什么内容？"}'
```

## API 接口

### 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/auth/login` | 用户登录 |
| `POST` | `/auth/refresh` | 刷新令牌 |
| `POST` | `/auth/users` | 创建用户（管理员） |
| `GET` | `/auth/me` | 获取当前用户 |

### 文档接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/documents/upload` | 上传文档（自动解析+向量化） |
| `GET` | `/documents` | 文档列表（分页） |
| `GET` | `/documents/{id}` | 文档详情 |
| `POST` | `/documents/{id}/parse` | 重新解析 |
| `GET` | `/documents/{id}/fragments` | 获取文本片段 |
| `POST` | `/documents/{id}/embed` | 重新向量化 |
| `GET` | `/documents/search` | 语义搜索 |

### AI 助理接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/assistant/ask` | RAG 问答 |
| `POST` | `/assistant/ask/stream` | 流式 RAG 问答 |

## 运行测试

```bash
# 运行所有测试
mvn test

# 运行单个测试类
mvn test -Dtest=AuthServiceTest

# 运行单个测试方法
mvn test -Dtest=RagServiceTest#testAsk_Success
```

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `AI_EMBEDDING_API_KEY` | Embedding API 密钥（SiliconFlow） | - |
| `AI_CHAT_API_KEY` | Chat API 密钥（DashScope） | - |
| `POSTGRES_URL` | PostgreSQL 连接 URL | `jdbc:postgresql://localhost:5432/moveon` |
| `POSTGRES_USER` | PostgreSQL 用户名 | `moveon` |
| `POSTGRES_PASSWORD` | PostgreSQL 密码 | `moveon` |
| `REDIS_URL` | Redis 连接 URL | `redis://localhost:6379` |
| `MINIO_ENDPOINT` | MinIO 端点 | `http://localhost:9000` |
| `MINIO_ACCESS_KEY` | MinIO 访问密钥 | `minioadmin` |
| `MINIO_SECRET_KEY` | MinIO 密钥 | `minioadmin` |
| `JWT_SECRET` | JWT 签名密钥 | (开发环境默认值) |

### 初始管理员账号

- 用户名：`moveon` / 密码：`moveon123` / 角色：管理员

## 业务流程

### 文档处理

```
上传 → MinIO 存储 → Apache Tika 解析 → 文本分段 → BGE-M3 向量化 → 就绪
```

文档状态：`PENDING → PARSING → PARSED → EMBEDDING → COMPLETED`（任一阶段可失败并重试）

### RAG 问答

```
提问 → 语义检索(pgvector) → 过滤低分结果 → 构建提示 → Qwen 生成答案 → 附带引用 → 审计日志
```

## 开发规范

### 代码风格

- 使用 Lombok 简化样板代码
- 所有 API 接口使用 Swagger 注解（`@Tag`、`@Operation`）
- 统一使用 `ApiResponse` 包装响应

### Git 提交规范

```
feat: 新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式调整
refactor: 重构
test: 测试相关
chore: 构建/工具相关
```

## 开发进度

| 阶段 | 内容 | 状态 |
|------|------|------|
| 1 | 基础工程初始化 | ✅ 完成 |
| 2 | 本地依赖环境搭建 | ✅ 完成 |
| 3 | 应用骨架与通用能力 | ✅ 完成 |
| 4 | 用户认证与权限基础 | ✅ 完成 |
| 5 | 文档上传与存储 | ✅ 完成 |
| 6 | 文档解析与内容入库 | ✅ 完成 |
| 7 | 向量化与检索 | ✅ 完成 |
| 8 | 基础 RAG 问答 | ✅ 完成 |
| 9 | 任务与待办管理 | ⬜ 待开始 |
| 10 | 提醒、观测与收尾 | ⬜ 待开始 |

## 文档索引

- [设计文档](memory-bank/design-document.md)
- [技术栈说明](memory-bank/tech-stack.md)
- [实施计划](memory-bank/implementation-plan.md)
- [架构说明](memory-bank/architecture.md)
- [进度记录](memory-bank/progress.md)

## License

MIT License
