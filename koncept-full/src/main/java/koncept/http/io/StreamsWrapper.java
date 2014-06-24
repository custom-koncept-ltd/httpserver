package koncept.http.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public interface StreamsWrapper extends Closeable {

	public InputStream getIn() throws IOException;
	public OutputStream getOut() throws IOException;
	
	
	public class SimpleWrapper implements StreamsWrapper {
	
	private final InputStream in;
	private final OutputStream out;
	
	public SimpleWrapper(InputStream in, OutputStream out) {
		this.in = in;
		this.out = out;
	}
	
	public InputStream getIn() {
		return in;
	}
	
	public OutputStream getOut() {
		return out;
	}
	
	public void close() throws IOException {
		in.close();
		out.close();
	}
	}
	
	public interface SocketWrapper {
		public StreamsWrapper wrap(Socket s) throws IOException;
	}
}
