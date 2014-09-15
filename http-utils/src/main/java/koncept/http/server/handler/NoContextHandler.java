package koncept.http.server.handler;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class NoContextHandler implements HttpHandler {
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		exchange.sendResponseHeaders(404, 0);
	}
}
