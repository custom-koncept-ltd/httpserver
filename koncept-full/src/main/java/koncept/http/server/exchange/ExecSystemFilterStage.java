package koncept.http.server.exchange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import koncept.http.server.sysfilter.AuthenticatorFilter;
import koncept.http.server.sysfilter.Expect100ContinueFilter;
import koncept.http.server.sysfilter.FiniteSizedInputStream;
import koncept.sp.ProcSplit;
import koncept.sp.stage.SplitProcStage;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

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
		filters.add(new FiniteSizedInputStream());
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
	
}