package koncept.http.server;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.sun.net.httpserver.spi.HttpServerProvider;

public class RequestHeadersTest extends HttpServerTestParameteriser {

	public RequestHeadersTest(HttpServerProvider provider) {
		super(provider);
	}

	@Test
	public void headerHandling() throws IOException {
		RecordingHandler handler = new RecordingHandler();
		server.createContext("/", handler);
		
//		System.out.println("\n\n" + server.getClass().getName());
		assertThat(simpleUrlWithCustomHeaders("/"), is(Code.HTTP_OK));
		Map<String, List<String>> headers = handler.lastHeaders;
		for(String headerName: headers.keySet()) {
			List<String> values = headers.get(headerName);
//			System.out.println("serverside header: \"" + headerName + "\" = \"" + values.get(0) + "\"");
		}
		assertThat(headers.get("Header1"), is(asList("\"quotes in value\"")));
		assertThat(headers.get("Header2"), is(asList("colon: test: value")));
		assertThat(headers.get("Header3"), is(asList("hardUp")));
		assertThat(headers.get("Header4"), is(asList("x     y"))); //trimmed value
		assertThat(headers.get("Header6"), is(asList("")));
		assertThat(headers.get("Header7"), is(asList("")));
		assertThat(headers.get("Header8"), is(asList("")));
		assertThat(headers.get("Header9"), is(asList("value 1", "value 2", "value 3")));
		
		assertThat(headers.get("Case"), is(asList("lower", "UPPER")));
		
		//inconsistent behaviours identified
		//note this is ALL not to spec, so the difference in behaviour isn't 
		//necessarily wrong, it just needs to be captured
		if (!headers.containsKey("Header5")) {
			List<String> nullHeaders = headers.get(null);
				assertThat(headers.get(null), is(asList("Header5", ":malformed2 value")));
		} else {
			assertThat(headers.get("Header5"), is(asList("")));
		}
		
		if (headers.containsKey(":malformed1")) {
			assertThat(headers.get(":malformed1"), is(asList("value")));
		} else {
			assertThat(headers.get(""), is(asList("malformed1: value", "malformed2 value")));
		}
	}
	
	//custom bad header testing, etc...
	private Integer simpleUrlWithCustomHeaders(String absolutePath) {
		Socket s = null;
		try {
			s = new Socket("localhost", server.getAddress().getPort());
			PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
			String newLine = "\r\n";
			out.write("GET / HTTP/1.1" + newLine);
			out.write("Header1: \"quotes in value\"" + newLine);
			out.write("Header2: colon: test: value" + newLine);
			out.write("Header3:hardUp" + newLine);
			out.write("Header4:    x     y     " + newLine);
			out.write("Header5" + newLine);
			out.write("Header6:" + newLine);
			out.write("Header7: " + newLine);
			out.write("Header8:  " + newLine);
			out.write("Header9: value 1" + newLine);
			out.write("Header9: value 2" + newLine);
			out.write("Header9: value 3" + newLine);
			
			//note that the case is re-written
			out.write("case: lower" + newLine);
			out.write("CASE: UPPER" + newLine);
			out.write(":malformed1: value" + newLine);
			out.write(":malformed2 value" + newLine);
			out.write("" + newLine);
			out.flush();
			
			BufferedReader bIn = new BufferedReader(new InputStreamReader(s.getInputStream()));
			String line = bIn.readLine();
			
//			System.out.println("client read status line : " + line);
			String statusLine[] = line.split(" "); //HTTP/1.1 200 OK
			while (line != null && !line.equals("")) {
				line = bIn.readLine();
//				System.out.println("client read: " + line);
			}
			return new Integer(statusLine[1]);
		} catch (IOException e) {
			if (e instanceof ConnectException)
				return null; //unable to connect
			throw new RuntimeException(e);
		} finally {
			if (s != null) try {s.close();}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
