package koncept.http.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.spi.HttpServerProvider;

/**
 * provider specialization marker interface
 * @author koncept
 *
 */
public abstract class ConfigurableHttpServerProvider extends HttpServerProvider {

	@Override
	public abstract ConfigurableHttpServer createHttpServer(InetSocketAddress addr, int backlog)
			throws IOException;
	
	@Override
	public abstract ConfigurableHttpsServer createHttpsServer(InetSocketAddress addr, int backlog)
			throws IOException;

}
