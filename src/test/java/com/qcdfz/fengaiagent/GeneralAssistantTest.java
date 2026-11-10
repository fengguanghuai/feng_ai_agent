package com.qcdfz.fengaiagent;

import com.qcdfz.fengaiagent.agent.FengManus;
import com.qcdfz.fengaiagent.agent.model.AgentState;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/** All responses are mocked: no paid API calls. */
class GeneralAssistantTest {
    @Test
    void greetingIsReturnedOnceInsteadOfBeingDiscarded() {
        var model = mock(ChatModel.class);
        when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(
                List.of(new Generation(new AssistantMessage("你好！有什么我可以帮你的吗？")))));
        var agent = new FengManus(new ToolCallback[0], () -> new ToolCallback[0], model);
        agent.setMaxSteps(2);
        String result = agent.run("你好");
        assertEquals("你好！有什么我可以帮你的吗？", result);
        assertEquals(AgentState.FINISHED, agent.getState());
        verify(model, times(1)).call(any(Prompt.class));
    }

    @Test
    void toolExecutesOnceThenModelProducesFinalAnswer() {
        var model = mock(ChatModel.class);
        var clock = new ClockTool();
        var tools = org.springframework.ai.tool.ToolCallbacks.from(clock);
        var call = new AssistantMessage("", java.util.Map.of(), List.of(
                new AssistantMessage.ToolCall("call-1", "function", "clock", "{}")));
        when(model.call(any(Prompt.class)))
                .thenReturn(response(call))
                .thenReturn(response(new AssistantMessage("现在是12:34。")));
        var agent = new FengManus(tools, () -> new ToolCallback[0], model);
        agent.setMaxSteps(2);
        String result = agent.run("现在几点？");
        assertTrue(result.contains("现在是12:34。"));
        assertFalse(result.contains("Reached max steps"));
        assertEquals(1, clock.calls);
        assertEquals(AgentState.FINISHED, agent.getState());
        var prompts = org.mockito.ArgumentCaptor.forClass(Prompt.class);
        verify(model, times(2)).call(prompts.capture());
        assertFalse(org.springframework.ai.model.tool.ToolCallingChatOptions
                .isInternalToolExecutionEnabled(prompts.getAllValues().getFirst().getOptions()));
        assertTrue(prompts.getAllValues().get(1).getInstructions().stream()
                .anyMatch(m -> m instanceof org.springframework.ai.chat.messages.ToolResponseMessage));
    }

    @Test
    void answerAlongsideTerminateIsNotLost() {
        var model = mock(ChatModel.class);
        var tools = org.springframework.ai.tool.ToolCallbacks.from(new com.qcdfz.fengaiagent.tools.TerminateTool());
        when(model.call(any(Prompt.class))).thenReturn(response(new AssistantMessage("任务已完成。",
                java.util.Map.of(), List.of(new AssistantMessage.ToolCall("call-1", "function", "doTerminate", "{}")))));
        var agent = new FengManus(tools, () -> new ToolCallback[0], model);
        assertTrue(agent.run("结束任务").contains("任务已完成。"));
        assertEquals(AgentState.FINISHED, agent.getState());
        verify(model, times(1)).call(any(Prompt.class));
    }

    @Test
    void providerFailureStopsWithoutRepeatedCallsOrLeakingDetails() {
        var model = mock(ChatModel.class);
        when(model.call(any(Prompt.class))).thenThrow(new IllegalStateException("private upstream details"));
        var agent = new FengManus(new ToolCallback[0], () -> new ToolCallback[0], model);
        assertEquals("通用助手执行失败，请稍后重试。", agent.run("你好"));
        assertEquals(AgentState.ERROR, agent.getState());
        verify(model, times(1)).call(any(Prompt.class));
    }

    @Test
    void emptyReplyFailsInsteadOfLooping() {
        var model = mock(ChatModel.class);
        when(model.call(any(Prompt.class))).thenReturn(response(new AssistantMessage("")));
        var agent = new FengManus(new ToolCallback[0], () -> new ToolCallback[0], model);
        assertEquals("通用助手执行失败，请稍后重试。", agent.run("你好"));
        assertEquals(AgentState.ERROR, agent.getState());
        verify(model, times(1)).call(any(Prompt.class));
    }

    @Test
    void literalJsonDoesNotBreakDiagnosticLogging() {
        var model = mock(ChatModel.class);
        when(model.call(any(Prompt.class))).thenReturn(response(new AssistantMessage("这是一个JSON对象。")));
        var agent = new FengManus(new ToolCallback[0], () -> new ToolCallback[0], model);
        assertEquals("这是一个JSON对象。", agent.run("解释这个JSON：{\"a\": 1}"));
        verify(model, times(1)).call(any(Prompt.class));
    }

    private static ChatResponse response(AssistantMessage message) {
        return new ChatResponse(List.of(new Generation(message)));
    }

    public static class ClockTool {
        int calls;
        @org.springframework.ai.tool.annotation.Tool(description = "Test clock only")
        public String clock() {
            calls++;
            return "12:34";
        }
    }
}
