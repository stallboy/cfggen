package configgen.tool;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.SimpleFileServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;

public class CfgEditorServer {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 10);

        var handler = SimpleFileServer.createFileHandler(Path.of(".").toAbsolutePath());
        HttpContext context = server.createContext("/", handler);
        context.setAuthenticator(new BasicAuthenticator("fs") {
            @Override
            public boolean checkCredentials(String username, String password) {
                return username.equals("1");
            }
        });

        server.start();
    }
}
