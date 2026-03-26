# Moveon Bot - AI Personal Assistant

一个基于 Java 21 和 Spring Boot 3 的智能 AI 助理，提供文档管理、知识库、任务待办和 RAG 问答功能。

## 技术栈

- **开发语言**: Java 21
- **后端框架**: Spring Boot 3.2.3
- **AI 框架**: LangChain4j
- **数据库**: PostgreSQL + pgvector
- **缓存**: Redis
- **对象存储**: MinIO
- **文档解析**: Apache Tika + PDFBox + POI
- **任务调度**: Quartz
- **安全认证**: Spring Security + JWT

## 项目结构

```
moveon-bot/
├── src/main/java/com/moveon/
│   ├── MoveonBotApplication.java    # 主应用入口
│   ├── auth/                         # 认证与授权模块
│   ├── document/                     # 文档管理模块
│   ├── knowledge/                    # 知识库模块
│   ├── assistant/                    # AI 助理问答模块
│   ├── task/                         # 任务待办模块
│   ├── notification/                 # 通知推送模块
│   └── infra/                        # 基础设施模块
├── src/main/resources/
│   ├── application.yml               # 默认配置
│   ├── application-dev.yml           # 开发环境配置
│   └── application-test.yml          # 测试环境配置
├── memory-bank/                      # 项目文档
└── pom.xml                           # Maven 配置
```

## 模块说明

| 模块 | 说明 |
|------|------|
| `auth` | 用户认证、授权、JWT 令牌管理 |
| `document` | 文件上传、解析、摘要、标签 |
| `knowledge` | 知识库、笔记、批注、版本管理 |
| `assistant` | 对话、RAG、记忆、工具调用 |
| `task` | 待办事项、提醒、日程管理 |
| `notification` | 通知推送、提醒发送 |
| `infra` | AI 客户端、缓存、存储、公共组件 |

## 快速开始

### 前置要求

- Java 21+
- Maven 3.8+
- Docker + Docker Compose（用于本地依赖环境）

### 1. 启动依赖环境

```bash
cd infra/docker
docker-compose up -d
```

这将启动：
- PostgreSQL (带 pgvector 扩展)
- Redis
- MinIO

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
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- 健康检查：`http://localhost:8080/api/actuator/health`

## 运行测试

```bash
# 运行所有测试
mvn test

# 运行单个测试类
mvn test -Dtest=ClassNameTest

# 运行单个测试方法
mvn test -Dtest=ClassNameTest#testMethodName
```

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `AI_API_KEY` | 大模型 API 密钥 | - |
| `AI_MODEL_PROVIDER` | 大模型提供商 | `aliyun` |
| `AI_EMBEDDING_MODEL` | Embedding 模型 | `ali-text-embedding` |
| `POSTGRES_URL` | PostgreSQL 连接 URL | `jdbc:postgresql://localhost:5432/moveon` |
| `POSTGRES_USER` | PostgreSQL 用户名 | `moveon` |
| `POSTGRES_PASSWORD` | PostgreSQL 密码 | `moveon` |
| `REDIS_URL` | Redis 连接 URL | `redis://localhost:6379` |
| `MINIO_ENDPOINT` | MinIO 端点 | `http://localhost:9000` |
| `MINIO_ACCESS_KEY` | MinIO 访问密钥 | `minioadmin` |
| `MINIO_SECRET_KEY` | MinIO 密钥 | `minioadmin` |
| `MINIO_BUCKET` | MinIO 存储桶 | `moveon-documents` |
| `JWT_SECRET` | JWT 密钥 | (开发环境默认值) |

### 初始管理员账号

- 用户名：`moveon`
- 密码：`moveon123`
- 角色：管理员

账号通过数据库初始化脚本或应用启动时的 `UserInitializer` 自动创建。

## 开发规范

### 代码风格

- 使用 Google Java Style
- 使用 Lombok 简化样板代码
- 所有 API 接口需要添加 Swagger 注解

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

## 实施计划

项目分 10 个阶段实施：

1. ✅ 基础工程初始化
2. ✅ 本地依赖环境搭建
3. ✅ 基础应用骨架与通用能力
4. ✅ 用户认证与权限基础
5. 文档上传与存储
6. 文档解析与内容入库
7. 向量化与检索准备
8. 基础 RAG 问答
9. 任务与待办管理
10. 提醒、观测与收尾验收

详见：`memory-bank/implementation-plan.md`

## 文档索引

- [设计文档](memory-bank/design-document.md)
- [技术栈说明](memory-bank/tech-stack.md)
- [实施计划](memory-bank/implementation-plan.md)
- [架构说明](memory-bank/architecture.md)
- [进度记录](memory-bank/progress.md)

## License

MIT License
