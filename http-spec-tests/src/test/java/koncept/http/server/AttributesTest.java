package koncept.http.server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Test;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.spi.HttpServerProvider;

public class AttributesTest extends ProviderSpecHttpServerTestParameteriser {

	public AttributesTest(HttpServerProvider provider, boolean https) {
		super(provider, https);
	}
	
	/**
	 * I don't like the attributes spec - there is no 'per http exchange' scope (!!)
	 * @throws IOException
	 */
	@Test
	public void attributes() throws IOException {
		AttributeHandler handler = new AttributeHandler();
		HttpContext context = server.createContext("/", handler);
		
		context.getAttributes().put("testAttr", "attr1");
		
		assertThat(simpleUrl("/"), is(Code.HTTP_OK));
		assertThat(handler.uris.size(), is(1)); //request still passed through
		assertThat(handler.testAttr, is("attr1"));
		assertThat(handler.count, is(1));
	
		
		assertThat(simpleUrl("/"), is(Code.HTTP_OK));
		assertThat(handler.count, is(2));
		assertThat((Integer)context.getAttributes().get("count"), is(2));
	}
	
	
	static class AttributeHandler extends RecordingHandler {
		public volatile String testAttr;
		public volatile Integer count;
		
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			testAttr = (String)exchange.getAttribute("testAttr");
			
			Integer count = (Integer)exchange.getAttribute("count");
			if (count == null) {
				count = 1;
			} else {
				count = count + 1;
			}
			exchange.setAttribute("count", count);
			this.count = count;
			
			super.handle(exchange);
		}
	}
}
