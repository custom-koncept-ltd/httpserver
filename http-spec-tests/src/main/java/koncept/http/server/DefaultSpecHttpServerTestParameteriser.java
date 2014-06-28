package koncept.http.server;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;

import com.sun.net.httpserver.spi.HttpServerProvider;

public abstract class DefaultSpecHttpServerTestParameteriser extends HttpServerTestParameteriser {
	
	@Parameters
	public static Collection<Object[]> data() {
		return KnownProviders.junitJoin(KnownProviders.configurableProviders());
	}
	
	public DefaultSpecHttpServerTestParameteriser(HttpServerProvider provider) {
		super(provider, false);
	}
	
	@Before
	public void init() throws Exception {
		initServer();
		//these test are for compatability with the provider implementation
		if (server instanceof ConfigurableHttpServer) {
			((ConfigurableHttpServer)server).resetOptionsToDefaults();
		} else 
			throw new IllegalArgumentException("test requires a ConfigurableHttpServer");
		
		server.start();
	}
	
	@After
	public void clean() throws Exception {
		cleanServer();
	}
}
