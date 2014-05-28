package koncept.http.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.spi.HttpServerProvider;

public class ResponseHeadersTest extends ProviderSpecHttpServerTestParameteriser {

	public ResponseHeadersTest(HttpServerProvider provider) {
		super(provider);
	}

	@Test
	public void headerHandling() throws IOException {
		HttpHandler handler = new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				exchange.getResponseHeaders().add("Test", "true");
				exchange.getResponseHeaders().add("Test2", "true2");
				exchange.getResponseHeaders().add("Test2", "true3");
				exchange.sendResponseHeaders(Code.HTTP_OK, -1); //-1 length...
				exchange.close();
			}
		};
		server.createContext("/", handler);
		
		Header[] headers = urlHeaders("/");
		assertTrue(containsHeader("Content-length", headers)); //auto header
		assertTrue(containsHeader("Date", headers)); //auto header
		
		assertTrue(containsHeader("Test", headers));
		assertTrue(containsHeader("Test2", headers));
		assertFalse(containsHeader("Test3", headers));
	}
	
	private boolean containsHeader(String headerName, Header[] headers) {
		for(Header header: headers) {
			if (headerName.equals(header.getName()))
				return true;
		}
		return false;
	}
	
	public Header[] urlHeaders(String absolutePath) {
		try {
			CloseableHttpClient httpclient = HttpClients.createDefault();
			CloseableHttpResponse response = httpclient.execute(new HttpGet("http://localhost:" + server.getAddress().getPort() + absolutePath));
			return response.getAllHeaders();
		} catch (ClientProtocolException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
