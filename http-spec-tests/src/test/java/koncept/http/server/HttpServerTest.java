package koncept.http.server;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.spi.HttpServerProvider;

@RunWith(Parameterized.class)
public class HttpServerTest {

	final HttpServerProvider provider;
	HttpServer server;
	
	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{new sun.net.httpserver.DefaultHttpServerProvider()},
				{new koncept.http.LegacyModHttpServerProvider()},
//				{new koncept.http.KonceptHttpApacheProvider()},
				{new koncept.http.KonceptHttpServerProvider()}
		});
	}
	
	public HttpServerTest(HttpServerProvider provider) {
		this.provider = provider;
	}
	
	public static int getPort() {
		String port = System.getProperty("port");
		if (port != null) return new Integer(port);
		return 9080;
	}
	
	@Before
	public void prepare() throws IOException {
		//Hmm... the server opens the port straight away - before any start() call is made (!!)
		server = provider.createHttpServer(new InetSocketAddress("localhost", getPort()), 0);
//		server.setExecutor(Executors.newSingleThreadExecutor());
		server.setExecutor(Executors.newFixedThreadPool(5)); //currently can't have just a single thread Executor
	}
	
	@After
	public void cleanUp() {
		if (server != null) {
			server.stop(0);
			server = null;
		}
	}
	
	@Test
	public void startAndStop() throws IOException {
		RecordingHandler handler = new RecordingHandler();
		server.createContext("/", handler);
		
		server.start();
		
		assertThat(simpleUrl("/"), is(200));
		assertThat(handler.uris.size(), is(1));
		
		server.stop(0);
//		server = null;
		
		assertNull(simpleUrl("/"));
		assertThat(handler.uris.size(), is(1));
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
		
		server.start();
		
		assertThat(simpleUrl("/"), is(200));
		assertThat(rootHandler.uris.size(), is(1));
		
		assertThat(simpleUrl("/1"), is(200));
		assertThat(handler1.uris.size(), is(1));
		
		assertThat(simpleUrl("/11"), is(200));
		assertThat(handler1.uris.size(), is(2));
		
		assertThat(simpleUrl("/1/"), is(200));
		assertThat(handler1.uris.size(), is(3));
		
		assertThat(simpleUrl("/1/1"), is(200));
		assertThat(handler1.uris.size(), is(4));
		
		assertThat(simpleUrl("/2"), is(200)); //no trailing slash
		assertThat(rootHandler.uris.size(), is(2));
		
		assertThat(simpleUrl("/2/"), is(200));
		assertThat(handler2.uris.size(), is(1));
		
		assertThat(simpleUrl("/2/2"), is(200));
		assertThat(handler2.uris.size(), is(2));
		
		assertThat(simpleUrl("/2/22"), is(200));
		assertThat(handler2.uris.size(), is(3));
		
		assertThat(simpleUrl("/2/2/"), is(200));
		assertThat(handler22.uris.size(), is(1));
		
		assertThat(simpleUrl("/2/2/2"), is(200));
		assertThat(handler22.uris.size(), is(2));
		
		assertThat(simpleUrl("/3"), is(200));
		assertThat(rootHandler.uris.size(), is(3));
		
	}
	
	@Test
	public void headerHandling() throws IOException {
		RecordingHandler handler = new RecordingHandler();
		server.createContext("/", handler);
		server.start();
		
		
		System.out.println("\n\n" + server.getClass().getName());
		assertThat(simpleUrlWithCustomHeaders("/"), is(200));
		Map<String, List<String>> headers = handler.lastHeaders;
		for(String headerName: headers.keySet()) {
			List<String> values = headers.get(headerName);
			System.out.println("serverside header: \"" + headerName + "\" = \"" + values.get(0) + "\"");
		}
//		assertThat(headers.size(), is(9));
		assertThat(headers.get("Header1"), is(asList("\"quotes in value\"")));
		assertThat(headers.get("Header2"), is(asList("colon: test: value")));
		assertThat(headers.get("Header3"), is(asList("hardUp")));
		assertThat(headers.get("Header4"), is(asList("x     y"))); //trimmed value
		assertThat(headers.get("Header6"), is(asList("")));
		assertThat(headers.get("Header7"), is(asList("")));
		assertThat(headers.get("Header8"), is(asList("")));
		assertThat(headers.get("Header9"), is(asList("value 1", "value 2", "value 3")));
		
		assertThat(headers.get("Case"), is(asList("lower", "UPPER")));
		
		//inconsistent behaviours identified
		//note this is ALL not to spec, so the difference in behaviour isn't 
		//necessarily wrong, it just needs to be captured
		if (!headers.containsKey("Header5")) {
			List<String> nullHeaders = headers.get(null);
				assertThat(headers.get(null), is(asList("Header5", ":malformed2 value")));
		} else {
			assertThat(headers.get("Header5"), is(asList("")));
		}
		
		if (headers.containsKey(":malformed1")) {
			assertThat(headers.get(":malformed1"), is(asList("value")));
		} else {
			assertThat(headers.get(""), is(asList("malformed1: value", "malformed2 value")));
		}
	}
	
	//custom bad header testing, etc...
	private Integer simpleUrlWithCustomHeaders(String absolutePath) {
		Socket s = null;
		try {
			s = new Socket("localhost", getPort());
			PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
			String newLine = "\r\n";
			out.write("GET / HTTP/1.1" + newLine);
			out.write("Header1: \"quotes in value\"" + newLine);
			out.write("Header2: colon: test: value" + newLine);
			out.write("Header3:hardUp" + newLine);
			out.write("Header4:    x     y     " + newLine);
			out.write("Header5" + newLine);
			out.write("Header6:" + newLine);
			out.write("Header7: " + newLine);
			out.write("Header8:  " + newLine);
			out.write("Header9: value 1" + newLine);
			out.write("Header9: value 2" + newLine);
			out.write("Header9: value 3" + newLine);
			
			//note that the case is re-written
			out.write("case: lower" + newLine);
			out.write("CASE: UPPER" + newLine);
			out.write(":malformed1: value" + newLine);
			out.write(":malformed2 value" + newLine);
			out.write("" + newLine);
			out.flush();
			
			BufferedReader bIn = new BufferedReader(new InputStreamReader(s.getInputStream()));
			String line = bIn.readLine();
			
			System.out.println("client read status line : " + line);
			String statusLine[] = line.split(" "); //HTTP/1.1 200 OK
			while (line != null && !line.equals("")) {
				line = bIn.readLine();
				System.out.println("client read: " + line);
			}
			return new Integer(statusLine[1]);
		} catch (IOException e) {
			if (e instanceof ConnectException)
				return null; //unable to connect
			throw new RuntimeException(e);
		} finally {
			if (s != null) try {s.close();}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private Integer simpleUrl(String absolutePath) {
		try {
			CloseableHttpClient httpclient = HttpClients.createDefault();
			CloseableHttpResponse response = httpclient.execute(new HttpGet("http://localhost:" + getPort() + absolutePath));
			return response.getStatusLine().getStatusCode();
		} catch (ClientProtocolException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			if (e.getCause() instanceof ConnectException)
				return null; //unable to connect
			throw new RuntimeException(e);
		}
	}
	
	static class RecordingHandler implements HttpHandler {
		public final List<String> uris = Collections.synchronizedList(new ArrayList<String>());
		public volatile Map<String, List<String>> lastHeaders = null;
		public volatile URI lastUri;
		public volatile String lastRequestMethod;
		
		public void handle(HttpExchange exchange) throws IOException {
			lastHeaders = new HashMap<String, List<String>>(exchange.getRequestHeaders());
			uris.add(exchange.getRequestURI().toString());
			lastUri = exchange.getRequestURI();
			lastRequestMethod = exchange.getRequestMethod();
			
			exchange.sendResponseHeaders(200, 0);
			exchange.close(); //shouldn't this be automatic?
		}
	}
}
