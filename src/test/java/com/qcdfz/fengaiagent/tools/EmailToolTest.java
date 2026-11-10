package com.qcdfz.fengaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// Legacy demos can call paid models or send email; never run implicitly.
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "RUN_LEGACY_EXTERNAL_TESTS", matches = "true")
@SpringBootTest
class EmailToolTest {

    @Autowired
    private EmailTool emailTool;

    @Test
    void sendSimpleEmail() {
        String to = "recipient1@example.invalid";
        String subject = "测试邮件";
        String content = "这是一封测试邮件";
        String result = emailTool.sendSimpleEmail(to, subject, content);
        assertNotNull(result);
    }

    @Test
    void sendHtmlEmail() {
        String to = "recipient2@example.invalid";
        String subject = "HTML测试邮件";
        String htmlContent = "<h1>这是一封HTML测试邮件</h1><p>Hello World!</p>";
        String result = emailTool.sendHtmlEmail(to, subject, htmlContent);
        assertNotNull(result);
    }

    @Test
    void sendBatchEmails() {
        List<String> toList = Arrays.asList("recipient3@example.invalid","recipient4@example.invalid");
        String subject = "批量测试邮件";
        String content = "这是一封批量测试邮件";
        String result = emailTool.sendBatchEmails(toList, subject, content);
        assertNotNull(result);
    }
}
