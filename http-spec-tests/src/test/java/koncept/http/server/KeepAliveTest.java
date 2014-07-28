package koncept.http.server;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import koncept.io.LineStreamer;

import org.junit.Ignore;
import org.junit.Test;

import com.sun.net.httpserver.spi.HttpServerProvider;

/**
 * Note that socket.isClosed() is NOT an accurate up-to-date representation of the state of the socket.
 * @author koncept
 *
 */
public class KeepAliveTest extends ProviderSpecHttpServerTestParameteriser {

	private final String KEEP_ALIVE = "Keep-Alive";
	private final String CLOSE = "close";
	
	public KeepAliveTest(HttpServerProvider provider, boolean https) {
		super(provider, https);
	}
	
	@Test
	public void http11ExplicitClose() throws IOException {
		RecordingHandler handler = new RecordingHandler();
		server.createContext("/", handler);
		final String httpVersion = "HTTP/1.1";
		Socket s = openDirectSocket();
		
		assertThat(url(s, "/1", httpVersion, CLOSE), is(Code.HTTP_OK));
		assertThat(handler.uris.size(), is(1));
		
		assertThat(url(s, "/2", httpVersion, null), nullValue());
		assertThat(handler.uris.size(), is(1));
	}
	
	@Test
	public void http10ExplicitClose() throws IOException {
		RecordingHandler handler = new RecordingHandler();
		server.createContext("/", handler);
		final String httpVersion = "HTTP/1.0";
		Socket s = openDirectSocket();
		
		assertThat(url(s, "/1", httpVersion, CLOSE), is(Code.HTTP_OK));
		assertThat(handler.uris.size(), is(1));
		
		assertThat(url(s, "/2", httpVersion, null), nullValue());
		assertThat(handler.uris.size(), is(1));
	}
	
	@Test
	public void http11ExplicitKeepAlive() throws IOException {
		RecordingHandler handler = new RecordingHandler();
		server.createContext("/", handler);
		final String httpVersion = "HTTP/1.1";
		Socket s = openDirectSocket();
		
		assertThat(url(s, "/1", httpVersion, KEEP_ALIVE), is(Code.HTTP_OK));
		assertThat(handler.uris.size(), is(1));
		
		assertThat(url(s, "/2", httpVersion, KEEP_ALIVE), is(Code.HTTP_OK));
		assertThat(handler.uris.size(), is(2));
		
		assertThat(url(s, "/3", httpVersion, CLOSE), is(Code.HTTP_OK));
		assertThat(handler.uris.size(), is(3));
		
		assertThat(url(s, "/4", httpVersion, null), nullValue());
	}
	
	@Test
	public void http10ExplicitKeepAlive() throws IOException {
		RecordingHandler handler = new RecordingHandler();
		server.createContext("/", handler);
		final String httpVersion = "HTTP/1.0";
		Socket s = openDirectSocket();
		
		assertThat(url(s, "/1", httpVersion, KEEP_ALIVE), is(Code.HTTP_OK));
		assertThat(handler.uris.size(), is(1));
		
		assertThat(url(s, "/2", httpVersion, KEEP_ALIVE), is(Code.HTTP_OK));
		assertThat(handler.uris.size(), is(2));
		
		assertThat(url(s, "/3", httpVersion, CLOSE), is(Code.HTTP_OK));
		assertThat(handler.uris.size(), is(3));
		
		assertThat(url(s, "/4", httpVersion, null), nullValue());
	}
	
	
	protected Integer url(Socket s, String url, String httpVersion, String connectionHeaderValue) throws IOException {
		PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
		String newLine = "\r\n";
		out.write("GET " + url + " " + httpVersion + newLine);
		out.write("Content-Length: 0" + newLine);
		if (connectionHeaderValue != null) {
			out.write("Connection: " + connectionHeaderValue + newLine);
		}
		out.write(newLine);
		out.flush();
		
		LineStreamer lines = new LineStreamer(s.getInputStream());
		
		String statusLine = lines.readLine(10);
		if (statusLine == null) return null;
		Integer returnCode = new Integer(statusLine.split(" ")[1]);
		
		for(String line = lines.readLine(10); line != null && !line.equals(""); line = lines.readLine(10)) {
			//just strip off the response
		}
		return returnCode;
	}
}
