package koncept.io;

import java.io.IOException;
import java.io.InputStream;
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
	
	public LineStreamer(InputStream wrapped) {
		this(wrapped, 1024, crlf);
	}
	
	public LineStreamer(InputStream wrapped, int size, byte[]... tokens) {
		this.wrapped = wrapped;
		this.size = size;
		this.tokens = tokens;
	}
	
	public LineStreamer(InputStream wrapped, int size, String... tokens) {
		this.wrapped = wrapped;
		this.size = size;
		this.tokens = new byte[tokens.length][];
		for(int i = 0; i < tokens.length; i++) {
			this.tokens[i] = tokens[i].getBytes(); //platform default encoding... err... not really what you want
		}
	}
	
/**
 * Strips off bytes one at a time and checks for the token as it goes along
 * @return
 * @throws IOException
 */
	public byte[] bytesToToken() throws IOException {
		List<byte[]> buffHist = new LinkedList<>();
		byte[] buff = new byte[size];
		byte[] recent = new byte[maxTokenLength()];
		int offsetRecent = 0;
		int index = 0;
		int stripLength = 0;
		try {
			int read = wrapped.read();
			while (read != -1) {
				buff[index] = (byte)read;
				recent[offsetRecent] = (byte)read;
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
				read = wrapped.read();
			} //end of stream or token reached
		} catch (SocketTimeoutException e) {
			//use as a 'break' - socket timed out, just take whats left
		}
		
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
		
		return result;
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
	
	protected String toString(byte[] b) {
		if (b == null) return null;
		if (b.length == 0) return "";
		return new String(b);
	}
	
	public String readLine() throws IOException {
		return toString(bytesToToken());
	}

}
