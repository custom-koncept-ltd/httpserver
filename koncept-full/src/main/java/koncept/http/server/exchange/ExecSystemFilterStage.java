package koncept.http.server.exchange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import koncept.http.server.ComposableHttpServer;
import koncept.http.server.ConfigurationOption;
import koncept.http.server.sysfilter.AuthenticatorFilter;
import koncept.http.server.sysfilter.Expect100ContinueFilter;
import koncept.http.server.sysfilter.FiniteSizedInputStream;
import koncept.sp.ProcSplit;
import koncept.sp.resource.SimpleCleanableResource;
import koncept.sp.stage.SplitProcStage;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ExecSystemFilterStage implements SplitProcStage {

	private final Map<ConfigurationOption, String> options;
	
	public ExecSystemFilterStage(Map<ConfigurationOption, String> options) {
		this.options = options;
	}
	
	public boolean expect100Continue() {
		return Boolean.parseBoolean(options.get(ComposableHttpServer.EXPECT_100_CONTINUE));
	}
	
	public ProcSplit run(ProcSplit last) throws Exception {
		HttpContext httpContext = (HttpContext)last.getResource("HttpContext");
		HttpExchange exchange = (HttpExchange)last.getResource("HttpExchange");
		boolean doNextStage = false;
		if (httpContext != null && exchange != null) {
			ExecuteNextStageHttpHandler executeNextStage = new ExecuteNextStageHttpHandler();
			Filter.Chain filterChain = new Filter.Chain(systemFilters(httpContext, exchange), executeNextStage);
			filterChain.doFilter(exchange);
			doNextStage = executeNextStage.executeNextStage();
		}
		last.add(ExecSystemFilterStage.class.getName(), new SimpleCleanableResource(doNextStage, null));
		return last;
	}
	
	
	public List<Filter> systemFilters(HttpContext httpContext, HttpExchange exchange) {
		List<Filter> filters = new ArrayList<>();
		if (expect100Continue())
			filters.add(new Expect100ContinueFilter());
		filters.add(new AuthenticatorFilter());
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