package koncept.http.server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Test;

import com.sun.net.httpserver.spi.HttpServerProvider;

public class StartAndStopTest extends HttpServerTestParameteriser {

	public StartAndStopTest(HttpServerProvider provider) {
		super(provider);
	}
	
	@Test
	public void startAndStop() throws IOException {
		RecordingHandler handler = new RecordingHandler();
		server.createContext("/", handler);
		
//		server.start(); //server is auto started
		
		assertThat(simpleUrl("/"), is(200));
		assertThat(handler.uris.size(), is(1));
		
		server.stop(0);
		
		assertNull(simpleUrl("/"));
		assertThat(handler.uris.size(), is(1));
	}
	
}
