package koncept.http.server.parse;

import java.io.InputStream;

import koncept.io.LineStreamer;
import koncept.sp.ProcSplit;
import koncept.sp.resource.SimpleCleanableResource;
import koncept.sp.stage.SplitProcStage;

/**
 * simply reads the initial request line and exposes it in the proc split<br/>
 * 
 * @author koncept
 *
 */
public class ReadRequestLineStage implements SplitProcStage {
	public static final String RequestLine = "RequestLine";
	
	public ProcSplit run(ProcSplit last) throws Exception {
		InputStream in = (InputStream)last.get("in");
		LineStreamer lines = new LineStreamer(in);
		String line = lines.readLine();
		while (line.equals("")) line = lines.readLine(); //skip blank lines
		return last.add(RequestLine, new SimpleCleanableResource(line, null));
	}
	
	
	
	
	
}
