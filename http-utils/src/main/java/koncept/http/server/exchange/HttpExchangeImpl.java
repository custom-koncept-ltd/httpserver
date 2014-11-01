package koncept.http.server.exchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import koncept.http.server.Code;
import koncept.http.server.ConfigurationOption;
import koncept.http.server.sysfilter.KeepAliveFilter.ConnectionPersistor;
import koncept.io.ChunkedOutputStream;
import koncept.io.StreamingSocketConnection;
import koncept.io.WrappedInputStream;
import koncept.io.WrappedOutputStream;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;


public class HttpExchangeImpl extends HttpExchange {
	public static final ConfigurationOption ATTRIBUTE_SCOPE = new ConfigurationOption("httpexchange.attribute.scoping", new String[]{"context", "exchange"});
	
	private static final String newLine = "\r\n".intern();
	private final StreamingSocketConnection<?> connection;
	
	private final String httpVersion;
	private final String requestMethod;
	private final URI requestURI;
	private final HttpContext httpContext;
	private final Map<String,Object> attributes;
	
	private final Headers requestHeaders = new Headers();
	private final Headers responseHeaders = new Headers();
	
	private InputStream in;
	private OutputStream out;
	
	private HttpPrincipal principal;
	private int responseCode = 0;
	private boolean closed = false;
	
	private ConnectionPersistor connectionPersistor;
	
	public HttpExchangeImpl(StreamingSocketConnection<?> connection, String httpVersion, String requestMethod, URI requestURI, final HttpContext httpContext, Map<ConfigurationOption, String> options) throws IOException {
		this.connection = connection;
		setStreams(new WrappedInputStream(connection.in()), new WrappedOutputStream(connection.out()));
		this.httpVersion = httpVersion;
		this.requestMethod = requestMethod;
		this.requestURI = requestURI;
		this.httpContext = httpContext;
		
		String option = options.get(ATTRIBUTE_SCOPE);
		 if (option.equals("exchange")) {
			 attributes = new ConcurrentHashMap<String, Object>();
		 } else if (option.equals("context")) {
			 attributes = httpContext.getAttributes();
		 } else throw new IllegalStateException("Unknown scope: " + option);
		
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

	@Override
	public void close() {
		if(closed) return;
		try {
			in.close();
			out.close();
			closed = true;
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

	public void sendPreviewCode(int rCode) {
		PrintStream p = new PrintStream(out);
		p.print(httpVersion + " " + rCode + Code.msg(rCode));
		p.print(newLine);
//		p.print("Content-length: 0"); //?? seems this is part of the reply (!!)
//		p.print(newLine);
		p.flush();
	}
	
	//TODO: Need a way to manage the auto headers better
	@Override
	public void sendResponseHeaders(int rCode, long responseLength)
			throws IOException {
//		HTTP/1.1 200 OK
		responseCode = rCode;
		
		if (connectionPersistor != null) 
			connectionPersistor.onResponse(this);
		
		PrintStream p = new PrintStream(out);
		p.print(httpVersion + " " + rCode + Code.msg(rCode));
		p.print(newLine);
		
		//responseLength:
		//-1 = nothing to send
		//0 = unknown length
		//n = n bytes

		//insert a content length header
		if (responseLength != 0 && !"head".equalsIgnoreCase(getRequestMethod())) {
			responseHeaders.set("Content-Length", Long.toString(responseLength== -1 ? 0 : responseLength));
		}
		if (responseLength == 0 && !"head".equalsIgnoreCase(getRequestMethod())) {
			responseHeaders.set("Transfer-Encoding", "chunked");
		}
		
		String pattern = "EEE, dd MMM yyyy HH:mm:ss zzz";
	    TimeZone gmtTZ = TimeZone.getTimeZone("GMT");
	    DateFormat df = new SimpleDateFormat(pattern, Locale.UK);
	    df.setTimeZone(gmtTZ);
		responseHeaders.set ("Date", df.format (new Date()));
		
		//responseHeaders
		for(String headerName: responseHeaders.keySet()) {
			List<String> headerValues = responseHeaders.get(headerName);
			for(String headerValue: headerValues) {
				p.print(headerName);
				p.print(": ");
				p.print(headerValue);
				p.print(newLine);
			}
		}
		p.print(newLine);
		p.flush();
		
		if (responseLength == 0) {
			((WrappedOutputStream) out).setWrapped(new ChunkedOutputStream(((WrappedOutputStream) out).getWrapped())); //insert a chunked output stream
		}
		
	}
	
	@Override
	public InetSocketAddress getRemoteAddress() {
		return connection.remoteAddress(); 
	}

	@Override
	public int getResponseCode() {
		return responseCode;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return connection.localAddress();
	}

	@Override
	public String getProtocol() { //request protocol
		return httpVersion;
	}

	@Override
	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	@Override
	public void setAttribute(String name, Object value) {
		attributes.put(name, value);
	}

	@Override
	public void setStreams(InputStream in, OutputStream out) {
		this.in = in;
		this.out = out;
	}

	@Override
	public HttpPrincipal getPrincipal() {
		return principal;
	}
	
	public void setPrincipal(HttpPrincipal principal) {
		this.principal = principal;
	}
	
	public ConnectionPersistor getConnectionPersistor() {
		return connectionPersistor;
	}
	
	public void setConnectionPersistor(ConnectionPersistor connectionPersistor) {
		this.connectionPersistor = connectionPersistor;
	}
	
}
