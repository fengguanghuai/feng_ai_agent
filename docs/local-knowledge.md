# 本地知识库

## 默认使用

`supabase` 配置默认启用本地检索，资料在[知识目录](../src/main/resources/knowledge/)，随应用打包。启动仍按[本机配置](supabase-deepseek.md)准备DeepSeek和Supabase，无需百炼、embedding模型或向量数据库。

```sh
mvn -Dtest=LocalKnowledgeRetrieverTest,LoveAppRagTest,GeneralAssistantTest,SseEmitterUtilsTest test
mvn -Dmaven.test.skip=true package
bash scripts/run-local.sh --app.rag.provider=local
```

默认读取 `classpath:knowledge/*.md`，按二级标题拆成独立问答，使用中文字符/双字词项和英文词项进行BM25词项匹配，每次最多返回5条。它是本地关键词检索，不是语义向量模型：同义改写、跨语言或很含糊的问题可能漏召回；没有相关资料时返回空结果，不编造检索成功。

资料在启动时加载，之后只读检索；修改打包资料需要重新构建和重启。通用助手不使用这份恋爱知识库；同步和流式恋爱聊天共用一次检索。

## 使用仓库外的自定义目录

```sh
bash scripts/run-local.sh \
  --app.rag.provider=local \
  '--app.rag.local.location=file:/absolute/path/to/knowledge/*.md' \
  --app.rag.local.top-k=5
```

这里的自定义目录替代默认资料，不与内置目录自动合并。对应环境变量为 `RAG_PROVIDER`、`RAG_LOCAL_LOCATION`、`RAG_LOCAL_TOP_K`；总开关为 `RAG_ENABLED`，兼容旧 `BAILIAN_RAG_ENABLED`，前者优先。命令行参数优先于环境变量。

只允许 `classpath:` / `classpath*:` / `file:`；不下载HTTP远程文档。top-k范围1～10。目录为空、文件读不了、缺少标题或问答正文时启动失败，不静默退回纯模型或百炼。

文件为UTF-8，可带BOM或使用CRLF。格式如下：

```md
# 文档名称
篇首范围、来源及核验日期说明。

## ID01｜一个明确问题？
直接回答、适用边界、具体例子。
来源：https://example.org/reference

## ID02｜另一个问题？
对应回答及来源。
```

篇首说明不单独进入检索；每条正文必须自足，带原有来源及必要安全边界，不依赖前一个问答。条目编号应保持唯一；来源不能只写在篇首。不要将密钥、真实聊天、私人联系方式或原稿备份混入被匹配目录。

## 切回百炼或关闭

```sh
# 需事先在仓库外配置百炼密钥和API Host
bash scripts/run-local.sh --app.rag.provider=bailian --app.rag.enabled=true

# 完全关闭知识检索
bash scripts/run-local.sh --app.rag.enabled=false
```

只有 `bailian` 分支创建云检索客户端；缺少其密钥时启动失败。拼错provider也启动失败。本地模式不会失败后偷偷调用百炼。切换模式需要重启，原百炼知识库不会被删除、停用或自动同步，基础套餐收费也不会因本地切换自动停止。

## 纳入资料

当前共9篇、59条问答，与已核验的云端有效正文一致：

| 文件 | 范围 |
|---|---|
| [温和沟通与冲突修复](<../src/main/resources/knowledge/01-温和沟通与冲突修复.md>) | 沟通、暂停争执、修复 |
| [个人边界与持续同意](<../src/main/resources/knowledge/02-个人边界与持续同意.md>) | 隐私、边界、持续同意 |
| [关系风险与安全优先](<../src/main/resources/knowledge/03-关系风险与安全优先.md>) | 威胁、控制、暴力及求助 |
| [线上交友与初次约会](<../src/main/resources/knowledge/04-线上交友与初次约会.md>) | 初次见面、隐私及骗局防范 |
| [分手失落与自我照顾](<../src/main/resources/knowledge/05-分手失落与自我照顾.md>) | 失落、联系边界及支持 |
| [关系评估与共同约定](<../src/main/resources/knowledge/06-关系评估与共同约定.md>) | 关系需求与共同决策 |
| [单身篇](<../src/main/resources/knowledge/恋爱常见问题和回答 - 单身篇.md>) | 自我定位、社交及主动交往 |
| [恋爱篇](<../src/main/resources/knowledge/恋爱常见问题和回答 - 恋爱篇.md>) | 日常回应、现实协商与信任 |
| [已婚篇](<../src/main/resources/knowledge/恋爱常见问题和回答 - 已婚篇.md>) | 家务全流程、预算及育儿支持 |

2026-11-10核验，正文保留NHS、Gottman、love is respect等来源URL、编辑练习与专业资料的区别。核验日期不是论文发表日期，不保证每条建议适用于所有人，也不能替代专业帮助。后续编辑以这个目录为准；原来的候选人资料示例不属于本知识库，不被默认检索。

## 数据与隐私边界

- **本地检索不等于全离线**：问题、历史上下文及命中片段仍发给DeepSeek生成回答；恋爱聊天仍存入Supabase。
- 本地模式不向百炼发送查询，也不使用embedding API；不会在浏览器读取本地文件。配置目录是服务端受信配置，不能由网页用户指定。
- 前端不存储服务端密钥；本次访问列表刷新清空，不等于删除数据库中的记录。
- 现有鉴权、权限隔离和限流尚未达到公网部署条件，继续只监听本机。
- Git忽略文件不能清除已提交历史。提交前检查当前改动与历史；作者姓名/邮箱、公开链接、截图和构建产物需分别审查，不能将“未命中密钥”解释为彻底匿名。
