package koncept.http.server.context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HttpContextImpl extends HttpContext {

	private final HttpServer server;
	private final String path;
	private final Map<String, Object> attributes;
	
	private HttpHandler handler;
	
	public HttpContextImpl(HttpServer server, String path) {
		this.server = server;
		this.path = path;
		attributes = new HashMap<String, Object>();
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Authenticator setAuthenticator(Authenticator auth) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Authenticator getAuthenticator() {
		// TODO Auto-generated method stub
		return null;
	}

}
