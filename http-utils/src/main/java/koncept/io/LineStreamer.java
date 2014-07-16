package koncept.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.List;

/**
 * The resource should read ONLY EXACTLY as much as is required.<br/>
 * <br/>
 * N.B. This class will NOT close any underlying resources.
 * @author koncept
 *
 */
public class LineStreamer {
	private final InputStream wrapped;
	public static final byte[] crlf = "\r\n".getBytes();
	
	private final byte[][] tokens;
	private final int size;
	
	//these are now stateful
	List<byte[]> buffHist = new LinkedList<>();
	byte[] buff; // = new byte[size];
	byte[] recent; // = new byte[maxTokenLength()];
	int offsetRecent = 0;
	int index = 0;
	int stripLength = 0;
	
	public LineStreamer(InputStream wrapped) {
		this(wrapped, 1024, crlf);
	}
	
	public LineStreamer(InputStream wrapped, int size, byte[]... tokens) {
		this.wrapped = wrapped;
		this.size = size;
		this.tokens = tokens;
		
		buff = new byte[size];
		recent = new byte[maxTokenLength()];
	}
	
	public LineStreamer(InputStream wrapped, int size, String... tokens) {
		this.wrapped = wrapped;
		this.size = size;
		this.tokens = new byte[tokens.length][];
		for(int i = 0; i < tokens.length; i++) {
			this.tokens[i] = tokens[i].getBytes(); //platform default encoding... err... not really what you want
		}
		
		buff = new byte[size];
		recent = new byte[maxTokenLength()];
	}
	
/**
 * Strips off bytes one at a time and checks for the token as it goes along
 * @return
 * @throws IOException
 */
	public byte[] readBytesToToken() throws IOException {
		return nonBlockingreadBytesToToken(-1);
	}
	
	public byte[] readBytesToToken(long durationMillis) throws IOException {
		if (durationMillis == -1L)
			return nonBlockingreadBytesToToken(-1);
		else
			return nonBlockingreadBytesToToken(System.currentTimeMillis() + durationMillis);
	}
	
	private byte[] nonBlockingreadBytesToToken(long endTimeMillis) throws IOException {
		Integer read = null;
		try {
			read = nonBlockingRead(endTimeMillis);
			while (read != null && read.intValue() != -1) {
				buff[index] = read.byteValue();
				recent[offsetRecent] = buff[index];
				index++;
				offsetRecent = (offsetRecent + 1) % recent.length;
				if (index == size) {
					index = 0;
					buffHist.add(buff);
					buff = new byte[size];
				}
				int tokenIndex = tokenIndex(offsetRecent, recent);
				if (tokenIndex != -1) {
					stripLength = tokens[tokenIndex].length;
					break;
				}
				 read = nonBlockingRead(endTimeMillis);
			} //end of stream or token reached
		} catch (SocketTimeoutException e) {
			//on read timeout, socket will be closed, so just force the abort
			return null; //return null for end of stream
		}
		if (read == null)
			return null; //return null for end of stream (read timed out)
		
		//didn't read any bytes, return null for end of stream
		if (buffHist.isEmpty() && index == 0) return null;
		
		index -= stripLength;
		if (index < 0) { //if a -ve index, do a pull back from the buffHist
			buff = buffHist.remove(buffHist.size() - 1);
			index += size;
		}
		
		//join the array components into one line
		int newArraySize = buffHist.size() * size + index;
		byte[] result = new byte[newArraySize];
		for(int i = 0; i < buffHist.size(); i++) {
			System.arraycopy(buffHist.get(i), 0, result, i * size, size);
		}
		System.arraycopy(buff, 0, result, buffHist.size() * size, index);
		
		//reset internal state
		//no need to clean a byte[] bufers contents - we scope read/writes correctly
		buffHist.clear();
//		clear(buff); // = new byte[size];
//		clear(recent); // = new byte[maxTokenLength()];
		offsetRecent = 0;
		index = 0;
		stripLength = 0;
		
		
		
		return result;
	}
	
	protected Integer nonBlockingRead(long endTimeMillis) throws IOException {
		try {
			byte[] b = new byte[1];
			int read = wrapped.read(b); //**IF** the underlying is non blocking on this read, we will read 0 bytes. otherwise... we are screwed (and need nio or nio2)
			if (read == -1) return -1;
			if (read == 1) return (int)b[0];
			while (endTimeMillis == -1 || System.currentTimeMillis() < endTimeMillis) {
				//do mini wait
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				read = wrapped.read(b);
				if (read == -1) return -1;
				if (read == 1) return (int)b[0];
			}
		} catch (SocketException e) {
			if (e.getMessage().startsWith("Software caused connection abort"))
				return null;
			throw e;
		}
		return null; //timeout!!
	}
	
	//TODO: need to protect against zeros matching zeroes (!!)
	protected int tokenIndex(int startOffset, byte[] scan) {
		for(int i = 0; i < tokens.length; i++) {
			if (isToken(startOffset, scan, tokens[i])) return i;
		}
		return -1;
	}
	
	protected boolean isToken(int startOffset, byte[] scan, byte[] token) {
		startOffset += (scan.length - token.length);
		for(int i = 0; i < token.length; i++) {
			if (token[i] != scan[(i + startOffset) % scan.length]) return false;
		}
		return true;
	}

	protected int maxTokenLength() {
		int length = 0;
		for(byte[] token: tokens)
			length = Math.max(length,  token.length);
		return length;
	}
	
	protected String convertToString(byte[] b) {
		if (b == null) return null;
		if (b.length == 0) return "";
		return new String(b);
	}
	
	public String readLine() throws IOException {
		return convertToString(readBytesToToken());
	}
	
	public String readLine(long durationMillis) throws IOException {
		return convertToString(readBytesToToken(durationMillis));
	}

}
