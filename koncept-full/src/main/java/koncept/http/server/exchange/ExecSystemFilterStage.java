package koncept.http.server.exchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import koncept.http.server.ConfigurationOption;
import koncept.http.server.handler.DummyHandler;
import koncept.http.server.io.ComposableHttpIOServer;
import koncept.http.server.sysfilter.AuthenticatorFilter;
import koncept.http.server.sysfilter.Expect100ContinueFilter;
import koncept.http.server.sysfilter.FiniteSizedInputStream;
import koncept.http.server.sysfilter.KeepAliveFilter;
import koncept.sp.ProcSplit;
import koncept.sp.stage.SplitProcStage;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;

public class ExecSystemFilterStage implements SplitProcStage {
	private final Map<ConfigurationOption, String> options;
	
	public ExecSystemFilterStage(Map<ConfigurationOption, String> options) {
		this.options = options;
	}
	
	public boolean expect100Continue() {
		return Boolean.parseBoolean(options.get(ComposableHttpIOServer.EXPECT_100_CONTINUE));
	}
	
	public ProcSplit run(ProcSplit last) throws Exception {
		HttpContext httpContext = (HttpContext)last.getResource("HttpContext");
		HttpExchange exchange = (HttpExchange)last.getResource("HttpExchange");
		Filter.Chain filterChain = new Filter.Chain(systemFilters(httpContext, exchange), new DummyHandler());
		filterChain.doFilter(exchange);
		return last;
	}
	
	
	public List<Filter> systemFilters(HttpContext httpContext, HttpExchange exchange) {
		List<Filter> filters = new ArrayList<>();
		if (expect100Continue())
			filters.add(new Expect100ContinueFilter());
		filters.add(new AuthenticatorFilter());
		filters.add(new FiniteSizedInputStream());
		filters.add(new KeepAliveFilter(options));
		return filters;
	}
	
}