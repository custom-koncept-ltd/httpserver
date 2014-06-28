package koncept.http;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;

import koncept.http.jce.SecurityUtil;
import koncept.http.server.ComposableHttpServer;
import koncept.http.server.ComposableHttpsServer;
import koncept.http.server.ConfigurableServer;
import koncept.http.server.ConfigurationOption;
import koncept.sp.pipe.ProcPipe;
import koncept.sp.tracker.JobTracker;
import sun.net.httpserver.DefaultHttpServerProvider;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.spi.HttpServerProvider;

public class TestApp {

	private final HttpServer server;

	public static void main(String[] args) throws Exception {
		HttpServerProvider provider = null;

		String providerType = args.length > 0 ? args[0] : "koncept";
		if (providerType.equals("system")) {
			provider = HttpServerProvider.provider(); //will probably pick up KonceptHttpServerProvider from the provider hook
		} else if (providerType.equals("koncept")) {
			provider = new KonceptHttpServerProvider();
		} else if (providerType.equals("default")){
			provider = new DefaultHttpServerProvider();
		} else {
			throw new RuntimeException("unknown provider type:" + providerType);
		}

		boolean https = args.length > 1 ? Boolean.parseBoolean(args[1]) : true;
		HttpServer httpServer;
		if (https) {
			SSLContext  sslContext = SecurityUtil.makeSSLContext();
			HttpsServer httpsServer = provider.createHttpsServer(
					new InetSocketAddress("localhost", 8080), 0);
			
			httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
			httpServer = httpsServer;
		} else {
			httpServer = provider.createHttpServer(new InetSocketAddress(
					"localhost", 8080), 0);
		}

		httpServer.setExecutor(Executors.newFixedThreadPool(10));
		new TestApp(httpServer).start();
	}

	public TestApp(HttpServer server) {
		this.server = server;

		server.createContext("/", new RootHttpHandler());
		server.createContext("/stop", new StopHandler());
		server.createContext("/serverDetails", new ServerDetails());
		if (serverIsConfigurable())
			server.createContext("/configurationDetails", new ConfigurationDetails());
		if (serverIsComposable())
			server.createContext("/compositionDetails", new ComposableDetails());
		
	}

	public void start() {
		server.start();
	}

	class RootHttpHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			URI requestUri = exchange.getRequestURI();
			System.out.println("received request: " + requestUri);

			exchange.sendResponseHeaders(200, 0);
			OutputStreamWriter out = new OutputStreamWriter(
					exchange.getResponseBody());

			out.write("\r\nInbound URI: " + requestUri);
			out.write("\r\nInbound URI path: " + requestUri.getPath());
			out.write("\r\n");
			
			out.write("\r\nAvailable:");
			out.write("\r\n/");
			out.write("\r\n/stop");
			out.write("\r\n/serverDetails");
			if (serverIsConfigurable())
				out.write("\r\n/configurationDetails");
			if (serverIsComposable())
				out.write("\r\n/compositionDetails");
			
			out.flush();
			exchange.close();

		}
	}
	
	private boolean serverIsConfigurable() {
		return ConfigurableServer.class.isAssignableFrom(server.getClass());
	}
	
	private boolean serverIsComposable() {
		return server instanceof ComposableHttpServer || server instanceof ComposableHttpsServer;
	}
	
	class StopHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			server.stop(0);
			exchange.sendResponseHeaders(200, 0);
			OutputStreamWriter out = new OutputStreamWriter(
					exchange.getResponseBody());

			out.write("server stopped");
			out.flush();
			exchange.close();
		}
	}
	
	class ServerDetails implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			exchange.sendResponseHeaders(200, 0);
			OutputStreamWriter out = new OutputStreamWriter(
					exchange.getResponseBody());

			out.write("\r\nServer Name: " + server.getClass().getName());
			out.write("\r\nServer Address: " + server.getAddress());
			out.write("\r\nServer is configurable? " + serverIsConfigurable());
			out.write("\r\nServer is a composable http server? " + serverIsComposable());
			out.flush();
			exchange.close();	
		}
	}
	
	class ConfigurationDetails implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			OutputStreamWriter out = new OutputStreamWriter(
					exchange.getResponseBody());

			if (!serverIsConfigurable()) {
				exchange.sendResponseHeaders(500, 0);
				out.write("\r\nServer is not configurable");
			} else {
				exchange.sendResponseHeaders(200, 0);
				out.write("\r\nOptions:");
				out.write("\r\n");
				ConfigurableServer cServer = (ConfigurableServer)server;
				Map<ConfigurationOption, String> options = cServer.options();
				for(ConfigurationOption option: options.keySet()) {
					out.write("\r\n" + option.key() + "=" + options.get(option));
				}
			}
			
			out.flush();
			exchange.close();	
		}
	}
	
	class ComposableDetails implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			OutputStreamWriter out = new OutputStreamWriter(
					exchange.getResponseBody());

			if (!serverIsComposable()) {
				exchange.sendResponseHeaders(500, 0);
				out.write("\r\nServer is not composable");
			} else {
				exchange.sendResponseHeaders(200, 0);
				
				ComposableHttpServer composableServer;
				if (server instanceof ComposableHttpServer)
					composableServer = (ComposableHttpServer)server;
				else{ // if (server instanceof ComposableHttpsServer) {
					composableServer = ((ComposableHttpsServer)server).getWrapped();
				}
				ProcPipe processor = composableServer.getProcessor();
				JobTracker tracker = processor.tracker();
				
				out.write("\r\nProcessor: " + processor.getClass());
				out.write("\r\nTracker: " + tracker.getClass());
				out.write("\r\nQueue size: " + tracker.queued().size());
				out.write("\r\nLive size: " + tracker.live().size());
			}
			
			out.flush();
			exchange.close();	
		}
	}



}
