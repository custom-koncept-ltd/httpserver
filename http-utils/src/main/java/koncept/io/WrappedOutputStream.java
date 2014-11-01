package koncept.io;

import java.io.IOException;
import java.io.OutputStream;

public class WrappedOutputStream extends OutputStream {
	private OutputStream wrapped;
	
	boolean closed = false;
	
	public OutputStream getWrapped() {
		return wrapped;
	}
	
	public void setWrapped(OutputStream wrapped) {
		this.wrapped = wrapped;
	}
	
	public WrappedOutputStream(OutputStream wrapped) {
		this.wrapped = wrapped;
	}
	
	@Override
	public void write(int b) throws IOException {
		if (closed)
			throw new StreamClosedIOException();
		wrapped.write(b);
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		if (closed)
			throw new StreamClosedIOException();
		wrapped.write(b);
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (closed)
			throw new StreamClosedIOException();
		wrapped.write(b, off, len);
	}
	
	@Override
	public void flush() throws IOException {
		if (closed)
			throw new StreamClosedIOException();
		wrapped.flush();
	}
	
	@Override
	public void close() throws IOException {
		if (!closed) {
			wrapped.close();
			closed = true;
		}
	}
}
