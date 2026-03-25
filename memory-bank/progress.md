# 项目进度记录

## 阶段 1：基础工程初始化 - ✅ 已完成

**完成日期**: 2026-03-25

### 完成的工作

#### 1.1 创建项目根工程

- [x] 创建基于 Java 21 和 Spring Boot 3.2.3 的项目根工程
- [x] 建立模块化单体目录结构
- [x] 创建主应用入口 `MoveonBotApplication.java`
- [x] 创建业务模块目录（auth, document, knowledge, assistant, task, notification, infra）
- [x] 创建基础设施目录（controller, service, repository, entity, dto, config, exception）

**目录结构**:
```
moveon-bot/
├── src/main/java/com/moveon/
│   ├── MoveonBotApplication.java
│   ├── auth/          # 认证与授权模块
│   ├── document/      # 文档管理模块
│   ├── knowledge/     # 知识库模块
│   ├── assistant/     # AI 助理问答模块
│   ├── task/          # 任务待办模块
│   ├── notification/  # 通知推送模块
│   └── infra/         # 基础设施模块
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── application-test.yml
├── src/test/java/com/moveon/
├── infra/docker/
│   └── docker-compose.yml
├── memory-bank/
├── CLAUDE.md
├── README.md
├── ENV.md
└── pom.xml
```

#### 1.2 建立统一依赖与版本管理

- [x] 配置 Maven pom.xml
- [x] 统一管理以下关键依赖版本：
  - Java 21
  - Spring Boot 3.2.3
  - LangChain4j 0.29.1
  - JJWT 0.12.5
  - SpringDoc 2.3.0
  - pgvector 0.1.6
  - MinIO 8.5.9
  - Apache Tika 2.9.1
  - Apache PDFBox 2.0.30
  - Apache POI 5.2.5

**主要依赖清单**:
- Spring Boot Core (Web, Validation, Actuator, Data JPA, Security, Data Redis)
- PostgreSQL + pgvector
- JWT (io.jsonwebtoken jjwt)
- LangChain4j (langchain4j, langchain4j-open-ai)
- MinIO
- Apache Tika + PDFBox + POI
- Quartz
- SpringDoc OpenAPI
- Micrometer Prometheus
- Lombok
- JUnit 5 + TestContainers + H2

#### 1.3 建立环境配置分层

- [x] 创建 `application.yml` - 默认配置
- [x] 创建 `application-dev.yml` - 开发环境配置
- [x] 创建 `application-test.yml` - 测试环境配置（使用 H2 内存数据库）
- [x] 创建 `ENV.md` - 环境变量说明文档

**配置说明**:
- 所有敏感配置通过环境变量读取
- 开发环境使用本地 Docker 服务
- 测试环境使用 H2 内存数据库
- 默认配置使用占位值或默认值

### 创建的基础代码

#### 基础设施模块 (infra)

**异常处理**:
- `BusinessException.java` - 业务异常基类
- `ResourceNotFoundException.java` - 资源未找到异常
- `InvalidArgumentException.java` - 参数无效异常
- `ErrorResponse.java` - 统一错误响应结构
- `GlobalExceptionHandler.java` - 全局异常处理器

**DTO**:
- `ApiResponse.java` - 统一 API 响应包装
- `PageResponse.java` - 分页响应包装

**Controller**:
- `HealthController.java` - 健康检查接口

**配置**:
- `OpenApiConfig.java` - OpenAPI/Swagger 配置

#### Docker 环境

- `infra/docker/docker-compose.yml` - 本地依赖环境编排
  - PostgreSQL (pgvector/pgvector:pg16)
  - Redis (redis:7-alpine)
  - MinIO (minio/minio:latest)
- `infra/docker/init-db/01-init.sql` - 数据库初始化脚本

#### 测试

- `MoveonBotApplicationTests.java` - 应用上下文加载测试
- `HealthControllerTest.java` - 健康检查接口测试

### 验证测试

**构建验证**:
```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 运行单个测试
mvn test -Dtest=HealthControllerTest
```

**启动验证**:
```bash
# 启动应用（开发环境）
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**API 验证**:
- 健康检查：`GET /api/health`
- Swagger UI: `GET /api/swagger-ui.html`
- Actuator: `GET /api/actuator/health`

### 已知问题

1. Maven 本地仓库可能存在缓存问题，需要清理或重新下载依赖
2. 需要 Java 21 环境
3. Docker 环境需要单独启动

### 下一步

阶段 2：本地依赖环境搭建
- 启动 Docker Compose 环境
- 初始化 PostgreSQL 和 pgvector
- 验证 Redis 和 MinIO 可用性

---

## 待办

- [ ] 阶段 2：本地依赖环境搭建
- [ ] 阶段 3：基础应用骨架与通用能力
- [ ] 阶段 4：用户认证与权限基础
- [ ] 阶段 5：文档上传与存储
- [ ] 阶段 6：文档解析与内容入库
- [ ] 阶段 7：向量化与检索准备
- [ ] 阶段 8：基础 RAG 问答
- [ ] 阶段 9：任务与待办管理
- [ ] 阶段 10：提醒、观测与收尾验收
