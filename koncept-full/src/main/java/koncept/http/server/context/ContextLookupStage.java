package koncept.http.server.context;

import koncept.http.server.exchange.HttpExchangeImpl;
import koncept.sp.ProcSplit;
import koncept.sp.resource.SimpleCleanableResource;
import koncept.sp.stage.SplitProcStage;

import com.sun.net.httpserver.HttpContext;

public class ContextLookupStage implements SplitProcStage {

	private final HttpContextHolder contexts;
	
	public ContextLookupStage(HttpContextHolder contexts) {
		this.contexts = contexts;
	}
	
	public ProcSplit run(ProcSplit last) {
		HttpExchangeImpl exchange = (HttpExchangeImpl)last.get("HttpExchange");
		HttpContext httpContext = contexts.findContext(exchange.getRequestURI().toString());
		if (httpContext != null && httpContext.getHandler() != null) {
			exchange.setHttpContext(httpContext);
			last.add("HttpHandler", new SimpleCleanableResource(httpContext.getHandler(), null));
		}
		return last;
	}
	
}
