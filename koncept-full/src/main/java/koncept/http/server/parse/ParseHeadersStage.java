package koncept.http.server.parse;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

import koncept.http.server.ConfigurationOption;
import koncept.http.server.exchange.HttpExchangeImpl;
import koncept.io.LineStreamer;
import koncept.io.StreamingSocketConnection;
import koncept.sp.ProcSplit;
import koncept.sp.resource.NonCleanableResource;
import koncept.sp.stage.SplitProcStage;

import com.sun.net.httpserver.HttpContext;

public class ParseHeadersStage implements SplitProcStage {
	private final Map<ConfigurationOption, String> options;
	
	public ParseHeadersStage(Map<ConfigurationOption, String> options) {
		this.options = options;
	}
	
	public ProcSplit run(ProcSplit last) throws Exception {
		StreamingSocketConnection connection = (StreamingSocketConnection)last.getResource("StreamingSocketConnection");
//		Socket socket = (Socket)last.getResource("Socket");
		String requestLine = (String)last.getResource(ReadRequestLineStage.RequestLine);
		if (requestLine == null) return last; //abort(!!)
		LineStreamer lines = (LineStreamer)last.getResource("LineStreamer");
		InputStream in = (InputStream)last.getResource("in");
		OutputStream out = (OutputStream)last.getResource("out");
		HttpContext httpContext = (HttpContext)last.getResource("HttpContext");

		String operation[] = requestLine.split(" ");
		String httpType = operation.length == 3 ? operation[2] : ""; //TODO
		
		HttpExchangeImpl exchange = new HttpExchangeImpl(connection, in, out, httpType, operation[0], new URI(operation[1]), httpContext, options);
		
		//handle headers
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
		
		return last.add("HttpExchange", new NonCleanableResource(exchange));
	}
	
	
	
	
	
}
