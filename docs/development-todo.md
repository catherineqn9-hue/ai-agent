# Sherry 督办助手开发待办清单

这份文档给另一台电脑继续开发使用。项目已经推到 GitHub：

```text
https://github.com/catherineqn9-hue/ai-agent.git
```

## 1. 新电脑初始化

### 1.1 拉取代码

```powershell
git clone https://github.com/catherineqn9-hue/ai-agent.git
cd ai-agent
```

### 1.2 必备环境

- JDK 17
- Maven 3.9+
- Node.js 20+
- Docker Desktop
- PostgreSQL 使用项目里的 `docker-compose.yml`

### 1.3 启动数据库

```powershell
docker compose up -d postgres
```

默认数据库配置：

```text
DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/sherry
DATABASE_USERNAME=sherry
DATABASE_PASSWORD=sherry_dev_password
```

### 1.4 配置 Kimi

不要把 key 写进代码或提交到 GitHub。只在本机环境变量里设置：

```powershell
$env:KIMI_API_KEY="你的 Kimi API Key"
$env:KIMI_BASE_URL="https://api.moonshot.ai/v1"
$env:KIMI_MODEL="moonshot-v1-8k"
```

如果要长期配置，可以使用系统环境变量，但仍然不要写入 `application.yml`。

### 1.5 启动后端

```powershell
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8010 --spring.flyway.enabled=false"
```

后端访问：

```text
http://127.0.0.1:8010/admin
http://127.0.0.1:8010/health
```

### 1.6 启动前端开发服务

```powershell
cd frontend
npm install
npm run dev
```

前端开发地址以 Vite 输出为准，通常是：

```text
http://127.0.0.1:8012/admin
```

## 2. 当前已完成能力

- 项目已从 Python MVP 迁移到 Java Spring Boot + Vue。
- 后端有统一响应体、业务响应码、全局异常处理。
- 后端使用 Spring Boot 3、MyBatis-Plus、PostgreSQL、LangChain4j。
- 前端使用 Vue 3 + Vite。
- 菜单从数据库动态加载。
- 已有通用 CRUD 管理页面。
- 已有 AI 助手问答页面。
- 已有督办事项列表、新增、编辑、删除。
- 已有督办事项详情接口和详情弹窗。
- 已有进度反馈新增和详情内反馈列表。
- 已有 Excel 导入入口、批次表 `supervision_batch`、导入错误表。
- 已有 Excel 导入模板表，模板字段支持 `source_columns` 和 `entity_fields` 对应。
- 已有 AI 工具调用日志表和日志管理页面。
- 已有 AI 通过业务接口查询事项、创建事项、更新状态、添加进度反馈等基础能力。
- 老 Python 代码已归档到 `legacy-python/`，后续主线以 Java + Vue 为准。

## 3. 下一步开发顺序

### 3.1 先修 Kimi 可用性

目标：让 AI 助手真正调用 Kimi，而不是只走降级回复。

要做：

- 确认使用的是 Kimi/Moonshot API 控制台生成的 API Key。
- 用环境变量 `KIMI_API_KEY` 启动后端。
- 调用 `/api/v1/chat/run` 验证不再返回 `401 Unauthorized`。
- 保留 `KimiConnectivityTest`，只在设置 `KIMI_API_KEY` 时手动跑。

验证命令：

```powershell
cd backend
$env:KIMI_API_KEY="你的 Kimi API Key"
mvn -q -Dtest=KimiConnectivityTest test
```

### 3.2 前端构建和静态资源同步

目标：确保 Vue 最新页面能打包，并同步到 Spring Boot 静态目录。

要做：

- 在 `frontend/` 运行 `npm run build`。
- 如果 Windows 出现 `esbuild spawn EPERM`，优先检查杀毒软件、权限、Node 安装路径。
- 构建成功后，把 `frontend/dist` 同步到：

```text
backend/src/main/resources/static
```

验证：

```powershell
cd backend
mvn -q -DskipTests package
java -jar target\sherry-supervision.jar --server.port=8010 --spring.flyway.enabled=false
```

然后访问：

```text
http://127.0.0.1:8010/admin
```

### 3.3 完善 Excel 模板设计

目标：不同 Excel 模板用不同策略解析，不把所有解析逻辑写在一起。

要做：

- `excel_import_template` 继续作为模板主表。
- 一条模板记录包含模板名称、模板 code、中文源字段 `source_columns`、英文实体字段 `entity_fields`。
- 导入时弹窗选择模板。
- 后端按 `template_code` 找到对应解析策略。
- 新增模板时只加策略类，不改核心导入流程。

建议结构：

```text
ExcelImportStrategy
StandardSupervisionExcelStrategy
MoreTemplateExcelStrategy
ExcelImportStrategyFactory
```

### 3.4 完善督办业务闭环

目标：让“督办事项”从创建到分派、反馈、完成形成完整流程。

要做：

- 责任人字段和责任人选择。
- 分派记录或分派状态。
- 状态流转规则：待分派、进行中、受阻、已完成、已取消。
- 进度反馈时间线。
- 催办记录。
- 完成确认。
- 列表筛选：状态、优先级、责任人、截止时间。

### 3.5 增强 AI 调业务接口能力

目标：AI 不只是聊天，而是能明确调用业务接口办事。

要做：

- 查询督办事项。
- 创建督办事项。
- 更新事项状态。
- 添加进度反馈。
- 查询某事项详情。
- 查询导入批次。
- 查询 AI 工具调用日志。
- 对高风险操作加确认步骤，例如删除、批量更新。

注意：

- AI 工具调用要记录到 `ai_tool_call_log`。
- 每次工具调用要记录入参、出参、耗时、状态。
- 不要让 AI 直接操作数据库，必须走 service/API 能力。

### 3.6 管理后台体验优化

目标：管理页面像后台系统，不做花哨营销页。

要做：

- 菜单支持排序、启用禁用、分组。
- CRUD 页面新增都使用弹窗。
- 列表页支持搜索、分页、刷新。
- 状态、优先级显示为标签。
- 表单字段根据配置动态渲染。
- 错误提示用统一 toast。

### 3.7 权限和安全

目标：后续能给真实用户使用。

要做：

- 登录鉴权。
- 管理员角色。
- 菜单权限。
- 接口权限。
- 操作审计日志。
- 环境变量管理，不提交任何 key、token、密码。

### 3.8 部署准备

目标：能打包到服务器稳定运行。

要做：

- `application-prod.yml` 完善生产配置。
- Dockerfile。
- 后端 jar 部署脚本。
- 数据库 migration 固化。
- 前端构建产物随后端 jar 发布。
- 日志目录和日志级别配置。
- 健康检查接口保留。

## 4. 代码规范

- Java 参考阿里巴巴 Java 开发规范。
- Controller 只做参数接收和响应返回，不写业务逻辑。
- Service 写业务流程。
- Mapper 只做数据库访问。
- DTO 和 Entity 分开。
- 异常走统一业务异常和全局异常处理。
- 前端组件保持单一职责。
- 不提交 `node_modules`、`target`、`dist`、日志、密钥。

## 5. 常用验证命令

后端测试：

```powershell
cd backend
mvn -q test
```

后端打包：

```powershell
cd backend
mvn -q -DskipTests package
```

前端构建：

```powershell
cd frontend
npm run build
```

检查 Git 状态：

```powershell
git status
```

提交并推送：

```powershell
git add .
git commit -m "说明本次改了什么"
git push
```

## 6. Git 分支建议

主分支：

```text
main
```

开发新功能建议从 main 拉分支：

```powershell
git checkout main
git pull
git checkout -b feature/功能名称
```

完成后：

```powershell
git add .
git commit -m "实现某某功能"
git push -u origin feature/功能名称
```

如果只是一个人开发，也可以直接在 `main` 提交，但每次提交前必须先确认：

- 没有 key。
- 没有日志。
- 没有构建产物。
- 后端测试能过。

## 7. 当前优先级最高的三件事

1. 换一个可用的 Kimi API Key，确认 AI 可以真实调用模型。
2. 修通前端 `npm run build`，同步最新 Vue 页面到后端静态资源。
3. 完善督办事项详情、责任人分派、催办记录和状态流转。
