package com.qcdfz.fengaiagent.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Offline tests: no network calls, no model calls. */
class ToolSafetyTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesPlainAndNestedNamesInsideBase() {
        Path base = tempDir.toAbsolutePath().normalize();
        assertEquals(base.resolve("a.txt"), ToolSafety.resolveInside(tempDir.toString(), "a.txt"));
        assertEquals(base.resolve("sub/a.txt"), ToolSafety.resolveInside(tempDir.toString(), "sub/./a.txt"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../escape.txt", "sub/../../escape.txt", "/etc/passwd", "", "  ", "."})
    void rejectsPathsThatEscapeOrAreEmpty(String name) {
        assertThrows(IllegalArgumentException.class, () -> ToolSafety.resolveInside(tempDir.toString(), name));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "file:///etc/passwd", "ftp://example.com/a", "jar:file:/x.jar!/a", "not a url", "http://",
            "http://127.0.0.1/", "http://localhost:8123/api", "http://10.0.0.8/", "http://172.16.1.1/",
            "http://192.168.1.1/", "http://169.254.169.254/latest/meta-data/", "http://0.0.0.0/",
            "http://100.64.0.1/", "http://[::1]/", "http://[fd00::1]/", "http://[::ffff:127.0.0.1]/"
    })
    void rejectsNonHttpAndInternalUrls(String url) {
        assertThrows(IllegalArgumentException.class, () -> ToolSafety.requirePublicHttpUrl(url));
    }

    @Test
    void acceptsPublicIpLiteral() {
        // IP literal: resolved locally without DNS, so the test stays offline.
        assertDoesNotThrow(() -> ToolSafety.requirePublicHttpUrl("https://93.184.215.14/index.html"));
    }

    @Test
    void classifiesPublicAddressAsPublic() throws Exception {
        assertFalse(ToolSafety.isNonPublic(InetAddress.getByName("8.8.8.8")));
        assertFalse(ToolSafety.isNonPublic(InetAddress.getByName("2606:4700:4700::1111")));
    }

    @Test
    void fileToolReadsAndWritesOnlyInsideItsDirectory() throws Exception {
        Path secret = tempDir.resolve("secret.txt");
        Files.writeString(secret, "do-not-leak");
        FileOperationTool tool = new FileOperationTool(tempDir.resolve("file").toString());

        assertTrue(tool.writeFile("note.txt", "hello").startsWith("File written successfully"));
        assertEquals("hello", tool.readFile("note.txt"));

        String leaked = tool.readFile("../secret.txt");
        assertFalse(leaked.contains("do-not-leak"));
        assertTrue(leaked.startsWith("Error reading file"));

        assertTrue(tool.writeFile("../evil.txt", "x").startsWith("Error writing to file"));
        assertFalse(Files.exists(tempDir.resolve("evil.txt")));
    }

    @Test
    void downloadToolRejectsInternalUrlAndEscapingNameBeforeAnyRequest() {
        ResourceDownloadTool tool = new ResourceDownloadTool(tempDir.toString());
        assertTrue(tool.downloadResource("http://127.0.0.1:1/x", "x.bin").startsWith("Error downloading resource"));
        assertTrue(tool.downloadResource("https://93.184.215.14/x", "../x.bin").startsWith("Error downloading resource"));
        assertFalse(Files.exists(tempDir.getParent().resolve("x.bin")));
    }

    @Test
    void scrapingToolRejectsInternalUrl() {
        String result = new WebScrapingTool().scrapeWebPage("http://169.254.169.254/latest/meta-data/");
        assertTrue(result.startsWith("Error scraping web page"));
    }
}
