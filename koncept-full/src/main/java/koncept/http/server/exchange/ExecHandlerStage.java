package koncept.http.server.exchange;

import koncept.sp.ProcSplit;
import koncept.sp.stage.SplitProcStage;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ExecHandlerStage implements SplitProcStage {

	public ProcSplit run(ProcSplit last) throws Exception {
		HttpHandler handler = (HttpHandler)last.get("HttpHandler");
		if (handler != null) {
			HttpExchange exchange = (HttpExchange)last.get("HttpExchange");
			handler.handle(exchange);
		}
		return last;
	}

}
