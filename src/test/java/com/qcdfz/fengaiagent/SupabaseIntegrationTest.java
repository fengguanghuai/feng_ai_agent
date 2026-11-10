package com.qcdfz.fengaiagent;

import com.qcdfz.fengaiagent.chatmemory.MySQLChatMemory;
import com.qcdfz.fengaiagent.tools.EmailTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Real PostgreSQL check; rolls test rows back and never calls a paid model API. */
@SpringBootTest(properties = "app.rag.enabled=false")
@ActiveProfiles("supabase")
@EnabledIfEnvironmentVariable(named = "SUPABASE_JDBC_URL", matches = ".+")
class SupabaseIntegrationTest {
    @Autowired MySQLChatMemory memory;
    @Autowired ApplicationContext context;
    @Autowired ToolCallback[] allTools;
    @Autowired ToolCallbackProvider toolCallbackProvider;

    @Test
    @Transactional
    void storesLatestMessagesInChronologicalOrderAndClears() {
        String id = "test-" + UUID.randomUUID();
        memory.add(id, new UserMessage("first"));
        memory.add(id, new UserMessage("second"));
        memory.add(id, new UserMessage("third"));
        var messages = memory.get(id, 2);
        assertEquals(2, messages.size());
        assertEquals("second", messages.get(0).getText());
        assertEquals("third", messages.get(1).getText());
        assertTrue(memory.get(id, 0).isEmpty());
        memory.clear(id);
        assertTrue(memory.get(id, 10).isEmpty());
    }

    @Test
    void onlyDeepSeekAndSafeToolsAreEnabled() {
        assertEquals(1, context.getBeansOfType(ChatModel.class).size());
        assertEquals("OpenAiChatModel", context.getBean(ChatModel.class).getClass().getSimpleName());
        assertTrue(context.getBeansOfType(EmailTool.class).isEmpty());
        assertEquals(0, toolCallbackProvider.getToolCallbacks().length);
        assertEquals(java.util.Set.of("getCurrentTime", "calculateDaysBetween", "formatTimestamp",
                        "getDayStartEndTime", "isDateInRange", "doTerminate"),
                java.util.Arrays.stream(allTools).map(t -> t.getToolDefinition().name())
                        .collect(java.util.stream.Collectors.toSet()));
        assertFalse(context.containsBean("loveAppRagCloudAdvisor"));
    }
}
