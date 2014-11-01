package koncept.io;

import java.io.Closeable;
import java.io.IOException;


public interface StreamingSocketAcceptor<T,U> extends Closeable {

	@Deprecated
	public T underlying();
	
	public StreamingSocketConnection<U> accept() throws SocketClosedException, IOException;

	
	public static class SocketClosedException extends IOException {
		public SocketClosedException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
