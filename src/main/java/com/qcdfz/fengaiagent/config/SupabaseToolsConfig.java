package com.qcdfz.fengaiagent.config;

import com.qcdfz.fengaiagent.tools.TerminateTool;
import com.qcdfz.fengaiagent.tools.TimeTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Minimal local setup: no email, search, shell, file writes, or external MCP servers. */
@Configuration
@Profile("supabase")
public class SupabaseToolsConfig {
    @Bean
    public ToolCallback[] allTools() {
        return ToolCallbacks.from(new TimeTool(), new TerminateTool());
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider() {
        return () -> new ToolCallback[0];
    }
}
