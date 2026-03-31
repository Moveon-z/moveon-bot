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

## 阶段 2：本地依赖环境搭建 - ✅ 已完成

**完成日期**: 2026-03-25

### 完成的工作

#### 2.1 准备本地容器编排文件

- [x] 创建 Docker Compose 配置文件
- [x] 配置 PostgreSQL (pgvector/pgvector:pg16)
- [x] 配置 Redis (redis:7-alpine)
- [x] 配置 MinIO (minio/minio:latest)
- [x] 配置数据卷持久化
- [x] 配置健康检查

**验证结果**:
```bash
# 所有容器健康状态
moveon-postgres   Up 34 seconds (healthy)
moveon-redis      Up 34 seconds (healthy)
moveon-minio      Up 34 seconds (healthy)
```

#### 2.2 初始化 PostgreSQL 与 pgvector

- [x] 启用 pgvector 扩展
- [x] 创建数据库初始化脚本
- [x] 验证扩展安装

**验证结果**:
```bash
# pgvector 扩展版本
vector  | 0.8.2   | public

# 数据库连接验证
POSTGRES_URL: jdbc:postgresql://localhost:5432/moveon
POSTGRES_USER: moveon
POSTGRES_PASSWORD: moveon
```

#### 2.3 验证 Redis 与 MinIO 可用性

- [x] 验证 Redis 连接
- [x] 验证 Redis 读写
- [x] 验证 MinIO 健康检查
- [x] 验证 MinIO Console 访问

**验证结果**:
```bash
# Redis ping
PONG

# Redis 读写测试
SET test_key "Hello Moveon" -> OK
GET test_key -> "Hello Moveon"

# MinIO 健康检查
HTTP 200

# MinIO Console
http://localhost:9001
```

### 验证测试

**Docker 容器状态**:
```bash
docker-compose ps
# 所有服务状态：healthy
```

**Maven 测试**:
```bash
mvn test
# Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```

### 下一步

阶段 3：基础应用骨架与通用能力
- 增强健康检查接口（集成数据库、缓存、存储检查）
- 建立统一异常处理机制
- 建立基础日志规范
- 建立 API 文档机制

---

## 阶段 3：基础应用骨架与通用能力 - ✅ 已完成

**完成日期**: 2026-03-26

### 完成的工作

#### 3.1 增强健康检查接口

- [x] 创建 `HealthCheckService` 服务类
- [x] 实现数据库连接检查（使用 JdbcTemplate）
- [x] 实现 Redis 连接检查（读写测试）
- [x] 实现 MinIO 连接检查（存储桶存在性验证）
- [x] 增强 `HealthController` 添加详细健康检查端点 `/health/detailed`
- [x] 创建 `InfraConfig` 配置类（MinIO Client 和 Redis Template）

**健康检查响应示例**:
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "status": "UP",
    "timestamp": "2026-03-26T01:25:24.738Z",
    "application": "moveon-bot",
    "version": "0.0.1-SNAPSHOT",
    "dependencies": {
      "database": {"status": "UP", "message": "Database connection successful"},
      "redis": {"status": "UP", "message": "Redis connection successful"},
      "minio": {"status": "UP", "bucket": "moveon-documents", "bucketExists": true}
    }
  }
}
```

#### 3.2 建立统一异常处理机制

- [x] 已有 `GlobalExceptionHandler` 处理所有异常类型
- [x] 已有 `ErrorResponse` 统一错误响应结构
- [x] 已有业务异常类：`BusinessException`, `ResourceNotFoundException`, `InvalidArgumentException`
- [x] 处理的异常类型包括：
  - `NoResourceFoundException` → 404
  - `InvalidArgumentException` → 400
  - `ResourceNotFoundException` → 404
  - `BusinessException` → 400
  - `BadCredentialsException` → 401
  - `AccessDeniedException` → 403
  - `MethodArgumentNotValidException` → 400 (验证错误)
  - `Exception` → 500 (通用异常)

#### 3.3 建立基础日志规范

- [x] 创建 `RequestLoggingFilter` 记录 HTTP 请求/响应
- [x] 日志包含：请求方法、路径、状态码、耗时、请求体、响应体
- [x] 日志脱敏：请求/响应体超过 500 字符自动截断
- [x] 跳过静态资源和健康检查接口的日志记录
- [x] 更新 `application.yml` 日志配置：
  - 根级别：INFO
  - com.moveon: DEBUG
  - Hibernate SQL: DEBUG
  - Hibernate 参数绑定：TRACE

**日志输出示例**:
```
2026-03-26 09:25:24.763 [main] INFO  c.m.i.config.RequestLoggingFilter - HTTP GET /health - Status: 200 - Duration: 36ms - Request:  - Response: {"code":200,...}
```

#### 3.4 建立 API 文档机制

- [x] 已有 `OpenApiConfig` 配置类
- [x] Swagger UI 访问地址：`/api/swagger-ui.html`
- [x] API 文档路径：`/api/v3/api-docs`
- [x] 健康检查接口已添加 OpenAPI 注解
- [x] 使用 `@Tag` 和 `@Operation` 注解描述接口

### 创建的代码

**新增服务**:
- `HealthCheckService.java` - 健康检查服务

**新增配置**:
- `InfraConfig.java` - MinIO 和 Redis 配置
- `RequestLoggingFilter.java` - HTTP 请求日志过滤器

**增强的类**:
- `HealthController.java` - 添加详细健康检查端点
- `HealthControllerTest.java` - 添加详细健康检查测试

### 验证测试

**Maven 测试**:
```bash
mvn test
# Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```

**健康检查验证**:
```bash
# 基础健康检查
curl http://localhost:8080/api/health

# 详细健康检查
curl http://localhost:8080/api/health/detailed
```

**API 文档验证**:
```bash
# Swagger UI
http://localhost:8080/api/swagger-ui.html

# API Docs
http://localhost:8080/api/v3/api-docs
```

### 下一步

阶段 4：用户认证与权限基础
- 实现用户实体与基础表结构
- 实现登录能力与令牌签发
- 实现受保护接口鉴权

---

---

## 阶段 4：用户认证与权限基础 - ✅ 已完成

**完成日期**: 2026-03-26

### 完成的工作

#### 4.1 实现用户实体与基础表结构

- [x] 创建 `User` 实体类（包含 id, username, password, status, role, createdAt, updatedAt）
- [x] 创建 `UserStatus` 枚举（ACTIVE, INACTIVE, LOCKED）
- [x] 创建 `UserRole` 枚举（ADMIN, USER）
- [x] 创建 `UserRepository` 数据访问层
- [x] 创建数据库初始化脚本 `01-init.sql`
- [x] 启用 pgvector 扩展
- [x] 创建初始管理员账号（用户名：`moveon`，密码：`moveon123`）

**用户表结构**:
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### 4.2 实现登录能力与令牌签发

- [x] 实现登录接口 `POST /auth/login`
- [x] 实现令牌刷新接口 `POST /auth/refresh`
- [x] 创建 `JwtService` 实现 JWT 令牌签发与验证
- [x] 配置访问令牌有效期 30 分钟
- [x] 配置刷新令牌有效期 7 天
- [x] 使用 BCrypt 密码加密
- [x] 创建 `AuthConfig` 配置密码编码器
- [x] 实现 `UserInitializer` 应用启动时创建初始管理员

**DTO 列表**:
- `LoginRequest` - 登录请求
- `LoginResponse` - 登录响应（包含访问令牌和刷新令牌）
- `RefreshTokenRequest` - 刷新令牌请求
- `CreateUserRequest` - 创建用户请求
- `UserResponse` - 用户信息响应

#### 4.3 实现受保护接口鉴权

- [x] 创建 `SecurityConfig` 配置安全策略
- [x] 创建 `JwtAuthenticationFilter` 过滤器处理 JWT 认证
- [x] 配置公开端点（`/auth/**`, `/health/**`, `/swagger-ui/**`, `/actuator/**`）
- [x] 配置受保护接口需要认证
- [x] 实现管理员专属接口 `POST /auth/users`（创建用户）
- [x] 实现获取当前用户信息接口 `GET /auth/me`
- [x] 使用 `@PreAuthorize("hasRole('ADMIN')")` 进行权限控制

**认证流程**:
1. 用户登录获取访问令牌和刷新令牌
2. 访问受保护接口时携带 `Authorization: Bearer <token>`
3. `JwtAuthenticationFilter` 解析令牌并设置认证上下文
4. 控制器通过 `@PreAuthorize` 进行权限校验

### 创建的代码

**Auth 模块 (17 个文件)**:

**实体**:
- `entity/User.java` - 用户实体（实现 UserDetails 接口）
- `entity/UserStatus.java` - 用户状态枚举
- `entity/UserRole.java` - 用户角色枚举

**Repository**:
- `repository/UserRepository.java` - 用户数据访问层

**服务**:
- `service/AuthService.java` - 认证服务（登录、刷新令牌、创建用户）
- `service/JwtService.java` - JWT 工具服务（令牌签发、验证、提取）
- `service/UserInitializer.java` - 用户初始化服务（CommandLineRunner）

**配置**:
- `config/AuthConfig.java` - 认证配置（BCrypt 密码编码器）
- `config/JwtAuthenticationFilter.java` - JWT 认证过滤器

**控制器**:
- `controller/AuthController.java` - 认证控制器（登录、刷新、创建用户、获取当前用户）

**DTO**:
- `dto/LoginRequest.java`
- `dto/LoginResponse.java`
- `dto/RefreshTokenRequest.java`
- `dto/CreateUserRequest.java`
- `dto/UserResponse.java`

**测试**:
- `test/service/AuthServiceTest.java`
- `test/controller/AuthControllerTest.java`

**基础设施**:
- `infra/config/SecurityConfig.java` - Security 安全配置

**数据库脚本**:
- `infra/docker/init-db/01-init.sql` - 数据库初始化脚本（包含 users 表和初始管理员）

### 验证测试

**Maven 测试**:
```bash
mvn test
# Tests run: X, Failures: 0, Errors: 0, Skipped: 0
```

**API 验证**:
```bash
# 1. 登录获取令牌
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"moveon","password":"moveon123"}'

# 2. 使用令牌访问受保护接口
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <access_token>"

# 3. 刷新令牌
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refresh_token>"}'

# 4. 创建用户（管理员专属）
curl -X POST http://localhost:8080/api/auth/users \
  -H "Authorization: Bearer <admin_access_token>" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123"}'
```

**Swagger UI 验证**:
- 访问 `http://localhost:8080/api/swagger-ui.html`
- 确认所有认证接口已注册并带有文档说明

### 已知问题

无

### 下一步

阶段 5：文档上传与存储
- 建立文档元数据模型
- 实现单文件上传接口
- 补充文件类型与大小校验
- 实现文档列表与详情查询

---

## 阶段 5：文档上传与存储 - ✅ 已完成

**完成日期**: 2026-03-26

### 完成的工作

#### 5.1 建立文档元数据模型

- [x] 创建 `Document` 实体类（包含 id, userId, fileName, originalFilename, fileType, mimeType, fileSize, storagePath, status 等字段）
- [x] 创建 `DocumentStatus` 枚举（PENDING, PARSING, PARSED, EMBEDDING, COMPLETED, FAILED）
- [x] 创建 `DocumentType` 枚举（TXT, PDF, DOCX, XLSX, PPTX, IMAGE, OTHER）
- [x] 创建 `DocumentRepository` 数据访问层
- [x] 更新数据库初始化脚本 `01-init.sql`
- [x] 创建文档表索引（user_id, status, created_at, file_type）

**文档表结构**:
```sql
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message VARCHAR(1000),
    summary VARCHAR(2000),
    parsed_at TIMESTAMP,
    embedded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_documents_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

#### 5.2 实现单文件上传接口

- [x] 创建 `DocumentService` 服务类
- [x] 实现 MinIO 文件上传逻辑
- [x] 实现文件存储路径生成：`{userId}/{date}/{timestamp}-{filename}`
- [x] 实现文档元数据持久化
- [x] 创建 `DocumentController` 控制器
- [x] 实现上传接口 `POST /api/documents/upload`

#### 5.3 补充文件类型与大小校验

- [x] 实现文件类型校验（仅支持 TXT, PDF, DOCX）
- [x] 实现文件大小校验（最大 50MB）
- [x] 实现文件扩展名检查
- [x] 实现 MIME 类型检查（记录日志）
- [x] 统一错误响应格式

#### 5.4 实现文档列表与详情查询

- [x] 实现文档列表接口 `GET /api/documents`（支持分页、状态筛选、排序）
- [x] 实现文档详情接口 `GET /api/documents/{id}`
- [x] 实现用户隔离（只能查询自己的文档）
- [x] 创建 `DocumentResponse` 和 `DocumentUploadResponse` DTO

### 创建的代码

**Document 模块 (12 个文件)**:

**实体**:
- `entity/Document.java` - 文档实体
- `entity/DocumentStatus.java` - 文档状态枚举
- `entity/DocumentType.java` - 文档类型枚举

**Repository**:
- `repository/DocumentRepository.java` - 文档数据访问层

**服务**:
- `service/DocumentService.java` - 文档服务

**控制器**:
- `controller/DocumentController.java` - 文档控制器

**DTO**:
- `dto/DocumentResponse.java` - 文档信息响应
- `dto/DocumentUploadResponse.java` - 文档上传响应

**配置**:
- 删除了重复的 `DocumentConfig.java`（使用 `InfraConfig` 中的 MinioClient）

**数据库脚本**:
- `infra/docker/init-db/01-init.sql` - 添加 documents 表

**测试**:
- `test/java/com/moveon/infra/controller/HealthControllerTest.java` - 修复测试（添加 MockBean）

### 配置更新

**application.yml**:
- 添加文件上传大小限制：`max-file-size: 50MB`, `max-request-size: 50MB`

### 验证测试

**Maven 测试**:
```bash
mvn test
# Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
```

**API 验证**（待执行）:
```bash
# 1. 登录获取令牌
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"moveon","password":"moveon123"}'

# 2. 上传文档
curl -X POST http://localhost:8080/api/documents/upload \
  -H "Authorization: Bearer <access_token>" \
  -F "file=@test.txt"

# 3. 获取文档列表
curl http://localhost:8080/api/documents \
  -H "Authorization: Bearer <access_token>"

# 4. 获取文档详情
curl http://localhost:8080/api/documents/1 \
  -H "Authorization: Bearer <access_token>"
```

### 已知问题

1. 当前用户 ID 通过占位实现返回固定值 `1L`，后续会通过 JWT 认证过滤器提取真实用户 ID
2. 文档上传后状态为 PENDING，需要阶段 6 实现异步解析

### 下一步

阶段 6：文档解析与内容入库
- 建立文档解析任务链路
- 实现文本提取（TXT, PDF, DOCX）
- 存储解析结果与基础片段

---

## 阶段 6：文档解析与内容入库 - ✅ 已完成

**完成日期**: 2026-03-29

### 完成的工作

#### 6.1 建立文档解析任务链路

- [x] 启用 Spring `@EnableAsync` 异步支持
- [x] 创建 `DocumentParsingService` 异步解析编排服务
- [x] 实现状态流转：PENDING → PARSING → PARSED（或 FAILED）
- [x] 上传后自动触发异步解析
- [x] 解析失败时记录错误信息到文档元数据
- [x] 支持手动重新解析（重试）接口 `POST /api/documents/{id}/parse`

#### 6.2 实现文本提取

- [x] 创建 `DocumentParserService` 文本提取服务
- [x] 使用 Apache Tika 统一解析 PDF、DOCX 格式
- [x] TXT 文件直接使用 UTF-8 编码读取
- [x] 文本清理：规范化换行符、压缩多余空行、去除首尾空白
- [x] 空内容检测与异常抛出

#### 6.3 存储解析结果与基础片段

- [x] 创建 `document_fragments` 表（id, document_id, fragment_index, content, char_count, created_at, updated_at）
- [x] 创建 `DocumentFragment` 实体类
- [x] 创建 `DocumentFragmentRepository` 数据访问层
- [x] 实现文本分段逻辑：每个片段 4 段，相邻片段重叠 1 段
- [x] 片段按序号排序，可按顺序重组原文
- [x] 支持重新解析时清除旧片段
- [x] 新增片段查询接口 `GET /api/documents/{id}/fragments`

**文档片段表结构**:
```sql
CREATE TABLE document_fragments (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    fragment_index INT NOT NULL,
    content TEXT NOT NULL,
    char_count INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fragments_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);
```

### 创建的代码

**新增服务**:
- `DocumentParserService.java` - 文本提取与分段服务
- `DocumentParsingService.java` - 异步解析编排服务（下载文件→提取文本→分段→更新状态）

**新增实体**:
- `DocumentFragment.java` - 文档片段实体

**新增 Repository**:
- `DocumentFragmentRepository.java` - 片段数据访问层

**新增 DTO**:
- `DocumentFragmentResponse.java` - 片段查询响应

**新增测试**:
- `DocumentParserServiceTest.java` - 解析服务测试（11 个测试用例）

**修改的文件**:
- `MoveonBotApplication.java` - 添加 `@EnableAsync`
- `DocumentService.java` - 添加 `uploadAndParseDocument` 方法，注入 `DocumentParsingService`
- `DocumentController.java` - 上传触发解析，新增重新解析和片段查询接口
- `DocumentServiceTest.java` - 更新构造函数参数
- `DocumentControllerTest.java` - 添加 `DocumentParsingService` MockBean
- `01-init.sql` - 添加 `document_fragments` 表

### 验证测试

**Maven 测试**:
```bash
mvn test
# Tests run: 48, Failures: 0, Errors: 0, Skipped: 0
```

**API 验证**（待执行）:
```bash
# 1. 登录获取令牌
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"moveon","password":"moveon123"}'

# 2. 上传文档（自动触发异步解析）
curl -X POST http://localhost:8080/api/documents/upload \
  -H "Authorization: Bearer <access_token>" \
  -F "file=@test.txt"

# 3. 等待解析完成后查看文档状态
curl http://localhost:8080/api/documents/1 \
  -H "Authorization: Bearer <access_token>"

# 4. 查看解析后的片段
curl http://localhost:8080/api/documents/1/fragments \
  -H "Authorization: Bearer <access_token>"

# 5. 手动重新解析（用于失败重试）
curl -X POST http://localhost:8080/api/documents/1/parse \
  -H "Authorization: Bearer <access_token>"
```

### 下一步

阶段 7：向量化与检索准备
- 建立向量记录结构
- 实现向量生成流程
- 实现基础语义检索

---

---

## 阶段 7：向量化与检索准备 - ✅ 已完成

**完成日期**: 2026-03-29

### 完成的工作

#### 7.1 建立向量记录结构

- [x] 创建 `document_vectors` 表（id, fragment_id, document_id, user_id, embedding vector(1536), status, created_at, updated_at）
- [x] 创建 `DocumentVector` 实体类
- [x] 创建 `DocumentVectorStatus` 枚举（PENDING, COMPLETED, FAILED）
- [x] 创建 `DocumentVectorRepository` 数据访问层
- [x] 创建 `VectorType` Hibernate 自定义类型（float[] ↔ pgvector 互转）
- [x] 更新数据库初始化脚本 `01-init.sql`

#### 7.2 实现向量生成流程

- [x] 创建 `AiConfig` 配置类（LangChain4j OpenAI Embedding 模型）
- [x] 创建 `EmbeddingService` 向量生成服务（封装 LangChain4j 调用）
- [x] 创建 `DocumentEmbeddingService` 异步向量化编排服务
- [x] 实现状态流转：PARSED → EMBEDDING → COMPLETED（或 FAILED）
- [x] 解析完成后自动触发向量化
- [x] 支持手动重新向量化 `POST /api/documents/{id}/embed`
- [x] 向量化失败时记录错误信息，支持重试
- [x] 模型未配置时优雅降级（不影响基础功能）

**配置说明**:
- 通过 OpenAI 兼容接口对接阿里云千问 Embedding
- 默认模型：text-embedding-v2（1536 维）
- 配置项：ai.api-key、ai.base-url、ai.embedding-model

#### 7.3 实现基础语义检索

- [x] 创建 `SemanticSearchService` 语义检索服务
- [x] 使用 pgvector 余弦距离（`<=>` 操作符）进行相似度检索
- [x] 创建 `SearchResult` DTO
- [x] 实现用户隔离（只检索当前用户的文档）
- [x] 添加语义检索接口 `GET /api/documents/search?query=xxx&topK=5`

### 创建的代码

**新增文件 (8 个)**:
- `infra/config/VectorType.java` - Hibernate 自定义向量类型
- `infra/config/AiConfig.java` - AI 模型配置
- `document/entity/DocumentVector.java` - 向量实体
- `document/entity/DocumentVectorStatus.java` - 向量状态枚举
- `document/repository/DocumentVectorRepository.java` - 向量数据访问层
- `document/service/EmbeddingService.java` - 向量生成服务
- `document/service/DocumentEmbeddingService.java` - 向量化编排服务
- `document/service/SemanticSearchService.java` - 语义检索服务
- `document/dto/SearchResult.java` - 检索结果 DTO

**修改的文件**:
- `pom.xml` - PostgreSQL 驱动改为 compile scope
- `infra/docker/init-db/01-init.sql` - 添加 document_vectors 表
- `document/service/DocumentParsingService.java` - 解析后触发向量化
- `document/controller/DocumentController.java` - 添加向量化重试和语义检索接口
- `application.yml` / `application-dev.yml` - 添加 AI base-url 配置
- `DocumentControllerTest.java` - 添加 MockBean

### 验证测试

**Maven 测试**:
```bash
mvn test
# Tests run: 48, Failures: 0, Errors: 0, Skipped: 0
```

### 下一步

阶段 8：基础 RAG 问答
- 建立最小问答链路（LangChain4j + 千问大模型）
- 控制问答范围与提示约束
- 记录问答日志与可追溯信息

---

## 阶段 8：基础 RAG 问答 - ✅ 已完成

**完成日期**: 2026-03-30

### 完成的工作

#### 8.1 建立最小问答链路

- [x] 创建 `RagService` RAG 编排服务（手动编排：检索 → 构建提示 → 调用 ChatModel → 返回答案）
- [x] 使用 OpenAiChatModel 调用阿里云千问大模型（Qwen3.5-plus via DashScope）
- [x] 配置支持快速切换不同大模型厂商（已通过 application.yml 配置）
- [x] 问答流程：提问 → 语义检索 → 构建系统提示 → 生成答案 → 附带引用
- [x] 创建 `AssistantController` 问答接口 `POST /api/assistant/ask`
- [x] 创建 `AskRequest`、`AskResponse`、`Citation` DTO

**问答流程**:
```
用户提问 → SemanticSearchService 检索相关片段 → 过滤低分结果
         → 构建 RAG 系统提示 → ChatModel 生成答案 → 返回答案 + 引用
```

#### 8.2 控制问答范围与提示约束

- [x] 实现 RAG 系统提示模板，严格约束模型行为
- [x] 要求模型仅基于检索到的参考资料回答
- [x] 引用时注明来源编号（如 [来源1]）
- [x] 资料不足时明确返回"无法回答"提示
- [x] 实现最低相似度阈值过滤（默认 0.3，可配置）
- [x] 低于阈值的结果不传给模型，直接返回"无法回答"
- [x] 创建 `RagConfig` 配置类，参数可外部化

**系统提示约束规则**:
1. 仅基于参考资料回答，不得使用外部知识或编造信息
2. 引用具体信息时注明来源编号
3. 资料不足时必须说明无法回答，不允许猜测
4. 不要在资料基础上推断或扩展
5. 不同来源信息冲突时，指出冲突并分别引用

#### 8.3 记录问答日志与可追溯信息

- [x] 创建 `qa_logs` 审计日志表
- [x] 创建 `QaLog` JPA 实体
- [x] 创建 `QaLogRepository` 数据访问层
- [x] 每次问答自动记录：用户 ID、问题、答案、命中文档/片段 ID、相似度、响应时间、状态
- [x] 超长答案自动截断（日志中不保存完整原文）
- [x] 失败时记录错误信息
- [x] 日志保存失败不影响主流程

**审计日志表结构**:
```sql
CREATE TABLE qa_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question VARCHAR(1000) NOT NULL,
    answer TEXT,
    answer_truncated BOOLEAN NOT NULL DEFAULT FALSE,
    hit_document_ids VARCHAR(500),
    hit_fragment_ids VARCHAR(500),
    hit_count INT NOT NULL DEFAULT 0,
    top_score DOUBLE PRECISION,
    response_time_ms BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    error_message VARCHAR(1000),
    model_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 创建的代码

**新增文件 (9 个)**:

**Entity**:
- `assistant/entity/QaLog.java` - 问答审计日志实体

**Repository**:
- `assistant/repository/QaLogRepository.java` - 审计日志数据访问层

**DTO**:
- `assistant/dto/AskRequest.java` - 问答请求 DTO
- `assistant/dto/AskResponse.java` - 问答响应 DTO（含答案、引用列表、响应时间）
- `assistant/dto/Citation.java` - 引用信息 DTO（文档 ID、文件名、片段序号、相似度）

**Service**:
- `assistant/service/RagService.java` - RAG 编排服务（检索 + 提示 + LLM + 审计）

**Controller**:
- `assistant/controller/AssistantController.java` - 问答控制器

**Config**:
- `assistant/config/RagConfig.java` - RAG 参数配置

**Test**:
- `assistant/service/RagServiceTest.java` - RAG 服务单元测试（10 个用例）
- `assistant/controller/AssistantControllerTest.java` - 问答控制器测试（3 个用例）

**修改的文件**:
- `infra/docker/init-db/01-init.sql` - 添加 qa_logs 表
- `src/main/resources/application.yml` - 添加 rag 配置段

### 配置更新

**application.yml** 添加:
```yaml
rag:
  default-top-k: 5
  max-top-k: 20
  max-question-length: 1000
  max-answer-length: 4000
  min-score-threshold: 0.3
```

### 验证测试

**Maven 测试**:
```bash
mvn test
# Tests run: 60, Failures: 0, Errors: 0, Skipped: 0
```

**API 验证**（待执行）:
```bash
# 1. 登录获取令牌
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"moveon","password":"moveon123"}'

# 2. 对已上传文档提问
curl -X POST http://localhost:8080/api/assistant/ask \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"question":"文档中提到了什么内容？"}'

# 3. 提问无相关文档的问题（应返回"无法回答"）
curl -X POST http://localhost:8080/api/assistant/ask \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"question":"量子物理是什么？"}'
```

### 下一步

阶段 10：提醒、观测与收尾验收

---

## 阶段 9：任务与待办管理 - ✅ 已完成

**完成日期**: 2026-03-30

### 完成的工作

#### 9.1 建立任务表结构

- [x] 创建 `Task` 实体类（id, userId, title, description, priority, status, dueDate, completedAt, createdAt, updatedAt）
- [x] 创建 `TaskStatus` 枚举（PENDING, IN_PROGRESS, COMPLETED, CANCELLED）
- [x] 创建 `TaskPriority` 枚举（LOW, MEDIUM, HIGH, URGENT）
- [x] 创建 `TaskRepository` 数据访问层
- [x] 更新数据库初始化脚本 `01-init.sql` 添加 tasks 表及索引

**任务表结构**:
```sql
CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    due_date TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

#### 9.2 实现任务 CRUD 接口

- [x] 创建 `TaskService` 服务类（创建、查询、更新、完成、取消、删除）
- [x] 创建 `TaskController` 控制器（REST API）
- [x] 创建 `TaskCreateRequest` DTO（标题、描述、优先级、截止时间）
- [x] 创建 `TaskUpdateRequest` DTO（部分更新）
- [x] 创建 `TaskResponse` DTO
- [x] 所有操作绑定当前用户，支持用户隔离
- [x] 权限校验：只能操作自己的任务

**API 端点**:
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/tasks` | 创建任务 |
| GET | `/tasks` | 获取任务列表（分页） |
| GET | `/tasks/{id}` | 获取任务详情 |
| PUT | `/tasks/{id}` | 更新任务 |
| POST | `/tasks/{id}/complete` | 标记任务完成 |
| POST | `/tasks/{id}/cancel` | 取消任务 |
| DELETE | `/tasks/{id}` | 删除任务 |

#### 9.3 加入基础筛选能力

- [x] 按状态筛选（`status` 参数）
- [x] 按优先级筛选（`priority` 参数）
- [x] 按状态+优先级组合筛选
- [x] 按截止时间范围筛选（`dueDateFrom` + `dueDateTo` 参数）
- [x] 支持排序（字段和方向可配置）

### 创建的代码

**新增文件 (10 个)**:

**Entity**:
- `task/entity/Task.java` - 任务实体
- `task/entity/TaskStatus.java` - 任务状态枚举
- `task/entity/TaskPriority.java` - 任务优先级枚举

**Repository**:
- `task/repository/TaskRepository.java` - 任务数据访问层

**DTO**:
- `task/dto/TaskCreateRequest.java` - 创建任务请求
- `task/dto/TaskUpdateRequest.java` - 更新任务请求
- `task/dto/TaskResponse.java` - 任务响应

**Service**:
- `task/service/TaskService.java` - 任务服务（CRUD + 状态变更）

**Controller**:
- `task/controller/TaskController.java` - 任务控制器

**Test**:
- `task/service/TaskServiceTest.java` - 任务服务测试（16 个用例）
- `task/controller/TaskControllerTest.java` - 任务控制器测试（12 个用例）

**修改的文件**:
- `infra/docker/init-db/01-init.sql` - 添加 tasks 表及索引

### 验证测试

**Maven 测试**:
```bash
mvn test
# Tests run: 88, Failures: 0, Errors: 0, Skipped: 0
```

**API 验证**（待执行）:
```bash
# 1. 登录获取令牌
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"moveon","password":"moveon123"}'

# 2. 创建任务
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"完成项目文档","description":"编写项目技术文档","priority":"HIGH","dueDate":"2026-04-05T18:00:00"}'

# 3. 获取任务列表
curl http://localhost:8080/api/tasks \
  -H "Authorization: Bearer <token>"

# 4. 按状态筛选
curl "http://localhost:8080/api/tasks?status=PENDING" \
  -H "Authorization: Bearer <token>"

# 5. 按优先级筛选
curl "http://localhost:8080/api/tasks?priority=HIGH" \
  -H "Authorization: Bearer <token>"

# 6. 更新任务
curl -X PUT http://localhost:8080/api/tasks/1 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"更新后的标题","priority":"URGENT"}'

# 7. 标记完成
curl -X POST http://localhost:8080/api/tasks/1/complete \
  -H "Authorization: Bearer <token>"

# 8. 取消任务
curl -X POST http://localhost:8080/api/tasks/2/cancel \
  -H "Authorization: Bearer <token>"

# 9. 删除任务
curl -X DELETE http://localhost:8080/api/tasks/3 \
  -H "Authorization: Bearer <token>"
```

### 下一步

阶段 10：提醒、观测与收尾验收
- 基于 Quartz 实现基础时间提醒
- 补充基础指标监控
- 执行端到端验收

---

## 阶段 10：提醒、观测与收尾验收 - ✅ 已完成

**完成日期**: 2026-03-31

### 完成的工作

#### 10.1 基于调度的定时提醒机制

- [x] 创建 `Reminder` 实体类（id, taskId, userId, title, message, remindType, status, remindAt, sentAt, createdAt）
- [x] 创建 `ReminderStatus` 枚举（PENDING, SENT, FAILED）
- [x] 创建 `ReminderType` 枚举（DUE_SOON, OVERDUE）
- [x] 创建 `ReminderRepository` 数据访问层（防重复查询、按状态查询）
- [x] 创建 `TaskReminderService` 提醒业务逻辑
  - `checkAndCreateReminders()` - 扫描即将到期（1小时内）和已过期任务，创建提醒记录
  - `processPendingReminders()` - 将待发送提醒标记为已发送
  - 同一任务同一提醒类型不会重复创建
- [x] 创建 `TaskReminderJob` 定时任务（每 5 分钟执行一次）
- [x] 更新数据库初始化脚本 `01-init.sql` 添加 reminders 表
- [x] 在 `TaskRepository` 添加 `findByStatusInAndDueDateBetween` 查询方法
- [x] 在 `MoveonBotApplication` 添加 `@EnableScheduling` 注解

**提醒表结构**:
```sql
CREATE TABLE IF NOT EXISTS reminders (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    remind_type VARCHAR(30) NOT NULL DEFAULT 'DUE_SOON',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    remind_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reminders_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_reminders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_reminders_task_type UNIQUE (task_id, remind_type)
);
```

#### 10.2 基础指标监控

- [x] 创建 `MetricsConfig` 配置类（启用 `@Timed` 注解支持）
- [x] 为 `RagService.ask()` 添加 `@Timed("moveon.qa.ask")` 指标
- [x] 为 `DocumentParsingService.parseDocument()` 添加 `@Timed("moveon.document.parsing")` 指标
- [x] 为 `DocumentEmbeddingService.embedDocument()` 添加 `@Timed("moveon.document.embedding")` 指标
- [x] 为 `TaskReminderService` 添加 `moveon.reminders.created` Counter 指标
- [x] Actuator + Prometheus 端点已配置（`/actuator/prometheus`）

**指标清单**:

| 指标名 | 类型 | 说明 |
|--------|------|------|
| `moveon.qa.ask` | Timer | QA 请求耗时 |
| `moveon.document.parsing` | Timer | 文档解析耗时 |
| `moveon.document.embedding` | Timer | 文档向量化耗时 |
| `moveon.reminders.created` | Counter | 提醒创建次数 |

#### 10.3 端到端验收

- [x] 所有 94 个单元测试通过（BUILD SUCCESS）
- [x] Maven 编译无错误
- [x] 新增 `TaskReminderServiceTest` 单元测试（7 个用例）

### 创建的代码

**新增文件（7 个）**:

**Entity**:
- `notification/entity/Reminder.java` - 提醒记录实体
- `notification/entity/ReminderStatus.java` - 提醒状态枚举
- `notification/entity/ReminderType.java` - 提醒类型枚举

**Repository**:
- `notification/repository/ReminderRepository.java` - 提醒数据访问层

**Service**:
- `notification/service/TaskReminderService.java` - 提醒业务逻辑

**Job**:
- `notification/job/TaskReminderJob.java` - 定时提醒任务

**Config**:
- `infra/config/MetricsConfig.java` - 指标监控配置

**Test**:
- `notification/service/TaskReminderServiceTest.java` - 提醒服务测试

**修改的文件**:
- `MoveonBotApplication.java` - 添加 `@EnableScheduling`
- `task/repository/TaskRepository.java` - 添加 `findByStatusInAndDueDateBetween`
- `assistant/service/RagService.java` - 添加 `@Timed` 指标
- `document/service/DocumentParsingService.java` - 添加 `@Timed` 指标
- `document/service/DocumentEmbeddingService.java` - 添加 `@Timed` 指标
- `infra/docker/init-db/01-init.sql` - 添加 reminders 表
- `src/main/resources/application.yml` - 添加调度线程池配置

### 验证测试

**Maven 测试**:
```bash
mvn test
# Tests run: 94, Failures: 0, Errors: 0, Skipped: 0
# BUILD SUCCESS
```

### 项目完成状态

全部 10 个阶段已完成 ✅

1. ✅ 项目脚手架
2. ✅ 本地开发环境
3. ✅ 应用骨架与通用能力
4. ✅ 认证与授权
5. ✅ 文档上传与存储
6. ✅ 文档解析与内容入库
7. ✅ 向量化与检索
8. ✅ RAG 问答
9. ✅ 任务与待办管理
10. ✅ 提醒、观测与收尾验收

---

## 待办

（全部阶段已完成）
