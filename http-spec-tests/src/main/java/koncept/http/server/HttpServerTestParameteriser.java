package koncept.http.server;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import koncept.junit.runner.Parameterized;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.spi.HttpServerProvider;

@RunWith(Parameterized.class) //custom runner, for prettier test decorations
public abstract class HttpServerTestParameteriser {

	public final HttpServerProvider provider;
	public HttpServer server;
	private ExecutorService executor;
	
	@Parameters
	public static Collection<Object[]> data() {
		return KnownProviders.providers();
	}
	
	public HttpServerTestParameteriser(HttpServerProvider provider) {
		this.provider = provider;
	}
	
	/**
	 * Unfortunately, due to a bug in the java-provider http server (?), its possible for a server not to 
	 * be able to close (even though it hasn't started).
	 * 
	 * @return
	 */
	public int getUnboundPort() {
		int startPort = 8080;
		for(int i = 0; i < 999; i++) { //ugh, was hoping not to have to port scan
			if (portAvailable(startPort + i)) {
//				System.out.println("port " + (startPort + i));
				return startPort + i;
			}
		}
		throw new RuntimeException("Unable to find an open port");
	}
	
	public int getThreadPoolSize() {
		return 5;
	}
	
	
	@Before
	public void prepare() throws IOException {
		if (server != null) throw new IllegalStateException("server should be null");
		if (executor != null) throw new IllegalStateException("executor should be null");
		
		//Hmm... the server opens the port straight away - before any start() call is made (!!)
		server = provider.createHttpServer(new InetSocketAddress("localhost", getUnboundPort()), 0);
		
		executor = Executors.newFixedThreadPool(getThreadPoolSize());
		server.setExecutor(executor); //currently can't have just a single thread Executor
	}
	
	@After
	public void cleanUp() throws Exception {
		if (server == null) throw new IllegalStateException();
		server.stop(1); //seconds
		if (!executor.isShutdown() && !executor.isTerminated())
			executor.shutdownNow();
	}
	
	private boolean portAvailable(int port) {
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(port);
			ss.setReuseAddress(true);
			return true;
		} catch (IOException e) {
			// already open (??)
		} finally {
			try {
				if (ss != null) ss.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
		return false;
	}
	
	public Integer simpleUrl(String absolutePath) {
		try {
			CloseableHttpClient httpclient = HttpClients.createDefault();
			CloseableHttpResponse response = httpclient.execute(new HttpGet("http://localhost:" + server.getAddress().getPort() + absolutePath));
			return response.getStatusLine().getStatusCode();
		} catch (ClientProtocolException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			if (e.getCause() instanceof ConnectException)
				return null; //unable to connect
			throw new RuntimeException(e);
		}
	}
	
	public static class RecordingHandler implements HttpHandler {
		public final List<String> uris = Collections.synchronizedList(new ArrayList<String>());
		public volatile Map<String, List<String>> lastHeaders = null;
		public volatile URI lastUri;
		public volatile String lastRequestMethod;
		
		public void handle(HttpExchange exchange) throws IOException {
			lastHeaders = new HashMap<String, List<String>>(exchange.getRequestHeaders());
			uris.add(exchange.getRequestURI().toString());
			lastUri = exchange.getRequestURI();
			lastRequestMethod = exchange.getRequestMethod();
			
			exchange.sendResponseHeaders(200, 0);
			exchange.close(); //shouldn't this be automatic?
		}
	}
}
