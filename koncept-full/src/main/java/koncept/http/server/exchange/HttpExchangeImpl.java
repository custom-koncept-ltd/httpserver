package koncept.http.server.exchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;


public class HttpExchangeImpl extends HttpExchange {
	private final Socket socket;
	
	private final String httpVersion;
	private final String requestMethod;
	private final URI requestURI;
	
	private final Headers requestHeaders = new Headers();
	private final Headers responseHeaders = new Headers();
	
	private HttpContext httpContext;
	
	private Map<String, Object> attribues = new HashMap<String, Object>();
	private InputStream in;
	private OutputStream out;
	
	public HttpExchangeImpl(Socket socket, String httpVersion, String requestMethod, URI requestURI) throws IOException {
		this.socket = socket;
		setStreams(socket.getInputStream(), socket.getOutputStream());
		this.httpVersion = httpVersion;
		this.requestMethod = requestMethod;
		this.requestURI = requestURI;
	}
	
	@Override
	public Headers getRequestHeaders() {
		return requestHeaders;
	}

	@Override
	public Headers getResponseHeaders() {
		return responseHeaders;
	}

	@Override
	public URI getRequestURI() {
		return requestURI;
	}

	@Override
	public String getRequestMethod() {
		return requestMethod;
	}

	@Override
	public HttpContext getHttpContext() {
		return httpContext;
	}
	public void setHttpContext(HttpContext httpContext) {
		this.httpContext = httpContext;
	}

	@Override
	public void close() {
		try {
			socket.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public InputStream getRequestBody() {
		return in;
	}

	@Override
	public OutputStream getResponseBody() {
		return out;
	}

	@Override
	public void sendResponseHeaders(int rCode, long responseLength)
			throws IOException {
//		HTTP/1.1 200 OK

		PrintStream p = new PrintStream(out);
		p.print(httpVersion + " " + rCode + " " + " OK"); //need a lookup on that
		p.flush();
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return new InetSocketAddress(socket.getInetAddress(), socket.getPort());
	}

	@Override
	public int getResponseCode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return new InetSocketAddress(socket.getLocalAddress(), socket.getLocalPort());
	}

	@Override
	public String getProtocol() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getAttribute(String name) {
		return attribues.get(name);
	}

	@Override
	public void setAttribute(String name, Object value) {
		attribues.put(name, value);
	}

	@Override
	public void setStreams(InputStream in, OutputStream out) {
		this.in = in;
		this.out = out;
	}

	@Override
	public HttpPrincipal getPrincipal() {
		// TODO Auto-generated method stub
		return null;
	}

	
	
}