package koncept.io;

import java.io.IOException;
import java.io.InputStream;

public class WrappedInputStream extends InputStream {
	
	private InputStream wrapped;
	
	boolean closed = false;
	
	public WrappedInputStream(InputStream wrapped) {
		this.wrapped = wrapped;
	}
	
	public InputStream getWrapped() {
		return wrapped;
	}
	
	public void setWrapped(InputStream wrapped) {
		this.wrapped = wrapped;
	}
	
	@Override
	public int read() throws IOException {
		if (closed)
			throw new StreamClosedIOException();
		return wrapped.read();
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (closed)
			throw new StreamClosedIOException();
		return wrapped.read(b, off, len);
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		if (closed)
			throw new StreamClosedIOException();
		return wrapped.read(b);
	}
	
	@Override
	public void close() throws IOException {
		if (!closed) {
			wrapped.close();
			closed = true;
		}
	}

}
