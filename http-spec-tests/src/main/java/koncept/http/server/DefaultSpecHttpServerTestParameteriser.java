package koncept.http.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;

import com.sun.net.httpserver.spi.HttpServerProvider;

public abstract class DefaultSpecHttpServerTestParameteriser extends HttpServerTestParameteriser {
	
	@Parameters
	public static Collection<Object[]> data() {
		return KnownProviders.configurableProviders();
	}
	
	public DefaultSpecHttpServerTestParameteriser(HttpServerProvider provider) {
		super(provider);
	}
	
	@Before
	public void prepare() throws IOException {
		if (server != null) throw new IllegalStateException("server should be null");
		
		//Hmm... the server opens the port straight away - before any start() call is made (!!)
		server = provider.createHttpServer(new InetSocketAddress("localhost", getUnboundPort()), 0);
		
		//these test are for compatability with the provider implementation
		if (server instanceof ConfigurableHttpServer) {
			((ConfigurableHttpServer)server).resetOptionsToDefaults();
		} else 
			throw new IllegalArgumentException("test requires a ConfigurableHttpServer");
		
		server.setExecutor(Executors.newFixedThreadPool(getThreadPoolSize()));
		server.start();
	}
	
	@After
	public void cleanUp() throws Exception {
		if (server == null) throw new IllegalStateException();
		server.stop(1); //seconds
	}
}
