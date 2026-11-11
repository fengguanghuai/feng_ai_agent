package com.qcdfz.fengaiagent;

import com.qcdfz.fengaiagent.app.LoveApp;
import com.qcdfz.fengaiagent.chatmemory.MySQLChatMemory;
import com.qcdfz.fengaiagent.rag.advice.LoveAppRagCloudAdvisorConfig;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** No network, database or paid model calls. */
class LoveAppRagTest {
    @Test
    void bothChatPathsRetrieveExactlyOnceAndKeepOriginalQuestion() {
        var retrievals = new AtomicInteger();
        var advisor = RetrievalAugmentationAdvisor.builder().documentRetriever(query -> {
            retrievals.incrementAndGet();
            return List.of(new Document("BC02 fixture: privacy is not proof of disloyalty."));
        }).build();
        var beans = new StaticListableBeanFactory();
        beans.addBean("loveAppRagCloudAdvisor", advisor);
        var memory = mock(MySQLChatMemory.class);
        when(memory.get(anyString(), anyInt())).thenReturn(List.of());
        var model = mock(ChatModel.class);
        var response = new ChatResponse(List.of(new Generation(new AssistantMessage("Answer"))));
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            assertAugmented(invocation.getArgument(0));
            return response;
        });
        when(model.stream(any(Prompt.class))).thenAnswer(invocation -> {
            assertAugmented(invocation.getArgument(0));
            return Flux.just(response);
        });
        var app = new LoveApp(model, memory, beans.getBeanProvider(Advisor.class));
        assertEquals("Answer", app.doChat("Can I refuse sharing passwords?", "sync-test"));
        assertEquals("Answer", app.doChatWithStream("Can I refuse sharing passwords?", "stream-test").collectList().block().getFirst());
        assertEquals(2, retrievals.get());
        verify(memory).get("sync-test", 10);
        verify(memory).get("stream-test", 10);
    }

    private void assertAugmented(Prompt prompt) {
        assertTrue(prompt.getContents().contains("BC02 fixture"));
        assertTrue(prompt.getContents().contains("Can I refuse sharing passwords?"));
    }

    @Test
    void explicitSwitchControlsRagAndMissingCredentialsFailClosed() {
        var runner = new ApplicationContextRunner().withUserConfiguration(LoveAppRagCloudAdvisorConfig.class);
        runner.withPropertyValues("app.rag.enabled=false").run(c -> {
            assertNull(c.getStartupFailure());
            assertFalse(c.containsBean("loveAppRagCloudAdvisor"));
        });
        runner.withPropertyValues("app.rag.enabled=true", "app.rag.provider=bailian", "app.rag.api-key=").run(c -> assertNotNull(c.getStartupFailure()));
        runner.withPropertyValues("app.rag.enabled=true", "app.rag.provider=bailian", "app.rag.api-key=unit-test-only").run(c -> {
            assertNull(c.getStartupFailure());
            assertTrue(c.containsBean("loveAppRagCloudAdvisor"));
        });
    }
}
