package koncept.http.server.sysfilter;

import java.io.IOException;

import koncept.http.server.exchange.HttpExchangeImpl;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Authenticator.Result;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

public class AuthenticatorFilter extends Filter {
	@Override
	public String description() {
		return "AuthenticatorFilter";
	}
	@Override
	public void doFilter(HttpExchange exchange, Chain chain)
			throws IOException {
		Authenticator authenticator = exchange.getHttpContext().getAuthenticator();
		if (authenticator == null)
			chain.doFilter(exchange);
		else {
			Result result = authenticator.authenticate(exchange);
			if (result instanceof Authenticator.Success) {
				HttpPrincipal principal = ((Authenticator.Success) result).getPrincipal();
				((HttpExchangeImpl)exchange).setPrincipal(principal);
				chain.doFilter(exchange);	
			} else if (result instanceof Authenticator.Failure) {
				exchange.sendResponseHeaders(((Authenticator.Failure) result).getResponseCode(), -1);
			} else if (result instanceof Authenticator.Retry) {
				exchange.sendResponseHeaders(((Authenticator.Retry) result).getResponseCode(), -1);
			}
		}
	}
}