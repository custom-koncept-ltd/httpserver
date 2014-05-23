package koncept.http;

import java.io.IOException;
import java.net.InetSocketAddress;

import koncept.http.sun.net.httpserver.HttpServerImpl;
import koncept.http.sun.net.httpserver.HttpsServerImpl;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.spi.HttpServerProvider;



public class LegacyModHttpServerProvider extends HttpServerProvider {
	
    public HttpServer createHttpServer (InetSocketAddress addr, int backlog) throws IOException {
        return new HttpServerImpl (addr, backlog);
    }

    public HttpsServer createHttpsServer (InetSocketAddress addr, int backlog) throws IOException {
        return new HttpsServerImpl (addr, backlog);
    }
}
