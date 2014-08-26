package koncept.http;

import java.io.IOException;
import java.net.InetSocketAddress;

import koncept.http.server.ComposableHttpIOServer;
import koncept.http.server.ComposableHttpsIOServer;
import koncept.http.server.ConfigurableHttpServer;
import koncept.http.server.ConfigurableHttpServerProvider;
import koncept.http.server.ConfigurableHttpsServer;

public class KonceptHttpIOServerProvider extends ConfigurableHttpServerProvider {

	@Override
	public ConfigurableHttpServer createHttpServer(InetSocketAddress addr, int backlog)
			throws IOException {
		ComposableHttpIOServer server = new ComposableHttpIOServer();
		server.bind(addr, backlog);
		return server;
	}
	
	@Override
	public ConfigurableHttpsServer createHttpsServer(InetSocketAddress addr, int backlog)
			throws IOException {
		ComposableHttpsIOServer server = new ComposableHttpsIOServer();
		server.bind(addr, backlog);
		return server;
	}
	
}
