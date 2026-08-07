# PostgreSQL（AI 知识库）准备说明

> 用途：AI 客服 RAG 的向量库 + 全文检索库。**当前本机未安装 PostgreSQL，需要先准备一个可用的 PG（≥12，启用 vector 扩展）。**

## 方案 A：本机安装 PostgreSQL（推荐开发用）

1. 下载安装：https://www.postgresql.org/download/windows/ （建议 16/17，默认端口 5432，记下 postgres 密码）
2. 安装 pgvector 扩展（Windows）：
   - 方式1：使用 EDB StackBuilder 安装 pgvector（PG 16 以上支持）
   - 方式2：从 https://github.com/pgvector/pgvector/releases 下载对应 PG 版本的 Windows 安装包
   - 方式3：`pip install pgvector`（需本机有 Python + 编译环境，较麻烦，不推荐）
3. 验证：`psql -U postgres -h 127.0.0.1` 后执行 `CREATE EXTENSION IF NOT EXISTS vector;`

## 方案 B：Docker 运行 PostgreSQL（本机需先装 Docker Desktop）

```powershell
docker run -d --name pg-ai -p 5432:5432 -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=group_buy_market_ai pgvector/pgvector:pg16
```

## 方案 C：使用已有服务器 / 云数据库

- 提供可访问的 host:port、用户名、密码，并确认能执行 `CREATE EXTENSION vector`（RDS/云 PG 需控制台开启插件）。

## 建库与执行 DDL

```sql
-- 建库（如不存在）
CREATE DATABASE group_buy_market_ai;
-- 执行 DDL（文件：docs/dev-ops/pgsql/ai_knowledge.sql）
-- psql -h <host> -p <port> -U postgres -d group_buy_market_ai -f docs/dev-ops/pgsql/ai_knowledge.sql
```

## 连通性自检

```powershell
Test-NetConnection -ComputerName <host> -Port <port>
```