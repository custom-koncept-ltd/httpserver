package koncept.http;

import java.io.IOException;
import java.net.InetSocketAddress;

import koncept.http.server.ComposableHttpServer;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.spi.HttpServerProvider;

public class KonceptHttpServerProvider extends HttpServerProvider {

	@Override
	public HttpServer createHttpServer(InetSocketAddress addr, int backlog)
			throws IOException {
		ComposableHttpServer server = new ComposableHttpServer();
		server.bind(addr, backlog);
		return server;
	}
	
	@Override
	public HttpsServer createHttpsServer(InetSocketAddress addr, int backlog)
			throws IOException {
		throw new UnsupportedOperationException();
	}
	
}
