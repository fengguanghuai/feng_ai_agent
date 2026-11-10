package com.qcdfz.fengaiagent;

import com.qcdfz.fengaiagent.rag.advice.LoveAppRagCloudAdvisorConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.rag.Query;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: one real, billable retrieval; no DeepSeek generation or database writes. */
@EnabledIfEnvironmentVariable(named = "RUN_BAILIAN_RETRIEVAL_TEST", matches = "true")
class BailianRetrievalIntegrationTest {
    @Test
    void retrievesNewKnowledgeWithOriginalSourceUrl() {
        var retriever = new LoveAppRagCloudAdvisorConfig().loveAppDocumentRetriever(
                System.getenv("BAILIAN_API_KEY"), System.getenv("BAILIAN_BASE_URL"), "恋爱大师");
        var documents = retriever.retrieve(new Query("分手后想每天喝酒麻痹自己，这样有帮助吗？有哪些替代方法？"));
        assertFalse(documents.isEmpty());
        assertTrue(documents.stream().anyMatch(d -> d.getText().contains("BL04")
                && d.getText().contains("https://www.nhs.uk/")), "Expected actual uploaded BL04 and its source");
    }
}
