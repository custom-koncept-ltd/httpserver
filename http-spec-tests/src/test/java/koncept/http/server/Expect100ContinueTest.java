package koncept.http.server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import koncept.io.LineStreamer;

import org.junit.Test;

import com.sun.net.httpserver.spi.HttpServerProvider;

public class Expect100ContinueTest extends ProviderSpecHttpServerTestParameteriser {

	public Expect100ContinueTest(HttpServerProvider provider, boolean https) {
		super(provider, https);
	}
	
	/**
	 * I don't like the attributes spec - there is no 'per http exchange' scope (!!)
	 * @throws IOException
	 */
	@Test
	public void expectContinue() throws IOException {
		RecordingHandler handler = new RecordingHandler();
		server.createContext("/", handler);
		assertThat(getExceptContinueUrl("/"), is(Code.HTTP_CONTINUE));
		assertThat(handler.uris.size(), is(1)); //request still passed through
	}
	
	/**
	 * returns the *temporary* status line.
	 * N.B. the ultime return code should be set at a later moment in time
	 * (excepting interrupted connections, like this one)
	 * @param absolutePath
	 * @return the first status line response code found
	 */
	public Integer getExceptContinueUrl(String absolutePath) {
		Socket s = null;
		try {
			s = openDirectSocket();
			PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
			String newLine = "\r\n";
			out.write("PUT / HTTP/1.1" + newLine);
			out.write("Expect: 100-continue" + newLine);
			out.write(newLine);
			out.flush();
			
			LineStreamer lines = new LineStreamer(s.getInputStream());
			String line = lines.readLine();
			String statusLine[] = line.split(" "); //HTTP/1.1 100 Continue
			while (line != null && !line.equals("")) { //need to read off the entire response
				line = lines.readLine(100);
			}
			return new Integer(statusLine[1]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (s != null) try {s.close();}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
