package configgen.mcpserver;

import com.github.codeboyzhou.mcp.declarative.McpServers;
import com.github.codeboyzhou.mcp.declarative.annotation.McpServerApplication;
import com.github.codeboyzhou.mcp.declarative.server.McpStreamableServerInfo;
import configgen.ctx.Context;
import configgen.ctx.WatchAndPostRun;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.schema.TableSchemaRefGraph;
import configgen.value.CfgValue;

import java.io.IOException;

@McpServerApplication(basePackage = "configgen.mcpserver")
public class CfgMcpServer extends GeneratorWithTag {
    private final int port;
    private final int waitSecondsAfterWatchEvt;
    private final String postRun;

    private Context context;
    private volatile CfgValue cfgValue;  // 引用可以被改变，指向不同的CfgValue
    private volatile TableSchemaRefGraph graph;


    public CfgMcpServer(Parameter parameter) {
        super(parameter);
        port = Integer.parseInt(parameter.get("port", "3456"));
        waitSecondsAfterWatchEvt = Integer.parseInt(parameter.get("watch", "0"));
        postRun = parameter.get("postrun", null);
    }

    @Override
    public void generate(Context ctx) throws IOException {
        if (waitSecondsAfterWatchEvt > 0) {
            WatchAndPostRun.INSTANCE.startWatch(context, waitSecondsAfterWatchEvt);
            WatchAndPostRun.INSTANCE.registerPostRunCallback(this::initFromCtx);
            if (postRun != null) {
                WatchAndPostRun.INSTANCE.registerPostRunBat(postRun);
            }
        }
        McpServers servers = McpServers.run(CfgMcpServer.class, null);
        servers.startStreamableServer(McpStreamableServerInfo.builder()
                .name("cfg-mcp-server")
                .version("1.0.0")
                .port(port)
                .build());
    }

    private void initFromCtx(Context newContext) {
        this.context = newContext;
        // 可以包含tag，这样更灵活，方便查看filter过后的数据
        // 此时所有的修改指令将返回错误 serverNotEditable
        cfgValue = context.makeValue(tag, true);
        graph = new TableSchemaRefGraph(cfgValue.schema());
    }

}

