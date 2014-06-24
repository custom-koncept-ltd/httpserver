package koncept.http;

import java.io.IOException;
import java.net.InetSocketAddress;

import koncept.http.server.ComposableHttpServer;
import koncept.http.server.ComposableHttpsServer;
import koncept.http.server.ConfigurableHttpServer;
import koncept.http.server.ConfigurableHttpServerProvider;
import koncept.http.server.ConfigurableHttpsServer;

public class KonceptHttpServerProvider extends ConfigurableHttpServerProvider {

	@Override
	public ConfigurableHttpServer createHttpServer(InetSocketAddress addr, int backlog)
			throws IOException {
		ComposableHttpServer server = new ComposableHttpServer();
		server.bind(addr, backlog);
		return server;
	}
	
	@Override
	public ConfigurableHttpsServer createHttpsServer(InetSocketAddress addr, int backlog)
			throws IOException {
		ComposableHttpsServer server = new ComposableHttpsServer();
		server.bind(addr, backlog);
		return server;
	}
	
}
