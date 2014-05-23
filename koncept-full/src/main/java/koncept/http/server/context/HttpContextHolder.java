package koncept.http.server.context;

import java.util.concurrent.CopyOnWriteArrayList;

import koncept.http.server.ComposableHttpServer;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;

public class HttpContextHolder {

	private final ComposableHttpServer server;
	CopyOnWriteArrayList<HttpContextImpl> contexts = new CopyOnWriteArrayList<HttpContextImpl>();
	
	public HttpContextHolder(ComposableHttpServer server) {
		this.server = server;
	}
	
	public synchronized HttpContext createContext(String path, HttpHandler handler) {
		if (!path.startsWith("/")) throw new IllegalArgumentException("Invalid path: " + path);
		for(HttpContext context: contexts) {
			if (context.getPath().equals(path)) {
				throw new RuntimeException("cannot overwite contexts - please remove it first");
			}
		}
		HttpContextImpl context = new HttpContextImpl(server, path);
		context.setHandler(handler); //handler may be null
		contexts.add(context);
		return context;
	}
	
	public synchronized void removeContext(HttpContext context) {
		contexts.remove(context);
	}

	public synchronized void removeContext(String path) throws IllegalArgumentException {
		for(HttpContext context: contexts) {
			if (context.getPath().equals(path)) {
				removeContext(context);
				return;
			}
		}
	}
	
	public HttpContext findContext(String path) {
		HttpContext found = null;
		for(HttpContext context: contexts) {
			if (path.startsWith(context.getPath())) {
				if (found == null) found = context;
				else if (context.getPath().length() > found.getPath().length()) found = context;
			}
		}
		return found;
	}
	
}
