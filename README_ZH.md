# 智能工单平台

这是一个面向企业内部 IT 服务台的 AI 辅助工单平台，采用 Java 业务中台 + Python AI 微服务的混合架构。

当前代码处于迁移计划的 Phase 3：前端统一访问 Java Gateway，业务域由 Java Platform Service 持有，Python 仅作为内部 AI 服务保留大模型与向量化能力。Phase 4 的 Nacos、Sentinel、Kubernetes 等治理与部署强化尚未开始。

英文文档：[README.md](README.md)

## 核心能力

### 普通用户

- 通过引导式表单提交 IT 支持工单。
- 创建工单前使用 AI 检索知识库，优先推荐已有解决方案。
- 查看自己的工单和状态变化。
- 接收工单状态变更通知。

### 工程师

- 在开放和处理中队列中处理工单。
- 原子化认领工单，避免多个工程师重复抢单。
- 添加评论和解决说明。
- 获取 AI 修复建议和相似知识库推荐。
- 解决工单后触发异步知识库草稿生成。

### 管理员

- 查看工单和知识库统计。
- 查看审计日志。
- 管理知识库草稿和已发布文章。
- 与前端一样通过统一 Java API 入口访问系统。

## 架构

```text
┌────────────────────┐
│ React + TypeScript │
│ Vite + Ant Design  │
│ localhost:5173     │
└─────────┬──────────┘
          │ 公共 API：/api/v1/**
          ▼
┌────────────────────────┐
│ Java Gateway           │
│ Spring Cloud Gateway   │
│ localhost:8080         │
└─────────┬──────────────┘
          │ 内部业务 API：/api/**
          ▼
┌────────────────────────┐        ┌────────────────────────┐
│ Java Platform Service  │───────▶│ Python AI Service      │
│ Spring Boot 3.5 + JPA  │        │ FastAPI + LangChain    │
│ localhost:8081         │        │ localhost:8100         │
└─────────┬──────────────┘        └─────────┬──────────────┘
          │                                 │
          ▼                                 ▼
┌────────────────────────┐        ┌────────────────────────┐
│ PostgreSQL 16          │        │ RabbitMQ 4.1           │
│ pgvector, HNSW, 384d   │        │ 异步 AI 工作流          │
└────────────────────────┘        └────────────────────────┘
```

## 服务边界

| 服务 | 职责 |
| --- | --- |
| `frontend` | 浏览器界面、路由守卫、通过 Java Gateway 调用 API |
| `platform-java/gateway` | 唯一公共入口、请求转发、后续治理组件接入点 |
| `platform-java/platform-service` | 认证、用户、工单、评论、通知、知识库元数据、审计日志、RabbitMQ 事件 |
| `ai-service` | 大模型调用、字段提取、追问生成、描述增强、修复建议、向量检索、向量重建、知识库草稿生成 |
| `postgres` | 共享 PostgreSQL 16 实例，启用 pgvector |
| `rabbitmq` | 异步工单和 AI 工作流事件 |

## 事件模型

Phase 3 使用 RabbitMQ 作为 AI 与知识库流程的异步骨架。

| 事件 | 用途 |
| --- | --- |
| `ticket.created` | 工单创建后触发非阻塞 AI 分析 |
| `ticket.updated` | 响应业务变更，不阻塞主请求 |
| `ticket.resolved` | 根据解决说明生成知识库草稿候选 |
| `knowledge.published` | 通知后续向量化或索引流程 |
| `ai.analysis.completed` | 通过 Java 边界持久化 AI 结果 |
| `knowledge.draft.generated` | 将生成的草稿展示给工程师或管理员审核 |

## 大模型与向量化

- 大模型配置保持服务商兼容，当前支持 DeepSeek 风格、OpenAI 风格和 Anthropic 风格的环境变量配置。
- Python 服务隔离 AI 核心逻辑，不直接持有业务主流程。
- 向量化模型使用 `sentence-transformers/all-MiniLM-L6-v2`。
- 向量维度为 384。
- 知识库相似度检索使用 pgvector HNSW 索引。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | React 19、TypeScript 5.9、Ant Design 6、Zustand 5、Vite 8 |
| 网关 | Spring Cloud Gateway、Spring Boot 3.5 |
| Java 业务中台 | Spring Boot 3.5、Java 17、Spring Data JPA、Spring Security、JWT、RabbitMQ |
| AI 微服务 | Python 3.11、FastAPI、LangChain、SQLAlchemy、sentence-transformers |
| 数据库 | PostgreSQL 16、pgvector、HNSW 索引、`vector(384)` |
| 消息中间件 | RabbitMQ 4.1 management 镜像 |
| 基础设施 | Docker Compose、Maven，后续扩展 Kubernetes |
| 后续治理 | Phase 4 接入 Nacos、Sentinel、Kubernetes、Helm 骨架 |

## 快速启动

### 前置要求

- Java 17 或更高版本
- Node.js 18 或更高版本以及 npm
- Docker Desktop 或 Docker Engine
- Python 3.11 或更高版本，用于本地 AI 服务开发
- 基于 [.env.example](.env.example) 创建本地 `.env`

### 1. 配置环境变量

本地创建 `.env`，至少设置：

```bash
POSTGRES_PASSWORD=zhigong123
JWT_SECRET=<base64-or-long-random-secret>
```

可选的大模型变量：

```bash
LLM_PROVIDER=deepseek
LLM_MODEL=deepseek-chat
LLM_API_KEY=<your-key>
LLM_BASE_URL=https://api.deepseek.com
```

不要提交 `.env`。

### 2. 启动基础设施和 AI 服务

```bash
docker compose up -d postgres rabbitmq ai-service
```

健康检查：

```bash
curl -fsS http://localhost:8100/health
docker compose ps
```

### 3. 启动 Java Platform Service

```bash
mvn -s .mvn/settings.xml -pl platform-java/platform-service spring-boot:run
```

Platform Service 健康检查：

```bash
curl -fsS http://localhost:8081/actuator/health
```

### 4. 启动 Java Gateway

```bash
PLATFORM_SERVICE_URL=http://localhost:8081 \
mvn -s .mvn/settings.xml -pl platform-java/gateway spring-boot:run
```

Gateway 健康检查：

```bash
curl -fsS http://localhost:8080/actuator/health
```

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173。

### 使用完整 Docker Compose

```bash
docker compose up --build
```

该命令会启动 PostgreSQL、RabbitMQ、AI 服务、Platform Service 和 Gateway。日常本地开发通常单独使用 `npm run dev` 启动前端。

## 演示账号

所有演示账号密码均为 `demo123`。

| 角色 | 用户名 |
| --- | --- |
| 普通用户 | `customer1` |
| 普通用户 | `customer2` |
| 普通用户 | `customer3` |
| 工程师 | `engineer1` |
| 工程师 | `engineer2` |
| 管理员 | `admin1` |

## API 概览

浏览器只允许调用 Gateway 的公共 API：`/api/v1/**`。Gateway 会将请求重写到 Platform Service 内部的 `/api/**` 路由。

### 认证

- `POST /api/v1/auth/login`

### 工单

- `POST /api/v1/tickets`
- `GET /api/v1/tickets`
- `GET /api/v1/tickets/{id}`
- `PUT /api/v1/tickets/{id}/assign`
- `PUT /api/v1/tickets/{id}/resolve`
- `GET /api/v1/tickets/{ticketId}/comments`
- `POST /api/v1/tickets/{ticketId}/comments`

### AI

- `POST /api/v1/ai/search`
- `POST /api/v1/ai/refine`
- `POST /api/v1/ai/suggest`
- `POST /api/v1/ai/similar`

### 知识库

- `GET /api/v1/kb/published`
- `GET /api/v1/kb/drafts`
- `PUT /api/v1/kb/{id}/publish`
- `PUT /api/v1/kb/{id}`
- `DELETE /api/v1/kb/{id}`

### 通知和管理

- `GET /api/v1/notifications`
- `GET /api/v1/notifications/unread-count`
- `PUT /api/v1/notifications/{id}/read`
- `GET /api/v1/admin/stats`
- `GET /api/v1/admin/audit-logs`

## 数据库初始化

`platform-service` 在本地开发中默认将 `spring.sql.init.mode` 设置为 `always`。`schema.sql` 使用 `CREATE TABLE IF NOT EXISTS` 保持幂等，`data.sql` 使用带条件的插入语句避免重复种子数据。生产风格运行可覆盖为：

```bash
SQL_INIT_MODE=never
```

## 质量检查

```bash
mvn -s .mvn/settings.xml test
cd frontend && npm run build
python3 -c "import py_compile; py_compile.compile('ai-service/main.py', cfile='/tmp/ai-service-main.pyc', doraise=True)"
docker compose config --quiet
docker compose build ai-service
```

## 项目结构

```text
.
├── ai-service/                       # FastAPI AI 与向量化服务
├── frontend/                         # React 前端应用
├── infra/                            # 基础设施说明和后续部署资产
├── platform-java/
│   ├── gateway/                      # Spring Cloud Gateway
│   └── platform-service/             # Spring Boot 业务中台
├── docker-compose.yml
├── DESIGN.md
├── README.md
└── README_ZH.md
```

## 安全说明

- JWT 使用 HS512 签名，并要求配置 `JWT_SECRET`。
- 密码使用 BCrypt 哈希。
- 基于角色的访问控制覆盖普通用户、工程师和管理员流程。
- 工单认领使用原子操作。
- 大模型输出在持久化前会进行清理。
- AI 交互和业务变更会记录审计信息。
- 密钥必须保存在 `.env` 或部署密钥系统中，不能提交到 Git。

## 路线图

- Phase 0：MVP 基线稳定。
- Phase 1：Java 基座和统一 Gateway。
- Phase 2：业务域迁移到 Java。
- Phase 3：AI 服务边界和 RabbitMQ 异步工作流。
- Phase 4：Nacos、Sentinel、Kubernetes、CI/CD 和部署强化。

## 许可证

MIT
