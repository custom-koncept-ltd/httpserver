package koncept.http.server.context;

import koncept.http.server.exchange.HttpExchangeImpl;
import koncept.sp.ProcSplit;
import koncept.sp.resource.SimpleCleanableResource;
import koncept.sp.stage.SplitProcStage;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ContextLookupStage implements SplitProcStage {

	private final HttpContextHolder contexts;
	
	public ContextLookupStage(HttpContextHolder contexts) {
		this.contexts = contexts;
	}
	
	public ProcSplit run(ProcSplit last) {
		HttpExchangeImpl exchange = (HttpExchangeImpl)last.get("HttpExchange");
		HttpContext httpContext = contexts.findContext(exchange.getRequestURI().toString());
		if (httpContext != null && httpContext.getHandler() != null) {
			HttpHandler handler = httpContext.getHandler();
			exchange.setHttpContext(httpContext);
//			last.add("HttpHandler", new SimpleCleanableResource(handler, null));
			Filter.Chain filterChain = new Filter.Chain(exchange.getHttpContext().getFilters(), handler);
			last.add("Filter.Chain", new SimpleCleanableResource(filterChain, null));
			
		}
		return last;
	}
	
}
