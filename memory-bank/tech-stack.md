# AI 智能助理项目技术栈建议

## 1. 技术选型结论

基于当前的设计文档，本项目推荐采用以下核心技术栈：

- 开发语言：Java 21
- 后端框架：Spring Boot 3
- AI 应用框架：LangChain4j
- 关系数据库：PostgreSQL
- 向量检索：pgvector
- 缓存：Redis
- 文件存储：MinIO
- 文档解析：Apache Tika、Apache PDFBox、Apache POI
- 任务调度：Quartz
- 安全认证：Spring Security + JWT
- 容器化部署：Docker + Docker Compose
- 监控：Micrometer + Prometheus + Grafana

这套方案兼顾了以下目标：

- 满足 AI 智能助理在文档处理、知识库、任务提醒、问答对话方面的核心需求
- 保持 Java 技术栈一致性，降低系统复杂度
- 适合从 MVP 平滑演进到后续多模块、多设备、多模态扩展

---

## 2. 为什么选择 Java 21

本项目最终选择 Java 21，而不是 Java 17，主要原因如下：

### 2.1 Java 21 是更新的 LTS 版本

Java 21 属于长期支持版本，适合新项目长期演进。对于计划持续扩展的 AI 智能助理系统，选择更新的 LTS 能获得更长的技术生命周期。

### 2.2 更适合高并发和 I/O 密集型场景

本项目包含大量 I/O 密集操作，例如：

- 文件上传与解析
- 调用大模型接口
- 向量检索与数据库访问
- 定时提醒与异步任务执行
- 多用户并发对话

Java 21 提供的虚拟线程非常适合这类场景，能在保证开发体验的同时提升并发处理能力。

### 2.3 更适合新项目架构演进

本项目不是单纯的 CRUD 系统，而是面向 AI 能力集成、任务编排、知识检索和多轮对话的综合系统。Java 21 在语言能力、并发模型和生态兼容性上更适合作为长期基础版本。

### 2.4 Java 17 不是不能用

Java 17 同样稳定可靠，也完全能够支撑项目开发。但在新项目没有历史包袱的前提下，Java 21 更值得优先选择。

---

## 3. 为什么需要 LangChain4j

本项目是 AI 智能助理，不只是简单调用一次大模型接口，而是需要构建完整的 AI 应用能力层。核心场景包括：

- 基于资料库的智能问答
- 文档摘要、标签提取、结构化信息抽取
- 多轮对话上下文记忆
- 工具调用，例如查询待办、创建提醒、关联日程
- 基于知识库的 RAG 检索增强生成
- 后续支持多模态和 Agent 化扩展

在 Java 生态中，LangChain4j 非常适合承担这一层能力封装。

### 3.1 选择 LangChain4j 的原因

- 更贴近 LLM 应用开发场景
- 支持 Chat Memory、多轮对话记忆
- 支持 RAG 和 Embedding 集成
- 支持 Tool Calling，便于接入任务、提醒、知识库等内部服务
- 支持结构化输出，方便生成标签、摘要、任务数据
- 与 Spring Boot 集成自然，适合 Java 项目落地

### 3.2 与 Spring AI 的取舍

Spring AI 也可以使用，且与 Spring 生态整合较好。但对于本项目这种以 AI 助理能力为核心的系统，LangChain4j 在 AI 场景适配上更直接，更适合作为主 AI 框架。

结论：

- 主 AI 框架推荐：LangChain4j
- Spring 生态基础框架推荐：Spring Boot 3

---

## 4. 推荐的整体技术栈

## 4.1 后端基础层

- Java 21
- Spring Boot 3
- Spring Web
- Spring Validation
- Spring Actuator

用途：

- 提供 REST API
- 管理项目配置、模块装配、应用生命周期
- 暴露健康检查与监控指标

## 4.2 AI 能力层

- LangChain4j
- 大模型接入：OpenAI、Azure OpenAI、通义千问、智谱、DeepSeek、Ollama
- Embedding 模型：根据部署环境选择云端或本地模型

用途：

- 对话生成
- 文档摘要
- 标签提取
- 知识问答
- 多轮对话记忆
- 工具调用
- RAG 编排

## 4.3 数据存储层

- PostgreSQL
- pgvector
- Redis
- MinIO

用途：

- PostgreSQL：存储用户、任务、文档元数据、知识条目、权限等结构化数据
- pgvector：存储文本向量，支撑语义检索
- Redis：缓存热点数据、会话状态、短期上下文、限流控制
- MinIO：存储原始文件和处理后的附件

## 4.4 文档处理层

- Apache Tika
- Apache PDFBox
- Apache POI

用途：

- Tika：统一解析多种文档格式
- PDFBox：增强 PDF 内容提取能力
- POI：处理 Word、Excel、PPT 等 Office 文件

## 4.5 安全与权限层

- Spring Security
- JWT
- BCrypt

用途：

- 用户认证与授权
- 接口访问控制
- 敏感数据保护
- 多角色权限管理

## 4.6 任务调度与异步处理

- Quartz
- Spring Scheduler
- Spring Async

用途：

- 定时提醒
- 延迟任务执行
- 文档解析异步处理
- 向量化和摘要生成后台任务

## 4.7 可观测性与运维

- Micrometer
- Prometheus
- Grafana
- Logback

用途：

- 收集系统指标
- 监控接口耗时和异常率
- 跟踪问答延迟、文档处理耗时、任务执行状态
- 支撑后续性能优化

## 4.8 部署方式

- Docker
- Docker Compose

后续扩展可考虑：

- Kubernetes
- Nginx

用途：

- 本地开发环境统一
- 测试与部署标准化
- 后期支持弹性扩容

---

## 5. 面向本项目的推荐架构

当前阶段最适合的不是一开始就上微服务，而是采用：

**模块化单体架构 + AI 能力中心化设计**

推荐原因：

- 当前需求仍处于 MVP 到第二阶段演进期
- 核心目标是快速验证文档问答、知识库、待办提醒等能力
- 微服务会显著增加部署、运维、调用链和数据一致性成本
- 模块化单体更适合前期快速迭代，后续也更容易按模块拆分

建议按业务拆分为以下模块：

- `auth`：用户、登录、权限
- `document`：文件上传、解析、摘要、标签
- `knowledge`：知识库、笔记、批注、版本管理
- `assistant`：对话、RAG、记忆、工具调用
- `task`：待办、提醒、日程
- `notification`：通知、推送、提醒发送
- `infra`：AI 客户端、缓存、存储、日志、公共组件

---

## 6. 对应设计文档的技术映射

### 6.1 资料管理与智能分析

推荐技术：

- 文件上传：Spring Boot + MinIO
- 文档解析：Tika + PDFBox + POI
- 摘要与标签：LangChain4j
- 存储索引：PostgreSQL + pgvector

### 6.2 智能问答与检索

推荐技术：

- AI 编排：LangChain4j
- 语义检索：pgvector
- 对话记忆：Redis + LangChain4j Memory
- 引用溯源：在检索结果中保留文档片段与来源信息

### 6.3 个人知识管理

推荐技术：

- 知识条目存储：PostgreSQL
- 收藏、批注、版本：PostgreSQL
- 智能推荐：LangChain4j + 检索策略 + 用户行为数据

### 6.4 日常事务管理

推荐技术：

- 任务管理：Spring Boot + PostgreSQL
- 定时提醒：Quartz
- 智能提醒：LangChain4j + 规则引擎 + 任务上下文

### 6.5 数据安全与隐私

推荐技术：

- 权限控制：Spring Security
- 认证机制：JWT
- 数据加密：数据库敏感字段加密 + HTTPS
- 文件访问控制：MinIO 策略控制

---

## 7. MVP 阶段建议采用的最小技术组合

如果以最小可行产品为目标，建议先采用以下组合：

- Java 21
- Spring Boot 3
- LangChain4j
- PostgreSQL
- pgvector
- Redis
- MinIO
- Apache Tika
- Quartz
- Spring Security + JWT

这套最小组合已经可以覆盖以下能力：

- 用户登录与权限管理
- 文件上传与文档解析
- 文档向量化与语义检索
- 基础 RAG 问答
- 简单任务与待办管理
- 基础智能提醒

---

## 8. 后续扩展建议

当项目进入第二阶段和第三阶段后，可以继续扩展以下技术能力：

- 全文搜索增强：Elasticsearch
- 消息队列：RabbitMQ 或 Kafka
- 多模态能力：语音识别、图像理解模型
- 工作流编排：引入更细粒度的 AI Agent 或任务编排机制
- 多设备同步：增加同步策略与推送机制
- 知识图谱：Neo4j 或图数据库方案

注意：

知识图谱和图数据库不建议在 MVP 阶段就引入。当前阶段优先把文档处理、问答、知识管理和任务提醒跑通，收益更高。

---

## 9. 最终推荐方案

本项目推荐采用以下主技术栈：

**Java 21 + Spring Boot 3 + LangChain4j + PostgreSQL + pgvector + Redis + MinIO + Apache Tika + Quartz + Spring Security**

这是当前最适合该 AI 智能助理项目的 Java 技术方案，原因如下：

- 充分体现 AI 应用特点，而不是传统业务系统思路
- 能覆盖文档处理、RAG 问答、知识库、任务提醒等核心需求
- 保持 Java 生态统一，利于长期维护
- 架构上适合从 MVP 平稳演进到复杂系统

如果后续需要，可以在此基础上继续补充：

- 项目模块划分文档
- 系统架构图
- 数据库表结构设计
- MVP 开发路线图
