package koncept.http.server.parse;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.util.Map;

import koncept.http.server.ConfigurationOption;
import koncept.http.server.exchange.HttpExchangeImpl;
import koncept.io.LineStreamer;
import koncept.sp.ProcSplit;
import koncept.sp.resource.SimpleCleanableResource;
import koncept.sp.stage.SplitProcStage;

import com.sun.net.httpserver.HttpContext;

public class ParseHeadersStage implements SplitProcStage {
	private final Map<ConfigurationOption, String> options;
	
	public ParseHeadersStage(Map<ConfigurationOption, String> options) {
		this.options = options;
	}
	
	public ProcSplit run(ProcSplit last) throws Exception {
		Socket socket = (Socket)last.get("Socket");
		String requestLine = (String)last.get(ReadRequestLineStage.RequestLine);
		InputStream in = (InputStream)last.get("in");
		OutputStream out = (OutputStream)last.get("out");
		HttpContext httpContext = (HttpContext)last.get("HttpContext");

		String operation[] = requestLine.split(" ");
		String httpType = operation.length == 3 ? operation[2] : "??"; //TODO
		
		HttpExchangeImpl exchange = new HttpExchangeImpl(socket, in, out, httpType, operation[0], new URI(operation[1]), httpContext, options);
		
		//handle headers
		LineStreamer lines = new LineStreamer(in);
		String line = lines.readLine();
//		while (line.equals("")) line = lines.readLine(); //skip blank lines - though there shouldn't be any
		while(line != null && !line.equals("")) {
//			System.out.println("server read:: " + line);
			int index = line.indexOf(":");
			if (index == -1) {
				exchange.getRequestHeaders().add(line, "");
			} else {
				String headerName = line.substring(0, index);
				if (index == line.length())
					exchange.getRequestHeaders().add(headerName, "");
				else
					exchange.getRequestHeaders().add(headerName, line.substring(index + 1).trim());
			}

			line = lines.readLine();
		}
		
		return last.add("HttpExchange", new SimpleCleanableResource(exchange, null));
	}
	
	
	
	
	
}
