package com.qcdfz.fengaiagent.rag.transform;

import com.qcdfz.fengaiagent.common.GlobalApiKeyProperties;
import com.qcdfz.fengaiagent.enums.TencentTranslateTargetLangEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "RUN_LEGACY_EXTERNAL_TESTS", matches = "true")
class TencentTranslationQueryTransformerTest {

    @Resource
    GlobalApiKeyProperties globalApiKeyProperties;
    @Test
    void transform() {
        TencentTranslationQueryTransformer build = TencentTranslationQueryTransformer.builder()
                .secretId(globalApiKeyProperties.getTencent().getSecretId())
                .secretKey(globalApiKeyProperties.getTencent().getSecretKey())
                .targetLang(TencentTranslateTargetLangEnum.EN.getValue())
                .build();
        String transform = build.transform("你好我是程序员：测试用户");
        Assertions.assertNotNull(transform);
    }
}
