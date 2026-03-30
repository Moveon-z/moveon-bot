# 阶段 7：向量化与语义检索 — 流程验证文档

## 1. 前置条件

### 1.1 启动本地依赖服务

```bash
cd infra/docker
docker-compose up -d
```

确认三个容器状态均为 `healthy`：

```bash
docker-compose ps
# 预期输出：
# moveon-postgres   running (healthy)
# moveon-redis      running (healthy)
# moveon-minio      running (healthy)
```

### 1.2 配置 AI API Key

向量化功能依赖 SiliconFlow BAAI/bge-m3 Embedding 模型，需配置 API Key：

**获取 API Key**：注册 [SiliconFlow](https://cloud.siliconflow.cn/) 账号，在控制台创建 API Key（BAAI/bge-m3 模型免费）。

**方式一**：环境变量（推荐）

```bash
export AI_API_KEY="sk-your-siliconflow-api-key"
```

**方式二**：直接写入 `application-dev.yml`

```yaml
ai:
  api-key: sk-your-siliconflow-api-key
```

> 如果不配置 API Key，应用可正常启动，但向量化功能会跳过，语义检索接口返回错误提示。

### 1.3 初始化数据库

确保 `infra/docker/init-db/01-init.sql` 已执行（Docker 首次启动时自动执行）。
如果数据库容器已存在但缺少 `document_vectors` 表，需要手动执行：

```bash
docker exec -i moveon-postgres psql -U moveon -d moveon -c "
CREATE TABLE IF NOT EXISTS document_vectors (
    id BIGSERIAL PRIMARY KEY,
    fragment_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    embedding vector(1024),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
"
```

### 1.4 启动应用

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

验证启动成功，日志中应无 `ERROR` 级别异常。

### 1.5 基础变量定义

后续验证步骤中会复用以下变量，请在终端中设置：

```bash
BASE_URL="http://localhost:8080/api"
```

---

## 2. 验证步骤

### 步骤 1：用户登录获取令牌

**请求**：

```bash
curl -s -X POST ${BASE_URL}/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"moveon","password":"moveon123"}' | jq .
```

**预期响应**（`200 OK`）：

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "username": "moveon",
    "role": "ADMIN"
  }
}
```

**保存令牌**：

```bash
TOKEN="<复制上面返回的 accessToken>"
```

**验证点**：
- `accessToken` 非空
- `role` 为 `ADMIN`

---

### 步骤 2：上传文档（触发解析 + 向量化）

准备一份测试文件，内容包含有意义的中文文本（建议 500 字以上以确保产生多个片段）：

```bash
cat > /tmp/test-doc.txt << 'EOF'
人工智能（Artificial Intelligence，简称 AI）是计算机科学的一个分支，致力于研究和开发能够模拟、延伸和扩展人类智能的理论、方法、技术及应用系统。

机器学习是人工智能的核心技术之一。它使计算机系统能够从数据中学习并改进性能，而无需显式编程。常见的机器学习方法包括监督学习、无监督学习和强化学习。

深度学习是机器学习的一个子领域，基于人工神经网络的研究。它通过构建多层的神经网络模型来学习数据的抽象表示，在图像识别、自然语言处理、语音识别等领域取得了突破性进展。

自然语言处理（NLP）是人工智能的重要应用方向，目标是让计算机理解和生成人类语言。NLP 技术广泛应用于机器翻译、文本摘要、情感分析、智能问答等场景。

向量数据库是专门用于存储和检索高维向量的数据库系统。在 AI 应用中，文本经过 Embedding 模型转换为向量后，可以存入向量数据库进行相似度检索。pgvector 是 PostgreSQL 的一个扩展，为 PostgreSQL 添加了向量存储和检索能力。

检索增强生成（RAG）是一种将信息检索与大语言模型结合的技术。它先从知识库中检索与用户问题相关的文档片段，然后将这些片段作为上下文传递给大模型，从而生成更准确、更有依据的回答。
EOF
```

**上传请求**：

```bash
curl -s -X POST ${BASE_URL}/documents/upload \
  -H "Authorization: Bearer ${TOKEN}" \
  -F "file=@/tmp/test-doc.txt" | jq .
```

**预期响应**（`200 OK`）：

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "fileName": "test-doc.txt",
    "fileType": "TXT",
    "fileSize": 756,
    "status": "PENDING",
    "message": "文档上传成功，等待解析"
  }
}
```

**保存文档 ID**：

```bash
DOC_ID="<复制上面返回的 id>"
```

**验证点**：
- `status` 为 `PENDING`
- `fileType` 为 `TXT`

---

### 步骤 3：等待异步处理完成

上传后系统会异步执行：解析 → 分片 → 向量化。等待约 10-30 秒后查询状态。

```bash
sleep 15

curl -s ${BASE_URL}/documents/${DOC_ID} \
  -H "Authorization: Bearer ${TOKEN}" | jq .
```

**预期响应**（`200 OK`）：

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "fileName": "test-doc.txt",
    "status": "COMPLETED",
    "parsedAt": "2026-03-29T16:30:00",
    "embeddedAt": "2026-03-29T16:30:05",
    "errorMessage": null
  }
}
```

**状态流转说明**：

```
PENDING → PARSING → PARSED → EMBEDDING → COMPLETED
                                     ↓
                                  FAILED（向量化失败时）
```

**验证点**：
- `status` 最终为 `COMPLETED`
- `parsedAt` 非空（解析完成时间）
- `embeddedAt` 非空（向量化完成时间）
- `errorMessage` 为 null

**如果状态为 `FAILED`**：检查 `errorMessage` 字段，常见原因：
- AI API Key 未配置或无效
- Embedding 模型名称错误
- 网络连接问题

---

### 步骤 4：验证文档片段

```bash
curl -s ${BASE_URL}/documents/${DOC_ID}/fragments \
  -H "Authorization: Bearer ${TOKEN}" | jq .
```

**预期响应**（`200 OK`）：

```json
{
  "code": 200,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "documentId": 1,
      "fragmentIndex": 0,
      "content": "人工智能（Artificial Intelligence...）...",
      "charCount": 420
    },
    {
      "id": 2,
      "documentId": 1,
      "fragmentIndex": 1,
      "content": "自然语言处理（NLP）...检索增强生成（RAG）...",
      "charCount": 380
    }
  ]
}
```

**验证点**：
- 片段数量 ≥ 1（500 字以上的文本应产生 2-3 个片段）
- 片段按 `fragmentIndex` 升序排列
- 相邻片段有内容重叠（overlap 设计）

---

### 步骤 5：验证向量记录（数据库直查）

```bash
docker exec -i moveon-postgres psql -U moveon -d moveon -c "
SELECT id, fragment_id, document_id, user_id, status,
       vector_dims(embedding) as dimensions,
       created_at
FROM document_vectors
WHERE document_id = ${DOC_ID};
"
```

**预期输出**：

```
 id | fragment_id | document_id | user_id |  status   | dimensions |          created_at
----+-------------+-------------+---------+-----------+------------+-------------------------------
  1 |           1 |           1 |       1 | COMPLETED |       1024 | 2026-03-29 16:30:05.123456
  2 |           2 |           1 |       1 | COMPLETED |       1024 | 2026-03-29 16:30:05.234567
```

**验证点**：
- 每个片段对应一条向量记录
- `dimensions` 为 1024（对应 BAAI/bge-m3 模型）
- `status` 为 `COMPLETED`
- `embedding` 字段非空

**额外验证**：查询向量相似度是否有效

```bash
docker exec -i moveon-postgres psql -U moveon -d moveon -c "
SELECT dv.id, df.fragment_index,
       1 - (dv.embedding <=> dv.embedding) AS self_similarity
FROM document_vectors dv
JOIN document_fragments df ON dv.fragment_id = df.id
WHERE dv.document_id = ${DOC_ID}
LIMIT 1;
"
```

**预期**：`self_similarity` 应为 `1.0`（自身与自身的余弦相似度）。

---

### 步骤 6：语义检索

**请求**：

```bash
curl -s "${BASE_URL}/documents/search?query=什么是机器学习&topK=3" \
  -H "Authorization: Bearer ${TOKEN}" | jq .
```

**预期响应**（`200 OK`）：

```json
{
  "code": 200,
  "message": "Success",
  "data": [
    {
      "fragmentId": 1,
      "documentId": 1,
      "fileName": "test-doc.txt",
      "fragmentIndex": 0,
      "content": "机器学习是人工智能的核心技术之一...",
      "score": 0.82
    }
  ]
}
```

**验证点**：
- `score` > 0.5（相关性较高）
- `content` 中确实包含与"机器学习"相关的内容
- `fileName` 包含文档来源信息
- 结果按 `score` 降序排列

**多组查询验证**：

```bash
# 查询 1：关于向量数据库
curl -s "${BASE_URL}/documents/search?query=向量数据库是什么&topK=3" \
  -H "Authorization: Bearer ${TOKEN}" | jq '.data[0].content'

# 预期：返回包含"向量数据库"和"pgvector"的片段

# 查询 2：关于 RAG
curl -s "${BASE_URL}/documents/search?query=RAG技术原理&topK=3" \
  -H "Authorization: Bearer ${TOKEN}" | jq '.data[0].content'

# 预期：返回包含"检索增强生成"的片段
```

---

### 步骤 7：用户隔离验证

创建一个新用户，验证无法检索到其他用户的文档。

**创建新用户**：

```bash
curl -s -X POST ${BASE_URL}/auth/users \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123"}' | jq .
```

**用新用户登录**：

```bash
TEST_TOKEN=$(curl -s -X POST ${BASE_URL}/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123"}' | jq -r '.data.accessToken')
```

**用新用户搜索（应无结果）**：

```bash
curl -s "${BASE_URL}/documents/search?query=机器学习&topK=3" \
  -H "Authorization: Bearer ${TEST_TOKEN}" | jq .
```

**预期响应**：

```json
{
  "code": 200,
  "message": "Success",
  "data": []
}
```

**验证点**：
- 新用户的搜索结果为空数组
- 管理员的文档不会泄露给其他用户

---

### 步骤 8：向量化失败重试

**场景**：模拟向量化失败后手动重试。

**手动触发向量化**：

```bash
curl -s -X POST ${BASE_URL}/documents/${DOC_ID}/embed \
  -H "Authorization: Bearer ${TOKEN}" | jq .
```

**预期响应**：

```json
{
  "code": 200,
  "message": "Success",
  "data": "向量化任务已提交"
}
```

等待几秒后检查状态是否回到 `COMPLETED`。

**验证点**：
- 即使文档已是 `COMPLETED` 状态，仍可手动触发重新向量化
- 旧的向量记录会被清除并重新生成

---

### 步骤 9：错误场景验证

**9.1 无效文件类型**

```bash
curl -s -X POST ${BASE_URL}/documents/upload \
  -H "Authorization: Bearer ${TOKEN}" \
  -F "file=@/tmp/test.exe" | jq .
```

**预期**：`400 Bad Request`，错误码 `UNSUPPORTED_FILE_TYPE`

**9.2 无令牌访问受保护接口**

```bash
curl -s ${BASE_URL}/documents | jq .
```

**预期**：`401 Unauthorized` 或 `403 Forbidden`

**9.3 无效令牌**

```bash
curl -s ${BASE_URL}/documents \
  -H "Authorization: Bearer invalid-token" | jq .
```

**预期**：`401 Unauthorized`

**9.4 访问其他用户的文档**

```bash
curl -s ${BASE_URL}/documents/${DOC_ID} \
  -H "Authorization: Bearer ${TEST_TOKEN}" | jq .
```

**预期**：`400 Bad Request`，错误码 `ACCESS_DENIED`

**9.5 空查询文本检索**

```bash
curl -s "${BASE_URL}/documents/search?query=&topK=3" \
  -H "Authorization: Bearer ${TOKEN}" | jq .
```

**预期**：`400 Bad Request`，错误码 `INVALID_QUERY`

---

## 3. 数据库验证

### 3.1 表结构检查

```bash
docker exec -i moveon-postgres psql -U moveon -d moveon -c "\d document_vectors"
```

**预期输出**：

```
                                         Table "public.document_vectors"
   Column    |            Type             | Collation | Nullable |               Default
-------------+-----------------------------+-----------+----------+-------------------------------------
 id          | bigint                      |           | not null | nextval('document_vectors_id_seq'::regclass)
 fragment_id | bigint                      |           | not null |
 document_id | bigint                      |           | not null |
 user_id     | bigint                      |           | not null |
 embedding   | vector(1024)                |           |          |
 status      | character varying(20)       |           | not null | 'PENDING'::character varying
 created_at  | timestamp without time zone |           | not null | CURRENT_TIMESTAMP
 updated_at  | timestamp without time zone |           | not null | CURRENT_TIMESTAMP
```

### 3.2 索引检查

```bash
docker exec -i moveon-postgres psql -U moveon -d moveon -c "
SELECT indexname, indexdef FROM pg_indexes WHERE tablename = 'document_vectors';
"
```

**预期索引**：
- `document_vectors_pkey`（主键）
- `idx_vectors_fragment_id`
- `idx_vectors_document_id`
- `idx_vectors_user_id`
- `idx_vectors_status`

### 3.3 数据完整性检查

```bash
docker exec -i moveon-postgres psql -U moveon -d moveon -c "
SELECT
    d.id as doc_id,
    d.status as doc_status,
    COUNT(DISTINCT df.id) as fragment_count,
    COUNT(DISTINCT dv.id) as vector_count,
    COUNT(DISTINCT dv.id) FILTER (WHERE dv.status = 'COMPLETED') as completed_vectors
FROM documents d
LEFT JOIN document_fragments df ON df.document_id = d.id
LEFT JOIN document_vectors dv ON dv.document_id = d.id
GROUP BY d.id, d.status
ORDER BY d.id;
"
```

**预期**：对于已完成的文档，`fragment_count` = `vector_count` = `completed_vectors`。

---

## 4. 日志验证

### 4.1 检查应用日志中的关键事件

上传文档后，在应用日志中应看到以下关键事件（按时间顺序）：

```
# 1. 文件上传
INFO  c.m.d.service.DocumentService - File uploaded to MinIO: bucket=moveon-documents, path=1/2026-03-29/...

# 2. 开始解析
INFO  c.m.d.service.DocumentParsingService - Starting async parsing for document 1

# 3. 文本提取
INFO  c.m.d.service.DocumentParsingService - Downloaded file for document 1: 756 bytes
INFO  c.m.d.service.DocumentParsingService - Extracted text from document 1: 756 chars
INFO  c.m.d.service.DocumentParsingService - Document 1 split into 2 fragments

# 4. 解析完成
INFO  c.m.d.service.DocumentParsingService - Document 1 parsed successfully

# 5. 开始向量化
INFO  c.m.d.service.DocumentEmbeddingService - Starting async embedding for document 1

# 6. 向量化完成
INFO  c.m.d.service.DocumentEmbeddingService - Document 1 embedding completed: 2/2 fragments embedded

# 7. 语义检索
INFO  c.m.d.service.SemanticSearchService - Semantic search: userId=1, query='什么是机器学习', topK=3
INFO  c.m.d.service.SemanticSearchService - Semantic search completed: found 1 results for userId=1
```

### 4.2 检查无 ERROR 日志

```bash
grep "ERROR" logs/application.log | grep -v "test"
# 预期：无输出（或仅有测试相关的错误）
```

---

## 5. Swagger UI 验证

访问 Swagger UI 确认所有新增接口已注册：

```
http://localhost:8080/api/swagger-ui.html
```

**预期可见接口**：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/documents/upload` | 上传文档 |
| GET | `/documents` | 文档列表 |
| GET | `/documents/{id}` | 文档详情 |
| POST | `/documents/{id}/parse` | 重新解析 |
| GET | `/documents/{id}/fragments` | 文档片段 |
| POST | `/documents/{id}/embed` | 手动向量化 |
| GET | `/documents/search` | 语义检索 |

---

## 6. 性能基准参考

| 操作 | 预期耗时（本地开发环境） |
|------|--------------------------|
| 文件上传 | < 1 秒 |
| 文档解析（TXT） | < 2 秒 |
| 单片段向量化（调用 API） | 1-3 秒 |
| 语义检索 | < 2 秒 |

> 实际耗时取决于网络状况和 AI API 响应速度。

---

## 7. 验证检查清单

| # | 验证项 | 预期结果 | 通过 |
|---|--------|----------|------|
| 1 | Docker 容器全部 healthy | 3/3 服务正常 | ☐ |
| 2 | 应用正常启动 | 无 ERROR 日志 | ☐ |
| 3 | 管理员登录成功 | 返回 accessToken | ☐ |
| 4 | 上传 TXT 文件成功 | status=PENDING | ☐ |
| 5 | 文档状态流转到 COMPLETED | parsedAt 和 embeddedAt 非空 | ☐ |
| 6 | 文档片段正确生成 | ≥ 1 个片段，按序排列 | ☐ |
| 7 | 向量记录已写入 | dimensions=1024, status=COMPLETED | ☐ |
| 8 | 语义检索返回相关结果 | score > 0.5 | ☐ |
| 9 | 用户隔离生效 | 新用户搜索结果为空 | ☐ |
| 10 | 手动向量化重试成功 | 可重新触发 | ☐ |
| 11 | 无效文件类型被拒绝 | 400 错误 | ☐ |
| 12 | 无令牌被拒绝 | 401/403 错误 | ☐ |
| 13 | 数据库表结构完整 | 字段和索引正确 | ☐ |
| 14 | 日志无异常 ERROR | 运行过程中无未处理异常 | ☐ |
