package com.qcdfz.fengaiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.qcdfz.fengaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.nio.file.Path;

/**
 * @version v1.0.0
 * @belongsProject: feng-ai-agent
 * @belongsPackage: com.qcdfz.fengaiagent.tools
 * @author: fgh
 * @description: 文件操作工具类，读写范围限制在 tmp/file 目录内
 * @createTime: 2025-05-16 23:03
 */

public class FileOperationTool {

    private final String fileDir;

    public FileOperationTool() {
        this(FileConstant.FILE_SAVE_DIR + "/file");
    }

    FileOperationTool(String fileDir) {
        this.fileDir = fileDir;
    }

    @Tool(description = "Read content from a file in the agent workspace directory")
    public String readFile(@ToolParam(description = "Relative name of the file to read") String fileName){
        try {
            Path path = ToolSafety.resolveInside(fileDir, fileName);
            return FileUtil.readUtf8String(path.toFile());
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(description = "Write content to a file in the agent workspace directory")
    public String writeFile(@ToolParam(description = "Relative name of the file to write") String fileName,
                            @ToolParam(description = "Content to write to the file") String content){
        try {
            Path path = ToolSafety.resolveInside(fileDir, fileName);
            FileUtil.writeUtf8String(content, path.toFile());
            return "File written successfully to: " + path;
        } catch (Exception e) {
            return "Error writing to file: " + e.getMessage();
        }
    }
}
