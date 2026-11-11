package com.qcdfz.fengaiagent.tools;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * @version v1.0.0
 * @belongsProject: feng-ai-agent
 * @belongsPackage: com.qcdfz.fengaiagent.tools
 * @author: fgh
 * @description: 网页内容抓取，只允许公网 http/https，限制超时与响应大小
 * @createTime: 2025-05-17 11:11
 */
public class WebScrapingTool {

    private static final int TIMEOUT_MS = 15_000;
    private static final int MAX_BODY_BYTES = 2 * 1024 * 1024;
    private static final int MAX_REDIRECTS = 5;

    @Tool(description = "Scrape the content of a public http/https web page")
    public String scrapeWebPage(@ToolParam(description = "Public http/https URL of the web page to scrape") String url){
        try {
            String current = url;
            // 手动处理重定向，每一跳都重新检查是否为公网地址
            for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
                ToolSafety.requirePublicHttpUrl(current);
                Connection.Response response = Jsoup.connect(current)
                        .followRedirects(false)
                        .ignoreHttpErrors(true)
                        .timeout(TIMEOUT_MS)
                        .maxBodySize(MAX_BODY_BYTES)
                        .execute();
                int status = response.statusCode();
                if (status >= 300 && status < 400 && response.hasHeader("Location")) {
                    current = response.url().toURI().resolve(response.header("Location")).toString();
                    continue;
                }
                if (status >= 400) {
                    return "Error scraping web page: HTTP " + status;
                }
                Document document = response.parse();
                return document.html();
            }
            return "Error scraping web page: too many redirects";
        } catch (Exception e) {
            return "Error scraping web page: " + e.getMessage();
        }
    }
}
