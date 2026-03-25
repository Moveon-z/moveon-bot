# 环境变量配置说明

本文档说明 moveon-bot 项目所需的环境变量配置。

## 环境变量清单

| 变量名 | 说明 | 默认值 | 是否必填 |
|--------|------|--------|----------|
| `AI_API_KEY` | 大模型 API 密钥 | - | 是（使用 AI 功能时） |
| `AI_MODEL_PROVIDER` | 大模型提供商 | `aliyun` | 否 |
| `AI_EMBEDDING_MODEL` | Embedding 模型名称 | `ali-text-embedding` | 否 |
| `POSTGRES_URL` | PostgreSQL 连接 URL | `jdbc:postgresql://localhost:5432/moveon` | 否 |
| `POSTGRES_USER` | PostgreSQL 用户名 | `moveon` | 否 |
| `POSTGRES_PASSWORD` | PostgreSQL 密码 | `moveon` | 否 |
| `REDIS_URL` | Redis 连接 URL | `redis://localhost:6379` | 否 |
| `MINIO_ENDPOINT` | MinIO 端点 | `http://localhost:9000` | 否 |
| `MINIO_ACCESS_KEY` | MinIO 访问密钥 | `minioadmin` | 否 |
| `MINIO_SECRET_KEY` | MinIO 密钥 | `minioadmin` | 否 |
| `MINIO_BUCKET` | MinIO 存储桶名 | `moveon-documents` | 否 |
| `JWT_SECRET` | JWT 密钥 | (内置默认值) | 否 |

## 配置文件说明

项目使用 Spring Profile 机制管理多环境配置：

- `application.yml` - 默认配置，使用环境变量或默认值
- `application-dev.yml` - 本地开发环境配置
- `application-test.yml` - 测试环境配置（使用 H2 内存数据库）

## 启动方式

### 本地开发

```bash
# 使用默认配置（连接本地 Docker 服务）
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 指定环境变量

```bash
# Linux/Mac
export AI_API_KEY=your-api-key
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Windows PowerShell
$env:AI_API_KEY="your-api-key"
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 使用 .env 文件

项目支持通过 IDE 或第三方工具加载 `.env` 文件：

```bash
# 在项目根目录创建 .env 文件（不要提交到 Git）
AI_API_KEY=your-api-key
POSTGRES_PASSWORD=your-password
```

## 安全提示

- 生产环境必须修改默认密码和密钥
- 不要将 `.env` 文件或自定义配置文件提交到版本控制
- `JWT_SECRET` 在生产环境必须使用强随机值
