package koncept.http.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

public class ComposableHttpsIOServer extends ConfigurableHttpsServer {

	private HttpsConfigurator configurator = null;
	
	private ComposableHttpServerWrapper wrapped;
	
	public ComposableHttpsIOServer() {
		wrapped = new ComposableHttpServerWrapper();
	}
	
	public HttpsServer getHttpsServer() {
		return this;
	}
	
	public ComposableHttpIOServer getWrapped() {
		return wrapped;
	}
	
	@Override
	public void setHttpsConfigurator(HttpsConfigurator configurator) {
		if (configurator == null) {
            throw new NullPointerException ("null HttpsConfigurator");
        }
//        if (started) {
//            throw new IllegalStateException ("server already started");
//        }
        this.configurator = configurator;
	}
	
	@Override
	public HttpsConfigurator getHttpsConfigurator() {
		return configurator;
	}
	
	private class ComposableHttpServerWrapper extends ComposableHttpIOServer {
		@Override
		public HttpServer getHttpServer() {
			return getHttpsServer();
		}
		
		@Override
		public ServerSocket openSocket(InetSocketAddress addr, int backlog)
				throws IOException {
			SSLServerSocketFactory ssf = (SSLServerSocketFactory)configurator.getSSLContext().getServerSocketFactory();
			SSLServerSocket ss = (SSLServerSocket)ssf.createServerSocket(addr.getPort(), backlog, addr.getAddress());
			ss.setEnabledCipherSuites(ssf.getSupportedCipherSuites());
			return ss;
		}
		
	}
	
	@Override
	public void resetOptionsToJVMStandard() {
		wrapped.resetOptionsToJVMStandard();
	}

	@Override
	public void resetOptionsToDefaults() {
		wrapped.resetOptionsToDefaults();
	}

	@Override
	public Map<ConfigurationOption, String> options() {
		return wrapped.options();
	}
	
	@Override
	public void bind(InetSocketAddress addr, int backlog) throws IOException {
		wrapped.bind(addr, backlog);
	}

	@Override
	public void start() {
		wrapped.start();
	}

	@Override
	public void setExecutor(Executor executor) {
		wrapped.setExecutor(executor);
	}

	@Override
	public Executor getExecutor() {
		return wrapped.getExecutor();
	}

	@Override
	public void stop(int secondsDelay) {
		wrapped.stop(secondsDelay);
	}

	@Override
	public HttpContext createContext(String path, HttpHandler handler) {
		return wrapped.createContext(path, handler);
	}

	@Override
	public HttpContext createContext(String path) {
		return wrapped.createContext(path);
	}

	@Override
	public void removeContext(String path) throws IllegalArgumentException {
		wrapped.removeContext(path);
	}

	@Override
	public void removeContext(HttpContext context) {
		wrapped.removeContext(context);
	}

	@Override
	public InetSocketAddress getAddress() {
		return wrapped.getAddress();
	}
	
}
