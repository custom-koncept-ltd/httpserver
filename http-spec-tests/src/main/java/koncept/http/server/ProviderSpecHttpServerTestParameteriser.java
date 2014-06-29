package koncept.http.server;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;

import com.sun.net.httpserver.spi.HttpServerProvider;

public abstract class ProviderSpecHttpServerTestParameteriser extends HttpServerTestParameteriser {

	
	@Parameters
	public static Collection<Object[]> data() {
		return KnownProviders.junitJoin(KnownProviders.providers(), KnownProviders.withHttps(null));
	}
	
	public ProviderSpecHttpServerTestParameteriser(HttpServerProvider provider, boolean https) {
		super(provider, https);
	}
		
	@Before
	public void init() throws Exception {
		initServer();
		
		//these test are for compatability with the provider implementation
		if (server instanceof ConfigurableServer) {
			((ConfigurableServer)server).resetOptionsToJVMStandard();
		}
		server.start();
	}
	
	@After
	public void clean() throws Exception {
		cleanServer();
	}
}
