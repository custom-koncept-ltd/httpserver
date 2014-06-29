package koncept.http.server.exchange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import koncept.http.server.Code;
import koncept.sp.ProcSplit;
import koncept.sp.stage.SplitProcStage;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Authenticator.Result;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpPrincipal;

public class ExecSystemFilterStage implements SplitProcStage {

	public ProcSplit run(ProcSplit last) throws Exception {
		HttpContext httpContext = (HttpContext)last.get("HttpContext");
		HttpExchange exchange = (HttpExchange)last.get("HttpExchange");
		if (httpContext != null && exchange != null) {
			ExecuteNextStageHttpHandler executeNextStage = new ExecuteNextStageHttpHandler();
			Filter.Chain filterChain = new Filter.Chain(systemFilters(httpContext, exchange), executeNextStage);
			filterChain.doFilter(exchange);
			if (! executeNextStage.executeNextStage()) {
				//this will have the effect of prevent further execution
				last.removeCleanableResource("HttpContext");
			}
		}
		return last;
	}
	
	
	public List<Filter> systemFilters(HttpContext httpContext, HttpExchange exchange) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Expect100ContinueFilter());
		filters.add(new AuthenticatorFilter(httpContext.getAuthenticator()));
		return filters;
	}
	
	
	private static class ExecuteNextStageHttpHandler implements HttpHandler {
		private boolean executeNextStage = false;
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			executeNextStage = true;
		}
		public boolean executeNextStage() {
			return executeNextStage;
		}
	}
	
	private static class Expect100ContinueFilter extends Filter {
		@Override
		public String description() {
			return "Expect100ContinueFilter";
		}
		@Override
		public void doFilter(HttpExchange exchange, Chain chain)
				throws IOException {
			String expect100 = exchange.getRequestHeaders().getFirst("Expect");
			if (expect100 != null && expect100.equals("100-continue")) {
				((HttpExchangeImpl)exchange).sendPreviewCode(Code.HTTP_CONTINUE);
			}
			chain.doFilter(exchange);
		}
	}

	
	private static class AuthenticatorFilter extends Filter {
		final Authenticator authenticator;
		public AuthenticatorFilter(Authenticator authenticator) {
			this.authenticator = authenticator;
		}
		@Override
		public String description() {
			return "AuthenticatorFilter";
		}
		@Override
		public void doFilter(HttpExchange exchange, Chain chain)
				throws IOException {
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
					exchange.close();
				} else if (result instanceof Authenticator.Retry) {
					exchange.sendResponseHeaders(((Authenticator.Retry) result).getResponseCode(), -1);
					exchange.close();
				}
			}
		}
	}
	
}