package com.qcdfz.fengaiagent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.qcdfz.fengaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * @version v1.0.0
 * @belongsProject: feng-ai-agent
 * @belongsPackage: com.qcdfz.fengaiagent.tools
 * @author: fgh
 * @description: 资源下载工具，只允许公网 http/https，保存到 tmp/download 目录内
 * @createTime: 2025-05-17 17:46
 */

public class ResourceDownloadTool {

    static final long MAX_BYTES = 20L * 1024 * 1024;
    private static final int TIMEOUT_MS = 15_000;

    private final String fileDir;

    public ResourceDownloadTool() {
        this(FileConstant.FILE_SAVE_DIR + "/download");
    }

    ResourceDownloadTool(String fileDir) {
        this.fileDir = fileDir;
    }

    @Tool(description = "Download a resource from a public http/https URL")
    public String downloadResource(@ToolParam(description = "Public http/https URL of the resource to download") String url,
                                   @ToolParam(description = "Relative name of the file to save the downloaded resource") String fileName) {
        try {
            Path target = ToolSafety.resolveInside(fileDir, fileName);
            ToolSafety.requirePublicHttpUrl(url);
            // 不自动跟随重定向：重定向目标可能指向内网地址，绕过上面的检查
            try (HttpResponse response = HttpUtil.createGet(url)
                    .setFollowRedirects(false)
                    .timeout(TIMEOUT_MS)
                    // executeAsync 不会预先把整个响应体读进内存，配合下面的大小上限
                    .executeAsync()) {
                if (!response.isOk()) {
                    return "Error downloading resource: HTTP " + response.getStatus();
                }
                long declared = response.contentLength();
                if (declared > MAX_BYTES) {
                    return "Error downloading resource: file is larger than " + MAX_BYTES + " bytes";
                }
                try (InputStream body = response.bodyStream()) {
                    byte[] bytes = body.readNBytes((int) MAX_BYTES + 1);
                    if (bytes.length > MAX_BYTES) {
                        return "Error downloading resource: file is larger than " + MAX_BYTES + " bytes";
                    }
                    FileUtil.writeBytes(bytes, target.toFile());
                }
            }
            return "Resource downloaded successfully to: " + target;
        } catch (Exception e) {
            return "Error downloading resource: " + e.getMessage();
        }
    }
}
