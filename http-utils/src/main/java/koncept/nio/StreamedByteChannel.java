package koncept.nio;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;


public class StreamedByteChannel implements Closeable, Flushable {

	private static final int capacity = 1024; //bytes
	
	private final ByteChannel chan;
	private volatile boolean closed = false;
	private final In in;
	private final Out out;
	
	private long readTimeout = -1;  //-1 = no timeout, 0 or more = elapsed ms timeout
	private long writeTimeout = -1; //-1 = no timeout, 0 or more = elapsed ms timeout
	
	public StreamedByteChannel(ByteChannel chan) {
		this.chan = chan;
		in = new In();
		out = new Out();
	}
	
	@Override
	public void close() throws IOException {
		out.flush();
		chan.close();
		closed = true;
	}
	
	@Override
	public void flush() throws IOException {
		out.flush();
	}
	
	public InputStream getIn() {
		return in;
	}
	
	public OutputStream getOut() {
		return out;
	}
	
	public void setWriteTimeout(long writeTimeout) {
		this.writeTimeout = writeTimeout;
	}
	
	public void setReadTimeout(long readTimeout) {
		this.readTimeout = readTimeout;
	}
	
	public class Out extends OutputStream {
		private final ByteBuffer buff = ByteBuffer.allocate(capacity);
		@Override
		public void write(int b) throws IOException {
			if(!buff.hasRemaining()) {
				if (writeTimeout >= 0) {
					long timeout = System.currentTimeMillis() + writeTimeout;
					while (!buff.hasRemaining() && timeout > System.currentTimeMillis() && !closed)
						flush();
				} else { //no timeout
					while (!buff.hasRemaining() && !closed)
						flush();
				}
			}
			
			buff.put((byte)b);
			if(!buff.hasRemaining())
				flush();
		}
		
		@Override
		public void flush() throws IOException {
			if (buff.position() == 0) 
				return; //nothing to do
			buff.flip(); //pos will now be 0
			try {
				chan.write(buff);
				if (buff.position() == 0) {
					if (writeTimeout >= 0) {
						long timeout = System.currentTimeMillis() + writeTimeout;
						while (buff.position() == 0 && timeout > System.currentTimeMillis() && !closed)
							chan.write(buff);
					} else { //no timeout
						while (buff.position() == 0 && !closed)
							chan.write(buff);
					}
				}
			} catch (IOException e) {
				closed = true;
				throw e;
			} finally {
			buff.compact();
			}
		}
		
		public long writeTimeout() {
			return writeTimeout;
		}
		
		public void writeTimeout(long writeTimeout) {
			StreamedByteChannel.this.writeTimeout = writeTimeout;
		}
		
		@Override
		public void close() throws IOException {
//			StreamedByteChannel.this.close();
		}
	}
	
	public class In extends InputStream {
		private final ByteBuffer buff = ByteBuffer.allocate(capacity);
		
		public In() {
			buff.flip();
		}
		
		@Override
		public int read() throws IOException {
			if(closed) return -1;
			if (!buff.hasRemaining() ) {
				poll(true);
	        }
			if (!buff.hasRemaining()) {
				return -1;
	        }
	        return buff.get() & 0xFF;
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if(closed) return -1;
			if (!buff.hasRemaining()) {
				poll(false);
	        }
			len = Math.min(len, available());
			for(int i = 0; i < len; i++) {
				b[i + off] = (byte)(buff.get() & 0xFF);
			}
			return len;
		}
		
		private void poll(boolean eligibleForWait) throws IOException {
			buff.compact();
			try {
				int currentPos = buff.position();
				
				//initial read
				chan.read(buff);
				if (eligibleForWait && buff.position() == currentPos) {
					if (readTimeout >= 0) {
						long timeout = System.currentTimeMillis() + readTimeout;
						while (buff.position() == currentPos && timeout > System.currentTimeMillis() && !closed) 
							 chan.read(buff);
					} else { //no timeout
						while (buff.position() == currentPos && !closed) 
							 chan.read(buff);
					}
				}
			} catch (IOException e) {
				closed = true;
				throw e;
			} finally {
				buff.flip();
			}
		}
		
		public long readTimeout() {
			return readTimeout;
		}
		
		public void readTimeout(long readTimeout) {
			StreamedByteChannel.this.readTimeout = readTimeout;
		}
		
		@Override
		public int available() throws IOException {
			return buff.remaining();
		}
		
		@Override
		public void close() throws IOException {
//			StreamedByteChannel.this.close();
		}
	}
	
}
