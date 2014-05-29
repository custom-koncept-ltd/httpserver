package koncept.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * adds a soft end-of-stream after *size* bytes
 * @author koncept
 *
 */
public class FixedSizeInputStream extends InputStream {
	
	private final InputStream wrapped;
	private final long size;
	private final boolean allowClose;
	
	private long read = 0;
	
	public FixedSizeInputStream(InputStream wrapped, long size, boolean allowClose) {
		this.wrapped = wrapped;
		this.size = size;
		this.allowClose = allowClose;
	}

	@Override
	public int read() throws IOException {
		if (read == size) return -1;
		read++;
		return wrapped.read();
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (read == size) return -1;
		long remaining = size - read;
		if (len > remaining)
			len = (int)remaining; //trim down to the remaining size
		int thisRead = wrapped.read(b, off, len);
		if (thisRead != -1)
			read += thisRead;
		return thisRead;
	}
	
	@Override
	public void close() throws IOException {
		if(allowClose) wrapped.close();
	}

}
