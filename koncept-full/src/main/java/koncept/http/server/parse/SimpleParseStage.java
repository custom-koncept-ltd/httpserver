package koncept.http.server.parse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

import koncept.http.server.exchange.HttpExchangeImpl;
import koncept.sp.ProcSplit;
import koncept.sp.resource.SimpleCleanableResource;
import koncept.sp.stage.SplitProcStage;

import com.sun.net.httpserver.HttpExchange;

public class SimpleParseStage implements SplitProcStage {

	public ProcSplit run(ProcSplit last) {
		Socket in = (Socket)last.get(ProcSplit.DEFAULT_VALUE_KEY);
		HttpExchange exchange = parse(in);
		return last.add("HttpExchange", new SimpleCleanableResource(exchange, null));
//		return new ProcSplit(new SimpleCleanableResource(exchange, null));
	}
	
	public HttpExchange parse(Socket socket) {
		try {
		BufferedReader bIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		String line = bIn.readLine();
		//parse GET
		while (line.equals("")) line = bIn.readLine(); //skip blank lines
		String operation[] = line.split(" ");
		String httpType = operation.length == 3 ? operation[2] : "??"; //TODO
		HttpExchangeImpl exchange = new HttpExchangeImpl(socket, httpType, operation[0], new URI(operation[1]));
		
		//handle headers
		line = bIn.readLine();
		while(line != null && !line.equals("")) {
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

			line = bIn.readLine();
		}
		
		// --> see a 'ServerImpl' for how this is implemented
//		if (exp != null && exp.equalsIgnoreCase ("100-continue")) {
//      logReply (100, requestLine, null);
//      sendReply (
//          Code.HTTP_CONTINUE, false, null
//      );
//  }
		
		return exchange;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	
	
	
}
