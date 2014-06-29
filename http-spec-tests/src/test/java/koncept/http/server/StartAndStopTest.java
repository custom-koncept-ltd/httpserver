package koncept.http.server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Test;

import com.sun.net.httpserver.spi.HttpServerProvider;

public class StartAndStopTest extends ProviderSpecHttpServerTestParameteriser {

	public StartAndStopTest(HttpServerProvider provider, boolean https) {
		super(provider, https);
	}
	
	@Test
	public void startAndStop() throws IOException {
		RecordingHandler handler = new RecordingHandler();
		server.createContext("/", handler);
		
//		server.start(); //server is auto started
		
		assertThat(simpleUrl("/"), is(Code.HTTP_OK));
		assertThat(handler.uris.size(), is(1));
		
		server.stop(0);
		
		assertNull(simpleUrl("/"));
		assertThat(handler.uris.size(), is(1));
	}
	
}
