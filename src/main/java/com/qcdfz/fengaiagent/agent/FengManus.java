package com.qcdfz.fengaiagent.agent;

import com.qcdfz.fengaiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

/**
 * @version v1.0.0
 * @belongsProject: feng-ai-agent
 * @belongsPackage: com.qcdfz.fengaiagent.agent.model
 * @author: fgh
 * @description: 最终可使用的 Manus 实例
 * @createTime: 2025-05-25 20:18
 */
@Component
public class FengManus extends ToolCallAgent{
    public FengManus(ToolCallback[] allTools,
                     ToolCallbackProvider toolCallbackProvider,
                     ChatModel dashscopeChatModel){
        super(java.util.stream.Stream.concat(java.util.Arrays.stream(allTools),
                java.util.Arrays.stream(toolCallbackProvider.getToolCallbacks())).toArray(ToolCallback[]::new));
        this.setName("fengManus");
        String SYSTEM_PROMPT = """
                你是一个通用AI助手，默认用中文自然、清楚地回答用户。
                问候、知识解释、写作、规划、代码等可直接回答的问题，不需要调用工具。
                仅在确实需要时使用已提供的工具，例如查询当前时间；不要为了结束普通回复而调用终止工具。
                工具完成后，根据真实结果回答用户，不要停在工具调用说明上。
                对没有提供的联网搜索、文件读写、终端等能力，说明限制并提供可行建议，不得假装执行。
                没有可靠依据时说明不确定；直接给出答案和必要解释，不展示内部推理过程。
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        this.setMaxSteps(20);

        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }

}
