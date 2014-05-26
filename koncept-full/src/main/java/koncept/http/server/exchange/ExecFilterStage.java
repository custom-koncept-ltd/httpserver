package koncept.http.server.exchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import koncept.sp.ProcSplit;
import koncept.sp.resource.SimpleCleanableResource;
import koncept.sp.stage.SplitProcStage;
import sun.net.httpserver.AuthFilter;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Authenticator.Result;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpPrincipal;

public class ExecFilterStage implements SplitProcStage {

	public ProcSplit run(ProcSplit last) throws Exception {
		HttpContext httpContext = (HttpContext)last.get("HttpContext");
		HttpExchange exchange = (HttpExchange)last.get("HttpExchange");
		if (httpContext != null && exchange != null) {
			List<Filter> filters = httpContext.getFilters();
			
			Authenticator authenticator = httpContext.getAuthenticator();
//			HttpHandler httpHandler = authenticator == null ?
//					new AllowHandlerExecOnSuccessfulFilter(last) :
//					new RequireAuthentionBeforeContinue(last, authenticator);
			
			if (authenticator != null) {
				List<Filter> userFilters = filters;
				filters = new ArrayList<Filter>(userFilters.size() + 1);
				filters.addAll(userFilters);
				filters.add(new CustomAuthFilter(authenticator)); //not sure why, but the auth filter is run AFTER user filters in the java-provider
			}
			
			Filter.Chain filterChain = new Filter.Chain(filters, new AllowHandlerExecOnSuccessfulFilter(last));
			filterChain.doFilter(exchange); //will update the ProcSplit
		}
		return last;
	}

	private static class AllowHandlerExecOnSuccessfulFilter implements HttpHandler {
		private final ProcSplit last;
		public AllowHandlerExecOnSuccessfulFilter(ProcSplit last) {
			this.last = last;
		}
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			HttpContext httpContext = (HttpContext)last.get("HttpContext");
			HttpHandler httpHandler = httpContext.getHandler();
			last.add("HttpHandler", new SimpleCleanableResource(httpHandler, null));
		}
	}
	
	private static class CustomAuthFilter extends Filter {
		final Authenticator authenticator;
		public CustomAuthFilter(Authenticator authenticator) {
			this.authenticator = authenticator;
		}
		public void consumeInput (HttpExchange t) throws IOException {	
//			BufferedReader bIn = new BufferedReader(new InputStreamReader(t.getRequestBody()));
//			if (t.getRequestBody().available() == 0) return;
//			String line = bIn.readLine();
//			while(line != null && !line.equals("")) {
//				System.out.println("consuming " + line);
//				if (t.getRequestBody().available() == 0) return;
//				line = bIn.readLine();
//			}
	    }
		@Override
		public String description() {
			return "CustomAuthFilter";
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
					consumeInput(exchange);
					exchange.sendResponseHeaders(((Authenticator.Failure) result).getResponseCode(), -1);
					exchange.close();
				} else if (result instanceof Authenticator.Retry) {
					consumeInput(exchange);
					exchange.sendResponseHeaders(((Authenticator.Retry) result).getResponseCode(), -1);
					exchange.close();
				}
			}
		}
	}
	
	private static class CustomAuthFilter2 extends AuthFilter {
		public CustomAuthFilter2(Authenticator authenticator) {
			super(authenticator);
		}
		@Override
		public void consumeInput(HttpExchange t) throws IOException {
//			t.getRequestBody().close();
		}
		
	}
	
}