package koncept.http.server.handler;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class DummyHandler implements HttpHandler {
	@Override
	public void handle(HttpExchange exchange) throws IOException {
	}
}
