package koncept.http.server.parse;

import koncept.io.LineStreamer;
import koncept.sp.ProcSplit;
import koncept.sp.resource.NonCleanableResource;
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
		if (last.getResourceNames().contains(RequestLine))
			return last; //nothing to do
		LineStreamer lines = (LineStreamer)last.getResource("LineStreamer");
		String line = lines.readLine();
		while (line != null && line.equals("")) line = lines.readLine(); //skip blank lines
		if (line != null && !line.equals(""))
			last.add(RequestLine, new NonCleanableResource(line));
		return last;
	}
	
	
}
