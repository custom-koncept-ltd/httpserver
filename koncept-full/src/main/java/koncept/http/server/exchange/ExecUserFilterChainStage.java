package koncept.http.server.exchange;

import java.util.ArrayList;
import java.util.List;

import koncept.sp.ProcSplit;
import koncept.sp.stage.SplitProcStage;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ExecUserFilterChainStage implements SplitProcStage {

	private final ExecSystemFilterStage systemFilters;
	
	public ExecUserFilterChainStage() {
		this(null);
	}
	
	public ExecUserFilterChainStage(ExecSystemFilterStage systemFilters) {
		this.systemFilters = systemFilters;
	}
	
	public ProcSplit run(ProcSplit last) throws Exception {
		HttpContext httpContext = (HttpContext)last.getResource("HttpContext");
		HttpExchange exchange = (HttpExchange)last.getResource("HttpExchange");
		List<Filter> filters = httpContext.getFilters();
		if (systemFilters != null) {
			List<Filter> systemFilters = this.systemFilters.systemFilters(httpContext, exchange);
			List<Filter> userFilters = filters;
			filters = new ArrayList<>(filters.size() + systemFilters.size());
			filters.addAll(userFilters);
			filters.addAll(systemFilters);
		}
		HttpHandler handler = httpContext.getHandler();
		Filter.Chain filterChain = new Filter.Chain(filters, handler);
		filterChain.doFilter(exchange);
		return last;
	}

	
}