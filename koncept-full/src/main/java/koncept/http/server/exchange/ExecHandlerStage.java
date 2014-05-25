package koncept.http.server.exchange;

import koncept.sp.ProcSplit;
import koncept.sp.stage.SplitProcStage;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

public class ExecHandlerStage implements SplitProcStage {

	public ProcSplit run(ProcSplit last) throws Exception {
		HttpExchange exchange = (HttpExchange)last.get("HttpExchange");
		Filter.Chain filterChain = (Filter.Chain)last.get("Filter.Chain");
		if (exchange != null && filterChain != null) {
			filterChain.doFilter(exchange);
		}
		return last;
	}

}
