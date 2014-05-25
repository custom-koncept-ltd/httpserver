package koncept.http.server.context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import koncept.http.server.ComposableHttpServer;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HttpContextImpl extends HttpContext {

	private final ComposableHttpServer server;
	private final String path;
	private final Map<String, Object> attributes;
	private final List<Filter> filters;
	
	private HttpHandler handler;
	private Authenticator authenticator;
	
	public HttpContextImpl(ComposableHttpServer server, String path) {
		this.server = server;
		this.path = path;
		attributes = new HashMap<String, Object>();
		filters = new CopyOnWriteArrayList<>();
	}
	
	
	@Override
	public HttpHandler getHandler() {
		return handler;
	}

	@Override
	public void setHandler(HttpHandler handler) {
		this.handler = handler;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public HttpServer getServer() {
		return server;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public List<Filter> getFilters() {
		return filters;
	}

	@Override
	public Authenticator setAuthenticator(Authenticator auth) {
		Authenticator previous = authenticator;
		authenticator = auth;
		return previous;
	}

	@Override
	public Authenticator getAuthenticator() {
		return authenticator;
	}

}
