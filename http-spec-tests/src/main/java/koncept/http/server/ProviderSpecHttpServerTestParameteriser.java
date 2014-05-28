package koncept.http.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import koncept.junit.runner.Parameterized;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import com.sun.net.httpserver.spi.HttpServerProvider;

public abstract class ProviderSpecHttpServerTestParameteriser extends HttpServerTestParameteriser {

	private ExecutorService executor;
	
	@Parameters
	public static Collection<Object[]> data() {
		return KnownProviders.providers();
	}
	
	public ProviderSpecHttpServerTestParameteriser(HttpServerProvider provider) {
		super(provider);
	}
	
	@Before
	public void prepare() throws IOException {
		if (server != null) throw new IllegalStateException("server should be null");
		if (executor != null) throw new IllegalStateException("executor should be null");
		
		//Hmm... the server opens the port straight away - before any start() call is made (!!)
		server = provider.createHttpServer(new InetSocketAddress("localhost", getUnboundPort()), 0);
		
		//these test are for compatability with the provider implementation
		if (server instanceof ConfigurableHttpServer) {
			((ConfigurableHttpServer)server).resetOptionsToJVMStandard();
		}
		
		executor = Executors.newFixedThreadPool(getThreadPoolSize());
		server.setExecutor(executor); //currently can't have just a single thread Executor
		
		server.start();
	}
	
	@After
	public void cleanUp() throws Exception {
		if (server == null) throw new IllegalStateException();
		server.stop(1); //seconds
		if (!executor.isShutdown() && !executor.isTerminated())
			executor.shutdownNow();
	}
}
