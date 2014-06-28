package koncept.http.server.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sun.net.httpserver.HttpContext;


public class HttpContextHolderTest {

	HttpContextHolder holder = new HttpContextHolder(null);
	
	@Test
	public void initiallyEmpty() {
		assertTrue(holder.contexts.isEmpty());
		assertNull(findContext("/"));
	}
	
	@Test
	public void findsRootContext() {
		createContext("/");
		assertNotNull(("/"));
	}
	
	@Test
	public void mutuallyExclusiveContexts() {
		HttpContext one = createContext("/one");
		HttpContext two = createContext("/two");
		assertNotNull(findContext("/one"));
		assertNotNull(findContext("/two"));
		assertEquals(one, findContext("/one"));
		assertEquals(two, findContext("/two"));
	}
	
	@Test
	public void nestedContexts() {
		HttpContext one = createContext("/one");
		HttpContext two = createContext("/onetwo");
		assertNotNull(findContext("/one"));
		assertNotNull(findContext("/onet"));
		assertNotNull(findContext("/onetwo"));
		assertEquals(one, findContext("/one"));
		assertEquals(one, findContext("/onet"));
		assertEquals(two, findContext("/onetwo"));
	}
	
	private HttpContext findContext(String path) {
		return holder.findContext(path);
	}
	
	private HttpContext createContext(String path) {
		return holder.createContext(path, null);
	}
}
