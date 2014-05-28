package koncept.http.server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Test;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.spi.HttpServerProvider;

public class AttributesWithScopingTest extends DefaultSpecHttpServerTestParameteriser {

	public AttributesWithScopingTest(HttpServerProvider provider) {
		super(provider);
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
		assertNull(handler.testAttr);
		assertThat(handler.contextTextAttr, is("attr1"));
		assertThat(handler.count, is(1));
	
		
		assertThat(simpleUrl("/"), is(Code.HTTP_OK));
		assertThat(handler.count, is(1)); //scoped count should always re-begin at 1
		assertThat(handler.uris.size(), is(2)); //2 tracked calls in total though
	}
	
	
	static class AttributeHandler extends RecordingHandler {
		public volatile String testAttr;
		public volatile String contextTextAttr;
		public volatile Integer count;
		
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			testAttr = (String)exchange.getAttribute("testAttr");
			contextTextAttr = (String)exchange.getHttpContext().getAttributes().get("testAttr");
			
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
