package com.qcdfz.fengaiagent.rag.advice;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version v1.0.0
 * @belongsProject: feng-ai-agent
 * @belongsPackage: com.qcdfz.fengaiagent.rag
 * @author: fgh
 * @description: 基于阿里云知识库的 RAG 增强生成
 * @createTime: 2025-05-05 08:04
 */
@Configuration
@ConditionalOnProperty(name = "app.rag.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class LoveAppRagCloudAdvisorConfig {
    // Keep the existing bean names so both chat entry points use exactly one advisor.
    @Bean("loveAppDocumentRetriever")
    public DocumentRetriever configuredLoveAppDocumentRetriever(
            @Value("${app.rag.provider:local}") String provider,
            @Value("${app.rag.local.location:classpath:knowledge/*.md}") String location,
            @Value("${app.rag.local.top-k:5}") int topK,
            org.springframework.core.io.support.ResourcePatternResolver resolver,
            @Value("${app.rag.api-key:${spring.ai.dashscope.api-key:}}") String apiKey,
            @Value("${app.rag.base-url:https://dashscope.aliyuncs.com}") String baseUrl,
            @Value("${app.rag.index-name:恋爱大师}") String indexName) throws java.io.IOException {
        if ("local".equals(provider)) {
            var retriever = new com.qcdfz.fengaiagent.rag.LocalKnowledgeRetriever(resolver, location, topK);
            log.info("Local knowledge loaded: {} question sections; no embedding API", retriever.documentCount());
            return retriever;
        }
        if ("bailian".equals(provider)) return loveAppDocumentRetriever(apiKey, baseUrl, indexName);
        throw new IllegalArgumentException("app.rag.provider must be local or bailian");
    }

    public DocumentRetriever loveAppDocumentRetriever(String apiKey, String baseUrl, String indexName) {
        org.springframework.util.Assert.hasText(apiKey, "BAILIAN_API_KEY is required for Bailian RAG");
        var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(java.time.Duration.ofSeconds(10));
        requestFactory.setReadTimeout(java.time.Duration.ofSeconds(20));
        var api = new DashScopeApi(baseUrl, apiKey,
                org.springframework.web.client.RestClient.builder().requestFactory(requestFactory),
                org.springframework.web.reactive.function.client.WebClient.builder(),
                org.springframework.ai.retry.RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
        var retriever = new DashScopeDocumentRetriever(api,
                DashScopeDocumentRetrieverOptions.builder()
                        .withIndexName(indexName)
                        .withDenseSimilarityTopK(5)
                        .withSparseSimilarityTopK(5)
                        .withEnableRewrite(false)
                        .withEnableReranking(false)
                        .build());
        return query -> {
            // Do not log private queries or document content; failures must not masquerade as successful RAG.
            var documents = retriever.retrieve(query);
            log.info("Bailian retrieval completed: {} chunks", documents.size());
            return documents;
        };
    }

    @Bean
    public Advisor loveAppRagCloudAdvisor(DocumentRetriever loveAppDocumentRetriever) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(loveAppDocumentRetriever)
                .queryAugmenter(org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter
                        .builder().allowEmptyContext(true).build())
                .build();
    }
}
