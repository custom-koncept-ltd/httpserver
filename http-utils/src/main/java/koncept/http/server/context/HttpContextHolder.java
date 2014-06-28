package koncept.http.server.context;

import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HttpContextHolder {

	private final HttpServer server;
	CopyOnWriteArrayList<HttpContextImpl> contexts = new CopyOnWriteArrayList<HttpContextImpl>();
	
	public HttpContextHolder(HttpServer server) {
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
	
	/**
	 * Finds the matching context.
	 * A context will match when its context-path is the beginning of the full path.
	 * if there are multiple matches, the closes match is defined as the context with the longest context-path
	 * @param path
	 * @return
	 */
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
