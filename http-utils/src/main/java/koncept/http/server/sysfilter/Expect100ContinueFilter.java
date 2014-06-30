package koncept.http.server.sysfilter;

import java.io.IOException;

import koncept.http.server.Code;
import koncept.http.server.exchange.HttpExchangeImpl;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

public class Expect100ContinueFilter extends Filter {
	@Override
	public String description() {
		return "Expect100ContinueFilter";
	}
	@Override
	public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
		String expect100 = exchange.getRequestHeaders().getFirst("Expect");
		if (expect100 != null && expect100.equals("100-continue")) {
			((HttpExchangeImpl)exchange).sendPreviewCode(Code.HTTP_CONTINUE);
		}
		chain.doFilter(exchange);
	}
}