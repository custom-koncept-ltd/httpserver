package koncept.nio;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;


public class StreamedByteChannel implements Closeable, Flushable{

	private static final int capacity = 1024; //bytes
	
	private final ByteChannel chan;
	private boolean closed = false;
	private final InputStream in;
	private final OutputStream out;
	
	public StreamedByteChannel(ByteChannel chan) {
		this.chan = chan;
		in = new In();
		out = new Out();
	}
	
	@Override
	public void close() throws IOException {
		chan.close();
		closed = true;
	}
	
	@Override
	public void flush() throws IOException {
		out.flush();
	}
	
	public ByteChannel getChan() {
		return chan;
	}
	
	public InputStream getIn() {
		return in;
	}
	
	public OutputStream getOut() {
		return out;
	}
	
	class Out extends OutputStream {
		private final ByteBuffer buff = ByteBuffer.allocate(capacity);
		@Override
		public void write(int b) throws IOException {
			buff.put((byte)b);
			if(buff.limit() == 0)
				flush();
		}
		
		@Override
		public void flush() throws IOException {
			buff.flip();
			while(buff.hasRemaining()) {
			    int written = chan.write(buff);
			    if (written == 0) break;
			    if (buff.hasRemaining()) try {
			    	Thread.sleep(1);
			    } catch (InterruptedException e) {
			    	e.printStackTrace();
			    }
			}
			buff.compact();
		}
		
		@Override
		public void close() throws IOException {
			//nop
		}
	}
	
	class In extends InputStream {
		private final ByteBuffer buff = ByteBuffer.allocate(capacity);
		
		public In() {
			buff.flip();
		}
		
		@Override
		public int read() throws IOException {
			if(closed) return -1;
			if (!buff.hasRemaining()) {
				poll();
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
				poll();
	        }
			len = Math.min(len, available());
			for(int i = 0; i < len; i++) {
				b[i + off] = (byte)(buff.get() & 0xFF);
			}
			return len;
		}
		
		private void poll() throws IOException {
			buff.compact();
			chan.read(buff);
			buff.flip();
		}
		
		@Override
		public int available() throws IOException {
			return buff.remaining();
		}
		
		@Override
		public void close() throws IOException {
			//nop
		}
	}
	
}