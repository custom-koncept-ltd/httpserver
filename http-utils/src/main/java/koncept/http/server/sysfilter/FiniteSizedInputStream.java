package koncept.http.server.sysfilter;

import java.io.IOException;
import java.io.InputStream;

import koncept.io.FixedSizeInputStream;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

public class FiniteSizedInputStream extends Filter {
	@Override
	public String description() {
		return "FiniteSizedInputStream";
	}
	@Override
	public void doFilter(HttpExchange exchange, Chain chain)
			throws IOException {
		Long size = null;
		String transferEncoding = exchange.getRequestHeaders().getFirst("Transfer-encoding");
		if (transferEncoding != null && transferEncoding.equals("chunked")){
			
		} else {
			String contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
			if (contentLength != null)
				size = new Long(contentLength);
		}
		
		if (size != null) {
			InputStream in = new FixedSizeInputStream(exchange.getRequestBody(), size, true);
			exchange.setStreams(in, exchange.getResponseBody()); //reset the input streams here for a fixed size stream
		}
		chain.doFilter(exchange);
	}
}