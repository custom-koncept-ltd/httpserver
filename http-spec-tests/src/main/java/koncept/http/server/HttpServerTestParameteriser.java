package koncept.http.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLContext;

import koncept.http.jce.SecurityUtil;
import koncept.junit.runner.Parameterized;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.runner.RunWith;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.spi.HttpServerProvider;

@RunWith(Parameterized.class) //custom runner, for prettier test decorations
public abstract class HttpServerTestParameteriser {

	private static final int startPort = 9000;;
	private static final AtomicLong portOffset = new AtomicLong(0);
	
	public final HttpServerProvider provider;
	public final boolean https;
	private static SSLContext sslContext;
	
	public HttpServer server;
	private ExecutorService executor;
	
	public HttpServerTestParameteriser(HttpServerProvider provider, boolean https) {
		this.provider = provider;
		this.https = https;
	}
	
	public void initServer() throws Exception {
		if (server != null) throw new IllegalStateException("server should be null");
		
		if (isHttps()) {
			HttpsServer server = provider.createHttpsServer(new InetSocketAddress("localhost", getUnboundPort()), 0);
			server.setHttpsConfigurator(new HttpsConfigurator(getSslContext()));
			this.server = server;
		} else {
			server = provider.createHttpServer(new InetSocketAddress("localhost", getUnboundPort()), 0);
		}
		executor = Executors.newFixedThreadPool(getThreadPoolSize());
		server.setExecutor(executor); //currently can't have just a single thread Executor	
	}
	
	public void cleanServer() {
		if (server == null) throw new IllegalStateException();
		server.stop(3);
		if (!executor.isShutdown())
			executor.shutdownNow();
		executor = null;
	}
	
	public int getUnboundPort() {
		int offset = (int)portOffset.getAndIncrement();
		for(int i = 0; i < 999; i++) { //ugh, was hoping not to have to port scan
			int port = (offset + i) % 1000;
			port += startPort;;
			if (portAvailable(port)) {
				return port;
			}
		}
		throw new RuntimeException("Unable to find an open port");
	}
	
	public int getThreadPoolSize() {
		return 5;
	}
	
	public boolean isHttps() {
		return https;
	}
	
	/**
	 * This is statically cached because its *very* expensive to make
	 * new SSLContexts all the time; </br>
	 * @return an sslcontext if isHttps() is true
	 */
	public SSLContext getSslContext() {
		if (!isHttps())
			return null;
		if (sslContext == null) try {
			sslContext = SecurityUtil.makeSSLContext();
		} catch (Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e;
			throw new RuntimeException(e);
		}
		return sslContext;
	}
	
	public String getProtocol() {
		return isHttps() ? "https://" : "http://";
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
	
	/**
	 * since Keep-Alive isn't (yet) supported, just create a new
	 * http clientent whenever
	 * @return
	 */
	public HttpClient httpClient() {
		return HttpClientBuilder.create()
				.setSslcontext(sslContext)
				.build();
	}
	
	public Socket openDirectSocket() throws IOException {
		if (isHttps()) {
			return getSslContext().getSocketFactory().createSocket("localhost", server.getAddress().getPort());
		} else {
			return new Socket("localhost", server.getAddress().getPort());
		}
	}
	
	public Integer simpleUrl(String absolutePath) {
		try {
			HttpResponse response = httpClient().execute(new HttpGet(getProtocol() +"localhost:" + server.getAddress().getPort() + absolutePath));
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
			
			try {
				//drain the request (!!)
				//even though there may be 0bytes, this seems to be required (!!)
				InputStream is = exchange.getRequestBody();
				while (is.available() > 0) {
					int read = is.read(); //uh... otherwise we just time out... and with NO timeout set... we hang(!!)
					while(read != -1) {
						read = is.read();
					}
				}
			} catch (SocketTimeoutException e) {
				//nop
			}
			
			exchange.sendResponseHeaders(Code.HTTP_OK, -1);
		}
	}
	
	public static class RecordingFilter extends Filter {
		public final List<String> uris = Collections.synchronizedList(new ArrayList<String>());
		public volatile URI lastUri;
		@Override
		public String description() {
			return "RecordingFilter";
		}
		@Override
		public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
			uris.add(exchange.getRequestURI().toString());
			lastUri = exchange.getRequestURI();
			chain.doFilter(exchange);
		}
	}
}
