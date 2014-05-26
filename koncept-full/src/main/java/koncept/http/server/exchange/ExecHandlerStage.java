package koncept.http.server.exchange;

import koncept.sp.ProcSplit;
import koncept.sp.stage.SplitProcStage;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ExecHandlerStage implements SplitProcStage {

	public ProcSplit run(ProcSplit last) throws Exception {
		HttpExchange exchange = (HttpExchange)last.get("HttpExchange");
		HttpHandler handler = (HttpHandler)last.get("HttpHandler");
		if (exchange != null && handler != null) {
			handler.handle(exchange);
		}
		return last;
	}

}
