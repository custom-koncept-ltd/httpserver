package koncept.http;

import java.io.IOException;
import java.net.InetSocketAddress;

import koncept.http.server.ConfigurableHttpServer;
import koncept.http.server.ConfigurableHttpServerProvider;
import koncept.http.server.ConfigurableHttpsServer;
import koncept.http.server.nio2.ComposableHttpNIO2Server;
import koncept.http.server.nio2.ComposableHttpsNIO2Server;

public class KonceptHttpNIO2ServerProvider extends ConfigurableHttpServerProvider {

	@Override
	public ConfigurableHttpServer createHttpServer(InetSocketAddress addr, int backlog)
			throws IOException {
		ConfigurableHttpServer server = new ComposableHttpNIO2Server();
		server.bind(addr, backlog);
		return server;
	}
	
	@Override
	public ConfigurableHttpsServer createHttpsServer(InetSocketAddress addr, int backlog)
			throws IOException {
		ConfigurableHttpsServer server = new ComposableHttpsNIO2Server();
		server.bind(addr, backlog);
		return server;
	}
	
}
