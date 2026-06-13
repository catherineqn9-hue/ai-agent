# Sherry MVP 可落地点代码导览

这份不是方案文档，是给演示和答疑用的代码索引。

## 1. Context 上下文

文件：`app/main.py`

落地点：

- `run_agent_turn()` 里把 `thread_id`、`user_id`、历史 `messages`、`context` 合成 `initial_state`
- 这就是对话系统里的 Context
- 用户补充信息时，系统不会从头开始，而是沿同一个 `thread_id` 继续

## 2. Flow 对话流程

文件：`app/agent/graph.py`

落地点：

- `StateGraph(AgentState)` 定义主流程
- 节点顺序：输入标准化 -> 意图理解 -> 槽位抽取 -> 完整性判断 -> 追问/回复 -> 输出校验
- `judge_completeness` 后面用条件分支决定走 `ask_clarification` 还是 `generate_answer`

## 3. Slot Filling 信息补全

文件：`app/agent/rules.py`

落地点：

- `extract_slots()` 抽取任务事项、涉及对象、完成时间、目标
- `find_missing_slots()` 判断缺哪些信息
- `build_questions()` 把缺失槽位转成追问问题

## 4. Action 动作体系

文件：`app/agent/actions.py`

一期已落地：

- `ClarifyAction`：信息不足时追问
- `ReplyAction`：信息足够时生成结构化回复

二期预留：

- `OaTaskDraftAction`：创建 OA 任务草稿
- `ParseAttachmentAction`：解析附件

关键点：

- 后续接 OA 不需要推翻主流程，只要新增 Action
- 涉及派单、办结、通知的动作建议先生成草稿，再人工确认

## 5. Memory / Tracker 状态保存

文件：`app/storage.py`

落地点：

- MVP 用 `InMemoryConversationStore` 保存会话状态
- 保存内容包括 `messages`、`slots`、`trace`
- 正式环境可以替换成 PostgreSQL，不影响 API 和 Flow

## 6. HTML 演示页

文件：`static/demo.html`

落地点：

- 左侧展示对话
- 右侧展示 `thread_id`、`intent`、`slots`、`trace`
- 适合给领导演示“为什么这不是简单聊天，而是任务型对话流程”

## 7. 测试

文件：`tests/test_chat_flow.py`

已覆盖：

- 信息不足时追问
- 用户补充后继续同一会话
- 状态接口可查询
- HTML 演示页可访问
- Action 扩展口可运行
