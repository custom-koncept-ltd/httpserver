package koncept.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class ChunkedOutputStream extends OutputStream {
	public static final String crlf = "\r\n".intern();
	private final OutputStream wrapped;
	private final PrintStream p;
	
	public ChunkedOutputStream(OutputStream wrapped) {
		this.wrapped = wrapped;
		p = new PrintStream(wrapped);
	}
	
	public OutputStream getWrapped() {
		return wrapped;
	}
	
	@Override
	public void write(int b) throws IOException {
		p.print(1);
		p.print(crlf);
		p.flush();
		wrapped.write(b);
		p.print(crlf);
		p.flush();
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (len == 0) return;
		//possible - if would write more than 4096b, split it up ??
		p.print(Integer.toHexString(len));
		p.print(crlf);
		p.flush();
		wrapped.write(b, off, len);
		p.print(crlf);
		p.flush();
	}
	
	@Override
	public void close() throws IOException {
		p.print(0);
		p.print(crlf); //send final zero chunk
		p.print(crlf); //'trailer' is just done
		p.flush();
		flush();
		wrapped.close();
	}
	
	@Override
	public void flush() throws IOException {
		wrapped.flush();
	}
}
