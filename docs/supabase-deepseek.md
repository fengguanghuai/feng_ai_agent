# Supabase + DeepSeek 本地启动

此配置使用 PostgreSQL 和 DeepSeek OpenAI-compatible API，默认启用随代码打包的本地知识库检索，不需要 MySQL、Ollama、SMTP、embedding、搜索密钥或 MCP 服务；也可显式切换百炼检索，回答模型仍为 DeepSeek。当前只用于本机开发，尚未提供用户登录与多用户权限隔离，不要直接发布公网。

## 准备

- JDK 21、Maven（或仓库 Maven Wrapper）、Node.js 22 LTS。
- Supabase 独立项目。通过控制台 SQL Editor 执行 [PostgreSQL 初始化脚本](../src/main/resources/db/ai_agent_postgres.sql)。脚本仅创建业务对象，不删除现有数据；六张表启用 RLS，撤销 anon/authenticated 访问。
- 若本机只支持 IPv4，使用控制台 Connect → Direct → Session pooler。长期 Java 服务使用 5432 会话池，不要混用 6543 事务池。JDBC 连接要求 SSL。
- DeepSeek API Key。密钥放在仓库外的本地环境配置中，文件权限 0600；不得放入 VITE_* 或提交 Git。

所需环境变量（示例没有真实密钥）：

```sh
export SUPABASE_JDBC_URL='jdbc:postgresql://<session-pooler-host>:5432/postgres?sslmode=require'
export SUPABASE_DB_USER='postgres.<project-ref>'
export SUPABASE_DB_PASSWORD='<database-password>'
export DEEPSEEK_API_KEY='<deepseek-api-key>'
export DEEPSEEK_MODEL='deepseek-flash'
```

## 启动

在仓库根目录，先加载上述环境变量，再运行：

```sh
mvn -Dtest=SupabaseIntegrationTest test
mvn -Dmaven.test.skip=true package
java -jar target/feng-ai-agent-0.0.1-SNAPSHOT.jar --spring.profiles.active=supabase
```

集成测试会连接真实数据库，以事务回滚测试消息，不调用模型。旧测试中有真实邮件、终端和付费 API 调用，不要在凭据齐全的环境下直接运行全量测试。

前端在另一个终端：

```sh
cd feng-ai-agent-frontend
npm ci
npm run dev
```

访问 http://127.0.0.1:3000 。Vite 将 `/api` 代理到后端 http://127.0.0.1:8123/api 。生产构建需要同源反向代理 `/api`，或通过 `VITE_API_BASE_URL` 指定公开的后端地址（绝不能包含密钥）。

## 新版聊天前端（2026-11-09）

- 使用 Vue 3.5.45、Ant Design X Vue 2.0.0 的 Bubble / Sender、Vite 8.3.6。Node 需满足 `^20.19.0 || >=22.12.0`。
- 首页直接进入恋爱咨询工作台；保留 `/love-app`、`/manus-app`。支持场景提示、Markdown、复制、流式状态、停止接收、移动布局和错误后恢复草稿。
- 单条消息最多 800 个字符，避免现有 GET SSE 接口的 URL/请求头过长；Enter 发送，Shift+Enter 换行，中文输入法组合输入不提交。
- “本次访问”只保存在页面内存，不把聊天写入 localStorage；刷新后列表消失。数据库中的恋爱聊天不会因此删除，新对话会生成新的 chatId。Manus 每次请求创建独立智能体，不保证多轮记忆。
- “停止接收”关闭客户端连接，不保证立即终止服务端推理或计费。连接错误后不自动重连，避免重复触发付费请求。
- Markdown 禁止原始 HTML、脚本链接和远程图片加载。SSE 文本发送器为每个 data 行加入一个协议空格，避免浏览器吞掉模型片段原有的前导空格。

前端验证：在前端目录执行 `npm test && npm run build`。后端选定回归：`mvn -Dtest=SseEmitterUtilsTest,SupabaseIntegrationTest test`。

本次浏览器检查覆盖 320/390px 窄屏与 1440px 桌面、恋爱真实流式回复、Markdown 空格、复制、停止接收、换行、新建/切换对话、断网与 800 字限制；Manus 真实终止工具调用通过。生产构建成功，JS gzip 约 232KB，仍有单块超过 500KB 的体积提示。依赖审计剩 4 项 low，来自 X Markdown / KaTeX / Mermaid 传递依赖链，当前消息使用独立 markdown-it 渲染；没有以强制降级框架隐藏审计结果。

## 通用助手普通问答修复（2026-11-10）

- 普通问题直接返回模型回答并结束，不再丢弃回复、显示“思考完成 - 无需行动”或无意义地循环至20步。页面不再为普通答案加 `Step 1` 前缀。
- 只有确实需要时才使用已注册工具；使用通用 `ToolCallingChatOptions` 关闭内置自动工具执行，并把回调传给手动执行器，避免重复执行。工具调用后继续生成面向用户的答案；终止工具附带的回答也会保留。
- 不再每步追加强制选工具的用户消息；通用助手不挂载恋爱知识库。既有邮件、搜索、终端等禁用策略不变。
- 日志顾问不再把原始用户文本重新当作模板解析，工具后续轮次的空userText和含大括号的JSON提问不会因此失败。模型异常或空回复立即停止；不循环重试、不把上游异常详情发给前端。
- 仍是每次请求独立的Manus实例，未新增跨请求对话记忆；提问请包含必要背景。SSE是按执行步骤返回，不保证逐token输出。

验证：`mvn -Dtest=GeneralAssistantTest,LoveAppRagTest,SseEmitterUtilsTest,SupabaseIntegrationTest test`。6项通用助手离线测试覆盖问候、手动工具执行、携带答案的终止、异常停止、空回答、JSON输入；加上RAG/SSE/数据库回归共11项通过。真实页面已验证“你好”、JSON解释和时间工具结果后续回答。没有运行历史全量工具测试。

## 本地与百炼知识库检索（2026-11-10）

默认链路现为「本地Markdown词项检索 → DeepSeek生成 → Supabase对话记忆」，九篇59条资料已纳入仓库。无需百炼密钥，也无需embedding API；它不是全离线聊天或语义向量检索。配置目录、修改方式及来源见[本地知识库说明](local-knowledge.md)。同步和流式恋爱聊天共用同一个RAG advisor，每次请求只检索一次。

`app.rag.enabled`控制总开关，`app.rag.provider=local|bailian`选择实现，默认local；本地资料加载失败或provider拼写错误时启动失败。以下仅为可选百炼模式配置：

在仓库外创建权限 `0600` 的 `~/.config/feng-ai-agent/bailian.env`：

```sh
export BAILIAN_API_KEY='<bailian-api-key>'
# 只填控制台 API Host 的 HTTPS 根地址，不追加 /api/v1 或 /compatible-mode/v1。
export BAILIAN_BASE_URL='https://<workspace-api-host>'
export RAG_ENABLED=true
export RAG_PROVIDER=bailian
```

[本机启动脚本](../scripts/run-local.sh)会先加载 `runtime.env`，再加载这个可选文件；可用 `FENG_ENV_FILE` / `FENG_RAG_ENV_FILE` 指定其他私密配置路径。编译后运行 `bash scripts/run-local.sh`。手工运行 Java 则需先自行加载两份环境变量。

- 仍复用 `DashScopeDocumentRetriever`，按名称「恋爱大师」定位原库。控制台新业务空间域名已验证兼容现有 SDK 的索引和检索接口；密钥及域名都不写进前端。
- 查询发送至百炼，命中文本发送至 DeepSeek；界面数据使用说明已补充第三方处理信息。连接超时10秒、读取超时20秒；错误继续传播至前端错误状态，不自动降级成“知识库回答”。无相关切片时可给一般建议，但不得编造引用。
- 最多请求5条语义候选和5条关键词候选；关闭额外的查询改写及重排调用，不切换生成模型。基础知识库及向量检索仍可能计费，以平台账单为准。
- 回复要求列出实际引用条目和已有来源URL；引用由模型生成，仍需核对，不能把引用展示当作正确率保证。
- 暂停应用检索：`bash scripts/run-local.sh --app.rag.enabled=false`。这不会删除云端库，也不保证停止云端按小时收费；停用/删除需在百炼另行确认。

离线与数据库回归：`mvn -Dtest=LoveAppRagTest,SseEmitterUtilsTest,SupabaseIntegrationTest test`。仅在已加载百炼私密变量、明确接受一次真实检索费用时运行：`RUN_BAILIAN_RETRIEVAL_TEST=true mvn -Dtest=BailianRetrievalIntegrationTest test`。该测试断言召回新增 BL04 及 NHS 原始URL，不调用 DeepSeek、不写数据库；禁止直接跑包含历史付费工具的全量旧测试。

### 知识内容更新与回滚

2026-11-10已将原来的单身、恋爱、已婚三篇重写为24条带来源的场景问答，去除课程推广和无出处成功案例。原库保留此前六篇35条专题问答，目前共9份文档、59条问答；新版三篇各解析为9个切片（含篇首说明），已分别验证SG02、DT01、MR01检索命中，实际聊天页也引用了MR01及NHS来源。

仓库中的[单身篇](../src/main/resources/knowledge/恋爱常见问题和回答%20-%20单身篇.md)、[恋爱篇](../src/main/resources/knowledge/恋爱常见问题和回答%20-%20恋爱篇.md)、[已婚篇](../src/main/resources/knowledge/恋爱常见问题和回答%20-%20已婚篇.md)已同步为新版。资料采用截至核验日可读的NHS、Gottman、love is respect科普；核验日不等于研究发表日，原创清单不应冒称临床验证方案。

云端按二级标题切分，最大分段长度2000；先验证新版解析和检索，再按精确文档ID移除旧三篇的知识索引。原数据集旧文件及仓库原稿本地备份保留，未删除整个库、未升级套餐。回滚需从原数据集重新选择对应旧文件加入原库；不应选整个类目自动同步，否则可能把已淘汰内容重新索引。新版本在云检索中生效，无需重启应用；仓库资源改动本身不会自动同步云端，也不会抹除既有聊天记录。

## 此配置的边界

- `supabase` profile 不注册邮件、搜索、终端、文件写入、下载等工具；Manus 只保留时间与终止工具。
- 外部 MCP 关闭，提供空工具集合以兼容现有注入；本地词项检索默认启用，百炼云 RAG 按上述配置显式选择，本地/PG向量库仍未启用。DeepSeek 图片理解不是 embedding 模型或图片搜索替代品。
- 原始 `rag` 根包是与子包重复的旧代码，已从组件扫描中排除；保留源文件供参考。
- 数据表 `user` 改名为 `app_user` 以避开 PostgreSQL 保留字。新 SQL 均已更新；旧 MySQL 实例需先执行 `RENAME TABLE user TO app_user`，不能直接切换代码。
- 聊天记忆按 ID 取最新 N 条，再按对话顺序返回；非正数返回空列表，最多 100 条。
- 后端与开发前端只监听 loopback，CORS 仅允许本机 3000 端口。仍不替代正式身份认证、会话授权、限流与 TLS。
- 本地运行当前使用数据库管理员连接；公开部署前应另建最小权限业务账号并启用证书校验。

## 图片与 MCP

截至 2026-11-09，[DeepSeek 图片文档](https://api-docs.deepseek.com/guides/vision/)说明 `deepseek-flash` 支持图片输入，可直接提交图片和问题，不需要额外 MCP。当前应用前端仍是文本聊天，尚未实现图片上传入口。

原仓库附带的是 Pexels 图片搜索 MCP，调用 `https://api.pexels.com/v1/search`，其 Key 仍为占位符，需要独立的 Pexels 凭据。不能把 DeepSeek API Key 填进 Pexels 配置，也不能把模型 API Key 当成图片搜索服务地址；[工具调用文档](https://api-docs.deepseek.com/guides/tool_calls/)要求应用自行执行外部工具。图片搜索和生成本次均未开通。
