package koncept.http;

import java.io.IOException;
import java.net.InetSocketAddress;

import koncept.http.server.ConfigurableHttpServer;
import koncept.http.server.ConfigurableHttpServerProvider;
import koncept.http.server.ConfigurableHttpsServer;
import koncept.http.sun.net.httpserver.HttpServerImpl;
import koncept.http.sun.net.httpserver.HttpsServerImpl;



public class LegacyModHttpServerProvider extends ConfigurableHttpServerProvider {
	
    public ConfigurableHttpServer createHttpServer (InetSocketAddress addr, int backlog) throws IOException {
        return new HttpServerImpl (addr, backlog);
    }

    public ConfigurableHttpsServer createHttpsServer (InetSocketAddress addr, int backlog) throws IOException {
        return new HttpsServerImpl (addr, backlog);
    }
}
