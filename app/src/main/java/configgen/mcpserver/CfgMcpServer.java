package configgen.mcpserver;

import com.github.codeboyzhou.mcp.declarative.McpServers;
import com.github.codeboyzhou.mcp.declarative.annotation.McpServerApplication;
import com.github.codeboyzhou.mcp.declarative.server.McpStreamableServerInfo;
import configgen.ctx.Context;
import configgen.ctx.WatchAndPostRun;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.util.Logger;
import configgen.value.CfgValue;

import java.io.IOException;

@McpServerApplication(basePackage = "configgen.mcpserver")
public class CfgMcpServer extends GeneratorWithTag {
    private static volatile CfgMcpServer INSTANCE = null;

    public record CfgValueWithContext(CfgValue cfgValue,
                                      Context context) {
    }

    private final int port;
    private final int waitSecondsAfterWatchEvt;
    private final String postRun;
    private volatile CfgValueWithContext cfgValueWithContext;

    public static CfgMcpServer getInstance() {
        return INSTANCE;
    }

    public CfgMcpServer(Parameter parameter) {
        super(parameter);
        port = Integer.parseInt(parameter.get("port", "3457"));
        waitSecondsAfterWatchEvt = Integer.parseInt(parameter.get("watch", "0"));
        postRun = parameter.get("postrun", null);
    }

    @Override
    public void generate(Context ctx) throws IOException {
        if (INSTANCE != null) {
            Logger.log("CfgMcpServer instance already exists! ignore this");
            return;
        }

        initFromCtx(ctx);
        INSTANCE = this;
        if (waitSecondsAfterWatchEvt > 0) {
            WatchAndPostRun.INSTANCE.startWatch(ctx, waitSecondsAfterWatchEvt);
            WatchAndPostRun.INSTANCE.registerPostRunCallback(this::reloadCfgValue);
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
        // 可以包含tag，这样更灵活，方便查看filter过后的数据
        // 此时所有的修改指令将返回错误 serverNotEditable
        CfgValue cfgValue = newContext.makeValue(tag, true);
        cfgValueWithContext = new CfgValueWithContext(cfgValue, newContext);
    }

    private void reloadCfgValue(Context newContext) {
        initFromCtx(newContext);
        Logger.log("reload value ok");
    }

    public CfgValueWithContext cfgValueWithContext() {
        return cfgValueWithContext;
    }

    public void updateCfgValue(CfgValue newCfgValue) {
        CfgValueWithContext old = cfgValueWithContext;
        cfgValueWithContext = new CfgValueWithContext(newCfgValue, old.context());
    }
}

