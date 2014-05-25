package koncept.http.server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.spi.HttpServerProvider;

public class FiltersTest extends HttpServerTestParameteriser {

	public FiltersTest(HttpServerProvider provider) {
		super(provider);
	}

	@Test
	public void basicFilterApplication() throws IOException {
		RecordingHandler handler = new RecordingHandler();
		RecordingFilter filter = new RecordingFilter();
		HttpContext context = server.createContext("/", handler);
		
		List<Filter> filters = context.getFilters();
		assertNotNull(filters);
		assertTrue(filters.isEmpty());
		filters.add(filter);
		
		assertThat(simpleUrl("/"), is(Code.HTTP_OK));
		assertThat(filter.uris.size(), is(1)); //filter incremented
		assertThat(handler.uris.size(), is(1)); //request still passed through
	}
	
}
