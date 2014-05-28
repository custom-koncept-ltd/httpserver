package koncept.io;

import java.io.IOException;
import java.io.InputStream;
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
	private static final byte[] crlf = "\r\n".getBytes();
	
	private final byte[] token;
	private final int size;
	
	public LineStreamer(InputStream wrapped) {
		this(wrapped, crlf, 1024);
	}
	
	public LineStreamer(InputStream wrapped, byte[] token, int size) {
		this.wrapped = wrapped;
		this.token = token;
		this.size = size;
	}
	
/**
 * Strips off bytes one at a time and checks for the token as it goes along
 * @return
 * @throws IOException
 */
	public byte[] bytesToToken() throws IOException {
		List<byte[]> buffHist = new LinkedList<>();
		byte[] buff = new byte[size];
		byte[] recent = new byte[token.length];
		int offsetRecent = 0;
		int index = 0;
		int read = wrapped.read();
		while (read != -1) {
			buff[index] = (byte)read;
			recent[offsetRecent] = (byte)read;
			index++;
			if (index == size) {
				index = 0;
				buffHist.add(buff);
				buff = new byte[size];
			}
			offsetRecent = (offsetRecent + 1) % recent.length;
			if (isToken(offsetRecent, recent)) {
				break;
			}
			read = wrapped.read();
		}
		//end of stream or token reached
		
		//didn't read any bytes, return null for end of stream
		if (buffHist.isEmpty() && index == 0) return null;
		
		index -= token.length;
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
	
	
	protected boolean isToken(int startOffset, byte[] scan) {
		for(int i = 0; i < scan.length; i++) {
			if (token[i] != scan[(i + startOffset) % scan.length]) return false;
		}
		return true;
	}

	protected String toString(byte[] b) {
		return new String(b);
	}
	
	public String readLine() throws IOException {
		return toString(bytesToToken());
	}

}
