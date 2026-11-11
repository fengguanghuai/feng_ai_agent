package com.qcdfz.fengaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "RUN_LEGACY_EXTERNAL_TESTS", matches = "true")
class WebScrapingToolTest {

    @Test
    void scrapeWebPage() {
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        String webPage = webScrapingTool.scrapeWebPage("https://example.com");
        Assertions.assertNotNull(webPage);
    }
}
