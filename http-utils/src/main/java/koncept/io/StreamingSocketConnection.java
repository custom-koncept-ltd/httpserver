package koncept.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface StreamingSocketConnection<T> extends Closeable{

	public T underlying();
	
	public InputStream in() throws IOException;
	public OutputStream out() throws IOException;
	
	//Inetlocaladdress
	
}
