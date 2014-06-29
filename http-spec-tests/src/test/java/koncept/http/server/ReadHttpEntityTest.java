package koncept.http.server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.spi.HttpServerProvider;

public class ReadHttpEntityTest extends ProviderSpecHttpServerTestParameteriser {

	public ReadHttpEntityTest(HttpServerProvider provider, boolean https) {
		super(provider, https);
	}
	
	/**
	 * I don't like the attributes spec - there is no 'per http exchange' scope (!!)
	 * @throws IOException
	 */
	@Test
	public void readData() throws IOException {
		ReadingHandler handler = new ReadingHandler();
		server.createContext("/", handler);
		
		String data = "line1\nline2\r\n3rd line\n\n5th";
		
		assertThat(putData("/", data), is(Code.HTTP_OK));
		
		assertThat(handler.read.size(), is(5));
		assertThat(handler.read.toArray(new String[5]), is(new String[]{"line1", "line2", "3rd line", "", "5th"}));
	}
	
	public Integer putData(String absolutePath, String data) {
		try {
			HttpPut put = new HttpPut(getProtocol() + "localhost:" + server.getAddress().getPort() + absolutePath);
			put.setEntity(new StringEntity(data));
			HttpResponse response = httpClient().execute(put);
			return response.getStatusLine().getStatusCode();
		} catch (ClientProtocolException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	static class ReadingHandler extends RecordingHandler {
		List<String> read = Collections.synchronizedList(new ArrayList<String>());
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			InputStream in = exchange.getRequestBody();
			BufferedReader lines = new BufferedReader(new InputStreamReader(in));
//			LineStreamer lines = new LineStreamer(in, 1024, "\r\n", "\n");
			String line = lines.readLine();
			while (line != null) {
				read.add(line);
				line = lines.readLine();
			}
			super.handle(exchange);
		}
	}
}
