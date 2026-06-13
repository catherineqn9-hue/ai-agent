# Sherry 智能督办后台

Sherry 是一个以督办事项为核心的 AI 助手后台。当前主栈已经迁移为：

- Backend: Spring Boot 3 + MyBatis-Plus + LangChain4j
- Frontend: Vue 3 + Vite
- Database: PostgreSQL
- Legacy: 旧 FastAPI/LangGraph 代码已归档到 `legacy-python/`

## 目录结构

```text
sherry-dialogue-agent-mvp/
  backend/          Spring Boot 后端，提供业务 API、AI 工具、Excel 导入
  frontend/         Vue 管理后台源码，后续前端开发主目录
  database/         PostgreSQL schema 和 seed SQL
  docs/             工程规范、表设计、结构说明
  legacy-python/    旧 Python MVP 归档，只做参考，不作为主入口
```

## 本地启动

启动 PostgreSQL：

```powershell
rtk docker compose up -d postgres
```

启动后端：

```powershell
cd backend
rtk mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8010 --spring.flyway.enabled=false"
```

启动前端开发服务：

```powershell
cd frontend
rtk npm install
rtk npm run dev
```

开发时访问：

```text
http://127.0.0.1:8012/admin
```

后端也会内置一份最新构建后的 Vue 页面，可直接访问：

```text
http://127.0.0.1:8010/admin
```

## 构建验证

后端：

```powershell
cd backend
rtk mvn -q test
rtk mvn -q -DskipTests package
```

前端：

```powershell
cd frontend
rtk npm run build
```

## 当前能力

- 动态菜单从数据库加载。
- AI 助手页面调用后端 Chat API。
- 后台通用 CRUD 通过菜单配置驱动。
- 督办事项支持列表、新增、编辑、删除。
- Excel 导入支持模板选择，模板字段包含 `source_columns` 与 `entity_fields` 的一一对应。
- AI 业务工具已支持查询事项、创建事项、更新状态、添加进度反馈、查询进度反馈。
