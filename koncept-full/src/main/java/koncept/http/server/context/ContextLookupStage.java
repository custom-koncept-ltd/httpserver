package koncept.http.server.context;

import koncept.http.server.parse.ReadRequestLineStage;
import koncept.sp.ProcSplit;
import koncept.sp.resource.NonCleanableResource;
import koncept.sp.stage.SplitProcStage;

import com.sun.net.httpserver.HttpContext;

public class ContextLookupStage implements SplitProcStage {

	private final HttpContextHolder contexts;
	
	public ContextLookupStage(HttpContextHolder contexts) {
		this.contexts = contexts;
	}
	
	public ProcSplit run(ProcSplit last) {
		String requestLine = (String)last.getResource(ReadRequestLineStage.RequestLine);
		if (requestLine == null) 
			return last;
		String operation[] = requestLine.split(" ");
		HttpContext httpContext = contexts.findContext(operation[1]);//use the URL part to look up the http context
		if (httpContext != null && httpContext.getHandler() != null) {
			last.add("HttpContext", new NonCleanableResource(httpContext));
		}
		return last;
	}
	
}
