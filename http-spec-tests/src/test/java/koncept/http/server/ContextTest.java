package koncept.http.server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.http.NoHttpResponseException;
import org.junit.Test;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.spi.HttpServerProvider;


public class ContextTest extends HttpServerTestParameteriser {

	public ContextTest(HttpServerProvider provider) {
		super(provider);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void cannotCreateInvalidContext() {
		server.createContext("invalid/", new RecordingHandler());
	}
	
	@Test
	public void contextSelectionTest() throws IOException {
		RecordingHandler rootHandler = new RecordingHandler();
		HttpContext rootContext = server.createContext("/", rootHandler);
		
		RecordingHandler handler1 = new RecordingHandler();
		HttpContext context1 = server.createContext("/1", handler1);
		
		RecordingHandler handler2 = new RecordingHandler();
		HttpContext context2 = server.createContext("/2/", handler2);

		RecordingHandler handler22 = new RecordingHandler();
		HttpContext context22 = server.createContext("/2/2/", handler22);
		
		assertThat(rootContext.getPath(), is("/"));
		assertThat(context1.getPath(), is("/1"));
		assertThat(context2.getPath(), is("/2/"));
		assertThat(context22.getPath(), is("/2/2/"));
		
		assertThat(simpleUrl("/"), is(Code.HTTP_OK));
		assertThat(rootHandler.uris.size(), is(1));
		
		assertThat(simpleUrl("/1"), is(Code.HTTP_OK));
		assertThat(handler1.uris.size(), is(1));
		
		assertThat(simpleUrl("/11"), is(Code.HTTP_OK));
		assertThat(handler1.uris.size(), is(2));
		
		assertThat(simpleUrl("/1/"), is(Code.HTTP_OK));
		assertThat(handler1.uris.size(), is(3));
		
		assertThat(simpleUrl("/1/1"), is(Code.HTTP_OK));
		assertThat(handler1.uris.size(), is(4));
		
		assertThat(simpleUrl("/2"), is(Code.HTTP_OK)); //no trailing slash
		assertThat(rootHandler.uris.size(), is(2));
		
		assertThat(simpleUrl("/2/"), is(Code.HTTP_OK));
		assertThat(handler2.uris.size(), is(1));
		
		assertThat(simpleUrl("/2/2"), is(Code.HTTP_OK));
		assertThat(handler2.uris.size(), is(2));
		
		assertThat(simpleUrl("/2/22"), is(Code.HTTP_OK));
		assertThat(handler2.uris.size(), is(3));
		
		assertThat(simpleUrl("/2/2/"), is(Code.HTTP_OK));
		assertThat(handler22.uris.size(), is(1));
		
		assertThat(simpleUrl("/2/2/2"), is(Code.HTTP_OK));
		assertThat(handler22.uris.size(), is(2));
		
		assertThat(simpleUrl("/3"), is(Code.HTTP_OK));
		assertThat(rootHandler.uris.size(), is(3));
	}
	
	@Test
	public void errorInHttpHandler() {
		server.createContext("/", new ExceptingHttpHandler());
		try {
			simpleUrl("/");
			fail("expected an empty response"); //TODO - just close the connection with a 500 error
		} catch (RuntimeException e) {
			if (e.getCause() != null && e.getCause() instanceof NoHttpResponseException) {
				// default behaviour - but I don't think this is correct
			} else {
				fail(e.getMessage());
			}
			
		}
	}
	
	
	static class ExceptingHttpHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			throw new RuntimeException("Test Exception");
		}
	}
	
}
