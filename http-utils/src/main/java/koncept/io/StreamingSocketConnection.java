package koncept.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public interface StreamingSocketConnection<T> extends Closeable {

	public T underlying();
	
	public InetSocketAddress localAddress();
	public InetSocketAddress remoteAddress();
	
	public InputStream in() throws IOException;
	public OutputStream out() throws IOException;
	
	//Inetlocaladdress
	
}
