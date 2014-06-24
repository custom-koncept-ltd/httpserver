package koncept.http;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.spi.HttpServerProvider;

public class TestApp implements HttpHandler {

	private final HttpServer server;
	
	public static void main(String[] args) throws Exception {
		HttpServerProvider provider = null;
		
		String providerType = args.length == 0 ? "koncept" : args[0];
		if (providerType.equals("system")) {
			provider = HttpServerProvider.provider();
		} else if (providerType.equals("koncept")) {
			provider = new KonceptHttpServerProvider();
		} else {
			throw new RuntimeException("unknown provider type:" + providerType);
		}
		
		HttpServer httpServer = provider.createHttpServer(new InetSocketAddress("localhost", 8080), 0);
		httpServer.createContext("/", new TestApp(httpServer));
		httpServer.setExecutor(Executors.newFixedThreadPool(10));
		httpServer.start();
	}

	public TestApp(HttpServer server) {
		this.server = server;
	}
	
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		URI requestUri = exchange.getRequestURI();
		System.out.println("received request: " + requestUri);
		
		exchange.sendResponseHeaders(200, 0);
		OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody());
		
		out.write("\r\nInbound URI: " + requestUri);
		out.write("\r\nInbound URI path: " + requestUri.getPath());
		out.write("\r\n");
		out.flush();
		exchange.close();
		
	}
	
	
	
	
	
	
	
	
	
}
