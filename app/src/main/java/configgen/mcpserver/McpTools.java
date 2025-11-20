package configgen.mcpserver;

import com.github.codeboyzhou.mcp.declarative.annotation.McpTool;
import com.github.codeboyzhou.mcp.declarative.annotation.McpToolParam;

public class McpTools {

    @McpTool(description = "read table schema")
    public String readTableSchema(@McpToolParam(name = "table", description = "table full name", required = true)
                                  String tableFullName) {
        return "test table schema result";
    }

}