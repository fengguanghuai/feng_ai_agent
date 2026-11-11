package com.qcdfz.fengaiagent;

import com.qcdfz.fengaiagent.rag.LocalKnowledgeRetriever;
import com.qcdfz.fengaiagent.rag.advice.LoveAppRagCloudAdvisorConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.rag.Query;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Local corpus and retrieval only: no database, embedding or chat calls. */
class LocalKnowledgeRetrieverTest {
    @Test
    void bundledCorpusContains59IndependentQuestionsAndRetrievesRelevantSources() throws Exception {
        var retriever = new LocalKnowledgeRetriever(new PathMatchingResourcePatternResolver(), "classpath:knowledge/*.md", 5);
        assertEquals(59, retriever.documentCount());
        var cases = Map.of(
                "社交圈很小又怕尴尬，怎样参加兴趣活动？", "SG02",
                "分享开心的好消息，对方只回嗯，我想被认真听见", "DT01",
                "家务总要我安排提醒，对方说你叫我我就做，我不想一直兜底", "MR01",
                "伴侣要求手机密码和全天定位，我可以拒绝吗", "BC02",
                "分手后每天喝酒麻痹自己有帮助吗", "BL04");
        for (var test : cases.entrySet()) {
            var results = retriever.retrieve(new Query(test.getKey()));
            assertFalse(results.isEmpty(), test.getValue());
            assertTrue(results.size() <= 5);
            assertTrue(results.getFirst().getText().contains(test.getValue()),
                    () -> "Expected " + test.getValue() + " first; got " + results.getFirst().getMetadata().get("heading"));
            assertTrue(results.getFirst().getText().contains("https://"));
            assertFalse(results.getFirst().getText().contains("codefather.cn"));
        }
        assertTrue(retriever.retrieve(new Query("Kubernetes ingress TLS")).isEmpty());
    }

    @Test
    void supportsExternalDirectoryWithoutUploadingDocuments(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("custom.md"), "\uFEFF# 自定义知识\r\n\r\n## FX01｜如何分配家庭预算？\r\n一起讨论收入、预算和个人自主支出。https://example.org/reference\r\n");
        var retriever = new LocalKnowledgeRetriever(new PathMatchingResourcePatternResolver(), directory.toUri() + "*.md", 1);
        assertEquals(1, retriever.documentCount());
        assertTrue(retriever.retrieve(new Query("家庭预算分配")).getFirst().getText().contains("FX01"));
        assertTrue(retriever.retrieve(new Query("预算")).getFirst().getText().contains("FX01"));
    }

    @Test
    void invalidLocalConfigurationFailsInsteadOfPretendingKnowledgeLoaded(@TempDir Path directory) {
        var resolver = new PathMatchingResourcePatternResolver();
        assertThrows(IllegalArgumentException.class, () -> new LocalKnowledgeRetriever(resolver, directory.toUri() + "*.md", 5));
        assertThrows(IllegalArgumentException.class, () -> new LocalKnowledgeRetriever(resolver, "https://example.org/*.md", 5));
        assertThrows(IllegalArgumentException.class, () -> new LocalKnowledgeRetriever(resolver, "classpath:knowledge/*.md", 0));
    }

    @Test
    void localModeNeedsNoCloudKeyAndRejectsUnknownProvider() {
        var runner = new ApplicationContextRunner().withUserConfiguration(LoveAppRagCloudAdvisorConfig.class);
        runner.withPropertyValues("app.rag.enabled=true", "app.rag.provider=local", "app.rag.api-key=").run(c -> {
            assertNull(c.getStartupFailure());
            assertInstanceOf(LocalKnowledgeRetriever.class, c.getBean("loveAppDocumentRetriever"));
            assertTrue(c.containsBean("loveAppRagCloudAdvisor"));
        });
        runner.withPropertyValues("app.rag.enabled=true", "app.rag.provider=typo").run(c -> assertNotNull(c.getStartupFailure()));
    }
}
