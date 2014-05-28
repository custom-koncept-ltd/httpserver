package koncept.http.server.parse;

import java.io.InputStream;
import java.net.Socket;

import koncept.io.LineStreamer;
import koncept.sp.ProcSplit;
import koncept.sp.resource.SimpleCleanableResource;
import koncept.sp.stage.SplitProcStage;

public class ReadRequestLineStage implements SplitProcStage {
	public static final String RequestLine = "RequestLine";
	
	public ProcSplit run(ProcSplit last) throws Exception {
		Socket socket = (Socket)last.get(ProcSplit.DEFAULT_VALUE_KEY);
		InputStream in = socket.getInputStream();
		last.add("in", new SimpleCleanableResource(in, null));
		last.add("out", new SimpleCleanableResource(socket.getOutputStream(), null));
			
		LineStreamer lines = new LineStreamer(in);
		String line = lines.readLine();
		while (line.equals("")) line = lines.readLine(); //skip blank lines
		
		return last.add(RequestLine, new SimpleCleanableResource(line, null));
	}
	
	
	
	
	
}
