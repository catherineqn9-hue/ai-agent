# Project Structure

当前项目已经从 Python MVP 迁移到 Java 后端 + Vue 前端。主代码必须按下面结构维护。

```text
sherry-dialogue-agent-mvp/
  backend/
    src/main/java/com/sherry/supervision/
      ai/                 LangChain4j 工具定义
      common/             通用响应结构
      config/             Jackson、MyBatis 类型处理器等配置
      configresource/     动态配置资源注册表
      controller/         HTTP API Controller
      dto/                请求和响应 DTO
      entity/             数据库实体
      excel/              Excel 导入策略
      mapper/             MyBatis-Plus Mapper
      service/            业务服务接口与编排
      service/impl/       业务服务实现
    src/main/resources/
      db/migration/       Flyway SQL
      static/             Vue 构建产物，仅由 frontend build 同步生成
  frontend/
    src/
      api/                后端 API 客户端
      components/         Vue 页面与业务组件
      styles/             全局样式
    vite.config.js        Vite 构建与开发代理
  database/               独立 SQL 脚本
  docs/                   工程文档
  legacy-python/          旧 FastAPI/LangGraph MVP 归档
```

## 维护规则

1. 新后端能力写在 `backend/`，不再向 `legacy-python/` 增加业务代码。
2. 新前端页面写在 `frontend/src/`，不再维护手写的单文件 `static/admin.html`。
3. `backend/src/main/resources/static/` 只保存 Vue 构建产物，不手写业务页面。
4. AI 不直接写数据库，必须通过 `service` 或显式业务工具调用。
5. Excel 导入按策略拆分，使用 `handler_code` 或模板 code 区分不同模板。

## 后端分层职责

| 目录 | 职责 | 不应承担 |
|---|---|---|
| `controller` | HTTP 入参、响应、状态码 | SQL、复杂业务判断 |
| `dto` | API 契约 | 持久化逻辑 |
| `service` | 业务流程、校验、事务边界 | HTTP 细节 |
| `mapper` | 数据访问 | 业务决策 |
| `ai` | AI 可调用工具 | 绕过服务层直接写库 |
| `excel` | Excel 模板解析策略 | Controller 逻辑 |

## 前端分层职责

| 目录 | 职责 |
|---|---|
| `src/api` | fetch 封装和接口函数 |
| `src/components` | 页面和可复用业务组件 |
| `src/styles` | 全局样式与布局 |

前端优先保持轻量，不默认引入大型 UI 组件库。确实需要组件库时，应先评估首屏包体、表格性能和长期维护成本。
